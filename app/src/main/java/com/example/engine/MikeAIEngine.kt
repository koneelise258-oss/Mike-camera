package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

object MikeAIEngine {

    var activeBackend: ImageEnhancementBackend = ClassicEngineBackend()

    /**
     * Decodes an image file efficiently with downsampling and applies EXIF orientation in memory.
     * The original image file on disk is NEVER modified, re-encoded, or altered.
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

        val origWidth = options.outWidth
        val origHeight = options.outHeight

        val exifOrientation = try {
            val exif = ExifInterface(imageFile.absolutePath)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val isSwapped = exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            exifOrientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE

        val visualWidth = if (isSwapped) origHeight else origWidth
        val visualHeight = if (isSwapped) origWidth else origHeight

        var inSampleSize = 1
        while ((visualHeight / inSampleSize) > maxHeight || (visualWidth / inSampleSize) > maxWidth) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        val rawBitmap = try {
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

        if (exifOrientation == ExifInterface.ORIENTATION_NORMAL ||
            exifOrientation == ExifInterface.ORIENTATION_UNDEFINED
        ) {
            return@withContext rawBitmap
        }

        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val rotatedBitmap = try {
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            System.gc()
            rawBitmap
        }

        if (rotatedBitmap != rawBitmap) {
            rawBitmap.recycle()
        }
        rotatedBitmap
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

    /**
     * Executes ultra-fast, lightweight local enhancement for live camera frames (LIVE_PREVIEW).
     * Strictly offline, local CPU, non-blocking.
     */
    fun processLivePreview(
        sourceBitmap: Bitmap,
        params: EnhancementParams = EnhancementParams(),
        analysis: ImageAnalysis? = null
    ): Bitmap {
        val resolvedAnalysis = analysis ?: MikeImageAnalyzer.analyzeDirect(sourceBitmap)
        val adaptedParams = MikePresetProcessor.adaptParamsForScene(params, resolvedAnalysis)
        return activeBackend.processLivePreview(sourceBitmap, resolvedAnalysis, adaptedParams)
    }
}
