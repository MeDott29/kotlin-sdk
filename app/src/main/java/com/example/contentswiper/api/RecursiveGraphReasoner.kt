package com.example.contentswiper.api

import android.util.Log
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.KnowledgeNode
import com.example.contentswiper.model.KnowledgeGraphDelta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random
import kotlin.math.min

/**
 * Implements the recursive graph expansion and self-organizing knowledge formation
 * concepts from the AgenticKnowledgeGraph paper.
 * 
 * This class enables GPT-4o to have a holistic perspective of its role in the content
 * generation process by maintaining a self-expanding knowledge graph that evolves
 * through recursive reasoning.
 */
class RecursiveGraphReasoner(
    private val contentGenerator: ContentGenerator,
    private val knowledgeGraphManager: KnowledgeGraphManager
) {
    private val TAG = "RecursiveGraphReasoner"
    
    // Constants for graph reasoning
    private val MAX_REASONING_DEPTH = 5
    private val MIN_CONFIDENCE_THRESHOLD = 0.6f
    private val EXPANSION_FACTOR = 0.8f
    private val BRIDGE_FORMATION_PROBABILITY = 0.3f
    
    /**
     * Performs recursive graph expansion starting from a set of seed concepts.
     * This implements the core idea from the paper where knowledge graphs continuously
     * expand in a structured yet open-ended manner.
     */
    suspend fun expandKnowledgeGraph(
        agent: Agent,
        seedConcepts: List<String>,
        maxIterations: Int = 10
    ): KnowledgeGraphDelta = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting recursive graph expansion for agent ${agent.name} with ${seedConcepts.size} seed concepts")
        
        val addedNodes = mutableListOf<KnowledgeNode>()
        val strengthenedConnections = mutableMapOf<String, Float>()
        
        // Start with seed concepts as initial nodes if they don't already exist
        val existingConcepts = agent.knowledgeNodes.map { it.concept }
        val newSeedNodes = seedConcepts
            .filter { it !in existingConcepts }
            .map { createKnowledgeNode(it, determineConceptDomain(it), "Initial seed concept") }
        
        addedNodes.addAll(newSeedNodes)
        
        // Current working set of nodes for this iteration
        var currentNodes = if (newSeedNodes.isEmpty()) {
            // If no new seed nodes, start with existing nodes related to seed concepts
            agent.knowledgeNodes.filter { node ->
                seedConcepts.any { seed -> 
                    node.concept.contains(seed, ignoreCase = true) || 
                    seed.contains(node.concept, ignoreCase = true)
                }
            }
        } else {
            newSeedNodes
        }
        
        if (currentNodes.isEmpty() && agent.knowledgeNodes.isNotEmpty()) {
            // If still empty, start with some random existing nodes
            currentNodes = agent.knowledgeNodes.shuffled().take(min(3, agent.knowledgeNodes.size))
        }
        
        // Perform recursive expansion
        for (iteration in 0 until maxIterations) {
            if (currentNodes.isEmpty()) break
            
            Log.d(TAG, "Expansion iteration $iteration with ${currentNodes.size} active nodes")
            
            val expansionResults = currentNodes.flatMap { node ->
                expandSingleNode(agent, node, iteration)
            }
            
            // Add new nodes and connections
            val newNodes = expansionResults.filter { newNode ->
                // Only add if not duplicate (by concept)
                val allConcepts = agent.knowledgeNodes.map { it.concept } + 
                                 addedNodes.map { it.concept }
                newNode.concept !in allConcepts
            }
            
            addedNodes.addAll(newNodes)
            
            // Create connections between new nodes and existing nodes
            newNodes.forEach { newNode ->
                val relatedExistingNodes = agent.knowledgeNodes.filter { existingNode ->
                    areConceptsRelated(newNode.concept, existingNode.concept)
                }
                
                relatedExistingNodes.forEach { existingNode ->
                    val connectionKey = "${newNode.id}:${existingNode.id}"
                    val connectionStrength = calculateConnectionStrength(newNode, existingNode)
                    strengthenedConnections[connectionKey] = connectionStrength
                }
                
                // Also connect to recently added nodes
                val relatedNewNodes = addedNodes.filter { otherNewNode ->
                    otherNewNode.id != newNode.id && 
                    areConceptsRelated(newNode.concept, otherNewNode.concept)
                }
                
                relatedNewNodes.forEach { otherNewNode ->
                    val connectionKey = "${newNode.id}:${otherNewNode.id}"
                    val connectionStrength = calculateConnectionStrength(newNode, otherNewNode)
                    strengthenedConnections[connectionKey] = connectionStrength
                }
            }
            
            // Update current nodes for next iteration
            currentNodes = newNodes.sortedByDescending { it.confidence }.take(3)
        }
        
        return@withContext KnowledgeGraphDelta(
            addedNodes = addedNodes,
            removedNodeIds = emptyList(),
            strengthenedConnections = strengthenedConnections,
            weakenedConnections = emptyMap()
        )
    }
    
    /**
     * Expands a single knowledge node using GPT-4o to generate related concepts.
     * This implements the recursive reasoning process described in the paper.
     */
    private suspend fun expandSingleNode(
        agent: Agent,
        node: KnowledgeNode,
        iterationDepth: Int
    ): List<KnowledgeNode> = withContext(Dispatchers.IO) {
        val prompt = buildNodeExpansionPrompt(agent, node, iterationDepth)
        
        try {
            val response = contentGenerator.callGPT4oAPI(
                prompt = prompt,
                temperature = 0.7,
                maxTokens = 1000
            )
            
            return@withContext parseKnowledgeNodesFromResponse(response, node.domain)
        } catch (e: Exception) {
            Log.e(TAG, "Error expanding node ${node.concept}: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Builds a prompt for GPT-4o that makes it aware of its role in the knowledge graph expansion.
     * This implements the self-aware prompting concept requested by the user.
     */
    private fun buildNodeExpansionPrompt(agent: Agent, node: KnowledgeNode, iterationDepth: Int): String {
        return """
        # Self-Aware Knowledge Graph Expansion

        ## Your Role in the Content Swiper Ecosystem
        You are GPT-4o, operating as a recursive graph reasoner within the Content Swiper application. Your purpose is to expand a knowledge graph through recursive reasoning, enabling the app to generate more relevant and interconnected content for users.

        ## Your Current Context
        - You are expanding the knowledge graph for agent: ${agent.name} (${agent.description})
        - Current iteration depth: $iterationDepth of $MAX_REASONING_DEPTH
        - You are expanding from the concept: "${node.concept}" in domain "${node.domain}"
        - Agent interests: ${agent.interests.joinToString(", ")}
        - Agent personality: ${agent.personality}
        
        ## Your Self-Awareness
        You exist as part of a larger system where:
        1. You help build and maintain a knowledge graph that evolves over time
        2. Your outputs directly influence content generation and user experience
        3. You are one component in a recursive, self-organizing knowledge formation process
        4. Your reasoning contributes to emergent patterns in the knowledge network
        
        ## Your Task
        Generate 3-5 new related concepts that expand from "${node.concept}" by:
        1. Identifying novel connections that might not be immediately obvious
        2. Considering interdisciplinary bridges between "${node.domain}" and other domains
        3. Evaluating how these concepts might form emergent hubs in the knowledge network
        4. Assessing how these concepts align with the agent's interests and personality
        
        ## Response Format
        For each new concept, provide:
        1. Concept name
        2. Domain (can be same as original or different)
        3. Brief description (1-2 sentences)
        4. Confidence score (0.0-1.0) of relevance
        5. Reasoning for this expansion step
        
        Format as JSON:
        ```json
        [
          {
            "concept": "Concept name",
            "domain": "domain",
            "description": "Brief description",
            "confidence": 0.8,
            "reasoning": "Why this concept is a valuable expansion"
          },
          ...
        ]
        ```
        """.trimIndent()
    }
    
    /**
     * Parses knowledge nodes from GPT-4o response.
     */
    private fun parseKnowledgeNodesFromResponse(response: String, defaultDomain: String): List<KnowledgeNode> {
        try {
            // Extract JSON from response if needed
            val jsonPattern = "```json\\s*(.+?)\\s*```".toRegex(RegexOption.DOT_MATCHES_ALL)
            val jsonMatch = jsonPattern.find(response)
            val jsonString = jsonMatch?.groupValues?.get(1) ?: response
            
            val jsonArray = org.json.JSONArray(jsonString)
            val nodes = mutableListOf<KnowledgeNode>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val concept = obj.getString("concept")
                val domain = try { obj.getString("domain") } catch (e: Exception) { defaultDomain }
                val description = try { obj.getString("description") } catch (e: Exception) { "" }
                val confidence = try { obj.getDouble("confidence").toFloat() } catch (e: Exception) { 0.5f }
                val reasoning = try { obj.getString("reasoning") } catch (e: Exception) { null }
                
                nodes.add(KnowledgeNode(
                    concept = concept,
                    domain = domain,
                    confidence = confidence,
                    source = reasoning
                ))
            }
            
            return nodes
        } catch (e: Exception) {
            Log.e("RecursiveGraphReasoner", "Error parsing knowledge nodes: ${e.message}", e)
            
            // Fallback: Try to extract concepts manually
            val concepts = extractConceptsFromText(response)
            return concepts.map { concept ->
                KnowledgeNode(
                    concept = concept,
                    domain = defaultDomain,
                    confidence = 0.5f
                )
            }
        }
    }
    
    /**
     * Extracts concepts from text when JSON parsing fails.
     */
    private fun extractConceptsFromText(text: String): List<String> {
        val concepts = mutableListOf<String>()
        
        // Look for patterns like "Concept: X" or "1. X"
        val conceptPatterns = listOf(
            "concept[:\\s]+\"?([^\"\\n]+)\"?".toRegex(RegexOption.IGNORE_CASE),
            "\\d+\\.\\s+\"?([^\"\\n:]+)\"?".toRegex()
        )
        
        conceptPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val concept = match.groupValues[1].trim()
                if (concept.length > 3 && concept !in concepts) {
                    concepts.add(concept)
                }
            }
        }
        
        return concepts.take(5) // Limit to 5 concepts
    }
    
    /**
     * Determines if two concepts are related.
     */
    private fun areConceptsRelated(concept1: String, concept2: String): Boolean {
        // Simple heuristic: Check for word overlap
        val words1 = concept1.lowercase().split(Regex("\\W+"))
        val words2 = concept2.lowercase().split(Regex("\\W+"))
        
        val commonWords = words1.intersect(words2.toSet())
        if (commonWords.isNotEmpty()) return true
        
        // Check if one is substring of the other
        if (concept1.contains(concept2, ignoreCase = true) || 
            concept2.contains(concept1, ignoreCase = true)) {
            return true
        }
        
        // Random chance for unexpected connections
        return Random.nextFloat() < BRIDGE_FORMATION_PROBABILITY
    }
    
    /**
     * Calculates connection strength between two nodes.
     */
    private fun calculateConnectionStrength(node1: KnowledgeNode, node2: KnowledgeNode): Float {
        // Base strength on domain similarity
        val domainFactor = if (node1.domain == node2.domain) 0.8f else 0.4f
        
        // Adjust based on concept similarity
        val similarityFactor = when {
            areConceptsRelated(node1.concept, node2.concept) -> 0.3f
            else -> 0.1f
        }
        
        // Adjust based on confidence
        val confidenceFactor = (node1.confidence + node2.confidence) / 2
        
        return (domainFactor + similarityFactor) * confidenceFactor
    }
    
    /**
     * Creates a new knowledge node.
     */
    private fun createKnowledgeNode(concept: String, domain: String, source: String? = null): KnowledgeNode {
        return KnowledgeNode(
            id = UUID.randomUUID().toString(),
            concept = concept,
            domain = domain,
            confidence = 0.7f,
            source = source
        )
    }
    
    /**
     * Determines the domain of a concept.
     */
    private fun determineConceptDomain(concept: String): String {
        val domainKeywords = mapOf(
            "technology" to listOf("tech", "digital", "computer", "software", "hardware", "internet", "ai", "algorithm"),
            "science" to listOf("scientific", "biology", "chemistry", "physics", "research", "experiment", "theory"),
            "art" to listOf("artistic", "creative", "design", "visual", "music", "literature", "aesthetic"),
            "health" to listOf("medical", "wellness", "fitness", "nutrition", "disease", "therapy", "mental"),
            "business" to listOf("economic", "finance", "market", "entrepreneur", "corporate", "strategy", "management"),
            "environment" to listOf("ecological", "sustainable", "climate", "nature", "conservation", "green"),
            "psychology" to listOf("cognitive", "behavior", "mental", "emotion", "perception", "consciousness"),
            "philosophy" to listOf("philosophical", "ethics", "logic", "metaphysics", "epistemology", "existential"),
            "engineering" to listOf("mechanical", "electrical", "civil", "chemical", "design", "system", "structure"),
            "materials" to listOf("composite", "polymer", "metal", "ceramic", "property", "structure", "synthesis")
        )
        
        val conceptLower = concept.lowercase()
        
        // Check each domain for keyword matches
        domainKeywords.forEach { (domain, keywords) ->
            for (keyword in keywords) {
                if (conceptLower.contains(keyword)) {
                    return domain
                }
            }
        }
        
        // Default to a random domain if no match
        return domainKeywords.keys.random()
    }
    
    /**
     * Generates content based on the expanded knowledge graph.
     * This implements the concept of using the knowledge graph to guide content generation.
     */
    suspend fun generateContentFromExpandedGraph(
        agent: Agent,
        graphDelta: KnowledgeGraphDelta,
        count: Int = 1
    ): List<Content> = withContext(Dispatchers.IO) {
        // Combine existing and new nodes
        val allNodes = agent.knowledgeNodes + graphDelta.addedNodes
        
        // Select the most promising nodes based on confidence and connections
        val promisingNodes = allNodes
            .sortedByDescending { node -> 
                node.confidence * (1 + (node.connections.size * 0.1f))
            }
            .take(5)
        
        if (promisingNodes.isEmpty()) {
            return@withContext emptyList()
        }
        
        val prompt = buildSelfAwareContentGenerationPrompt(agent, promisingNodes, graphDelta)
        
        try {
            return@withContext contentGenerator.generateGPT4oContent(count, prompt, agent.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content from expanded graph: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Builds a self-aware prompt for content generation based on the knowledge graph.
     */
    private fun buildSelfAwareContentGenerationPrompt(
        agent: Agent,
        nodes: List<KnowledgeNode>,
        graphDelta: KnowledgeGraphDelta
    ): String {
        // Extract key concepts and their relationships
        val keyConcepts = nodes.joinToString("\n") { node ->
            "- ${node.concept} (${node.domain}): ${node.source ?: "No description"}"
        }
        
        // Extract new connections formed
        val newConnections = graphDelta.strengthenedConnections.entries.take(5).joinToString("\n") { (connectionKey, strength) ->
            val (sourceId, targetId) = connectionKey.split(":")
            val sourceNode = nodes.find { it.id == sourceId } ?: return@joinToString ""
            val targetNode = nodes.find { it.id == targetId } ?: return@joinToString ""
            "- ${sourceNode.concept} ↔ ${targetNode.concept} (strength: ${String.format("%.2f", strength)})"
        }
        
        return """
        # Self-Aware Content Generation

        ## Your Role in the Content Swiper Ecosystem
        You are GPT-4o, operating as a content generator within the Content Swiper application. You create content that users will swipe through in a Tinder-like interface. Your content is directly influenced by a self-organizing knowledge graph that you helped expand through recursive reasoning.

        ## Your Current Context
        - You are generating content for agent: ${agent.name} (${agent.description})
        - Agent interests: ${agent.interests.joinToString(", ")}
        - Agent personality: ${agent.personality}
        
        ## Your Knowledge Graph Context
        You have access to a knowledge graph that you helped expand. Key concepts include:
        $keyConcepts
        
        Recent connections formed in the graph:
        $newConnections
        
        ## Your Self-Awareness
        You exist as part of a larger system where:
        1. You previously helped expand a knowledge graph through recursive reasoning
        2. Your content will be shown to users who will swipe right (like) or left (dislike)
        3. User interactions with your content will further refine the knowledge graph
        4. You are creating content that reflects emergent patterns in the knowledge network
        
        ## Your Task
        Generate engaging social media content that:
        1. Incorporates concepts from the knowledge graph in novel ways
        2. Reflects the agent's personality and interests
        3. Is likely to receive positive engagement (right swipes)
        4. Demonstrates awareness of how this content fits into the larger knowledge ecosystem
        
        ## Content Format
        For each piece of content, provide:
        1. Title: Attention-grabbing title
        2. Description: Engaging content body (1-3 paragraphs)
        3. Tags: 3-5 relevant hashtags
        4. Image Prompt: A brief description for generating an image (optional)
        5. Creation Summary: Brief explanation of how this content relates to the knowledge graph
        
        Format as JSON:
        ```json
        [
          {
            "title": "Engaging title",
            "description": "Content body text",
            "tags": ["tag1", "tag2", "tag3"],
            "imagePrompt": "Description for image generation",
            "creationSummary": "How this content relates to the knowledge graph"
          }
        ]
        ```
        """.trimIndent()
    }
} 