package com.example.contentswiper.api

import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Retrofit service interface for OpenAI API.
 */
interface OpenAIService {
    
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun generateContent(@Body request: ChatCompletionRequest): ChatCompletionResponse
    
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun predictUserPreference(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String = "gpt-4o",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 500,
    val top_p: Double = 1.0,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
) 