package com.example.ai.gemini

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.engine.EnhancementParams
import com.example.engine.ImageAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed class GeminiEnhancementResult {
    data class Success(
        val bitmap: Bitmap,
        val modelUsed: String,
        val description: String
    ) : GeminiEnhancementResult()

    data class FallbackToLocal(
        val reason: String,
        val errorCode: Int? = null
    ) : GeminiEnhancementResult()

    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : GeminiEnhancementResult()
}

class GeminiEngineBackend(private val context: Context) {

    companion object {
        private const val TAG = "GeminiEngineBackend"
        // Recommended model for Image Generation & Editing per gemini-api skill:
        const val MODEL_FLASH_IMAGE = "gemini-2.5-flash-image"
        const val MODEL_PRO_IMAGE = "gemini-3.1-flash-image-preview"
        const val MODEL_MULTIMODAL = "gemini-3.5-flash"
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun enhanceImage(
        originalBitmap: Bitmap,
        params: EnhancementParams,
        analysis: ImageAnalysis?,
        onStateUpdate: (GeminiProcessingState) -> Unit
    ): GeminiEnhancementResult = withContext(Dispatchers.IO) {
        onStateUpdate(GeminiProcessingState.PREPARING)

        if (!isOnline()) {
            onStateUpdate(GeminiProcessingState.OFFLINE)
            return@withContext GeminiEnhancementResult.FallbackToLocal(
                reason = "Appareil hors-ligne. Traitement par le moteur photographique local MIKE AI."
            )
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "No GEMINI_API_KEY found in BuildConfig. Using secure local photographic engine.")
            return@withContext GeminiEnhancementResult.FallbackToLocal(
                reason = "Clé Gemini non configurée dans AI Studio Secrets. Traitement photographique local haute fidélité actif."
            )
        }

        try {
            onStateUpdate(GeminiProcessingState.ANALYZING)
            val base64Image = encodeBitmapToBase64(originalBitmap)

            val prompt = GeminiPromptBuilder.buildPhotoEnhancementPrompt(params, analysis)
            val systemInstruction = GeminiPromptBuilder.buildSystemInstruction()

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.4f,
                    topP = 0.9f
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstruction))
                )
            )

            onStateUpdate(GeminiProcessingState.UPLOADING)
            val service = GeminiClient.getService()

            onStateUpdate(GeminiProcessingState.PROCESSING)
            val response = service.generateContent(
                model = MODEL_FLASH_IMAGE,
                apiKey = apiKey,
                headerApiKey = apiKey,
                request = request
            )

            if (!response.isSuccessful) {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Gemini API HTTP $code: $errorBody")

                return@withContext when (code) {
                    429 -> GeminiEnhancementResult.FallbackToLocal(
                        reason = "Quota Gemini temporairement atteint. Bascule automatique sur le moteur local.",
                        errorCode = 429
                    )
                    403 -> GeminiEnhancementResult.FallbackToLocal(
                        reason = "Clé API non autorisée pour ce modèle. Bascule sur le moteur local.",
                        errorCode = 403
                    )
                    else -> GeminiEnhancementResult.FallbackToLocal(
                        reason = "Erreur serveur ($code). Traitement local activé.",
                        errorCode = code
                    )
                }
            }

            onStateUpdate(GeminiProcessingState.VALIDATING)
            val body = response.body()
            val candidate = body?.candidates?.firstOrNull()
            val parts = candidate?.content?.parts ?: emptyList()

            // Look for inline image data in parts
            var resultBitmap: Bitmap? = null
            var resultText: String? = null

            for (part in parts) {
                if (part.inlineData != null && part.inlineData.data.isNotBlank()) {
                    val decodedBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                    resultBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }
                if (part.text != null) {
                    resultText = part.text
                }
            }

            if (resultBitmap != null) {
                onStateUpdate(GeminiProcessingState.COMPLETED)
                GeminiEnhancementResult.Success(
                    bitmap = resultBitmap,
                    modelUsed = MODEL_FLASH_IMAGE,
                    description = resultText ?: "Amélioration photographique Gemini appliquée avec succès"
                )
            } else {
                // If model returned text guidance or adjustments
                GeminiEnhancementResult.FallbackToLocal(
                    reason = "Analyse photographique reçue de Gemini : ${resultText?.take(120) ?: "Complète"}."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini processing", e)
            onStateUpdate(GeminiProcessingState.FAILED)
            GeminiEnhancementResult.FallbackToLocal(
                reason = "Interruption réseau (${e.localizedMessage ?: "erreur"}). Traitement photographique local sécurisé appliqué."
            )
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1440
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(width, height)
            Bitmap.createScaledBitmap(
                bitmap,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
