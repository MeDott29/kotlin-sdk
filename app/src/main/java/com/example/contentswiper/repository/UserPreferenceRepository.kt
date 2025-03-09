package com.example.contentswiper.repository

import com.example.contentswiper.data.UserPreferenceDao
import com.example.contentswiper.model.UserPreference
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing UserPreference data.
 */
class UserPreferenceRepository(private val userPreferenceDao: UserPreferenceDao) {
    
    fun getUserPreferences(userId: String): Flow<List<UserPreference>> {
        return userPreferenceDao.getUserPreferences(userId)
    }
    
    fun getLikedContent(userId: String): Flow<List<UserPreference>> {
        return userPreferenceDao.getLikedContent(userId)
    }
    
    suspend fun getPreferenceForContent(contentId: String, userId: String): UserPreference? {
        return userPreferenceDao.getPreferenceForContent(contentId, userId)
    }
    
    suspend fun insertPreference(preference: UserPreference) {
        userPreferenceDao.insertPreference(preference)
    }
    
    /**
     * Alias for insertPreference to maintain API consistency.
     */
    suspend fun insertUserPreference(preference: UserPreference) {
        insertPreference(preference)
    }
    
    suspend fun updatePreference(preference: UserPreference) {
        userPreferenceDao.updatePreference(preference)
    }
    
    suspend fun deletePreference(preference: UserPreference) {
        userPreferenceDao.deletePreference(preference)
    }
    
    suspend fun deleteAllUserPreferences(userId: String) {
        userPreferenceDao.deleteAllUserPreferences(userId)
    }
    
    suspend fun getLikedContentCount(userId: String): Int {
        return userPreferenceDao.getLikedContentCount(userId)
    }
    
    suspend fun getDislikedContentCount(userId: String): Int {
        return userPreferenceDao.getDislikedContentCount(userId)
    }
    
    /**
     * Gets all preferences for a specific content item.
     */
    fun getContentPreferences(contentId: String): Flow<List<UserPreference>> {
        return userPreferenceDao.getContentPreferences(contentId)
    }
    
    /**
     * Gets the number of likes for a specific content item.
     */
    suspend fun getContentLikeCount(contentId: String): Int {
        return userPreferenceDao.getContentLikeCount(contentId)
    }
    
    /**
     * Gets the number of dislikes for a specific content item.
     */
    suspend fun getContentDislikeCount(contentId: String): Int {
        return userPreferenceDao.getContentDislikeCount(contentId)
    }
    
    /**
     * Gets the most liked content IDs.
     */
    suspend fun getMostLikedContentIds(limit: Int): List<String> {
        return userPreferenceDao.getMostLikedContentIds(limit)
    }
} 