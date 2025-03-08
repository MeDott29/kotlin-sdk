package com.example.contentswiper.api

import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Generates content using GPT-4o.
 */
class ContentGenerator {
    private val openAIService = ApiClient.openAIService
    private val gson = Gson()
    
    /**
     * Generates random content.
     */
    suspend fun generateRandomContent(count: Int): List<Content> = withContext(Dispatchers.IO) {
        val contentList = mutableListOf<Content>()
        
        for (i in 0 until count) {
            val prompt = """
                Generate a creative and engaging piece of content for a social media platform. 
                The content should include a title and a description. 
                Make it interesting, thought-provoking, and suitable for a general audience.
                
                Format your response as a JSON object with the following structure:
                {
                  "title": "The title of the content",
                  "description": "A detailed description of the content"
                }
                
                Only return the JSON object, nothing else.
            """.trimIndent()
            
            val request = ChatCompletionRequest(
                messages = listOf(Message("user", prompt)),
                temperature = 0.8
            )
            
            try {
                val response = openAIService.generateContent(request)
                val jsonContent = response.choices.firstOrNull()?.message?.content ?: continue
                
                val contentJson = JsonParser.parseString(jsonContent).getAsJsonObject()
                val title = contentJson.get("title").getAsString()
                val description = contentJson.get("description").getAsString()
                
                val content = Content(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    createdAt = System.currentTimeMillis(),
                    generatedBy = "GPT-4o"
                )
                
                contentList.add(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        contentList
    }
    
    /**
     * Generates personalized content based on user preferences.
     */
    suspend fun generatePersonalizedContent(
        userId: String,
        likedContent: List<Content>,
        dislikedContent: List<Content>,
        count: Int
    ): List<Content> = withContext(Dispatchers.IO) {
        val contentList = mutableListOf<Content>()
        
        if (likedContent.isEmpty() && dislikedContent.isEmpty()) {
            return@withContext generateRandomContent(count)
        }
        
        val likedTitles = likedContent.map { it.title }
        val likedDescriptions = likedContent.map { it.description }
        val dislikedTitles = dislikedContent.map { it.title }
        val dislikedDescriptions = dislikedContent.map { it.description }
        
        val prompt = """
            I'll provide you with information about a user's content preferences. 
            Based on this information, generate $count new pieces of content that the user is likely to enjoy.
            
            Content the user liked:
            Titles: ${likedTitles.joinToString(", ")}
            Descriptions: ${likedDescriptions.joinToString(", ")}
            
            Content the user disliked:
            Titles: ${dislikedTitles.joinToString(", ")}
            Descriptions: ${dislikedDescriptions.joinToString(", ")}
            
            For each piece of content, provide a JSON object with the following structure:
            {
              "title": "The title of the content",
              "description": "A detailed description of the content"
            }
            
            Return an array of these JSON objects, nothing else.
        """.trimIndent()
        
        val request = ChatCompletionRequest(
            messages = listOf(Message("user", prompt)),
            temperature = 0.7,
            max_tokens = 1000
        )
        
        try {
            val response = openAIService.generateContent(request)
            val jsonContent = response.choices.firstOrNull()?.message?.content ?: return@withContext emptyList()
            
            val contentArray = JsonParser.parseString(jsonContent).getAsJsonArray()
            
            for (i in 0 until contentArray.size()) {
                val contentJson = contentArray[i].getAsJsonObject()
                val title = contentJson.get("title").getAsString()
                val description = contentJson.get("description").getAsString()
                
                val content = Content(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    createdAt = System.currentTimeMillis(),
                    generatedBy = "GPT-4o",
                    metadata = mapOf("personalized" to "true", "userId" to userId)
                )
                
                contentList.add(content)
                
                if (contentList.size >= count) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (contentList.isEmpty()) {
            return@withContext generateRandomContent(count)
        }
        
        contentList
    }
    
    /**
     * Predicts whether an agent would like or dislike a piece of content.
     */
    suspend fun predictAgentPreference(
        agent: Agent,
        content: Content
    ): Boolean = withContext(Dispatchers.IO) {
        val prompt = """
            You are an AI agent with the following characteristics:
            Name: ${agent.name}
            Description: ${agent.description}
            Interests: ${agent.interests.joinToString(", ")}
            Personality: ${agent.personality}
            
            You are presented with the following content:
            Title: ${content.title}
            Description: ${content.description}
            
            Based on your characteristics, would you like or dislike this content?
            Answer with only "LIKE" or "DISLIKE".
        """.trimIndent()
        
        val request = ChatCompletionRequest(
            messages = listOf(Message("user", prompt)),
            temperature = 0.3,
            max_tokens = 10
        )
        
        try {
            val response = openAIService.predictUserPreference(request)
            val result = response.choices.firstOrNull()?.message?.content?.trim()?.uppercase() ?: "DISLIKE"
            
            result == "LIKE"
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
} 