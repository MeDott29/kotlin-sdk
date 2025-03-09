package com.example.contentswiper.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a user's preference (like or dislike) for a specific content item.
 */
@Entity(
    tableName = "user_preferences",
    foreignKeys = [
        ForeignKey(
            entity = Content::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contentId")]
)
data class UserPreference(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val contentId: String,
    val userId: String,
    val isLiked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) 