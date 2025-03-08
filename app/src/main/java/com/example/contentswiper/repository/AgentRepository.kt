package com.example.contentswiper.repository

import com.example.contentswiper.data.AgentDao
import com.example.contentswiper.model.Agent
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Agent data.
 */
class AgentRepository(private val agentDao: AgentDao) {
    
    val allActiveAgents: Flow<List<Agent>> = agentDao.getAllActiveAgents()
    
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
} 