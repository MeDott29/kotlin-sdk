package com.example.contentswiper.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.contentswiper.api.AgentManager
import com.example.contentswiper.api.ContentGenerator
import com.example.contentswiper.api.KnowledgeGraphManager
import com.example.contentswiper.api.generateSurprisingContent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

/**
 * ViewModel for the main activity.
 */
class MainViewModel(
    private val contentRepository: ContentRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val agentRepository: AgentRepository,
    private val contentGenerator: ContentGenerator?,
    private val agentManager: AgentManager?,
    private val knowledgeGraphManager: KnowledgeGraphManager? = null
) : ViewModel() {
    private val TAG = "MainViewModel"
    
    // User ID for the current user
    private val userId = "current_user"
    
    // Content to display
    private val _contentList = MutableLiveData<List<Content>>()
    val contentList: LiveData<List<Content>> = _contentList
    
    // Error message
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // Surprising content flag
    private val _showingSurprisingContent = MutableLiveData<Boolean>(false)
    val showingSurprisingContent: LiveData<Boolean> = _showingSurprisingContent
    
    init {
        Log.d(TAG, "MainViewModel initialized")
    }
    
    /**
     * Clears any error messages.
     */
    fun clearError() {
        _error.value = ""
    }
    
    /**
     * Loads initial content.
     */
    fun loadContent() {
        viewModelScope.launch {
            try {
                // First, try to get unseen content
                var contentList = contentRepository.getUnseenContent(userId, 10)
                
                // If there's not enough content, generate more
                if (contentList.size < 5) {
                    Log.d(TAG, "Not enough content, generating more...")
                    try {
                        val newContent = generateMoreContent(10)
                        contentList = contentList + newContent
                    } catch (e: Exception) {
                        Log.e(TAG, "Error generating more content: ${e.message}", e)
                        if (contentList.isEmpty()) {
                            // If we have no content at all, create some fallback content
                            contentList = createFallbackContent(5)
                        }
                    }
                }
                
                // Sort content by popularity if available
                contentList = contentList.sortedByDescending { content ->
                    content.metadata["popularity"]?.toDoubleOrNull() ?: 0.0
                }
                
                _contentList.value = contentList
                
                if (contentList.isEmpty()) {
                    _error.value = "No content available. Please try again later."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading content: ${e.message}", e)
                _error.value = "Error loading content: ${e.message}"
                
                // Provide fallback content
                val fallbackContent = createFallbackContent(5)
                _contentList.value = fallbackContent
            }
        }
    }
    
    /**
     * Refreshes content when the user has swiped through a significant number of cards.
     * This ensures new content is loaded as the user progresses.
     */
    fun refreshContentIfNeeded(currentPosition: Int) {
        viewModelScope.launch {
            try {
                val currentContentList = _contentList.value ?: return@launch
                
                // If we've swiped through more than half the content, load more
                if (currentPosition >= currentContentList.size / 2 && currentContentList.size < 20) {
                    Log.d(TAG, "User has swiped through half the content, loading more...")
                    
                    // Get more unseen content
                    val moreContent = contentRepository.getUnseenContent(userId, 5)
                    
                    // If there's not enough unseen content, generate more
                    if (moreContent.isEmpty()) {
                        Log.d(TAG, "No more unseen content, generating new content...")
                        val newContent = generateMoreContent(5)
                        
                        // Add the new content to the existing list
                        val updatedList = currentContentList + newContent
                        _contentList.value = updatedList
                        
                        Log.d(TAG, "Added ${newContent.size} new content items, total now: ${updatedList.size}")
                    } else {
                        // Add the unseen content to the existing list
                        val updatedList = currentContentList + moreContent
                        _contentList.value = updatedList
                        
                        Log.d(TAG, "Added ${moreContent.size} unseen content items, total now: ${updatedList.size}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing content: ${e.message}", e)
            }
        }
    }
    
    /**
     * Creates fallback content when content generation fails.
     */
    private fun createFallbackContent(count: Int): List<Content> {
        val fallbackContent = mutableListOf<Content>()
        
        for (i in 1..count) {
            fallbackContent.add(
                Content(
                    id = UUID.randomUUID().toString(),
                    title = "Fallback Content #$i",
                    description = "This is fallback content created because content generation failed. " +
                            "It contains general information that might be interesting to you.",
                    imageUrl = "https://picsum.photos/500/300?random=${System.currentTimeMillis() + i}",
                    imagePrompt = null,
                    tags = listOf("fallback", "general", "offline"),
                    generatedBy = "Fallback Generator",
                    metadata = mapOf("fallback" to "true")
                )
            )
        }
        
        // Save the fallback content to the database
        viewModelScope.launch {
            contentRepository.insertAllContent(fallbackContent)
        }
        
        return fallbackContent
    }
    
    /**
     * Generates more content.
     */
    private suspend fun generateMoreContent(count: Int): List<Content> {
        if (contentGenerator == null) {
            Log.e(TAG, "ContentGenerator is null, cannot generate content")
            throw IllegalStateException("ContentGenerator is not initialized")
        }
        
        try {
            // First, check if we should run a social network simulation cycle
            val shouldRunNetworkCycle = Random.nextDouble() < 0.7 // 70% chance to run a network cycle
            
            if (shouldRunNetworkCycle) {
                Log.d(TAG, "Running social network simulation cycle")
                agentManager?.simulateSocialNetworkCycle(count)
                
                // Get the most recent content after the simulation
                val recentContent = contentRepository.getRecentContent(count)
                if (recentContent.isNotEmpty()) {
                    Log.d(TAG, "Using ${recentContent.size} content items from social network simulation")
                    return recentContent
                }
            }
            
            // If we didn't run a network cycle or it didn't produce enough content,
            // generate personalized content for the user
            val likedPreferences = userPreferenceRepository.getLikedContent(userId).first()
            val likedContentIds = likedPreferences.map { it.contentId }
            
            val likedContent = mutableListOf<Content>()
            for (contentId in likedContentIds) {
                val content = contentRepository.getContentById(contentId)
                if (content != null) {
                    likedContent.add(content)
                }
            }
            
            val dislikedPreferences = userPreferenceRepository.getUserPreferences(userId).first().filter { !it.isLiked }
            val dislikedContentIds = dislikedPreferences.map { it.contentId }
            
            val dislikedContent = mutableListOf<Content>()
            for (contentId in dislikedContentIds) {
                val content = contentRepository.getContentById(contentId)
                if (content != null) {
                    dislikedContent.add(content)
                }
            }
            
            // Generate content based on user preferences
            val newContent = if (likedContent.isNotEmpty() || dislikedContent.isNotEmpty()) {
                Log.d(TAG, "Generating personalized content based on user preferences")
                contentGenerator.generatePersonalizedContent(
                    userId = userId,
                    likedContent = likedContent,
                    dislikedContent = dislikedContent,
                    count = count
                )
            } else {
                Log.d(TAG, "Generating random content (no user preferences yet)")
                contentGenerator.generateRandomContent(count)
            }
            
            // Save the new content to the database
            contentRepository.insertAllContent(newContent)
            
            // Simulate agent interactions with new content
            for (content in newContent) {
                agentManager?.simulateAgentInteractions(content)
            }
            
            return newContent
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Generates content using simulated AI.
     */
    private suspend fun generateSimulatedAIContent(count: Int): List<Content> {
        Log.d(TAG, "Generating simulated AI content")
        
        try {
            // Get user preferences for personalization
            val likedPreferences = userPreferenceRepository.getLikedContent(userId).first()
            val likedContentIds = likedPreferences.map { it.contentId }
            
            val likedContent = mutableListOf<Content>()
            for (contentId in likedContentIds) {
                val content = contentRepository.getContentById(contentId)
                if (content != null) {
                    likedContent.add(content)
                }
            }
            
            // Create a prompt based on user preferences
            val prompt = if (likedContent.isNotEmpty()) {
                val likedTags = likedContent.flatMap { it.tags }.groupBy { it }
                    .mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }
                    .take(5)
                    .map { it.key }
                    .joinToString(", ")
                
                "Generate content related to these topics: $likedTags"
            } else {
                "Generate interesting content about trending topics"
            }
            
            // Generate content with GPT-4o
            val newContent = contentGenerator?.generateRandomContent(count) ?: emptyList()
            
            // Save the new content to the database
            contentRepository.insertAllContent(newContent)
            
            // Simulate agent interactions with new content
            for (content in newContent) {
                agentManager?.simulateAgentInteractions(content)
            }
            
            return newContent
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI content: ${e.message}", e)
            return generateRandomContent(count)
        }
    }
    
    /**
     * Generates random content as a fallback.
     */
    private suspend fun generateRandomContent(count: Int): List<Content> {
        return contentGenerator?.generateRandomContent(count) ?: createFallbackContent(count)
    }
    
    /**
     * Explicitly generates content using GPT-4o when requested by the user.
     * This is called when the user clicks the AI button.
     */
    fun generateGPT4oContentExplicitly(count: Int = 5) {
        viewModelScope.launch {
            try {
                _error.value = ""
                
                // Show loading state
                _contentList.value = emptyList()
                
                // Generate content with GPT-4o
                val newContent = contentGenerator?.generateRandomContent(count) ?: emptyList()
                
                // Update the UI with the new content
                _contentList.value = newContent
                
                if (newContent.isEmpty()) {
                    _error.value = "No content was generated. Please try again."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateGPT4oContentExplicitly: ${e.message}", e)
                _error.value = "Error generating content: ${e.message}"
            }
        }
    }
    
    /**
     * Records a user preference for a piece of content.
     * Also updates the knowledge graph with the content.
     */
    fun recordUserPreference(content: Content, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                // Create a user preference
                val userPreference = UserPreference(
                    userId = userId,
                    contentId = content.id,
                    isLiked = isLiked,
                    timestamp = System.currentTimeMillis()
                )
                
                // Save the preference
                userPreferenceRepository.insertUserPreference(userPreference)
                
                // Update content like/dislike count
                val updatedContent = if (isLiked) {
                    content.copy(likeCount = content.likeCount + 1)
                } else {
                    content.copy(dislikeCount = content.dislikeCount + 1)
                }
                
                // Update the content
                contentRepository.updateContent(updatedContent)
                
                // Update the knowledge graph with this content
                knowledgeGraphManager?.updateKnowledgeGraph(updatedContent)
                
                // Apply self-healing if needed (for disliked content)
                if (!isLiked && content.likeCount > 0) {
                    // Content has some likes but was disliked by this user
                    // This is a good candidate for self-healing
                    val feedback = mapOf(
                        "satisfaction" to 0.3f,
                        "relevance" to 0.5f,
                        "novelty" to 0.4f
                    )
                    
                    knowledgeGraphManager?.applySelfHealing(updatedContent, feedback)
                }
                
                // Simulate agent interactions with this content
                agentManager?.simulateAgentInteractions(updatedContent)
                
                Log.d(TAG, "Recorded user preference for content ${content.id}: isLiked=$isLiked")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording user preference: ${e.message}", e)
                _error.value = "Error recording preference: ${e.message}"
            }
        }
    }
    
    /**
     * Loads surprising content based on the knowledge graph.
     */
    suspend fun loadSurprisingContent(count: Int): List<Content> {
        try {
            // Get all agents
            val agents = agentRepository.getAllAgents().first()
            if (agents.isEmpty()) {
                Log.w(TAG, "No agents available for generating surprising content")
                return emptyList()
            }
            
            // Collect knowledge nodes from all agents
            val allKnowledgeNodes = agents.flatMap { it.knowledgeNodes }
            if (allKnowledgeNodes.isEmpty()) {
                Log.w(TAG, "No knowledge nodes available for generating surprising content")
                return emptyList()
            }
            
            // Get unique concepts and domains
            val concepts = allKnowledgeNodes.map { it.concept }.distinct().shuffled().take(10)
            val domains = allKnowledgeNodes.map { it.domain }.distinct().shuffled()
            
            // Generate surprising content
            val surprisingContent = contentGenerator?.generateSurprisingContent(concepts, domains, count) ?: emptyList()
            
            // Save the content to the database
            if (surprisingContent.isNotEmpty()) {
                contentRepository.insertAllContent(surprisingContent)
                _showingSurprisingContent.value = true
            }
            
            return surprisingContent
        } catch (e: Exception) {
            Log.e(TAG, "Error loading surprising content: ${e.message}", e)
            _error.value = "Error loading surprising content: ${e.message}"
            return emptyList()
        }
    }
    
    /**
     * Resets the surprising content flag.
     */
    fun resetSurprisingContentFlag() {
        _showingSurprisingContent.value = false
    }
    
    /**
     * Updates the popularity metrics for a piece of content.
     */
    private suspend fun updateContentPopularity(content: Content) {
        try {
            val preferences = userPreferenceRepository.getContentPreferences(content.id).first()
            val likeCount = preferences.count { it.isLiked }
            val dislikeCount = preferences.size - likeCount
            
            // Update content metadata with popularity metrics
            val updatedContent = content.copy(
                metadata = content.metadata + mapOf(
                    "likeCount" to likeCount.toString(),
                    "dislikeCount" to dislikeCount.toString(),
                    "popularity" to calculatePopularityScore(likeCount, dislikeCount).toString()
                )
            )
            
            contentRepository.updateContent(updatedContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating content popularity: ${e.message}", e)
        }
    }
    
    /**
     * Calculates a popularity score based on likes and dislikes.
     */
    private fun calculatePopularityScore(likes: Int, dislikes: Int): Double {
        if (likes + dislikes == 0) return 0.0
        
        // Simple popularity formula: (likes - dislikes) / total interactions
        return (likes - dislikes).toDouble() / (likes + dislikes)
    }
    
    /**
     * Generates content similar to a piece of content the user liked.
     */
    private suspend fun generateSimilarContent(likedContent: Content) {
        if (contentGenerator == null) return
        
        try {
            Log.d(TAG, "Generating content similar to: ${likedContent.title}")
            
            val prompt = "Generate 3 social media posts similar to this one that the user liked:\n" +
                    "Title: ${likedContent.title}\n" +
                    "Description: ${likedContent.description}\n" +
                    "Tags: ${likedContent.tags.joinToString(", ")}\n\n" +
                    "Each post should have a title, description, and relevant tags. Make them similar in topic but not identical."
            
            val similarContent = contentGenerator.generateGPT4oContent(3, prompt)
            
            if (similarContent.isNotEmpty()) {
                // Save the new content to the database
                contentRepository.insertAllContent(similarContent)
                
                // Simulate agent interactions with new content
                for (content in similarContent) {
                    agentManager?.simulateAgentInteractions(content)
                }
                
                Log.d(TAG, "Generated ${similarContent.size} similar content items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating similar content: ${e.message}", e)
        }
    }
    
    /**
     * Adds new content to the current content list.
     * This is used for content generated through recursive reasoning or other special methods.
     */
    fun addContent(newContent: List<Content>) {
        viewModelScope.launch {
            try {
                // Save the new content to the repository
                contentRepository.insertAll(newContent)
                
                // Get the current content list
                val currentContent = _contentList.value ?: emptyList()
                
                // Add the new content to the beginning of the list
                val updatedContent = newContent + currentContent
                
                // Update the LiveData
                _contentList.value = updatedContent
                
                Log.d(TAG, "Added ${newContent.size} new pieces of content to the list")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding new content: ${e.message}", e)
                _error.value = "Error adding new content: ${e.message}"
            }
        }
    }
    
    /**
     * Factory for creating MainViewModel instances.
     */
    class Factory(
        private val contentRepository: ContentRepository,
        private val userPreferenceRepository: UserPreferenceRepository,
        private val agentRepository: AgentRepository,
        private val contentGenerator: ContentGenerator,
        private val agentManager: AgentManager
    ) : ViewModelProvider.Factory {
        
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    contentRepository,
                    userPreferenceRepository,
                    agentRepository,
                    contentGenerator,
                    agentManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 