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
} 