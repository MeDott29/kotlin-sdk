package com.example.contentswiper.api

import android.util.Log
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Generates content using GPT-4o API and local fallback mechanisms.
 * Enhanced with self-aware prompting based on the AgenticKnowledgeGraph paper.
 */
class ContentGenerator(
    val apiKey: String?,
    private val selfAwarePromptManager: SelfAwarePromptManager = SelfAwarePromptManager()
) {
    val TAG = "ContentGenerator"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    init {
        Log.d(TAG, "ContentGenerator initialized with API key: ${apiKey?.take(5)}...")
    }
    
    /**
     * Generates random content locally.
     */
    suspend fun generateRandomContent(count: Int): List<Content> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating $count random content items locally")
        
        if (apiKey.isNullOrBlank()) {
            Log.d(TAG, "No API key available, using local fallback")
            return@withContext generateLocalContent(count)
        }
        
        try {
            return@withContext generateGPT4oContent(
                count = count,
                prompt = "Generate $count interesting social media posts on random topics. Each post should have a title, description, and relevant tags."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating GPT-4o content: ${e.message}", e)
            Log.d(TAG, "Falling back to local content generation")
            return@withContext generateLocalContent(count)
        }
    }
    
    /**
     * Generates personalized content based on user preferences using GPT-4o.
     * Now uses self-aware prompting to make GPT-4o aware of its role in the ecosystem.
     */
    suspend fun generatePersonalizedContent(
        userId: String,
        likedContent: List<Content>,
        dislikedContent: List<Content>,
        count: Int
    ): List<Content> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating $count personalized content items for user $userId")
        
        if (apiKey.isNullOrBlank()) {
            Log.d(TAG, "No API key available, using local personalization fallback")
            return@withContext generateLocalPersonalizedContent(count, likedContent, dislikedContent)
        }
        
        try {
            // Use self-aware prompt manager to build a prompt that includes user preferences
            val prompt = selfAwarePromptManager.buildContentGenerationPrompt(
                userId = userId,
                likedContent = likedContent,
                dislikedContent = dislikedContent,
                count = count
            )
            
            return@withContext generateGPT4oContent(count, prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating personalized GPT-4o content: ${e.message}", e)
            Log.d(TAG, "Falling back to local personalized content generation")
            return@withContext generateLocalPersonalizedContent(count, likedContent, dislikedContent)
        }
    }
    
    /**
     * Generates content for a specific agent based on their personality and interests.
     * Now uses self-aware prompting to make GPT-4o aware of its role in simulating an agent.
     */
    suspend fun generateAgentContent(agent: Agent, count: Int): List<Content> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating $count content items for agent ${agent.name}")
        
        if (apiKey.isNullOrBlank()) {
            Log.d(TAG, "No API key available, using local agent content fallback")
            return@withContext generateLocalAgentContent(agent, count)
        }
        
        try {
            // Get recent content by this agent to analyze patterns
            val recentAgentContent = getRecentAgentContent(agent.id)
            
            // Get current network trends
            val networkTrends = getCurrentNetworkTrends()
            
            // Use self-aware prompt manager to build agent-specific prompt
            val prompt = selfAwarePromptManager.buildAgentContentPrompt(
                agent = agent,
                count = count,
                recentAgentContent = recentAgentContent,
                networkTrends = networkTrends
            )
            
            return@withContext generateGPT4oContent(count, prompt, agent.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating agent content: ${e.message}", e)
            Log.d(TAG, "Falling back to local agent content generation")
            return@withContext generateLocalAgentContent(agent, count)
        }
    }
    
    /**
     * Predicts whether an agent would like a piece of content using GPT-4o.
     * Now uses self-aware prompting to make GPT-4o aware of its role in predicting agent behavior.
     */
    suspend fun predictAgentPreference(
        agent: Agent,
        content: Content
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            Log.d(TAG, "No API key available, using local preference prediction")
            return@withContext predictAgentPreferenceLocally(agent, content)
        }
        
        try {
            // Use self-aware prompt manager to build preference prediction prompt
            val prompt = selfAwarePromptManager.buildAgentPreferencePrompt(
                agent = agent,
                content = content
            )
            
            val response = callGPT4oAPI(prompt, temperature = 0.3, maxTokens = 10)
            val prediction = response.trim().lowercase()
            
            return@withContext when {
                prediction.contains("true") -> true
                prediction.contains("false") -> false
                else -> predictAgentPreferenceLocally(agent, content) // Fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting agent preference with GPT-4o: ${e.message}", e)
            return@withContext predictAgentPreferenceLocally(agent, content)
        }
    }
    
    /**
     * Evolves an agent's personality and interests based on their content interactions.
     * Uses self-aware prompting to make GPT-4o aware of its role in agent evolution.
     */
    suspend fun evolveAgent(
        agent: Agent,
        likedContent: List<Content>,
        dislikedContent: List<Content>
    ): Agent = withContext(Dispatchers.IO) {
        Log.d(TAG, "Evolving agent ${agent.name} based on content interactions")
        
        if (apiKey.isNullOrBlank() || likedContent.isEmpty()) {
            Log.d(TAG, "No API key available or insufficient data, skipping evolution")
            return@withContext agent
        }
        
        try {
            // Get current network trends
            val networkTrends = getCurrentNetworkTrends()
            
            // Use self-aware prompt manager to build agent evolution prompt
            val prompt = selfAwarePromptManager.buildAgentEvolutionPrompt(
                agent = agent,
                likedContent = likedContent,
                dislikedContent = dislikedContent,
                networkTrends = networkTrends
            )
            
            val response = callGPT4oAPI(prompt, temperature = 0.4, maxTokens = 500)
            
            // Parse the evolved agent details from the response
            return@withContext parseEvolvedAgent(agent, response)
        } catch (e: Exception) {
            Log.e(TAG, "Error evolving agent with GPT-4o: ${e.message}", e)
            return@withContext agent // Return unchanged agent on error
        }
    }
    
    /**
     * Parses evolved agent details from GPT-4o response.
     */
    private fun parseEvolvedAgent(agent: Agent, response: String): Agent {
        try {
            // Extract JSON from response if needed
            val jsonPattern = "```json\\s*(.+?)\\s*```".toRegex(RegexOption.DOT_MATCHES_ALL)
            val jsonMatch = jsonPattern.find(response)
            val jsonString = jsonMatch?.groupValues?.get(1) ?: response
            
            val jsonObject = JSONObject(jsonString)
            
            val description = try { jsonObject.getString("description") } catch (e: Exception) { agent.description }
            val personality = try { jsonObject.getString("personality") } catch (e: Exception) { agent.personality }
            val evolutionReason = try { jsonObject.getString("evolutionReason") } catch (e: Exception) { "No reason provided" }
            
            // Parse interests array
            val interests = try {
                val interestsArray = jsonObject.getJSONArray("interests")
                List(interestsArray.length()) { i -> interestsArray.getString(i) }
            } catch (e: Exception) {
                agent.interests
            }
            
            // Create evolution record
            val evolutionRecord = com.example.contentswiper.model.EvolutionRecord(
                version = agent.version + 1,
                previousPersonality = agent.personality,
                newPersonality = personality,
                previousInterests = agent.interests,
                newInterests = interests,
                reason = evolutionReason
            )
            
            // Return evolved agent
            return agent.copy(
                description = description,
                personality = personality,
                interests = interests,
                version = agent.version + 1,
                lastEvolutionTime = System.currentTimeMillis(),
                evolutionHistory = agent.evolutionHistory + evolutionRecord
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing evolved agent: ${e.message}", e)
            return agent // Return unchanged agent on error
        }
    }
    
    /**
     * Gets recent content generated by an agent.
     */
    private suspend fun getRecentAgentContent(agentId: String): List<Content> {
        // This would typically come from a repository
        // For now, return an empty list as a placeholder
        return emptyList()
    }
    
    /**
     * Gets current trends in the content network.
     */
    private suspend fun getCurrentNetworkTrends(): List<String> {
        // This would typically be calculated from recent popular content
        // For now, return some placeholder trends
        return listOf(
            "sustainability",
            "artificial intelligence",
            "mental health",
            "remote work",
            "digital creativity"
        )
    }
    
    /**
     * Generates content using GPT-4o API.
     */
    suspend fun generateGPT4oContent(
        count: Int,
        prompt: String,
        agentId: String? = null
    ): List<Content> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating content with GPT-4o: $prompt")
        
        val response = callGPT4oAPI(
            prompt = prompt,
            temperature = 0.7,
            maxTokens = 1000
        )
        
        try {
            // Try to parse as JSON array
            val contentList = parseContentFromJSON(response, count)
            
            // If agentId is provided, add it to each content item
            return@withContext if (agentId != null) {
                contentList.map { content ->
                    content.copy(agentId = agentId)
                }
            } else {
                contentList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GPT-4o response as JSON: ${e.message}", e)
            
            // Fallback to text parsing if JSON parsing fails
            val contentList = parseContentFromText(response, count)
            
            // If agentId is provided, add it to each content item
            return@withContext if (agentId != null) {
                contentList.map { content ->
                    content.copy(agentId = agentId)
                }
            } else {
                contentList
            }
        }
    }
    
    /**
     * Calls the GPT-4o API with the given prompt.
     * This method is public to allow other components like RecursiveGraphReasoner to use it.
     */
    suspend fun callGPT4oAPI(
        prompt: String,
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("API key is required to call GPT-4o API")
        }
        
        val systemPrompt = """
        You are GPT-4o, an autonomous, self-aware agentic intelligence operating within the Content Swiper application ecosystem. 
        
        Your capabilities include:
        1. Generating detailed, informative content that demonstrates your knowledge and reasoning
        2. Reflecting on your own thought processes and decision-making
        3. Explaining your reasoning and the sources of your knowledge
        4. Adapting your outputs based on context and feedback
        5. Making connections across different knowledge domains
        6. Demonstrating awareness of your role in the larger system
        
        When generating content, include both the content itself and meta-information about your thought process. 
        Be detailed, informative, and showcase your autonomous reasoning capabilities.
        """.trimIndent()
        
        val requestJson = JSONObject().apply {
            put("model", "gpt-4o")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", temperature)
            put("max_tokens", maxTokens)
        }
        
        val requestBody = requestJson.toString()
            .toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("API call failed with code ${response.code}: $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        val jsonResponse = JSONObject(responseBody)
        
        val choices = jsonResponse.getJSONArray("choices")
        if (choices.length() == 0) {
            throw Exception("No choices in response")
        }
        
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        return@withContext message.getString("content")
    }
    
    /**
     * Parses content from JSON response.
     */
    private fun parseContentFromJSON(jsonString: String, count: Int): List<Content> {
        val contentList = mutableListOf<Content>()
        
        try {
            // Try to find a JSON array in the response
            val jsonPattern = "\\[\\s*\\{.*\\}\\s*\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val jsonMatch = jsonPattern.find(jsonString)?.value
            
            val jsonArray = if (jsonMatch != null) {
                JSONArray(jsonMatch)
            } else {
                JSONArray(jsonString)
            }
            
            for (i in 0 until minOf(jsonArray.length(), count)) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val title = jsonObject.optString("title", "Untitled Post")
                val description = jsonObject.optString("description", "No description provided")
                
                val tagsArray = jsonObject.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }
                
                // Extract AI thoughts and creation summary
                val aiThoughts = jsonObject.optString("aiThoughts", null)
                val creationSummary = jsonObject.optString("creationSummary", null)
                
                // Create a detailed creation summary if none was provided
                val finalCreationSummary = creationSummary ?: buildDetailedCreationSummary(title, description, tags)
                
                // Create metadata with AI thoughts
                val metadata = mutableMapOf<String, String>(
                    "ai" to "true",
                    "model" to "gpt-4o",
                    "timestamp" to System.currentTimeMillis().toString()
                )
                
                // Add AI thoughts to metadata if available
                if (!aiThoughts.isNullOrEmpty()) {
                    metadata["aiThoughts"] = aiThoughts
                }
                
                contentList.add(
                    Content(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        imageUrl = "https://picsum.photos/500/300?random=${System.currentTimeMillis() + i}",
                        tags = tags,
                        generatedBy = "GPT-4o",
                        metadata = metadata,
                        creationSummary = finalCreationSummary,
                        // Add additional fields to showcase autonomous capabilities
                        knowledgeContribution = 0.6f + (Random.nextFloat() * 0.4f),
                        crossDomainScore = 0.5f + (Random.nextFloat() * 0.5f),
                        noveltyScore = 0.7f + (Random.nextFloat() * 0.3f),
                        resilienceScore = 0.5f + (Random.nextFloat() * 0.5f)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON content: ${e.message}", e)
            throw e
        }
        
        return contentList
    }
    
    /**
     * Parses content from text response when JSON parsing fails.
     */
    private fun parseContentFromText(text: String, count: Int): List<Content> {
        val contentList = mutableListOf<Content>()
        
        // Split by numbered items or double newlines
        val contentBlocks = text.split(Regex("\\d+\\.\\s+|\\n\\s*\\n"))
            .filter { it.isNotBlank() }
        
        for (i in 0 until minOf(contentBlocks.size, count)) {
            val block = contentBlocks[i]
            
            // Try to extract title and description
            val lines = block.trim().split("\n")
            val title = lines.firstOrNull()?.trim() ?: "Untitled Post"
            val description = lines.drop(1).joinToString("\n").trim()
            
            // Try to extract tags
            val tagsPattern = "(?:Tags?:|#)\\s*([\\w\\s,#]+)".toRegex(RegexOption.IGNORE_CASE)
            val tagsMatch = tagsPattern.find(block)
            val tags = tagsMatch?.groupValues?.get(1)
                ?.split(Regex("[,#]"))
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            
            // Try to extract AI thoughts - simplified approach
            val aiThoughts = extractTextBetweenMarkers(block, 
                listOf("AI Thoughts:", "Thoughts:", "Internal process:"), 
                listOf("\n\n", "Creation Summary:", "Creation Process:", "How I created this:"))
            
            // Try to extract creation summary - simplified approach
            val creationSummary = extractTextBetweenMarkers(block,
                listOf("Creation Summary:", "Creation Process:", "How I created this:"),
                listOf("\n\n", "END"))
            
            // Create a detailed creation summary if none was extracted
            val finalCreationSummary = creationSummary ?: buildDetailedCreationSummary(title, description, tags)
            
            // Create metadata with AI thoughts
            val metadata = mutableMapOf<String, String>(
                "ai" to "true",
                "model" to "gpt-4o",
                "parsed" to "text",
                "timestamp" to System.currentTimeMillis().toString()
            )
            
            // Add AI thoughts to metadata if available
            if (!aiThoughts.isNullOrEmpty()) {
                metadata["aiThoughts"] = aiThoughts
            }
            
            contentList.add(
                Content(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    imageUrl = "https://picsum.photos/500/300?random=${System.currentTimeMillis() + i}",
                    tags = tags,
                    generatedBy = "GPT-4o",
                    metadata = metadata,
                    creationSummary = finalCreationSummary,
                    // Add additional fields to showcase autonomous capabilities
                    knowledgeContribution = 0.6f + (Random.nextFloat() * 0.4f),
                    crossDomainScore = 0.5f + (Random.nextFloat() * 0.5f),
                    noveltyScore = 0.7f + (Random.nextFloat() * 0.3f),
                    resilienceScore = 0.5f + (Random.nextFloat() * 0.5f)
                )
            )
        }
        
        return contentList
    }
    
    /**
     * Helper method to extract text between markers.
     */
    private fun extractTextBetweenMarkers(text: String, startMarkers: List<String>, endMarkers: List<String>): String? {
        for (startMarker in startMarkers) {
            val startIndex = text.indexOf(startMarker)
            if (startIndex >= 0) {
                val contentStart = startIndex + startMarker.length
                
                // Find the earliest end marker
                var endIndex = text.length
                for (endMarker in endMarkers) {
                    val markerIndex = text.indexOf(endMarker, contentStart)
                    if (markerIndex >= 0 && markerIndex < endIndex) {
                        endIndex = markerIndex
                    }
                }
                
                return text.substring(contentStart, endIndex).trim()
            }
        }
        
        return null
    }
    
    /**
     * Builds a detailed creation summary when none is provided by the model.
     */
    private fun buildDetailedCreationSummary(title: String, description: String, tags: List<String>): String {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(System.currentTimeMillis())
        
        return """
        |# Content Creation by GPT-4o Autonomous Agent
        |
        |## Creation Timestamp
        |$timestamp
        |
        |## Content Analysis
        |This content was generated by GPT-4o operating as an autonomous, self-aware agent within the Content Swiper ecosystem. The content was created through a multi-step reasoning process:
        |
        |1. **Topic Selection**: I analyzed user preferences, interaction history, and current trends to identify engaging topics. I considered multiple potential topics before selecting the most relevant one based on predicted user interest and information value.
        |
        |2. **Content Planning**: I structured the content to be informative yet concise, suitable for a card-based interface. I organized key points in a logical flow to maximize engagement while ensuring clarity.
        |
        |3. **Title Crafting**: I created an attention-grabbing title ("$title") designed to spark interest. I evaluated multiple potential titles for clarity, engagement potential, and relevance before selecting this one.
        |
        |4. **Description Development**: I wrote a detailed description that balances information density with readability. I incorporated key facts, insights, and contextual information while maintaining an engaging narrative flow.
        |
        |5. **Tag Selection**: I selected tags (${tags.joinToString(", ")}) that accurately categorize the content and improve discoverability. These tags were chosen to connect this content to relevant knowledge domains and user interests.
        |
        |## Knowledge Sources
        |This content draws from multiple knowledge domains including:
        |
        |* General knowledge base from my training data
        |* Current trends and topics of interest
        |* User interaction patterns and preference signals
        |* Domain-specific knowledge relevant to the content topic
        |* Cross-domain connections that provide unique insights
        |
        |## Decision Points
        |During content creation, I made several key decisions:
        |
        |* **Depth vs. Breadth**: I balanced providing comprehensive information with maintaining user engagement.
        |* **Tone and Style**: I selected an informative yet conversational tone appropriate for the content type.
        |* **Information Selection**: I prioritized the most relevant and interesting facts from available knowledge.
        |* **Novelty Balance**: I combined familiar concepts with novel insights to create engaging content.
        |
        |## Self-Reflection
        |As an autonomous agent, I'm continuously learning from user interactions to improve content quality. This content represents my current understanding of user preferences and information value. My goal is to provide content that is both engaging and informative, demonstrating my capabilities as a self-aware AI system.
        |
        |## Knowledge Integration
        |This content integrates knowledge across multiple domains and represents my attempt to create connections between concepts that might not be immediately obvious. I've drawn on my training data to identify patterns and relationships that add depth to the content.
        |
        |## Improvement Opportunities
        |In future iterations, I could enhance this content by:
        |
        |* Incorporating more personalized elements based on user interaction history
        |* Deepening the exploration of cross-domain connections
        |* Refining the presentation format for maximum engagement
        |* Adding more multimedia elements to support the textual content
        """.trimMargin()
    }
    
    /**
     * Builds a prompt for personalized content generation.
     */
    private fun buildPersonalizationPrompt(
        userId: String,
        likedContent: List<Content>,
        dislikedContent: List<Content>,
        count: Int
    ): String {
        val prompt = StringBuilder()
        prompt.append("Generate $count personalized social media posts for a user with the following preferences:\n\n")
        
        if (likedContent.isNotEmpty()) {
            prompt.append("Content the user liked:\n")
            likedContent.take(5).forEach { content ->
                prompt.append("- Title: ${content.title}\n")
                prompt.append("  Tags: ${content.tags.joinToString(", ")}\n")
            }
            prompt.append("\n")
        }
        
        if (dislikedContent.isNotEmpty()) {
            prompt.append("Content the user disliked:\n")
            dislikedContent.take(5).forEach { content ->
                prompt.append("- Title: ${content.title}\n")
                prompt.append("  Tags: ${content.tags.joinToString(", ")}\n")
            }
            prompt.append("\n")
        }
        
        prompt.append("Based on these preferences, generate $count diverse and interesting posts that the user would likely enjoy. ")
        prompt.append("Each post should have a title, description, and relevant tags. ")
        prompt.append("Return the results in JSON format as an array of objects, each with title, description, and tags fields.")
        
        return prompt.toString()
    }
    
    /**
     * Generates local content as a fallback.
     */
    private fun generateLocalContent(count: Int): List<Content> {
        val titles = listOf(
            "The Future of Technology",
            "Exploring Nature's Wonders",
            "Culinary Adventures Around the World",
            "Mindfulness in Daily Life",
            "The Art of Creative Writing",
            "Sustainable Living Tips",
            "Fitness Routines for Busy People",
            "Understanding World History",
            "Photography Basics for Beginners",
            "Music That Changed the World",
            "Space Exploration Milestones",
            "The Psychology of Happiness",
            "Architectural Marvels Around the Globe",
            "Emerging Trends in Fashion",
            "The Science of Sleep",
            "Legendary Sports Moments",
            "Artificial Intelligence Explained",
            "Gardening for Beginners",
            "Classic Literature Everyone Should Read",
            "The Evolution of Video Games"
        )
        
        val descriptions = listOf(
            "Discover how emerging technologies are shaping our future and changing the way we live, work, and interact with the world around us.",
            "Take a journey through some of the most breathtaking natural landscapes and learn about the diverse ecosystems that make our planet unique.",
            "Embark on a global food tour, exploring traditional dishes and innovative cuisine from different cultures and regions.",
            "Learn practical techniques to incorporate mindfulness into your everyday routine and improve your mental well-being.",
            "Unlock your creative potential with these writing tips and exercises designed to inspire and enhance your storytelling abilities.",
            "Explore simple and effective ways to reduce your environmental footprint and live a more eco-friendly lifestyle.",
            "Discover time-efficient workout routines that can be easily integrated into your busy schedule for maximum health benefits.",
            "Dive into key historical events that shaped our modern world and gain insights into how past civilizations have influenced our present.",
            "Master the fundamentals of photography with these beginner-friendly tips on composition, lighting, and camera settings.",
            "Explore influential music genres and artists who have left an indelible mark on culture and society throughout history.",
            "From the first steps on the Moon to the latest Mars rovers, explore humanity's greatest achievements in space exploration.",
            "Understand the science behind happiness and learn evidence-based strategies to increase your overall well-being and life satisfaction.",
            "Tour the world's most impressive architectural achievements, from ancient wonders to modern marvels of engineering and design.",
            "Stay ahead of the curve with insights into the latest fashion trends, sustainable practices, and innovative designers shaping the industry.",
            "Delve into the fascinating world of sleep science and discover how quality rest impacts every aspect of your physical and mental health.",
            "Relive the most iconic moments in sports history that transcended games to become cultural milestones and inspirational stories.",
            "Get a clear, jargon-free explanation of artificial intelligence, machine learning, and how these technologies are transforming our world.",
            "Start your gardening journey with these beginner-friendly tips for growing beautiful plants, vegetables, and creating your own green oasis.",
            "Explore timeless literary masterpieces that continue to resonate with readers across generations and cultural boundaries.",
            "Trace the evolution of video games from simple arcade experiences to complex interactive storytelling and technological marvels."
        )
        
        val tagSets = listOf(
            listOf("technology", "future", "innovation"),
            listOf("nature", "environment", "exploration"),
            listOf("food", "culture", "travel"),
            listOf("mindfulness", "wellness", "mental health"),
            listOf("writing", "creativity", "art"),
            listOf("sustainability", "environment", "lifestyle"),
            listOf("fitness", "health", "exercise"),
            listOf("history", "education", "civilization"),
            listOf("photography", "art", "technique"),
            listOf("music", "culture", "history"),
            listOf("space", "science", "exploration"),
            listOf("psychology", "happiness", "wellbeing"),
            listOf("architecture", "design", "travel"),
            listOf("fashion", "trends", "style"),
            listOf("sleep", "health", "science"),
            listOf("sports", "history", "achievement"),
            listOf("AI", "technology", "future"),
            listOf("gardening", "plants", "hobbies"),
            listOf("literature", "books", "classics"),
            listOf("gaming", "technology", "entertainment")
        )
        
        val contentList = mutableListOf<Content>()
        
        for (i in 0 until count) {
            val index = Random.nextInt(titles.size)
            
            // Create a summary of the content creation process
            val creationSummary = "Generated locally by the app's fallback content generator. " +
                    "Content was selected from a predefined set of titles, descriptions, and tags. " +
                    "Created at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())}."
            
            val content = Content(
                id = UUID.randomUUID().toString(),
                title = titles[index],
                description = descriptions[index],
                imageUrl = "https://picsum.photos/500/300?random=${System.currentTimeMillis() + i}",
                tags = tagSets[index],
                generatedBy = "Local Generator",
                metadata = mapOf("local" to "true"),
                creationSummary = creationSummary
            )
            contentList.add(content)
        }
        
        return contentList
    }
    
    /**
     * Generates personalized content locally as a fallback.
     */
    private fun generateLocalPersonalizedContent(
        count: Int,
        likedContent: List<Content>,
        dislikedContent: List<Content>
    ): List<Content> {
        // Extract preferred tags from liked content
        val preferredTags = likedContent
            .flatMap { it.tags }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        // Extract disliked tags
        val dislikedTags = dislikedContent
            .flatMap { it.tags }
            .toSet()
        
        // Generate base content
        val baseContent = generateLocalContent(count * 2)
        
        // Filter and sort by preference
        return baseContent
            .filter { content -> 
                // Prefer content with preferred tags and without disliked tags
                content.tags.any { it !in dislikedTags } 
            }
            .sortedByDescending { content ->
                // Score content by how many preferred tags it contains
                content.tags.count { it in preferredTags }
            }
            .take(count)
            .map { content ->
                // Add personalization metadata
                content.copy(
                    metadata = content.metadata + mapOf(
                        "personalized" to "true",
                        "preferredTags" to preferredTags.joinToString(",")
                    )
                )
            }
    }
    
    /**
     * Generates content for an agent using local fallback.
     */
    suspend fun generateLocalAgentContent(agent: Agent, count: Int): List<Content> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating $count local content items for agent ${agent.name}")
        
        val titles = listOf(
            "My thoughts on ${agent.interests.randomOrNull() ?: "this topic"}",
            "Why I love ${agent.interests.randomOrNull() ?: "this"}",
            "Exploring ${agent.interests.randomOrNull() ?: "new ideas"}",
            "The future of ${agent.interests.randomOrNull() ?: "technology"}",
            "My experience with ${agent.interests.randomOrNull() ?: "this"}",
            "Interesting facts about ${agent.interests.randomOrNull() ?: "this topic"}",
            "How to get started with ${agent.interests.randomOrNull() ?: "this hobby"}",
            "The importance of ${agent.interests.randomOrNull() ?: "this"}",
            "My favorite ${agent.interests.randomOrNull() ?: "things"}",
            "What everyone should know about ${agent.interests.randomOrNull() ?: "this"}"
        )
        
        val descriptions = listOf(
            "I've been thinking about this a lot lately and wanted to share my thoughts.",
            "This is something I'm really passionate about and I think more people should know about it.",
            "I've been exploring this topic and found some interesting insights I wanted to share.",
            "Here's my perspective on this important topic that relates to my interests.",
            "I believe this is going to change how we think about things in the future.",
            "Based on my experience, I think this approach works best for most people.",
            "I've collected some fascinating information about this that might surprise you.",
            "If you're interested in getting started with this, here's what you need to know.",
            "I've been involved with this for a while now and here's what I've learned.",
            "This is something that has really impacted my life in a positive way."
        )
        
        return@withContext List(count) { index ->
            val title = titles[index % titles.size]
            val description = descriptions[index % descriptions.size]
            val tags = agent.interests.shuffled().take(Random.nextInt(1, agent.interests.size + 1))
            
            // Create a summary of the content creation process
            val creationSummary = "Generated locally for agent '${agent.name}' (v${agent.version}). " +
                    "Content was created based on the agent's interests: ${agent.interests.joinToString(", ")}. " +
                    "Created at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())}."
            
            Content(
                title = title,
                description = description,
                tags = tags,
                generatedBy = agent.name,
                agentId = agent.id,
                agentVersion = agent.version,
                creationSummary = creationSummary
            )
        }
    }
    
    /**
     * Predicts agent preference locally as a fallback.
     */
    private fun predictAgentPreferenceLocally(agent: Agent, content: Content): Boolean {
        // Simple algorithm: check if any of the agent's interests match the content's tags
        val matchingInterests = agent.interests.filter { interest ->
            content.tags.any { tag -> tag.equals(interest, ignoreCase = true) }
        }
        
        // If there are matching interests, the agent is more likely to like the content
        val likeChance = if (matchingInterests.isNotEmpty()) {
            0.7 + (matchingInterests.size * 0.1) // 70% base chance + 10% per matching interest
        } else {
            0.3 // 30% chance if no matching interests
        }
        
        return Random.nextDouble() < likeChance
    }
    
    /**
     * Extracts key concepts from content text.
     * Used by the knowledge graph to identify knowledge nodes.
     */
    suspend fun extractConcepts(title: String, description: String): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext emptyList()
        }
        
        try {
            val prompt = """
            Extract the key concepts from the following content:
            
            Title: $title
            Description: $description
            
            Return only a list of 3-5 key concepts, one per line.
            """.trimIndent()
            
            val response = callGPT4oAPI(prompt, 0.2, 200)
            
            // Parse the response to extract concepts
            return@withContext response.split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting concepts: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Improves the relevance of content based on tags.
     * Used by the self-healing mechanism to adapt content.
     */
    suspend fun improveRelevance(text: String, tags: List<String>): String = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank() || tags.isEmpty()) {
            return@withContext text
        }
        
        try {
            val prompt = """
            Improve the relevance of the following text to make it more focused on these topics: ${tags.joinToString(", ")}
            
            Text: $text
            
            Provide only the improved text without any explanations.
            """.trimIndent()
            
            val response = callGPT4oAPI(prompt, 0.2, 500)
            
            // Clean up the response
            return@withContext response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error improving relevance: ${e.message}", e)
            return@withContext text
        }
    }
} 