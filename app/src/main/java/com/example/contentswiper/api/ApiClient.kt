package com.example.contentswiper.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API client for OpenAI.
 */
object ApiClient {
    private const val BASE_URL = "https://api.openai.com/"
    private const val OPENAI_API_KEY = "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"
    
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .build()
        chain.proceed(request)
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val openAIService: OpenAIService = retrofit.create(OpenAIService::class.java)
} 