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

    fun processLivePreview(
        sourceBitmap: Bitmap,
        analysis: ImageAnalysis,
        params: EnhancementParams
    ): Bitmap
}

/**
 * High-performance, memory-efficient native CPU image enhancement pipeline.
 */
class ClassicEngineBackend : ImageEnhancementBackend {

    override fun processLivePreview(
        sourceBitmap: Bitmap,
        analysis: ImageAnalysis,
        params: EnhancementParams
    ): Bitmap {
        val intensity = params.aiIntensity.coerceIn(0.0f, 1.0f)
        if (intensity <= 0.001f) {
            return sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val totalPixels = width * height

        val pixels = IntArray(totalPixels)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val wbGains = MikeColorProcessor.calculateWhiteBalanceGains(analysis, params.warmth, intensity)
        val scaleR = wbGains.scaleR
        val scaleG = wbGains.scaleG
        val scaleB = wbGains.scaleB

        val toneLut = MikeExposureProcessor.buildToneLut(analysis, params, intensity)
        val isMonochrome = (params.preset == EnhancementPreset.BLACK_AND_WHITE || params.vibrance <= -0.98f)
        val isPortrait = analysis.sceneType == SceneType.PORTRAIT
        val hsvBuffer = FloatArray(3)
        val processedPixels = IntArray(totalPixels)

        for (i in 0 until totalPixels) {
            val p = pixels[i]
            val origR = (p shr 16) and 0xFF
            val origG = (p shr 8) and 0xFF
            val origB = p and 0xFF

            val wbR = (origR * scaleR).toInt().coerceIn(0, 255)
            val wbG = (origG * scaleG).toInt().coerceIn(0, 255)
            val wbB = (origB * scaleB).toInt().coerceIn(0, 255)

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

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(processedPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

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

        // 7. Cinematic LUT or Portrait Bokeh / Skin Polish
        if (params.cinematicLut.isNotEmpty()) {
            onProgress?.invoke("Application du profil cinéma ${params.cinematicLut}…", 0.92f)
            applyCinematicLut(processedPixels, width, height, params.cinematicLut, intensity)
        }

        if (params.portraitSkinSmoothing > 5f || params.portraitAperture > 0.1f) {
            onProgress?.invoke("Rendu portrait & flou d'ouverture…", 0.95f)
            applyPortraitEffects(processedPixels, width, height, params.portraitSkinSmoothing, params.portraitAperture, params.portraitLighting)
        }

        onProgress?.invoke("Finalisation du rendu Liquid Glass…", 1.0f)

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(processedPixels, 0, width, 0, 0, width, height)
        outBitmap
    }

    private fun applyCinematicLut(pixels: IntArray, width: Int, height: Int, lut: String, intensity: Float) {
        val total = width * height
        for (i in 0 until total) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            var newR = r
            var newG = g
            var newB = b

            when {
                lut.contains("Or", ignoreCase = true) || lut.contains("Teal", ignoreCase = true) -> {
                    // Teal & Orange: warm highlights towards gold/amber, cool shadows towards cyan/teal
                    val lum = (0.299f * r + 0.587f * g + 0.114f * b)
                    if (lum > 128f) {
                        val factor = (lum - 128f) / 128f
                        newR = (r + 32f * factor).toInt().coerceIn(0, 255)
                        newG = (g + 12f * factor).toInt().coerceIn(0, 255)
                        newB = (b - 20f * factor).toInt().coerceIn(0, 255)
                    } else {
                        val factor = (128f - lum) / 128f
                        newR = (r - 20f * factor).toInt().coerceIn(0, 255)
                        newG = (g + 8f * factor).toInt().coerceIn(0, 255)
                        newB = (b + 32f * factor).toInt().coerceIn(0, 255)
                    }
                }
                lut.contains("Bleach", ignoreCase = true) -> {
                    // Bleach bypass: high contrast, desaturated
                    val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                    val contrastR = if (r > 128) 128 + ((r - 128) * 1.35f).toInt() else 128 - ((128 - r) * 1.35f).toInt()
                    val contrastG = if (g > 128) 128 + ((g - 128) * 1.35f).toInt() else 128 - ((128 - g) * 1.35f).toInt()
                    val contrastB = if (b > 128) 128 + ((b - 128) * 1.35f).toInt() else 128 - ((128 - b) * 1.35f).toInt()
                    newR = (contrastR * 0.65f + gray * 0.35f).toInt().coerceIn(0, 255)
                    newG = (contrastG * 0.65f + gray * 0.35f).toInt().coerceIn(0, 255)
                    newB = (contrastB * 0.65f + gray * 0.35f).toInt().coerceIn(0, 255)
                }
                lut.contains("Kodak", ignoreCase = true) || lut.contains("Vision", ignoreCase = true) -> {
                    // Kodak Vision3: Warm golden midtones, rich contrast
                    newR = (r * 1.08f + 8f).toInt().coerceIn(0, 255)
                    newG = (g * 1.02f + 4f).toInt().coerceIn(0, 255)
                    newB = (b * 0.94f).toInt().coerceIn(0, 255)
                }
                lut.contains("Noir", ignoreCase = true) -> {
                    // Deep Cinema B&W
                    val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                    val contrastGray = if (gray > 128) 128 + ((gray - 128) * 1.4f).toInt() else 128 - ((128 - gray) * 1.4f).toInt()
                    newR = contrastGray.coerceIn(0, 255)
                    newG = newR
                    newB = newR
                }
                lut.contains("Sci-Fi", ignoreCase = true) || lut.contains("Froid", ignoreCase = true) -> {
                    // Sci-Fi cool cyan
                    newR = (r * 0.90f).toInt().coerceIn(0, 255)
                    newG = (g * 1.05f + 6f).toInt().coerceIn(0, 255)
                    newB = (b * 1.15f + 14f).toInt().coerceIn(0, 255)
                }
                else -> {
                    // Golden Hour
                    newR = (r * 1.12f + 10f).toInt().coerceIn(0, 255)
                    newG = (g * 1.04f + 4f).toInt().coerceIn(0, 255)
                    newB = (b * 0.88f).toInt().coerceIn(0, 255)
                }
            }

            val finalR = (r + (newR - r) * intensity).toInt().coerceIn(0, 255)
            val finalG = (g + (newG - g) * intensity).toInt().coerceIn(0, 255)
            val finalB = (b + (newB - b) * intensity).toInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }
    }

    private fun applyPortraitEffects(
        pixels: IntArray,
        width: Int,
        height: Int,
        skinSmoothing: Float,
        aperture: Float,
        lighting: String
    ) {
        val centerX = width / 2f
        val centerY = height * 0.45f
        val maxRadius = kotlin.math.sqrt((width * width + height * height).toDouble()).toFloat() * 0.5f

        // 1. Subtle radial depth-of-field blur on periphery if aperture is active (f/1.4 to f/4.0)
        if (aperture in 0.1f..4.5f) {
            val blurStrength = ((4.5f - aperture) / 4.5f).coerceIn(0.2f, 1.0f)
            val focusRadius = width * 0.38f // Subject in center remains pin-sharp
            val copy = pixels.clone()

            val step = if (blurStrength > 0.6f) 3 else 2
            for (y in step until height - step step step) {
                for (x in step until width - step step step) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    if (dist > focusRadius) {
                        val blurWeight = ((dist - focusRadius) / (maxRadius - focusRadius)).coerceIn(0f, 1f) * blurStrength
                        if (blurWeight > 0.15f) {
                            var rSum = 0
                            var gSum = 0
                            var bSum = 0
                            var count = 0
                            for (ky in -step..step step step) {
                                for (kx in -step..step step step) {
                                    val idx = (y + ky) * width + (x + kx)
                                    val px = copy[idx]
                                    rSum += (px shr 16) and 0xFF
                                    gSum += (px shr 8) and 0xFF
                                    bSum += px and 0xFF
                                    count++
                                }
                            }
                            val avgR = rSum / count
                            val avgG = gSum / count
                            val avgB = bSum / count

                            val originalPx = copy[y * width + x]
                            val origR = (originalPx shr 16) and 0xFF
                            val origG = (originalPx shr 8) and 0xFF
                            val origB = originalPx and 0xFF

                            val blendR = (origR + (avgR - origR) * blurWeight).toInt().coerceIn(0, 255)
                            val blendG = (origG + (avgG - origG) * blurWeight).toInt().coerceIn(0, 255)
                            val blendB = (origB + (avgB - origB) * blurWeight).toInt().coerceIn(0, 255)

                            pixels[y * width + x] = (0xFF shl 24) or (blendR shl 16) or (blendG shl 8) or blendB
                        }
                    }
                }
            }
        }

        // 2. Studio Lighting effect (e.g. Scène darkens background, Studio adds gentle key light)
        if (lighting.contains("Scène", ignoreCase = true) || lighting.contains("Stage", ignoreCase = true)) {
            val isMono = lighting.contains("Mono", ignoreCase = true)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val falloff = (dist / (width * 0.55f)).coerceIn(0f, 1f)
                    val darkFactor = 1.0f - (falloff * falloff * 0.85f)

                    val idx = y * width + x
                    val p = pixels[idx]
                    var r = ((p shr 16) and 0xFF)
                    var g = ((p shr 8) and 0xFF)
                    var b = (p and 0xFF)

                    if (isMono) {
                        val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                        r = gray
                        g = gray
                        b = gray
                    }

                    r = (r * darkFactor).toInt().coerceIn(0, 255)
                    g = (g * darkFactor).toInt().coerceIn(0, 255)
                    b = (b * darkFactor).toInt().coerceIn(0, 255)
                    pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
    }
}
