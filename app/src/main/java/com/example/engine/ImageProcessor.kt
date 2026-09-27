package com.example.engine

import android.graphics.Bitmap
import java.io.File

/**
 * ImageProcessor maintains API compatibility while routing all processing through MikeAIEngine.
 */
object ImageProcessor {

    suspend fun decodeSampledBitmap(
        imageFile: File,
        maxWidth: Int = 2560,
        maxHeight: Int = 2560
    ): Bitmap {
        return MikeAIEngine.decodeSampledBitmap(imageFile, maxWidth, maxHeight)
    }

    suspend fun detectSceneType(bitmap: Bitmap): SceneType {
        val analysis = MikeAIEngine.analyzeImage(bitmap)
        return analysis.sceneType
    }

    suspend fun processImage(
        sourceBitmap: Bitmap,
        params: EnhancementParams
    ): Bitmap {
        val result = MikeAIEngine.processImage(sourceBitmap, params)
        return result.processedBitmap
    }
}
