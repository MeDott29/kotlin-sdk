package com.example.contentswiper.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.contentswiper.api.AgentManager
import com.example.contentswiper.api.ContentGenerator
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the main activity.
 */
class MainViewModel(
    private val contentRepository: ContentRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val agentRepository: AgentRepository,
    private val contentGenerator: ContentGenerator,
    private val agentManager: AgentManager
) : ViewModel() {
    
    // User ID for the current user
    private val userId = "current_user"
    
    // Content to display
    private val _contents = MutableLiveData<List<Content>>()
    val contents: LiveData<List<Content>> = _contents
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Empty state
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    
    init {
        loadContents()
    }
    
    /**
     * Loads content to display.
     */
    fun loadContents() {
        viewModelScope.launch {
            _isLoading.value = true
            _isEmpty.value = false
            
            // First, try to get unseen content
            var contentList = contentRepository.getUnseenContent(userId, 10)
            
            // If there's not enough content, generate more
            if (contentList.size < 5) {
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
                
                val newContent = if (likedContent.isNotEmpty() || dislikedContent.isNotEmpty()) {
                    contentGenerator.generatePersonalizedContent(
                        userId = userId,
                        likedContent = likedContent,
                        dislikedContent = dislikedContent,
                        count = 10
                    )
                } else {
                    contentGenerator.generateRandomContent(10)
                }
                
                contentRepository.insertAllContent(newContent)
                
                // Simulate agent interactions with new content
                for (content in newContent) {
                    agentManager.simulateAgentInteractions(content)
                }
                
                contentList = contentList + newContent
            }
            
            _contents.value = contentList
            _isLoading.value = false
            _isEmpty.value = contentList.isEmpty()
        }
    }
    
    /**
     * Records a user preference (like or dislike).
     */
    fun recordPreference(content: Content, isLiked: Boolean) {
        viewModelScope.launch {
            val preference = UserPreference(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                userId = userId,
                isLiked = isLiked
            )
            
            userPreferenceRepository.insertPreference(preference)
            
            // If we're running low on content, load more
            val currentContents = _contents.value ?: emptyList()
            if (currentContents.size <= 3) {
                loadContents()
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