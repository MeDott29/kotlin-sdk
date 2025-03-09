package com.example.contentswiper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.contentswiper.api.AgentManager
import com.example.contentswiper.api.ContentGenerator
import com.example.contentswiper.api.KnowledgeGraphManager
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository

/**
 * Factory for creating MainViewModel instances.
 */
class MainViewModelFactory(
    private val contentRepository: ContentRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val agentRepository: AgentRepository,
    private val contentGenerator: ContentGenerator?,
    private val agentManager: AgentManager?,
    private val knowledgeGraphManager: KnowledgeGraphManager? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                contentRepository,
                userPreferenceRepository,
                agentRepository,
                contentGenerator,
                agentManager,
                knowledgeGraphManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
} 