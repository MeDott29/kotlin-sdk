package com.example.contentswiper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.contentswiper.model.Agent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the Agent entity.
 */
@Dao
interface AgentDao {
    @Query("SELECT * FROM agents WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveAgents(): Flow<List<Agent>>
    
    /**
     * Gets all agents, both active and inactive.
     */
    @Query("SELECT * FROM agents ORDER BY createdAt DESC")
    fun getAllAgents(): Flow<List<Agent>>
    
    /**
     * Gets the total count of all agents.
     */
    @Query("SELECT COUNT(*) FROM agents")
    suspend fun getAgentCount(): Int
    
    /**
     * Gets agents by their IDs.
     */
    @Query("SELECT * FROM agents WHERE id IN (:ids)")
    suspend fun getAgentsByIds(ids: List<String>): List<Agent>
    
    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): Agent?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: Agent): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAgents(agents: List<Agent>)
    
    @Update
    suspend fun updateAgent(agent: Agent)
    
    @Delete
    suspend fun deleteAgent(agent: Agent)
    
    @Query("DELETE FROM agents")
    suspend fun deleteAllAgents()
    
    @Query("SELECT COUNT(*) FROM agents WHERE isActive = 1")
    suspend fun getActiveAgentCount(): Int
    
    /**
     * Gets agents that are due for evolution based on content generation count or time.
     */
    @Query("SELECT * FROM agents WHERE isActive = 1 AND (contentGenerationCount >= :contentThreshold OR (lastEvolutionTime > 0 AND :currentTime - lastEvolutionTime >= :timeThreshold))")
    suspend fun getAgentsDueForEvolution(contentThreshold: Int, timeThreshold: Long, currentTime: Long): List<Agent>
    
    /**
     * Gets agents sorted by their evolution version.
     */
    @Query("SELECT * FROM agents WHERE isActive = 1 ORDER BY version DESC")
    fun getAgentsByEvolutionVersion(): Flow<List<Agent>>
    
    /**
     * Gets the evolution history for a specific agent.
     */
    @Query("SELECT * FROM agents WHERE id = :agentId")
    suspend fun getAgentEvolutionHistory(agentId: String): Agent?
    
    /**
     * Increments the content generation count for an agent.
     */
    @Query("UPDATE agents SET contentGenerationCount = contentGenerationCount + 1 WHERE id = :agentId")
    suspend fun incrementContentGenerationCount(agentId: String)
    
    /**
     * Updates an agent's content metrics.
     */
    @Query("UPDATE agents SET contentLikeRate = :likeRate, contentEngagementScore = :engagementScore WHERE id = :agentId")
    suspend fun updateAgentContentMetrics(agentId: String, likeRate: Float, engagementScore: Float)
} 