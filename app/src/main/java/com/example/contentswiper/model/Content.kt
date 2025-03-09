package com.example.contentswiper.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a piece of content that can be swiped in the app.
 * Includes fields for tracking agent evolution and content performance.
 * Incorporates concepts from the Agentic Deep Graph Reasoning paper for self-organizing knowledge networks
 * and self-healing material concepts.
 */
@Entity(tableName = "content")
data class Content(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val imagePrompt: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "Local Generator",
    val metadata: Map<String, String> = emptyMap(),
    
    // Evolution tracking fields
    val agentId: String? = null,
    val agentVersion: Int = 1,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val engagementScore: Float = 0f,
    val feedbackSummary: String? = null,
    val evolutionContribution: Float = 0f, // How much this content influenced agent evolution
    
    // Content creation process summary
    val creationSummary: String? = null,
    
    // Knowledge graph integration
    val knowledgeNodeIds: List<String> = emptyList(), // IDs of knowledge nodes this content relates to
    val knowledgeContribution: Float = 0f, // How much new knowledge this content adds to the network
    val crossDomainScore: Float = 0f, // How well this content bridges different knowledge domains
    val noveltyScore: Float = 0f, // How novel this content is compared to existing knowledge
    
    // Self-healing material concepts
    val resilienceScore: Float = 0f, // How well this content maintains relevance over time
    val adaptiveElements: List<AdaptiveElement> = emptyList(), // Elements that can adapt based on feedback
    val selfHealingTriggers: List<String> = emptyList(), // What triggers content to self-heal/adapt
    val healingHistory: List<HealingEvent> = emptyList() // Record of healing/adaptation events
)

/**
 * Represents an element of content that can adapt based on feedback.
 */
data class AdaptiveElement(
    val id: String = UUID.randomUUID().toString(),
    val elementType: String, // e.g., "title", "description", "tag"
    val originalValue: String,
    val currentValue: String,
    val adaptationCount: Int = 0,
    val lastAdaptedAt: Long = 0L,
    val performanceImprovement: Float = 0f // How much adaptation improved performance
)

/**
 * Records a healing/adaptation event for content.
 */
data class HealingEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val triggerType: String, // What triggered the healing
    val changedElements: List<String>, // IDs of elements that changed
    val performanceBefore: Map<String, Float>, // Performance metrics before healing
    val performanceAfter: Map<String, Float>, // Performance metrics after healing
    val healingStrategy: String // Strategy used for healing
) 