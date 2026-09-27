package com.example.ai.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "imageConfig") val imageConfig: GeminiImageConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String? = null,
    @Json(name = "imageSize") val imageSize: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @Json(name = "promptFeedback") val promptFeedback: GeminiPromptFeedback? = null,
    @Json(name = "error") val error: GeminiErrorDetails? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPromptFeedback(
    @Json(name = "blockReason") val blockReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDetails(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

enum class GeminiProcessingState(val label: String) {
    IDLE("En attente"),
    PREPARING("Préparation de l'image…"),
    ANALYZING("Analyse diagnostique Gemini AI…"),
    UPLOADING("Envoi de l'image au moteur Gemini…"),
    PROCESSING("Traitement photographique Gemini AI…"),
    VALIDATING("Validation & vérification de cohérence…"),
    COMPLETED("Traitement terminé"),
    FAILED("Échec du traitement"),
    OFFLINE("Mode hors-ligne")
}
