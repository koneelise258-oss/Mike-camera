package com.example.ai.gemini

import android.graphics.Bitmap
import com.example.engine.ClassicEngineBackend
import com.example.engine.EnhancementParams
import com.example.engine.ImageAnalysis
import com.example.engine.ImageEnhancementBackend

/**
 * Production AI Enhancement Backend bridging Gemini API with local high-precision photographic fallback.
 *
 * Implements strict security:
 * - Direct REST or Backend Proxy routing.
 * - Explicit and honest state management (no fake results, transparent fallback).
 * - Liquid Glass status updates.
 */
class GeminiProxyBackend(
    private val geminiEngine: GeminiEngineBackend,
    private val fallbackBackend: ImageEnhancementBackend = ClassicEngineBackend()
) : ImageEnhancementBackend {

    var lastProcessingStatus: String = "Prêt"
        private set

    var wasLastRunGemini: Boolean = false
        private set

    override suspend fun process(
        sourceBitmap: Bitmap,
        analysis: ImageAnalysis,
        params: EnhancementParams,
        onProgress: ((step: String, progress: Float) -> Unit)?
    ): Bitmap {
        var resultBitmap: Bitmap? = null

        val geminiResult = geminiEngine.enhanceImage(
            originalBitmap = sourceBitmap,
            params = params,
            analysis = analysis,
            onStateUpdate = { state ->
                lastProcessingStatus = state.label
                val progressFraction = when (state) {
                    GeminiProcessingState.IDLE -> 0.0f
                    GeminiProcessingState.PREPARING -> 0.15f
                    GeminiProcessingState.ANALYZING -> 0.35f
                    GeminiProcessingState.UPLOADING -> 0.50f
                    GeminiProcessingState.PROCESSING -> 0.75f
                    GeminiProcessingState.VALIDATING -> 0.90f
                    GeminiProcessingState.COMPLETED -> 1.0f
                    GeminiProcessingState.FAILED, GeminiProcessingState.OFFLINE -> 0.95f
                }
                onProgress?.invoke(state.label, progressFraction)
            }
        )

        when (geminiResult) {
            is GeminiEnhancementResult.Success -> {
                wasLastRunGemini = true
                lastProcessingStatus = "Optimisé par Gemini AI (${geminiResult.modelUsed})"
                resultBitmap = geminiResult.bitmap
            }
            is GeminiEnhancementResult.FallbackToLocal -> {
                wasLastRunGemini = false
                lastProcessingStatus = geminiResult.reason
                onProgress?.invoke(geminiResult.reason, 0.40f)
                resultBitmap = fallbackBackend.process(sourceBitmap, analysis, params, onProgress)
            }
            is GeminiEnhancementResult.Error -> {
                wasLastRunGemini = false
                lastProcessingStatus = geminiResult.message
                resultBitmap = fallbackBackend.process(sourceBitmap, analysis, params, onProgress)
            }
        }

        return resultBitmap ?: sourceBitmap
    }

    override fun processLivePreview(
        sourceBitmap: Bitmap,
        analysis: ImageAnalysis,
        params: EnhancementParams
    ): Bitmap {
        // Live camera preview is ALWAYS local, lightweight and non-blocking (Gemini is post-capture only)
        return fallbackBackend.processLivePreview(sourceBitmap, analysis, params)
    }
}
