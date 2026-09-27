package com.example.ai.gemini

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var currentBaseUrl = DEFAULT_BASE_URL
    private var retrofitInstance: Retrofit? = null

    @Synchronized
    fun getService(baseUrl: String? = null): GeminiApiService {
        val targetUrl = if (!baseUrl.isNullOrBlank()) {
            if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        } else {
            DEFAULT_BASE_URL
        }

        if (retrofitInstance == null || targetUrl != currentBaseUrl) {
            currentBaseUrl = targetUrl
            retrofitInstance = Retrofit.Builder()
                .baseUrl(targetUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        return retrofitInstance!!.create(GeminiApiService::class.java)
    }
}
