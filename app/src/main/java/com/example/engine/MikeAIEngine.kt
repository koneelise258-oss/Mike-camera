package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

object MikeAIEngine {

    var activeBackend: ImageEnhancementBackend = ClassicEngineBackend()

    /**
     * Decodes an image file efficiently with downsampling if required.
     */
    suspend fun decodeSampledBitmap(
        imageFile: File,
        maxWidth: Int = 2048,
        maxHeight: Int = 2048
    ): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)

        var inSampleSize = 1
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        while ((origHeight / inSampleSize) > maxHeight || (origWidth / inSampleSize) > maxWidth) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        try {
            BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
                ?: throw IllegalStateException("Impossible de décoder le fichier image: ${imageFile.name}")
        } catch (e: OutOfMemoryError) {
            System.gc()
            // Retry with halved resolution if memory is critical
            val fallbackOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize * 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, fallbackOptions)
                ?: throw IllegalStateException("Mémoire insuffisante pour décoder: ${imageFile.name}")
        }
    }

    /**
     * Diagnoses and extracts technical metrics from the original image without altering it.
     */
    suspend fun analyzeImage(bitmap: Bitmap): ImageAnalysis = withContext(Dispatchers.Default) {
        MikeImageAnalyzer.analyze(bitmap)
    }

    /**
     * Executes the complete MIKE AI photographic enhancement pipeline.
     */
    suspend fun processImage(
        sourceBitmap: Bitmap,
        params: EnhancementParams = EnhancementParams(),
        onProgress: ((step: String, progress: Float) -> Unit)? = null
    ): MikeProcessingResult = withContext(Dispatchers.Default) {
        onProgress?.invoke("Diagnostic de la dynamique et scène…", 0.05f)

        val analysis = analyzeImage(sourceBitmap)
        val adaptedParams = MikePresetProcessor.adaptParamsForScene(params, analysis)

        var processedBmp: Bitmap? = null
        val duration = measureTimeMillis {
            processedBmp = activeBackend.process(sourceBitmap, analysis, adaptedParams, onProgress)
        }

        MikeProcessingResult(
            processedBitmap = processedBmp!!,
            originalBitmap = sourceBitmap,
            analysis = analysis,
            appliedParams = adaptedParams,
            durationMs = duration,
            isFullResolution = false
        )
    }

    /**
     * Executes high-resolution processing for export/gallery saving without quality loss.
     */
    suspend fun processFullResolution(
        imageFile: File,
        params: EnhancementParams = EnhancementParams(),
        onProgress: ((step: String, progress: Float) -> Unit)? = null
    ): MikeProcessingResult = withContext(Dispatchers.IO) {
        val maxDim = if (com.example.util.DeviceOptimizer.isTecnoCamon15Air) 2048 else 3072
        val fullBitmap = decodeSampledBitmap(imageFile, maxWidth = maxDim, maxHeight = maxDim)
        val result = processImage(fullBitmap, params, onProgress)
        result.copy(isFullResolution = true)
    }
}
