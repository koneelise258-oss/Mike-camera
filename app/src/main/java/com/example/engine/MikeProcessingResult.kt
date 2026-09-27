package com.example.engine

import android.graphics.Bitmap

data class MikeProcessingResult(
    val processedBitmap: Bitmap,
    val originalBitmap: Bitmap,
    val analysis: ImageAnalysis,
    val appliedParams: EnhancementParams,
    val durationMs: Long,
    val isFullResolution: Boolean
) {
    val diagnosticSummary: String
        get() = buildString {
            append("Scène: ${analysis.sceneType.displayName} (${(analysis.sceneConfidence * 100).toInt()}%)")
            append(" • Lum: ${analysis.avgLuminance.toInt()}/255")
            append(" • Dynamique: ${analysis.dynamicRange.toInt()} pts")
            if (analysis.estimatedNoiseLevel > 0.25f) {
                append(" • Débruitage actif")
            }
            if (appliedParams.aiIntensity < 1.0f) {
                append(" • Intensité: ${(appliedParams.aiIntensity * 100).toInt()}%")
            }
        }
}
