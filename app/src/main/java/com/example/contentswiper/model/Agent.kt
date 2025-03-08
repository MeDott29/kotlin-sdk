package com.example.contentswiper.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a GPT-4o agent that acts as a user in the simulated social network.
 */
@Entity(tableName = "agents")
data class Agent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val interests: List<String> = emptyList(),
    val personality: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) 