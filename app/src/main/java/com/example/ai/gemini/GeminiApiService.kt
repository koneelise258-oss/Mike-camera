package com.example.ai.gemini

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Secure Retrofit interface for Gemini API and Backend Proxy.
 *
 * Supports both direct REST calls (with API key header or query param)
 * and secure backend proxy routing to ensure production security.
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String?,
        @Header("x-goog-api-key") headerApiKey: String?,
        @Body request: GeminiGenerateContentRequest
    ): Response<GeminiGenerateContentResponse>

    @POST("api/v1/enhance")
    suspend fun proxyEnhance(
        @Header("Authorization") authHeader: String?,
        @Body request: GeminiGenerateContentRequest
    ): Response<GeminiGenerateContentResponse>
}
