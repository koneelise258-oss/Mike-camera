package com.example.engine

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ImageEnhancementBackend {
    suspend fun process(
        sourceBitmap: Bitmap,
        analysis: ImageAnalysis,
        params: EnhancementParams,
        onProgress: ((step: String, progress: Float) -> Unit)? = null
    ): Bitmap
}

/**
 * High-performance, memory-efficient native CPU image enhancement pipeline.
 */
class ClassicEngineBackend : ImageEnhancementBackend {

    override suspend fun process(
        sourceBitmap: Bitmap,
        analysis: ImageAnalysis,
        params: EnhancementParams,
        onProgress: ((step: String, progress: Float) -> Unit)?
    ): Bitmap = withContext(Dispatchers.Default) {
        val intensity = params.aiIntensity.coerceIn(0.0f, 1.0f)

        // 1. If intensity is 0, return clean identical copy
        if (intensity <= 0.001f) {
            onProgress?.invoke("Original préservé (Intensité 0%)", 1.0f)
            return@withContext sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }

        onProgress?.invoke("Optimisation de l'exposition & dynamique HDR…", 0.25f)

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val totalPixels = width * height

        val pixels = IntArray(totalPixels)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 2. White balance gains
        val wbGains = MikeColorProcessor.calculateWhiteBalanceGains(analysis, params.warmth, intensity)
        val scaleR = wbGains.scaleR
        val scaleG = wbGains.scaleG
        val scaleB = wbGains.scaleB

        // 3. Tone Mapping LUT
        val toneLut = MikeExposureProcessor.buildToneLut(analysis, params, intensity)

        val isMonochrome = (params.preset == EnhancementPreset.BLACK_AND_WHITE || params.vibrance <= -0.98f)
        val isPortrait = analysis.sceneType == SceneType.PORTRAIT
        val hsvBuffer = FloatArray(3)

        val processedPixels = IntArray(totalPixels)

        // 4. Main pixel conversion pass (WB + Tone + Color/Monochrome)
        for (i in 0 until totalPixels) {
            val p = pixels[i]
            val origR = (p shr 16) and 0xFF
            val origG = (p shr 8) and 0xFF
            val origB = p and 0xFF

            // Apply white balance gains
            val wbR = (origR * scaleR).toInt().coerceIn(0, 255)
            val wbG = (origG * scaleG).toInt().coerceIn(0, 255)
            val wbB = (origB * scaleB).toInt().coerceIn(0, 255)

            // Apply tone curve
            val toneR = toneLut[wbR]
            val toneG = toneLut[wbG]
            val toneB = toneLut[wbB]

            if (isMonochrome) {
                val (finalR, finalG, finalB) = MikeColorProcessor.convertToMonochrome(
                    toneR, toneG, toneB, origR, origG, origB, intensity
                )
                processedPixels[i] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                continue
            }

            // Color processing with skin tone preservation
            val colorProcessed = MikeColorProcessor.processPixelColor(
                r = toneR,
                g = toneG,
                b = toneB,
                vibrance = params.vibrance,
                intensity = intensity,
                isPortraitScene = isPortrait,
                hsvBuffer = hsvBuffer
            )

            if (intensity < 0.999f) {
                val enhR = (colorProcessed shr 16) and 0xFF
                val enhG = (colorProcessed shr 8) and 0xFF
                val enhB = colorProcessed and 0xFF

                val blendedR = (origR + (enhR - origR) * intensity).toInt().coerceIn(0, 255)
                val blendedG = (origG + (enhG - origG) * intensity).toInt().coerceIn(0, 255)
                val blendedB = (origB + (enhB - origB) * intensity).toInt().coerceIn(0, 255)

                processedPixels[i] = (0xFF shl 24) or (blendedR shl 16) or (blendedG shl 8) or blendedB
            } else {
                processedPixels[i] = (0xFF shl 24) or (colorProcessed and 0x00FFFFFF)
            }
        }

        // 5. Edge-preserving Noise Reduction
        onProgress?.invoke("Réduction adaptative du bruit…", 0.60f)
        MikeNoiseReducer.reduceNoise(processedPixels, width, height, analysis, intensity)

        // 6. Halo-free Detail & Micro-contrast Enhancement
        onProgress?.invoke("Rehaussement des détails & netteté…", 0.85f)
        MikeDetailProcessor.enhanceDetails(
            pixels = processedPixels,
            width = width,
            height = height,
            analysis = analysis,
            sharpnessParam = params.sharpness,
            intensity = intensity
        )

        onProgress?.invoke("Finalisation du rendu Liquid Glass…", 1.0f)

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(processedPixels, 0, width, 0, 0, width, height)
        outBitmap
    }
}
