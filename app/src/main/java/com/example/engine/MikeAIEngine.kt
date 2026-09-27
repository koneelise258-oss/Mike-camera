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
        maxWidth: Int = 2560,
        maxHeight: Int = 2560
    ): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)

        var inSampleSize = 1
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        if (origHeight > maxHeight || origWidth > maxWidth) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2
            while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Impossible de décoder le fichier image: ${imageFile.name}")
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
        val fullBitmap = decodeSampledBitmap(imageFile, maxWidth = 4096, maxHeight = 4096)
        val result = processImage(fullBitmap, params, onProgress)
        result.copy(isFullResolution = true)
    }
}
