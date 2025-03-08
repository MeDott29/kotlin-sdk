package com.example.contentswiper.repository

import com.example.contentswiper.data.ContentDao
import com.example.contentswiper.model.Content
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Content data.
 */
class ContentRepository(private val contentDao: ContentDao) {
    
    val allContent: Flow<List<Content>> = contentDao.getAllContent()
    
    suspend fun getContentById(id: String): Content? {
        return contentDao.getContentById(id)
    }
    
    suspend fun getUnseenContent(userId: String, limit: Int): List<Content> {
        return contentDao.getUnseenContent(userId, limit)
    }
    
    suspend fun insertContent(content: Content) {
        contentDao.insertContent(content)
    }
    
    suspend fun insertAllContent(contents: List<Content>) {
        contentDao.insertAllContent(contents)
    }
    
    suspend fun updateContent(content: Content) {
        contentDao.updateContent(content)
    }
    
    suspend fun deleteContent(content: Content) {
        contentDao.deleteContent(content)
    }
    
    suspend fun deleteAllContent() {
        contentDao.deleteAllContent()
    }
} 