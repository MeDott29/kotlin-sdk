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
} 