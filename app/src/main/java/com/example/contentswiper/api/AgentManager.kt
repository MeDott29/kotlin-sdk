package com.example.contentswiper.api

import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages simulated agents that act as users in the social network.
 */
class AgentManager(
    private val agentRepository: AgentRepository,
    private val contentRepository: ContentRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val contentGenerator: ContentGenerator
) {
    
    /**
     * Creates a set of default agents with different personalities and interests.
     */
    suspend fun createDefaultAgents() = withContext(Dispatchers.IO) {
        val agents = listOf(
            Agent(
                name = "Tech Enthusiast",
                description = "Loves technology, gadgets, and innovation",
                interests = listOf("technology", "AI", "gadgets", "programming", "science"),
                personality = "Analytical and forward-thinking"
            ),
            Agent(
                name = "Creative Artist",
                description = "Passionate about art, design, and creative expression",
                interests = listOf("art", "design", "creativity", "photography", "music"),
                personality = "Imaginative and expressive"
            ),
            Agent(
                name = "Fitness Guru",
                description = "Dedicated to health, fitness, and wellness",
                interests = listOf("fitness", "health", "nutrition", "sports", "wellness"),
                personality = "Disciplined and energetic"
            ),
            Agent(
                name = "Travel Explorer",
                description = "Loves traveling, cultures, and adventures",
                interests = listOf("travel", "culture", "adventure", "food", "photography"),
                personality = "Curious and adventurous"
            ),
            Agent(
                name = "Business Professional",
                description = "Focused on business, finance, and professional growth",
                interests = listOf("business", "finance", "leadership", "entrepreneurship", "economics"),
                personality = "Ambitious and strategic"
            ),
            Agent(
                name = "Mindful Meditator",
                description = "Interested in mindfulness, spirituality, and personal growth",
                interests = listOf("mindfulness", "meditation", "spirituality", "psychology", "self-improvement"),
                personality = "Reflective and calm"
            ),
            Agent(
                name = "Foodie",
                description = "Passionate about food, cooking, and culinary experiences",
                interests = listOf("food", "cooking", "recipes", "restaurants", "culinary arts"),
                personality = "Enthusiastic and appreciative"
            ),
            Agent(
                name = "Environmental Advocate",
                description = "Committed to environmental causes and sustainability",
                interests = listOf("environment", "sustainability", "climate", "nature", "conservation"),
                personality = "Passionate and principled"
            ),
            Agent(
                name = "Gaming Enthusiast",
                description = "Loves video games, gaming culture, and esports",
                interests = listOf("gaming", "video games", "esports", "technology", "entertainment"),
                personality = "Competitive and strategic"
            ),
            Agent(
                name = "Literary Scholar",
                description = "Passionate about literature, writing, and storytelling",
                interests = listOf("literature", "books", "writing", "poetry", "storytelling"),
                personality = "Thoughtful and articulate"
            )
        )
        
        agentRepository.insertAllAgents(agents)
    }
    
    /**
     * Simulates agent interactions with content.
     */
    suspend fun simulateAgentInteractions(content: Content) = withContext(Dispatchers.IO) {
        val agents = agentRepository.allActiveAgents.first()
        
        for (agent in agents) {
            val isLiked = contentGenerator.predictAgentPreference(agent, content)
            
            val preference = UserPreference(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                userId = agent.id,
                isLiked = isLiked
            )
            
            userPreferenceRepository.insertPreference(preference)
        }
    }
    
    /**
     * Gets content recommendations for an agent.
     */
    suspend fun getAgentRecommendations(
        agentId: String,
        count: Int
    ): List<Content> = withContext(Dispatchers.IO) {
        val agent = agentRepository.getAgentById(agentId) ?: return@withContext emptyList()
        
        val likedPreferences = userPreferenceRepository.getLikedContent(agentId).first()
        val likedContentIds = likedPreferences.map { it.contentId }
        
        val likedContent = mutableListOf<Content>()
        for (contentId in likedContentIds) {
            val content = contentRepository.getContentById(contentId)
            if (content != null) {
                likedContent.add(content)
            }
        }
        
        val dislikedPreferences = userPreferenceRepository.getUserPreferences(agentId).first().filter { !it.isLiked }
        val dislikedContentIds = dislikedPreferences.map { it.contentId }
        
        val dislikedContent = mutableListOf<Content>()
        for (contentId in dislikedContentIds) {
            val content = contentRepository.getContentById(contentId)
            if (content != null) {
                dislikedContent.add(content)
            }
        }
        
        contentGenerator.generatePersonalizedContent(
            userId = agentId,
            likedContent = likedContent,
            dislikedContent = dislikedContent,
            count = count
        )
    }
} 