package com.example.contentswiper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.contentswiper.model.UserPreference
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the UserPreference entity.
 */
@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserPreferences(userId: String): Flow<List<UserPreference>>
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId AND isLiked = 1 ORDER BY timestamp DESC")
    fun getLikedContent(userId: String): Flow<List<UserPreference>>
    
    @Query("SELECT * FROM user_preferences WHERE contentId = :contentId AND userId = :userId")
    suspend fun getPreferenceForContent(contentId: String, userId: String): UserPreference?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference): Long
    
    @Update
    suspend fun updatePreference(preference: UserPreference)
    
    @Delete
    suspend fun deletePreference(preference: UserPreference)
    
    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    suspend fun deleteAllUserPreferences(userId: String)
    
    @Query("SELECT COUNT(*) FROM user_preferences WHERE userId = :userId AND isLiked = 1")
    suspend fun getLikedContentCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_preferences WHERE userId = :userId AND isLiked = 0")
    suspend fun getDislikedContentCount(userId: String): Int
    
    /**
     * Gets all preferences for a specific content item.
     */
    @Query("SELECT * FROM user_preferences WHERE contentId = :contentId ORDER BY timestamp DESC")
    fun getContentPreferences(contentId: String): Flow<List<UserPreference>>
    
    /**
     * Gets the number of likes for a specific content item.
     */
    @Query("SELECT COUNT(*) FROM user_preferences WHERE contentId = :contentId AND isLiked = 1")
    suspend fun getContentLikeCount(contentId: String): Int
    
    /**
     * Gets the number of dislikes for a specific content item.
     */
    @Query("SELECT COUNT(*) FROM user_preferences WHERE contentId = :contentId AND isLiked = 0")
    suspend fun getContentDislikeCount(contentId: String): Int
    
    /**
     * Gets the most liked content IDs.
     */
    @Query("SELECT contentId FROM user_preferences WHERE isLiked = 1 GROUP BY contentId ORDER BY COUNT(*) DESC LIMIT :limit")
    suspend fun getMostLikedContentIds(limit: Int): List<String>
} 