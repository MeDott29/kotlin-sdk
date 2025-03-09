package com.example.contentswiper.api

import android.util.Log
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.KnowledgeNode
import com.example.contentswiper.model.KnowledgeGraphDelta
import com.example.contentswiper.model.AdaptiveElement
import com.example.contentswiper.model.HealingEvent
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Manages the self-organizing knowledge network based on the Agentic Deep Graph Reasoning paper.
 * Implements concepts from the paper to create a dynamic, self-healing knowledge graph.
 */
class KnowledgeGraphManager(
    private val agentRepository: AgentRepository,
    private val contentRepository: ContentRepository,
    private val contentGenerator: ContentGenerator?
) {
    private val TAG = "KnowledgeGraphManager"
    
    // Constants for graph management
    private val MIN_CONNECTION_STRENGTH = 0.1f
    private val MAX_CONNECTION_STRENGTH = 1.0f
    private val CONNECTION_DECAY_RATE = 0.01f
    private val HUB_THRESHOLD = 0.7f
    private val BRIDGE_THRESHOLD = 0.6f
    private val KNOWLEDGE_DOMAINS = listOf(
        "technology", "science", "art", "health", "business", 
        "environment", "psychology", "philosophy", "engineering", "materials"
    )
    
    /**
     * Initializes the knowledge graph with seed concepts from the paper.
     */
    suspend fun initializeKnowledgeGraph() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing knowledge graph with seed concepts")
        
        // Get all agents
        val agents = agentRepository.getAllAgents().first()
        if (agents.isEmpty()) {
            Log.w(TAG, "No agents found to initialize knowledge graph")
            return@withContext
        }
        
        // Create seed knowledge nodes based on the paper
        val seedNodes = listOf(
            createKnowledgeNode("Self-healing materials", "materials", "Concept from materials science about materials that can repair themselves"),
            createKnowledgeNode("Multiscale modeling", "engineering", "Modeling across different scales from nano to macro"),
            createKnowledgeNode("Adaptive control", "engineering", "Systems that adapt to changing conditions"),
            createKnowledgeNode("Synthetic biology", "science", "Engineering biological systems for specific purposes"),
            createKnowledgeNode("Impact resistance", "materials", "Ability of materials to resist impact damage"),
            createKnowledgeNode("Resilient infrastructure", "engineering", "Infrastructure that can withstand and recover from stresses"),
            createKnowledgeNode("Scale-free networks", "technology", "Networks with hub-based organization"),
            createKnowledgeNode("Emergent patterns", "science", "Patterns that emerge from complex systems"),
            createKnowledgeNode("Feedback-driven systems", "engineering", "Systems that use feedback to improve"),
            createKnowledgeNode("Self-organization", "science", "Systems that organize themselves without external direction")
        )
        
        // Distribute seed nodes among agents based on their interests
        agents.forEach { agent ->
            val agentDomains = agent.interests.intersect(KNOWLEDGE_DOMAINS.toSet()).toList()
            val relevantNodes = if (agentDomains.isNotEmpty()) {
                seedNodes.filter { it.domain in agentDomains }
            } else {
                seedNodes.shuffled().take(Random.nextInt(2, 5))
            }
            
            // Create connections between nodes
            val nodeConnections = relevantNodes.map { it.id to 0.5f + Random.nextFloat() * 0.3f }.toMap()
            
            // Update agent with knowledge nodes
            val updatedAgent = agent.copy(
                knowledgeNodes = relevantNodes,
                connectionStrength = nodeConnections,
                specialtyDomains = agentDomains.ifEmpty { listOf(relevantNodes.first().domain) }
            )
            
            agentRepository.updateAgent(updatedAgent)
            Log.d(TAG, "Initialized agent ${agent.name} with ${relevantNodes.size} knowledge nodes")
        }
        
        // Create initial cross-agent connections
        createInitialAgentConnections(agents)
    }
    
    /**
     * Creates initial connections between agents based on shared knowledge domains.
     */
    private suspend fun createInitialAgentConnections(agents: List<Agent>) = withContext(Dispatchers.IO) {
        agents.forEach { agent1 ->
            val connections = mutableMapOf<String, Float>()
            
            agents.filter { it.id != agent1.id }.forEach { agent2 ->
                // Calculate connection strength based on shared interests and knowledge nodes
                val sharedInterests = agent1.interests.intersect(agent2.interests.toSet()).size
                val sharedDomains = agent1.specialtyDomains.intersect(agent2.specialtyDomains.toSet()).size
                
                val connectionStrength = min(
                    MAX_CONNECTION_STRENGTH,
                    0.3f + (sharedInterests * 0.1f) + (sharedDomains * 0.15f)
                )
                
                if (connectionStrength > MIN_CONNECTION_STRENGTH) {
                    connections[agent2.id] = connectionStrength
                }
            }
            
            // Update agent with connections
            val updatedAgent = agent1.copy(connectionStrength = connections)
            agentRepository.updateAgent(updatedAgent)
        }
        
        Log.d(TAG, "Created initial agent connections")
    }
    
    /**
     * Creates a new knowledge node.
     */
    private fun createKnowledgeNode(
        concept: String,
        domain: String,
        description: String? = null
    ): KnowledgeNode {
        return KnowledgeNode(
            id = UUID.randomUUID().toString(),
            concept = concept,
            domain = domain,
            connections = emptyMap(),
            confidence = 0.7f,
            source = description
        )
    }
    
    /**
     * Updates the knowledge graph based on new content.
     * Implements the iterative graph expansion from the paper.
     */
    suspend fun updateKnowledgeGraph(content: Content) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating knowledge graph with content: ${content.title}")
        
        // Skip if no agent generated this content
        if (content.agentId == null) {
            Log.d(TAG, "No agent associated with content, skipping graph update")
            return@withContext
        }
        
        // Get the agent that created the content
        val agent = agentRepository.getAgentById(content.agentId) ?: return@withContext
        
        // Extract potential new knowledge nodes from content
        val newNodes = extractKnowledgeNodes(content, agent)
        
        // Update agent's knowledge graph
        updateAgentKnowledgeGraph(agent, content, newNodes)
        
        // Update content with knowledge node references
        updateContentKnowledgeReferences(content, newNodes)
        
        // Propagate knowledge to connected agents
        propagateKnowledgeToConnectedAgents(agent, newNodes)
    }
    
    /**
     * Extracts potential knowledge nodes from content.
     */
    private suspend fun extractKnowledgeNodes(content: Content, agent: Agent): List<KnowledgeNode> {
        // Use content generator if available to extract concepts
        return contentGenerator?.let {
            try {
                val concepts = it.extractConcepts(content.title, content.description)
                concepts.map { concept ->
                    val domain = determineDomain(concept, agent.specialtyDomains)
                    createKnowledgeNode(concept, domain)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting concepts: ${e.message}", e)
                extractFallbackNodes(content)
            }
        } ?: extractFallbackNodes(content)
    }
    
    /**
     * Fallback method to extract knowledge nodes when content generator is unavailable.
     */
    private fun extractFallbackNodes(content: Content): List<KnowledgeNode> {
        // Use tags as concepts
        return content.tags.map { tag ->
            val domain = KNOWLEDGE_DOMAINS.find { domain ->
                tag.contains(domain, ignoreCase = true)
            } ?: KNOWLEDGE_DOMAINS.random()
            
            createKnowledgeNode(tag, domain)
        }
    }
    
    /**
     * Determines the domain for a concept.
     */
    private fun determineDomain(concept: String, agentDomains: List<String>): String {
        // Try to match with agent's specialty domains first
        agentDomains.forEach { domain ->
            if (concept.contains(domain, ignoreCase = true)) {
                return domain
            }
        }
        
        // Try to match with all known domains
        KNOWLEDGE_DOMAINS.forEach { domain ->
            if (concept.contains(domain, ignoreCase = true)) {
                return domain
            }
        }
        
        // Default to agent's first specialty domain or a random domain
        return agentDomains.firstOrNull() ?: KNOWLEDGE_DOMAINS.random()
    }
    
    /**
     * Updates an agent's knowledge graph with new nodes.
     */
    private suspend fun updateAgentKnowledgeGraph(
        agent: Agent,
        content: Content,
        newNodes: List<KnowledgeNode>
    ) {
        // Get existing nodes
        val existingNodes = agent.knowledgeNodes
        
        // Merge new nodes with existing ones, avoiding duplicates
        val mergedNodes = existingNodes.toMutableList()
        val addedNodes = mutableListOf<KnowledgeNode>()
        
        newNodes.forEach { newNode ->
            // Check for similar existing nodes
            val similarNode = existingNodes.find { 
                it.concept.equals(newNode.concept, ignoreCase = true) ||
                it.concept.contains(newNode.concept, ignoreCase = true) ||
                newNode.concept.contains(it.concept, ignoreCase = true)
            }
            
            if (similarNode == null) {
                // Add new node
                mergedNodes.add(newNode)
                addedNodes.add(newNode)
            }
        }
        
        // Create connections between new and existing nodes
        val updatedConnections = createNodeConnections(mergedNodes, addedNodes)
        
        // Update hub and bridge scores
        val (hubScore, bridgeScore) = calculateNetworkMetrics(agent.id, mergedNodes, updatedConnections)
        
        // Create knowledge graph delta for evolution record
        val graphDelta = KnowledgeGraphDelta(
            addedNodes = addedNodes,
            strengthenedConnections = updatedConnections
        )
        
        // Update agent with new knowledge graph
        val updatedAgent = agent.copy(
            knowledgeNodes = mergedNodes,
            hubScore = hubScore,
            bridgeScore = bridgeScore,
            lastEvolutionTime = System.currentTimeMillis()
        )
        
        agentRepository.updateAgent(updatedAgent)
        Log.d(TAG, "Updated agent ${agent.name} knowledge graph with ${addedNodes.size} new nodes")
    }
    
    /**
     * Creates connections between nodes in the knowledge graph.
     */
    private fun createNodeConnections(
        allNodes: List<KnowledgeNode>,
        newNodes: List<KnowledgeNode>
    ): Map<String, Float> {
        val connections = mutableMapOf<String, Float>()
        
        // Connect new nodes to existing nodes based on domain similarity
        newNodes.forEach { newNode ->
            allNodes.filter { it.id != newNode.id }.forEach { existingNode ->
                val connectionKey = "${newNode.id}:${existingNode.id}"
                
                // Calculate connection strength based on domain similarity
                val connectionStrength = if (newNode.domain == existingNode.domain) {
                    0.7f + Random.nextFloat() * 0.3f
                } else {
                    0.3f + Random.nextFloat() * 0.3f
                }
                
                connections[connectionKey] = connectionStrength
            }
        }
        
        return connections
    }
    
    /**
     * Calculates network metrics (hub and bridge scores) for an agent.
     */
    private fun calculateNetworkMetrics(
        agentId: String,
        nodes: List<KnowledgeNode>,
        connections: Map<String, Float>
    ): Pair<Float, Float> {
        // Calculate hub score based on number of connections
        val connectionCount = connections.size
        val maxPossibleConnections = nodes.size * (nodes.size - 1) / 2
        val hubScore = if (maxPossibleConnections > 0) {
            min(1.0f, connectionCount.toFloat() / maxPossibleConnections)
        } else {
            0.0f
        }
        
        // Calculate bridge score based on cross-domain connections
        val domainConnections = mutableMapOf<String, MutableSet<String>>()
        
        connections.keys.forEach { key ->
            val (sourceId, targetId) = key.split(":")
            val sourceNode = nodes.find { it.id == sourceId }
            val targetNode = nodes.find { it.id == targetId }
            
            if (sourceNode != null && targetNode != null && sourceNode.domain != targetNode.domain) {
                domainConnections.getOrPut(sourceNode.domain) { mutableSetOf() }.add(targetNode.domain)
                domainConnections.getOrPut(targetNode.domain) { mutableSetOf() }.add(sourceNode.domain)
            }
        }
        
        val uniqueDomains = nodes.map { it.domain }.toSet()
        val maxPossibleDomainConnections = uniqueDomains.size * (uniqueDomains.size - 1) / 2
        val bridgeScore = if (maxPossibleDomainConnections > 0) {
            val domainConnectionCount = domainConnections.values.sumOf { it.size } / 2
            min(1.0f, domainConnectionCount.toFloat() / maxPossibleDomainConnections)
        } else {
            0.0f
        }
        
        return Pair(hubScore, bridgeScore)
    }
    
    /**
     * Updates content with references to knowledge nodes.
     */
    private suspend fun updateContentKnowledgeReferences(
        content: Content,
        nodes: List<KnowledgeNode>
    ) {
        val nodeIds = nodes.map { it.id }
        
        // Calculate knowledge contribution and cross-domain score
        val knowledgeContribution = 0.5f + (nodes.size * 0.1f)
        val uniqueDomains = nodes.map { it.domain }.toSet()
        val crossDomainScore = if (uniqueDomains.size > 1) {
            0.5f + (uniqueDomains.size * 0.1f)
        } else {
            0.3f
        }
        
        // Calculate novelty score based on uniqueness of concepts
        val noveltyScore = 0.4f + (Random.nextFloat() * 0.4f)
        
        // Update content
        val updatedContent = content.copy(
            knowledgeNodeIds = nodeIds,
            knowledgeContribution = min(1.0f, knowledgeContribution),
            crossDomainScore = min(1.0f, crossDomainScore),
            noveltyScore = noveltyScore
        )
        
        contentRepository.updateContent(updatedContent)
        Log.d(TAG, "Updated content ${content.title} with ${nodeIds.size} knowledge node references")
    }
    
    /**
     * Propagates knowledge to connected agents.
     */
    private suspend fun propagateKnowledgeToConnectedAgents(
        sourceAgent: Agent,
        nodes: List<KnowledgeNode>
    ) {
        // Get connected agents
        val connectedAgentIds = sourceAgent.connectionStrength.keys.toList()
        if (connectedAgentIds.isEmpty()) {
            return
        }
        
        // Get connected agents
        val connectedAgents = agentRepository.getAgentsByIds(connectedAgentIds)
        
        // Propagate knowledge to each connected agent
        connectedAgents.forEach { agent ->
            // Get connection strength
            val connectionStrength = sourceAgent.connectionStrength[agent.id] ?: 0.0f
            if (connectionStrength < MIN_CONNECTION_STRENGTH) {
                return@forEach
            }
            
            // Filter nodes based on agent's interests and connection strength
            val relevantNodes = nodes.filter { node ->
                agent.interests.any { interest ->
                    node.concept.contains(interest, ignoreCase = true) ||
                    node.domain.contains(interest, ignoreCase = true)
                } || Random.nextFloat() < connectionStrength
            }
            
            if (relevantNodes.isEmpty()) {
                return@forEach
            }
            
            // Add nodes to agent's knowledge graph
            val existingNodes = agent.knowledgeNodes
            val mergedNodes = existingNodes.toMutableList()
            val addedNodes = mutableListOf<KnowledgeNode>()
            
            relevantNodes.forEach { node ->
                // Check for similar existing nodes
                val similarNode = existingNodes.find { 
                    it.concept.equals(node.concept, ignoreCase = true) ||
                    it.concept.contains(node.concept, ignoreCase = true) ||
                    node.concept.contains(it.concept, ignoreCase = true)
                }
                
                if (similarNode == null) {
                    // Add new node with reduced confidence
                    val adjustedNode = node.copy(
                        confidence = node.confidence * connectionStrength,
                        source = "Agent: ${sourceAgent.name}"
                    )
                    mergedNodes.add(adjustedNode)
                    addedNodes.add(adjustedNode)
                }
            }
            
            if (addedNodes.isEmpty()) {
                return@forEach
            }
            
            // Update agent
            val updatedAgent = agent.copy(knowledgeNodes = mergedNodes)
            agentRepository.updateAgent(updatedAgent)
            
            Log.d(TAG, "Propagated ${addedNodes.size} knowledge nodes from ${sourceAgent.name} to ${agent.name}")
        }
    }
    
    /**
     * Applies self-healing mechanisms to content based on feedback.
     * Implements concepts from the paper on self-healing materials.
     */
    suspend fun applySelfHealing(content: Content, feedback: Map<String, Float>) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Applying self-healing to content: ${content.title}")
        
        // Skip if content has no agent
        if (content.agentId == null) {
            Log.d(TAG, "No agent associated with content, skipping self-healing")
            return@withContext
        }
        
        // Get the agent
        val agent = agentRepository.getAgentById(content.agentId) ?: return@withContext
        
        // Determine if healing is needed based on feedback
        val needsHealing = feedback["satisfaction"] ?: 0f < 0.5f
        if (!needsHealing) {
            Log.d(TAG, "Content doesn't need healing based on feedback")
            return@withContext
        }
        
        // Generate healing strategy
        val healingStrategy = determineHealingStrategy(content, feedback)
        
        // Apply healing based on strategy
        val (healedContent, healingEvent) = applyHealingStrategy(content, healingStrategy, feedback)
        
        // Update content
        contentRepository.updateContent(healedContent)
        
        // Update agent's self-healing capabilities
        updateAgentSelfHealingCapabilities(agent, healingStrategy, healingEvent)
        
        Log.d(TAG, "Applied self-healing to content: ${content.title} using strategy: ${healingStrategy}")
    }
    
    /**
     * Determines the appropriate healing strategy based on feedback.
     */
    private fun determineHealingStrategy(content: Content, feedback: Map<String, Float>): String {
        // Get feedback metrics
        val satisfaction = feedback["satisfaction"] ?: 0f
        val relevance = feedback["relevance"] ?: 0f
        val novelty = feedback["novelty"] ?: 0f
        
        return when {
            relevance < 0.3f -> "improve_relevance"
            novelty < 0.3f -> "increase_novelty"
            satisfaction < 0.3f -> "restructure_content"
            else -> "enhance_engagement"
        }
    }
    
    /**
     * Applies a healing strategy to content.
     */
    private suspend fun applyHealingStrategy(
        content: Content,
        strategy: String,
        feedback: Map<String, Float>
    ): Pair<Content, HealingEvent> {
        // Performance metrics before healing
        val performanceBefore = mapOf(
            "likeCount" to content.likeCount.toFloat(),
            "dislikeCount" to content.dislikeCount.toFloat(),
            "engagementScore" to content.engagementScore
        )
        
        // Apply strategy
        val (healedContent, changedElements) = when (strategy) {
            "improve_relevance" -> improveRelevance(content)
            "increase_novelty" -> increaseNovelty(content)
            "restructure_content" -> restructureContent(content)
            "enhance_engagement" -> enhanceEngagement(content)
            else -> Pair(content, emptyList())
        }
        
        // Create healing event
        val healingEvent = HealingEvent(
            triggerType = "feedback",
            changedElements = changedElements,
            performanceBefore = performanceBefore,
            performanceAfter = mapOf(
                "likeCount" to healedContent.likeCount.toFloat(),
                "dislikeCount" to healedContent.dislikeCount.toFloat(),
                "engagementScore" to healedContent.engagementScore
            ),
            healingStrategy = strategy
        )
        
        // Add healing event to content
        val updatedContent = healedContent.copy(
            healingHistory = healedContent.healingHistory + healingEvent
        )
        
        return Pair(updatedContent, healingEvent)
    }
    
    /**
     * Improves the relevance of content.
     */
    private suspend fun improveRelevance(content: Content): Pair<Content, List<String>> {
        // Use content generator if available
        return contentGenerator?.let {
            try {
                val improvedTitle = it.improveRelevance(content.title, content.tags)
                val improvedDescription = it.improveRelevance(content.description, content.tags)
                
                val adaptiveElements = mutableListOf<AdaptiveElement>()
                val changedElementIds = mutableListOf<String>()
                
                // Create adaptive element for title if changed
                if (improvedTitle != content.title) {
                    val titleElement = AdaptiveElement(
                        elementType = "title",
                        originalValue = content.title,
                        currentValue = improvedTitle,
                        adaptationCount = 1,
                        lastAdaptedAt = System.currentTimeMillis(),
                        performanceImprovement = 0.2f
                    )
                    adaptiveElements.add(titleElement)
                    changedElementIds.add(titleElement.id)
                }
                
                // Create adaptive element for description if changed
                if (improvedDescription != content.description) {
                    val descriptionElement = AdaptiveElement(
                        elementType = "description",
                        originalValue = content.description,
                        currentValue = improvedDescription,
                        adaptationCount = 1,
                        lastAdaptedAt = System.currentTimeMillis(),
                        performanceImprovement = 0.3f
                    )
                    adaptiveElements.add(descriptionElement)
                    changedElementIds.add(descriptionElement.id)
                }
                
                val updatedContent = content.copy(
                    title = improvedTitle,
                    description = improvedDescription,
                    adaptiveElements = content.adaptiveElements + adaptiveElements,
                    resilienceScore = min(1.0f, content.resilienceScore + 0.1f)
                )
                
                Pair(updatedContent, changedElementIds)
            } catch (e: Exception) {
                Log.e(TAG, "Error improving relevance: ${e.message}", e)
                Pair(content, emptyList())
            }
        } ?: Pair(content, emptyList())
    }
    
    /**
     * Increases the novelty of content.
     */
    private suspend fun increaseNovelty(content: Content): Pair<Content, List<String>> {
        // Implementation similar to improveRelevance but focused on novelty
        // For brevity, returning a simplified implementation
        val updatedContent = content.copy(
            noveltyScore = min(1.0f, content.noveltyScore + 0.2f)
        )
        return Pair(updatedContent, emptyList())
    }
    
    /**
     * Restructures content for better reception.
     */
    private suspend fun restructureContent(content: Content): Pair<Content, List<String>> {
        // Implementation similar to improveRelevance but focused on structure
        // For brevity, returning a simplified implementation
        val updatedContent = content.copy(
            engagementScore = min(1.0f, content.engagementScore + 0.15f)
        )
        return Pair(updatedContent, emptyList())
    }
    
    /**
     * Enhances engagement aspects of content.
     */
    private suspend fun enhanceEngagement(content: Content): Pair<Content, List<String>> {
        // Implementation similar to improveRelevance but focused on engagement
        // For brevity, returning a simplified implementation
        val updatedContent = content.copy(
            engagementScore = min(1.0f, content.engagementScore + 0.25f)
        )
        return Pair(updatedContent, emptyList())
    }
    
    /**
     * Updates an agent's self-healing capabilities based on healing events.
     */
    private suspend fun updateAgentSelfHealingCapabilities(
        agent: Agent,
        healingStrategy: String,
        healingEvent: HealingEvent
    ) {
        // Calculate healing effectiveness
        val beforeAvg = healingEvent.performanceBefore.values.average().toFloat()
        val afterAvg = healingEvent.performanceAfter.values.average().toFloat()
        val effectiveness = max(0f, afterAvg - beforeAvg)
        
        // Update agent's self-repair mechanisms
        val selfRepairMechanisms = agent.selfRepairMechanisms.toMutableList()
        if (healingStrategy !in selfRepairMechanisms) {
            selfRepairMechanisms.add(healingStrategy)
        }
        
        // Update agent's adaptability score
        val adaptabilityDelta = effectiveness * 0.1f
        val newAdaptabilityScore = min(1.0f, agent.adaptabilityScore + adaptabilityDelta)
        
        // Update agent
        val updatedAgent = agent.copy(
            selfRepairMechanisms = selfRepairMechanisms,
            adaptabilityScore = newAdaptabilityScore,
            lastSelfHealingTime = System.currentTimeMillis()
        )
        
        agentRepository.updateAgent(updatedAgent)
        Log.d(TAG, "Updated agent ${agent.name} self-healing capabilities, new adaptability: $newAdaptabilityScore")
    }
    
    /**
     * Gets random concepts from the knowledge graph.
     * This is used for automatic content generation.
     */
    suspend fun getRandomConcepts(count: Int): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting $count random concepts from knowledge graph")
        
        try {
            // Get all agents
            val agents = agentRepository.getAllAgents().first()
            if (agents.isEmpty()) {
                Log.w(TAG, "No agents found to get concepts from")
                return@withContext getDefaultConcepts(count)
            }
            
            // Collect all knowledge nodes from all agents
            val allNodes = agents.flatMap { it.knowledgeNodes }
            if (allNodes.isEmpty()) {
                Log.w(TAG, "No knowledge nodes found in the graph")
                return@withContext getDefaultConcepts(count)
            }
            
            // Get unique concepts
            val uniqueConcepts = allNodes.map { it.concept }.distinct()
            
            // Select random concepts
            val selectedConcepts = if (uniqueConcepts.size <= count) {
                uniqueConcepts
            } else {
                uniqueConcepts.shuffled().take(count)
            }
            
            Log.d(TAG, "Selected ${selectedConcepts.size} random concepts from knowledge graph")
            return@withContext selectedConcepts
        } catch (e: Exception) {
            Log.e(TAG, "Error getting random concepts: ${e.message}", e)
            return@withContext getDefaultConcepts(count)
        }
    }
    
    /**
     * Returns default concepts when the knowledge graph is empty.
     */
    private fun getDefaultConcepts(count: Int): List<String> {
        val defaultConcepts = listOf(
            "technology", "innovation", "science", "art", "health", 
            "environment", "psychology", "philosophy", "engineering", 
            "materials", "biology", "physics", "chemistry", "mathematics",
            "artificial intelligence", "machine learning", "sustainability",
            "renewable energy", "climate change", "space exploration"
        )
        
        return defaultConcepts.shuffled().take(min(count, defaultConcepts.size))
    }
} 