package com.example.contentswiper

import android.app.Application
import com.example.contentswiper.api.AgentManager
import com.example.contentswiper.api.ContentGenerator
import com.example.contentswiper.data.AppDatabase
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Application class for the Content Swiper app.
 */
class ContentSwiperApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob())
    
    // Database
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Repositories
    val contentRepository by lazy { ContentRepository(database.contentDao()) }
    val userPreferenceRepository by lazy { UserPreferenceRepository(database.userPreferenceDao()) }
    val agentRepository by lazy { AgentRepository(database.agentDao()) }
    
    // API Services
    val contentGenerator by lazy { ContentGenerator() }
    
    // Managers
    val agentManager by lazy {
        AgentManager(
            agentRepository,
            contentRepository,
            userPreferenceRepository,
            contentGenerator
        )
    }
} 