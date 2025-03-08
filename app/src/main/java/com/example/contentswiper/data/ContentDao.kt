package com.example.contentswiper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.contentswiper.model.Content
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the Content entity.
 */
@Dao
interface ContentDao {
    @Query("SELECT * FROM content ORDER BY createdAt DESC")
    fun getAllContent(): Flow<List<Content>>
    
    @Query("SELECT * FROM content WHERE id = :id")
    suspend fun getContentById(id: String): Content?
    
    @Query("SELECT * FROM content WHERE id NOT IN (SELECT contentId FROM user_preferences WHERE userId = :userId) ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getUnseenContent(userId: String, limit: Int): List<Content>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: Content): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContent(contents: List<Content>)
    
    @Update
    suspend fun updateContent(content: Content)
    
    @Delete
    suspend fun deleteContent(content: Content)
    
    @Query("DELETE FROM content")
    suspend fun deleteAllContent()
} 