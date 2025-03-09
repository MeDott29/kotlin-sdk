package com.example.contentswiper.repository

import com.example.contentswiper.data.AgentDao
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.EvolutionRecord
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Agent data.
 */
class AgentRepository(private val agentDao: AgentDao) {
    
    val allActiveAgents: Flow<List<Agent>> = agentDao.getAllActiveAgents()
    val agentsByEvolutionVersion: Flow<List<Agent>> = agentDao.getAgentsByEvolutionVersion()
    
    /**
     * Gets all agents, both active and inactive.
     */
    fun getAllAgents(): Flow<List<Agent>> {
        return agentDao.getAllAgents()
    }
    
    /**
     * Gets the total count of all agents.
     */
    suspend fun getAgentCount(): Int {
        return agentDao.getAgentCount()
    }
    
    /**
     * Gets agents by their IDs.
     */
    suspend fun getAgentsByIds(ids: List<String>): List<Agent> {
        return agentDao.getAgentsByIds(ids)
    }
    
    suspend fun getAgentById(id: String): Agent? {
        return agentDao.getAgentById(id)
    }
    
    suspend fun insertAgent(agent: Agent) {
        agentDao.insertAgent(agent)
    }
    
    suspend fun insertAllAgents(agents: List<Agent>) {
        agentDao.insertAllAgents(agents)
    }
    
    suspend fun updateAgent(agent: Agent) {
        agentDao.updateAgent(agent)
    }
    
    suspend fun deleteAgent(agent: Agent) {
        agentDao.deleteAgent(agent)
    }
    
    suspend fun deleteAllAgents() {
        agentDao.deleteAllAgents()
    }
    
    suspend fun getActiveAgentCount(): Int {
        return agentDao.getActiveAgentCount()
    }
    
    /**
     * Gets agents that are due for evolution based on content generation count or time.
     */
    suspend fun getAgentsDueForEvolution(contentThreshold: Int, timeThreshold: Long): List<Agent> {
        val currentTime = System.currentTimeMillis()
        return agentDao.getAgentsDueForEvolution(contentThreshold, timeThreshold, currentTime)
    }
    
    /**
     * Gets the evolution history for a specific agent.
     */
    suspend fun getAgentEvolutionHistory(agentId: String): Agent? {
        return agentDao.getAgentEvolutionHistory(agentId)
    }
    
    /**
     * Increments the content generation count for an agent.
     */
    suspend fun incrementContentGenerationCount(agentId: String) {
        agentDao.incrementContentGenerationCount(agentId)
    }
    
    /**
     * Updates an agent's content metrics.
     */
    suspend fun updateAgentContentMetrics(agentId: String, likeRate: Float, engagementScore: Float) {
        agentDao.updateAgentContentMetrics(agentId, likeRate, engagementScore)
    }
    
    /**
     * Evolves an agent based on content performance and feedback.
     * Returns the updated agent with a new version and evolution record.
     */
    suspend fun evolveAgent(
        agent: Agent,
        newPersonality: String,
        newInterests: List<String>,
        reason: String,
        contentMetrics: Map<String, Float>
    ): Agent {
        val evolutionRecord = EvolutionRecord(
            version = agent.version + 1,
            previousPersonality = agent.personality,
            newPersonality = newPersonality,
            previousInterests = agent.interests,
            newInterests = newInterests,
            reason = reason,
            contentMetrics = contentMetrics
        )
        
        val updatedAgent = agent.copy(
            version = agent.version + 1,
            personality = newPersonality,
            interests = newInterests,
            lastEvolutionTime = System.currentTimeMillis(),
            evolutionHistory = agent.evolutionHistory + evolutionRecord,
            contentGenerationCount = 0 // Reset after evolution
        )
        
        agentDao.updateAgent(updatedAgent)
        return updatedAgent
    }
    
    /**
     * Adds a discussion record to an agent's metadata.
     */
    suspend fun addDiscussionRecord(agentId: String, discussionRecord: Map<String, Any>) {
        val agent = agentDao.getAgentById(agentId) ?: return
        
        // Get existing discussion records or create a new list
        val discussionRecords = agent.metadata["discussionRecords"]?.let {
            try {
                @Suppress("UNCHECKED_CAST")
                it as MutableList<Map<String, Any>>
            } catch (e: ClassCastException) {
                mutableListOf<Map<String, Any>>()
            }
        } ?: mutableListOf()
        
        // Add the new record
        discussionRecords.add(discussionRecord)
        
        // Update the agent with the new discussion records
        val updatedMetadata = agent.metadata.toMutableMap().apply {
            put("discussionRecords", discussionRecords)
            put("discussionCount", discussionRecords.size)
        }
        
        val updatedAgent = agent.copy(metadata = updatedMetadata)
        agentDao.updateAgent(updatedAgent)
    }
    
    /**
     * Gets all discussion records for an agent.
     */
    suspend fun getAgentDiscussionRecords(agentId: String): List<Map<String, String>> {
        val agent = agentDao.getAgentById(agentId) ?: return emptyList()
        
        return try {
            @Suppress("UNCHECKED_CAST")
            (agent.metadata["discussionRecords"] as? List<Map<String, String>>) ?: emptyList()
        } catch (e: ClassCastException) {
            emptyList()
        }
    }
    
    /**
     * Gets the number of discussions an agent has participated in.
     */
    suspend fun getAgentDiscussionCount(agentId: String): Int {
        val agent = agentDao.getAgentById(agentId) ?: return 0
        return agent.metadata["discussionCount"]?.toString()?.toIntOrNull() ?: 0
    }
    
    /**
     * Clears all discussion records for an agent.
     */
    suspend fun clearDiscussionRecords(agentId: String) {
        val agent = agentDao.getAgentById(agentId) ?: return
        
        val updatedMetadata = agent.metadata.toMutableMap().apply {
            put("discussionRecords", emptyList<Map<String, Any>>())
            put("discussionCount", 0)
        }
        
        val updatedAgent = agent.copy(metadata = updatedMetadata)
        agentDao.updateAgent(updatedAgent)
    }
} 