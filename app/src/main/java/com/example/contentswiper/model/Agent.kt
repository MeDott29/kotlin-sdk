package com.example.contentswiper.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a GPT-4o agent that acts as a user in the simulated social network.
 * Includes fields for tracking evolution over time and knowledge graph connections.
 * Implements self-organizing knowledge network concepts from the Agentic Deep Graph Reasoning paper.
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
    val isActive: Boolean = true,
    
    // Evolution tracking fields
    val version: Int = 1,
    val lastEvolutionTime: Long = 0L,
    val evolutionHistory: List<EvolutionRecord> = emptyList(),
    val contentGenerationCount: Int = 0,
    val contentLikeRate: Float = 0f,
    val contentEngagementScore: Float = 0f,
    val learningFactor: Float = 0.5f, // How quickly the agent adapts (0.0-1.0)
    
    // Knowledge graph connections
    val knowledgeNodes: List<KnowledgeNode> = emptyList(),
    val connectionStrength: Map<String, Float> = emptyMap(), // Agent ID to connection strength
    val bridgeScore: Float = 0f, // How well this agent connects disparate knowledge clusters
    val hubScore: Float = 0f, // How central this agent is in the knowledge network
    val specialtyDomains: List<String> = emptyList(), // Domains where this agent has expertise
    
    // Self-healing capabilities
    val adaptabilityScore: Float = 0.5f, // How well the agent adapts to new information (0.0-1.0)
    val selfRepairMechanisms: List<String> = emptyList(), // Strategies for recovering from poor performance
    val lastSelfHealingTime: Long = 0L,
    
    // Dynamic data storage
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Records a single evolution event for an agent.
 */
data class EvolutionRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int,
    val previousPersonality: String,
    val newPersonality: String,
    val previousInterests: List<String>,
    val newInterests: List<String>,
    val reason: String,
    val contentMetrics: Map<String, Float> = emptyMap(),
    val knowledgeGraphChanges: KnowledgeGraphDelta? = null
)

/**
 * Represents a node in the agent's knowledge graph.
 */
data class KnowledgeNode(
    val id: String = UUID.randomUUID().toString(),
    val concept: String,
    val domain: String,
    val connections: Map<String, Float> = emptyMap(), // Node ID to connection strength
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 0.5f,
    val source: String? = null // Where this knowledge came from
)

/**
 * Records changes to the knowledge graph during evolution.
 */
data class KnowledgeGraphDelta(
    val addedNodes: List<KnowledgeNode> = emptyList(),
    val removedNodeIds: List<String> = emptyList(),
    val strengthenedConnections: Map<String, Float> = emptyMap(), // Node ID pairs to new strength
    val weakenedConnections: Map<String, Float> = emptyMap() // Node ID pairs to new strength
) 