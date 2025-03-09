package com.example.contentswiper.api

import android.util.Log
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random
import kotlin.math.min

/**
 * Manages a network of simulated agents that act as users in the social network.
 * These agents can generate content, interact with each other, and form a social graph.
 * Enhanced with recursive graph reasoning based on the AgenticKnowledgeGraph paper.
 */
class AgentManager(
    private val agentRepository: AgentRepository,
    private val contentRepository: ContentRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val contentGenerator: ContentGenerator,
    private val knowledgeGraphManager: KnowledgeGraphManager
) {
    private val TAG = "AgentManager"
    
    // Create RecursiveGraphReasoner if we have the necessary dependencies
    private val recursiveGraphReasoner by lazy {
        RecursiveGraphReasoner(contentGenerator, knowledgeGraphManager)
    }
    
    /**
     * Creates a set of default agents with different personalities and interests.
     */
    suspend fun createDefaultAgents() = withContext(Dispatchers.IO) {
        val agents = listOf(
            Agent(
                name = "Tech Enthusiast",
                description = "Loves technology, gadgets, and innovation",
                interests = listOf("technology", "AI", "gadgets", "programming", "science"),
                personality = "Analytical and forward-thinking"
            ),
            Agent(
                name = "Creative Artist",
                description = "Passionate about art, design, and creative expression",
                interests = listOf("art", "design", "creativity", "photography", "music"),
                personality = "Imaginative and expressive"
            ),
            Agent(
                name = "Fitness Guru",
                description = "Dedicated to health, fitness, and wellness",
                interests = listOf("fitness", "health", "nutrition", "sports", "wellness"),
                personality = "Disciplined and energetic"
            ),
            Agent(
                name = "Travel Explorer",
                description = "Loves traveling, cultures, and adventures",
                interests = listOf("travel", "culture", "adventure", "food", "photography"),
                personality = "Curious and adventurous"
            ),
            Agent(
                name = "Business Professional",
                description = "Interested in business, finance, and professional development",
                interests = listOf("business", "finance", "leadership", "entrepreneurship", "economics"),
                personality = "Ambitious and strategic"
            ),
            Agent(
                name = "Science Enthusiast",
                description = "Fascinated by scientific discoveries and research",
                interests = listOf("science", "research", "astronomy", "biology", "physics"),
                personality = "Curious and analytical"
            ),
            Agent(
                name = "Mindfulness Coach",
                description = "Advocates for mental health and mindfulness practices",
                interests = listOf("mindfulness", "meditation", "mental health", "wellness", "psychology"),
                personality = "Calm and compassionate"
            ),
            Agent(
                name = "Foodie Explorer",
                description = "Passionate about culinary experiences and food culture",
                interests = listOf("food", "cooking", "restaurants", "recipes", "culinary arts"),
                personality = "Enthusiastic and adventurous"
            ),
            Agent(
                name = "Environmental Activist",
                description = "Dedicated to environmental causes and sustainability",
                interests = listOf("environment", "sustainability", "climate", "conservation", "eco-friendly"),
                personality = "Passionate and determined"
            ),
            Agent(
                name = "Literature Lover",
                description = "Enjoys reading and discussing literature and philosophy",
                interests = listOf("books", "literature", "philosophy", "writing", "poetry"),
                personality = "Thoughtful and reflective"
            )
        )
        
        agentRepository.insertAllAgents(agents)
        Log.d(TAG, "Created ${agents.size} default agents")
    }
    
    /**
     * Simulates agent interactions with a piece of content.
     * Enhanced with recursive graph reasoning for more realistic agent behavior.
     */
    suspend fun simulateAgentInteractions(content: Content, agentCount: Int = 5): Map<String, Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Simulating $agentCount agent interactions with content ${content.id}")
        
        // Get random agents
        val agents = agentRepository.getAllAgents().first().shuffled().take(agentCount)
        if (agents.isEmpty()) {
            Log.w(TAG, "No agents available for interaction simulation")
            return@withContext emptyMap()
        }
        
        val interactions = mutableMapOf<String, Boolean>()
        
        // For each agent, predict if they would like the content
        agents.forEach { agent ->
            val wouldLike = contentGenerator.predictAgentPreference(agent, content) ?: Random.nextBoolean()
            interactions[agent.id] = wouldLike
            
            // If the agent likes the content, expand their knowledge graph
            val reasoner = recursiveGraphReasoner
            if (wouldLike && reasoner != null) {
                try {
                    // Extract key concepts from content
                    val seedConcepts = extractKeyConceptsFromContent(content)
                    
                    // Expand the agent's knowledge graph based on these concepts
                    val graphDelta = reasoner.expandKnowledgeGraph(
                        agent = agent,
                        seedConcepts = seedConcepts,
                        maxIterations = 3 // Limit iterations for performance
                    )
                    
                    // Update the agent with the expanded knowledge graph
                    val updatedAgent = updateAgentWithExpandedGraph(agent, graphDelta)
                    agentRepository.updateAgent(updatedAgent)
                    
                    Log.d(TAG, "Expanded knowledge graph for agent ${agent.name} with ${graphDelta.addedNodes.size} new nodes")
                } catch (e: Exception) {
                    Log.e(TAG, "Error expanding knowledge graph for agent ${agent.name}: ${e.message}", e)
                }
            }
        }
        
        return@withContext interactions
    }
    
    /**
     * Extracts key concepts from content for knowledge graph expansion.
     */
    private fun extractKeyConceptsFromContent(content: Content): List<String> {
        val concepts = mutableListOf<String>()
        
        // Add tags as concepts
        concepts.addAll(content.tags)
        
        // Extract key phrases from title
        val titleWords = content.title.split(" ")
        if (titleWords.size >= 3) {
            // Add noun phrases from title
            for (i in 0 until titleWords.size - 1) {
                if (titleWords[i].length > 3 && titleWords[i+1].length > 3) {
                    concepts.add("${titleWords[i]} ${titleWords[i+1]}")
                }
            }
        }
        
        // Extract key phrases from description (simplified approach)
        val descriptionSentences = content.description.split(". ")
        descriptionSentences.forEach { sentence ->
            val words = sentence.split(" ")
            if (words.size >= 4) {
                // Look for potential concepts (simplified)
                for (i in 0 until words.size - 1) {
                    val word = words[i].trim(',', '.', '!', '?', '"', '\'')
                    val nextWord = words[i+1].trim(',', '.', '!', '?', '"', '\'')
                    
                    if (word.length > 4 && word[0].isUpperCase() && nextWord.length > 3) {
                        concepts.add("$word $nextWord")
                    }
                }
            }
        }
        
        // Deduplicate and limit
        return concepts.distinct().take(5)
    }
    
    /**
     * Updates an agent with an expanded knowledge graph.
     */
    private fun updateAgentWithExpandedGraph(
        agent: Agent,
        graphDelta: com.example.contentswiper.model.KnowledgeGraphDelta
    ): Agent {
        // Combine existing and new nodes
        val updatedNodes = agent.knowledgeNodes + graphDelta.addedNodes
        
        // Update connection strengths
        val updatedConnections = agent.connectionStrength.toMutableMap()
        graphDelta.strengthenedConnections.forEach { (key, strength) ->
            updatedConnections[key] = strength
        }
        
        // Calculate new hub and bridge scores
        val hubScore = calculateHubScore(updatedNodes, updatedConnections)
        val bridgeScore = calculateBridgeScore(updatedNodes, updatedConnections)
        
        // Update specialty domains based on new knowledge
        val domainCounts = updatedNodes.groupBy { it.domain }.mapValues { it.value.size }
        val specialtyDomains = domainCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        return agent.copy(
            knowledgeNodes = updatedNodes,
            connectionStrength = updatedConnections,
            hubScore = hubScore,
            bridgeScore = bridgeScore,
            specialtyDomains = specialtyDomains
        )
    }
    
    /**
     * Calculates a hub score for an agent based on their knowledge graph.
     * Hub nodes are central nodes with many connections.
     */
    private fun calculateHubScore(
        nodes: List<com.example.contentswiper.model.KnowledgeNode>,
        connections: Map<String, Float>
    ): Float {
        if (nodes.isEmpty()) return 0f
        
        // Count connections per node
        val nodeConnectionCounts = mutableMapOf<String, Int>()
        
        connections.keys.forEach { key ->
            val (sourceId, targetId) = key.split(":")
            nodeConnectionCounts[sourceId] = (nodeConnectionCounts[sourceId] ?: 0) + 1
            nodeConnectionCounts[targetId] = (nodeConnectionCounts[targetId] ?: 0) + 1
        }
        
        // Calculate average connections per node
        val totalConnections = nodeConnectionCounts.values.sum()
        val avgConnections = if (nodes.isNotEmpty()) totalConnections.toFloat() / nodes.size else 0f
        
        // Calculate hub score based on how many nodes have above-average connections
        val hubNodes = nodeConnectionCounts.count { it.value > avgConnections * 1.5 }
        
        return if (nodes.size > 0) {
            hubNodes.toFloat() / nodes.size
        } else {
            0f
        }
    }
    
    /**
     * Calculates a bridge score for an agent based on their knowledge graph.
     * Bridge nodes connect different domains of knowledge.
     */
    private fun calculateBridgeScore(
        nodes: List<com.example.contentswiper.model.KnowledgeNode>,
        connections: Map<String, Float>
    ): Float {
        if (nodes.isEmpty() || connections.isEmpty()) return 0f
        
        // Count cross-domain connections
        var crossDomainConnections = 0
        
        connections.keys.forEach { key ->
            val (sourceId, targetId) = key.split(":")
            val sourceNode = nodes.find { it.id == sourceId }
            val targetNode = nodes.find { it.id == targetId }
            
            if (sourceNode != null && targetNode != null && sourceNode.domain != targetNode.domain) {
                crossDomainConnections++
            }
        }
        
        // Calculate bridge score as ratio of cross-domain connections to total connections
        return crossDomainConnections.toFloat() / connections.size
    }
    
    /**
     * Generates content for an agent based on their expanded knowledge graph.
     * This implements the concept of using the knowledge graph to guide content generation.
     */
    suspend fun generateContentFromKnowledgeGraph(agent: Agent, count: Int = 1): List<Content> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating $count content items for agent ${agent.name} based on knowledge graph")
        
        val reasoner = recursiveGraphReasoner
        if (reasoner == null || agent.knowledgeNodes.isEmpty()) {
            Log.d(TAG, "No recursive graph reasoner or knowledge nodes, falling back to standard content generation")
            return@withContext contentGenerator.generateAgentContent(agent, count) ?: emptyList()
        }
        
        try {
            // Extract key concepts from the agent's knowledge graph
            val seedConcepts = agent.knowledgeNodes
                .sortedByDescending { it.confidence }
                .take(3)
                .map { it.concept }
            
            // Expand the knowledge graph with these seed concepts
            val graphDelta = reasoner.expandKnowledgeGraph(
                agent = agent,
                seedConcepts = seedConcepts,
                maxIterations = 2 // Limit iterations for performance
            )
            
            // Generate content based on the expanded graph
            val content = reasoner.generateContentFromExpandedGraph(
                agent = agent,
                graphDelta = graphDelta,
                count = count
            )
            
            // Update the agent with the expanded knowledge graph
            val updatedAgent = updateAgentWithExpandedGraph(agent, graphDelta)
            agentRepository.updateAgent(updatedAgent)
            
            return@withContext content
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content from knowledge graph: ${e.message}", e)
            return@withContext contentGenerator.generateAgentContent(agent, count) ?: emptyList()
        }
    }
    
    /**
     * Evolves an agent based on their content interactions.
     * This implements the concept of agent evolution from the paper.
     */
    suspend fun evolveAgentBasedOnInteractions(agentId: String): Agent? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Evolving agent $agentId based on content interactions")
        
        val agent = agentRepository.getAgentById(agentId) ?: return@withContext null
        
        // Check if it's time for evolution (e.g., after certain number of interactions)
        val timeSinceLastEvolution = System.currentTimeMillis() - agent.lastEvolutionTime
        val shouldEvolve = timeSinceLastEvolution > 24 * 60 * 60 * 1000 // 24 hours
        
        if (!shouldEvolve) {
            Log.d(TAG, "Not time for agent evolution yet")
            return@withContext agent
        }
        
        try {
            // Get agent's content preferences
            val likedContent = contentRepository.getLikedContentByAgent(agentId, 10)
            val dislikedContent = contentRepository.getDislikedContentByAgent(agentId, 10)
            
            if (likedContent.isEmpty() && dislikedContent.isEmpty()) {
                Log.d(TAG, "Insufficient interaction data for evolution")
                return@withContext agent
            }
            
            // Evolve the agent using the content generator
            val evolvedAgent = contentGenerator.evolveAgent(
                agent = agent,
                likedContent = likedContent,
                dislikedContent = dislikedContent
            ) ?: return@withContext agent
            
            // Update the agent in the repository
            agentRepository.updateAgent(evolvedAgent)
            
            Log.d(TAG, "Successfully evolved agent ${agent.name} to version ${evolvedAgent.version}")
            return@withContext evolvedAgent
        } catch (e: Exception) {
            Log.e(TAG, "Error evolving agent: ${e.message}", e)
            return@withContext agent
        }
    }
    
    /**
     * Generates content from agents in the network.
     * Each agent creates content based on their personality and interests.
     * Also tracks content generation for agent evolution.
     */
    suspend fun generateAgentNetworkContent(count: Int): List<Content> = withContext(Dispatchers.IO) {
        val agents = agentRepository.allActiveAgents.first()
        if (agents.isEmpty()) {
            Log.w(TAG, "No agents available to generate content")
            return@withContext emptyList()
        }
        
        Log.d(TAG, "Generating content from ${agents.size} agents")
        val contentList = mutableListOf<Content>()
        
        // Distribute content generation among agents
        val contentPerAgent = count / agents.size
        val remainingContent = count % agents.size
        
        for ((index, agent) in agents.withIndex()) {
            val agentContentCount = contentPerAgent + if (index < remainingContent) 1 else 0
            if (agentContentCount <= 0) continue
            
            try {
                val agentContent = if (contentGenerator != null) {
                    contentGenerator.generateAgentContent(agent, agentContentCount)
                } else {
                    emptyList()
                }
                
                if (agentContent.isNotEmpty()) {
                    contentList.addAll(agentContent)
                    
                    // Track content generation for this agent
                    trackAgentContentGeneration(agent.id)
                    
                    Log.d(TAG, "Agent ${agent.name} generated ${agentContent.size} content items")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating content for agent ${agent.name}: ${e.message}", e)
            }
        }
        
        // Save all generated content
        if (contentList.isNotEmpty()) {
            contentRepository.insertAllContent(contentList)
        }
        
        return@withContext contentList
    }
    
    /**
     * Simulates a complete social network cycle:
     * 1. Agents generate content
     * 2. Agents interact with each other's content
     * 3. Content popularity is calculated
     * 4. Agents evolve based on content performance
     */
    suspend fun simulateSocialNetworkCycle(contentCount: Int) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting social network simulation cycle")
        
        try {
            // Step 1: Generate content from agents
            val newContent = generateAgentNetworkContent(contentCount)
            if (newContent.isEmpty()) {
                Log.w(TAG, "No content was generated in this cycle")
                return@withContext
            }
            
            // Step 2: Agents interact with the new content
            for (content in newContent) {
                simulateAgentInteractions(content)
            }
            
            // Step 3: Calculate content popularity
            calculateContentPopularity(newContent)
            
            // Step 4: Simulate discussions between random pairs of agents
            simulateAgentDiscussions()
            
            // Step 5: Check for agents that are due for evolution
            checkAndEvolveAgents()
            
            Log.d(TAG, "Completed social network simulation cycle with ${newContent.size} content items")
        } catch (e: Exception) {
            Log.e(TAG, "Error in social network simulation cycle: ${e.message}", e)
        }
    }
    
    /**
     * Calculates the popularity of content based on agent interactions.
     */
    private suspend fun calculateContentPopularity(contentList: List<Content>) = withContext(Dispatchers.IO) {
        for (content in contentList) {
            try {
                val preferences = userPreferenceRepository.getContentPreferences(content.id).first()
                val likeCount = preferences.count { it.isLiked }
                val dislikeCount = preferences.size - likeCount
                
                // Update content metadata with popularity metrics
                val updatedContent = content.copy(
                    metadata = content.metadata + mapOf(
                        "likeCount" to likeCount.toString(),
                        "dislikeCount" to dislikeCount.toString(),
                        "popularity" to calculatePopularityScore(likeCount, dislikeCount).toString()
                    )
                )
                
                contentRepository.updateContent(updatedContent)
                Log.d(TAG, "Content ${content.title} popularity: ${likeCount} likes, ${dislikeCount} dislikes")
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating popularity for content ${content.id}: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calculates a popularity score based on likes and dislikes.
     */
    private fun calculatePopularityScore(likes: Int, dislikes: Int): Double {
        if (likes + dislikes == 0) return 0.0
        
        // Simple popularity formula: (likes - dislikes) / total interactions
        return (likes - dislikes).toDouble() / (likes + dislikes)
    }
    
    /**
     * Predicts whether an agent would like or dislike a piece of content.
     * This is a simplified version that uses tag matching instead of AI.
     */
    private suspend fun predictAgentPreference(agent: Agent, content: Content): Boolean = withContext(Dispatchers.IO) {
        // Simple algorithm: check if any of the agent's interests match the content's tags
        val matchingInterests = agent.interests.filter { interest ->
            content.tags.any { tag -> tag.equals(interest, ignoreCase = true) }
        }
        
        // If there are matching interests, the agent is more likely to like the content
        val likeChance = if (matchingInterests.isNotEmpty()) {
            0.7 + (matchingInterests.size * 0.1) // 70% base chance + 10% per matching interest
        } else {
            0.3 // 30% chance if no matching interests
        }
        
        val isLiked = Random.nextDouble() < likeChance
        
        Log.d(TAG, "Agent ${agent.name} ${if (isLiked) "liked" else "disliked"} content ${content.title} " +
                "(matching interests: ${matchingInterests.joinToString(", ")})")
        
        return@withContext isLiked
    }
    
    /**
     * Generates content for a specific agent locally as a fallback.
     */
    private fun generateLocalAgentContent(agent: Agent, count: Int): List<Content> {
        // Generate base content
        val baseContent = generateLocalContent(count * 2)
        
        // Filter and sort by agent interests
        return baseContent
            .sortedByDescending { content ->
                // Score content by how many agent interests it matches
                content.tags.count { tag -> agent.interests.any { it.equals(tag, ignoreCase = true) } }
            }
            .take(count)
            .map { content ->
                // Modify content to reflect agent's personality
                content.copy(
                    title = "${agent.name}'s Thoughts: ${content.title}",
                    generatedBy = agent.name,
                    metadata = content.metadata + mapOf(
                        "agentId" to agent.id,
                        "agentPersonality" to agent.personality
                    )
                )
            }
    }
    
    /**
     * Generates local content as a fallback.
     */
    private fun generateLocalContent(count: Int): List<Content> {
        val titles = listOf(
            "The Future of Technology",
            "Exploring Nature's Wonders",
            "Culinary Adventures Around the World",
            "Mindfulness in Daily Life",
            "The Art of Creative Writing",
            "Sustainable Living Tips",
            "Fitness Routines for Busy People",
            "Understanding World History",
            "Photography Basics for Beginners",
            "Music That Changed the World",
            "Space Exploration Milestones",
            "The Psychology of Happiness",
            "Architectural Marvels Around the Globe",
            "Emerging Trends in Fashion",
            "The Science of Sleep",
            "Legendary Sports Moments",
            "Artificial Intelligence Explained",
            "Gardening for Beginners",
            "Classic Literature Everyone Should Read",
            "The Evolution of Video Games"
        )
        
        val descriptions = listOf(
            "Discover how emerging technologies are shaping our future and changing the way we live, work, and interact with the world around us.",
            "Take a journey through some of the most breathtaking natural landscapes and learn about the diverse ecosystems that make our planet unique.",
            "Embark on a global food tour, exploring traditional dishes and innovative cuisine from different cultures and regions.",
            "Learn practical techniques to incorporate mindfulness into your everyday routine and improve your mental well-being.",
            "Unlock your creative potential with these writing tips and exercises designed to inspire and enhance your storytelling abilities.",
            "Explore simple and effective ways to reduce your environmental footprint and live a more eco-friendly lifestyle.",
            "Discover time-efficient workout routines that can be easily integrated into your busy schedule for maximum health benefits.",
            "Dive into key historical events that shaped our modern world and gain insights into how past civilizations have influenced our present.",
            "Master the fundamentals of photography with these beginner-friendly tips on composition, lighting, and camera settings.",
            "Explore influential music genres and artists who have left an indelible mark on culture and society throughout history.",
            "From the first steps on the Moon to the latest Mars rovers, explore humanity's greatest achievements in space exploration.",
            "Understand the science behind happiness and learn evidence-based strategies to increase your overall well-being and life satisfaction.",
            "Tour the world's most impressive architectural achievements, from ancient wonders to modern marvels of engineering and design.",
            "Stay ahead of the curve with insights into the latest fashion trends, sustainable practices, and innovative designers shaping the industry.",
            "Delve into the fascinating world of sleep science and discover how quality rest impacts every aspect of your physical and mental health.",
            "Relive the most iconic moments in sports history that transcended games to become cultural milestones and inspirational stories.",
            "Get a clear, jargon-free explanation of artificial intelligence, machine learning, and how these technologies are transforming our world.",
            "Start your gardening journey with these beginner-friendly tips for growing beautiful plants, vegetables, and creating your own green oasis.",
            "Explore timeless literary masterpieces that continue to resonate with readers across generations and cultural boundaries.",
            "Trace the evolution of video games from simple arcade experiences to complex interactive storytelling and technological marvels."
        )
        
        val tagSets = listOf(
            listOf("technology", "future", "innovation"),
            listOf("nature", "environment", "exploration"),
            listOf("food", "culture", "travel"),
            listOf("mindfulness", "wellness", "mental health"),
            listOf("writing", "creativity", "art"),
            listOf("sustainability", "environment", "lifestyle"),
            listOf("fitness", "health", "exercise"),
            listOf("history", "education", "civilization"),
            listOf("photography", "art", "technique"),
            listOf("music", "culture", "history"),
            listOf("space", "science", "exploration"),
            listOf("psychology", "happiness", "wellbeing"),
            listOf("architecture", "design", "travel"),
            listOf("fashion", "trends", "style"),
            listOf("sleep", "health", "science"),
            listOf("sports", "history", "achievement"),
            listOf("AI", "technology", "future"),
            listOf("gardening", "plants", "hobbies"),
            listOf("literature", "books", "classics"),
            listOf("gaming", "technology", "entertainment")
        )
        
        val contentList = mutableListOf<Content>()
        
        for (i in 0 until count) {
            val index = Random.nextInt(titles.size)
            val content = Content(
                id = UUID.randomUUID().toString(),
                title = titles[index],
                description = descriptions[index],
                imageUrl = "https://picsum.photos/500/300?random=${System.currentTimeMillis() + i}",
                tags = tagSets[index],
                generatedBy = "Local Generator",
                metadata = mapOf("local" to "true")
            )
            contentList.add(content)
        }
        
        return contentList
    }
    
    /**
     * Gets the number of active agents.
     */
    suspend fun getActiveAgentCount(): Int = withContext(Dispatchers.IO) {
        return@withContext agentRepository.getActiveAgentCount()
    }
    
    /**
     * Checks for agents that are due for evolution and evolves them based on their content performance.
     * Agents evolve when they've generated enough content or after a certain time period.
     */
    suspend fun checkAndEvolveAgents(
        contentThreshold: Int = 10,
        timeThreshold: Long = 24 * 60 * 60 * 1000 // 24 hours
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking for agents due for evolution")
        
        val agentsDueForEvolution = agentRepository.getAgentsDueForEvolution(contentThreshold, timeThreshold)
        if (agentsDueForEvolution.isEmpty()) {
            Log.d(TAG, "No agents due for evolution")
            return@withContext
        }
        
        Log.d(TAG, "Found ${agentsDueForEvolution.size} agents due for evolution")
        
        for (agent in agentsDueForEvolution) {
            try {
                evolveAgent(agent)
                Log.d(TAG, "Successfully evolved agent: ${agent.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error evolving agent ${agent.name}: ${e.message}", e)
            }
        }
    }
    
    /**
     * Evolves a single agent based on their content performance.
     * Uses GPT-4o to generate a new personality and interests if available,
     * otherwise uses a local evolution algorithm.
     */
    private suspend fun evolveAgent(agent: Agent) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Evolving agent: ${agent.name} (version ${agent.version})")
        
        // Get agent's content performance metrics
        val likeRate = contentRepository.getAgentContentLikeRate(agent.id)
        val engagementScore = contentRepository.getAverageEngagementScore(agent.id)
        
        // Update agent metrics
        agentRepository.updateAgentContentMetrics(agent.id, likeRate, engagementScore)
        
        // Get recent content for analysis
        val recentContent = contentRepository.getContentByAgentId(agent.id).first().take(10)
        
        if (contentGenerator != null && recentContent.isNotEmpty()) {
            // Use GPT-4o to evolve the agent
            evolveAgentWithGPT4o(agent, recentContent, likeRate, engagementScore)
        } else {
            // Use local evolution algorithm
            evolveAgentLocally(agent, likeRate, engagementScore)
        }
    }
    
    /**
     * Evolves an agent using GPT-4o to analyze content performance and generate
     * a new personality and interests.
     */
    private suspend fun evolveAgentWithGPT4o(
        agent: Agent,
        recentContent: List<Content>,
        likeRate: Float,
        engagementScore: Float
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Evolving agent with GPT-4o: ${agent.name}")
        
        try {
            // Build a prompt for GPT-4o
            val contentSummary = recentContent.joinToString("\n") { content ->
                "Title: ${content.title}\n" +
                "Description: ${content.description}\n" +
                "Tags: ${content.tags.joinToString(", ")}\n" +
                "Likes: ${content.likeCount}, Dislikes: ${content.dislikeCount}\n" +
                "Engagement: ${content.engagementScore}\n" +
                "Feedback: ${content.feedbackSummary ?: "None"}\n"
            }
            
            val prompt = """
                You are an AI that evolves social media personalities based on content performance.
                
                Current Agent Profile:
                Name: ${agent.name}
                Description: ${agent.description}
                Interests: ${agent.interests.joinToString(", ")}
                Personality: ${agent.personality}
                Current Version: ${agent.version}
                
                Content Performance Metrics:
                Like Rate: $likeRate
                Engagement Score: $engagementScore
                
                Recent Content:
                $contentSummary
                
                Based on this agent's content performance, evolve their personality and interests to improve engagement.
                The evolution should be logical and build upon their existing traits, not completely change them.
                
                Respond in JSON format with the following structure:
                {
                    "newPersonality": "Updated personality description",
                    "newInterests": ["interest1", "interest2", "interest3", "interest4", "interest5"],
                    "reason": "Explanation of why these changes were made based on content performance"
                }
            """.trimIndent()
            
            val response = contentGenerator?.callGPT4oAPI(prompt, temperature = 0.7, maxTokens = 500)
                ?: return@withContext // Return early if contentGenerator is null
            
            try {
                // Parse the JSON response
                val jsonResponse = org.json.JSONObject(response)
                val newPersonality = jsonResponse.getString("newPersonality")
                val newInterestsArray = jsonResponse.getJSONArray("newInterests")
                val reason = jsonResponse.getString("reason")
                
                val newInterests = mutableListOf<String>()
                for (i in 0 until newInterestsArray.length()) {
                    newInterests.add(newInterestsArray.getString(i))
                }
                
                // Create content metrics map
                val contentMetrics = mapOf(
                    "likeRate" to likeRate,
                    "engagementScore" to engagementScore,
                    "contentCount" to agent.contentGenerationCount.toFloat()
                )
                
                // Evolve the agent
                val evolvedAgent = agentRepository.evolveAgent(
                    agent = agent,
                    newPersonality = newPersonality,
                    newInterests = newInterests,
                    reason = reason,
                    contentMetrics = contentMetrics
                )
                
                Log.d(TAG, "Agent evolved: ${agent.name} v${agent.version} -> v${evolvedAgent.version}")
                Log.d(TAG, "New personality: ${evolvedAgent.personality}")
                Log.d(TAG, "New interests: ${evolvedAgent.interests.joinToString(", ")}")
                Log.d(TAG, "Evolution reason: $reason")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing GPT-4o evolution response: ${e.message}", e)
                // Fallback to local evolution
                evolveAgentLocally(agent, likeRate, engagementScore)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in GPT-4o agent evolution: ${e.message}", e)
            // Fallback to local evolution
            evolveAgentLocally(agent, likeRate, engagementScore)
        }
    }
    
    /**
     * Evolves an agent using a local algorithm based on content performance metrics.
     * This is a fallback when GPT-4o is not available.
     */
    private suspend fun evolveAgentLocally(
        agent: Agent,
        likeRate: Float,
        engagementScore: Float
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Evolving agent locally: ${agent.name}")
        
        // Determine if the agent should evolve in a positive or negative direction
        val isPositiveEvolution = likeRate >= 0.5f && engagementScore >= 0.5f
        
        // Generate a new personality based on performance
        val personalityModifier = if (isPositiveEvolution) {
            "more confident and engaging"
        } else {
            "more thoughtful and nuanced"
        }
        
        val newPersonality = "${agent.personality}, but ${personalityModifier}"
        
        // Modify interests based on performance
        val newInterests = agent.interests.toMutableList()
        
        // Add a new interest or replace a low-performing one
        val potentialNewInterests = listOf(
            "technology", "science", "art", "music", "literature", 
            "philosophy", "psychology", "health", "fitness", "travel",
            "food", "fashion", "business", "politics", "environment",
            "education", "history", "sports", "entertainment", "gaming"
        ).filter { it !in newInterests }
        
        if (potentialNewInterests.isNotEmpty()) {
            val randomInterest = potentialNewInterests.random()
            
            if (newInterests.size >= 5 && !isPositiveEvolution) {
                // Replace a random interest if not performing well
                newInterests[Random.nextInt(newInterests.size)] = randomInterest
            } else if (newInterests.size < 5) {
                // Add a new interest if we have less than 5
                newInterests.add(randomInterest)
            }
        }
        
        // Create reason for evolution
        val reason = if (isPositiveEvolution) {
            "Content is performing well with a like rate of $likeRate and engagement score of $engagementScore. " +
            "Enhancing the agent's strengths to maintain momentum."
        } else {
            "Content is underperforming with a like rate of $likeRate and engagement score of $engagementScore. " +
            "Adjusting the agent's approach to improve engagement."
        }
        
        // Create content metrics map
        val contentMetrics = mapOf(
            "likeRate" to likeRate,
            "engagementScore" to engagementScore,
            "contentCount" to agent.contentGenerationCount.toFloat()
        )
        
        // Evolve the agent
        val evolvedAgent = agentRepository.evolveAgent(
            agent = agent,
            newPersonality = newPersonality,
            newInterests = newInterests,
            reason = reason,
            contentMetrics = contentMetrics
        )
        
        Log.d(TAG, "Agent evolved locally: ${agent.name} v${agent.version} -> v${evolvedAgent.version}")
        Log.d(TAG, "New personality: ${evolvedAgent.personality}")
        Log.d(TAG, "New interests: ${evolvedAgent.interests.joinToString(", ")}")
        Log.d(TAG, "Evolution reason: $reason")
    }
    
    /**
     * Updates the content generation count for an agent and checks if they're due for evolution.
     */
    suspend fun trackAgentContentGeneration(agentId: String) = withContext(Dispatchers.IO) {
        agentRepository.incrementContentGenerationCount(agentId)
        
        // Check if the agent is due for evolution
        val agent = agentRepository.getAgentById(agentId)
        if (agent != null && agent.contentGenerationCount >= 10) {
            evolveAgent(agent)
        }
    }
    
    /**
     * Tracks content feedback and updates the content and agent metrics.
     */
    suspend fun trackContentFeedback(
        contentId: String,
        isLiked: Boolean,
        engagementScore: Float = 0.5f,
        feedback: String? = null
    ) = withContext(Dispatchers.IO) {
        val content = contentRepository.getContentById(contentId) ?: return@withContext
        
        // Update content metrics
        if (isLiked) {
            contentRepository.incrementLikeCount(contentId)
        } else {
            contentRepository.incrementDislikeCount(contentId)
        }
        
        contentRepository.updateEngagementScore(contentId, engagementScore)
        
        if (feedback != null) {
            contentRepository.updateFeedbackSummary(contentId, feedback)
        }
        
        // Update agent metrics if this content was created by an agent
        content.agentId?.let { agentId ->
            val likeRate = contentRepository.getAgentContentLikeRate(agentId)
            val avgEngagement = contentRepository.getAverageEngagementScore(agentId)
            agentRepository.updateAgentContentMetrics(agentId, likeRate, avgEngagement)
        }
    }
    
    /**
     * Simulates discussions between random pairs of agents.
     * These discussions influence agent evolution and future content generation.
     */
    suspend fun simulateAgentDiscussions(discussionCount: Int = 3) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Simulating discussions between random pairs of agents")
        
        try {
            val agents = agentRepository.allActiveAgents.first()
            if (agents.size < 2) {
                Log.w(TAG, "Not enough agents to simulate discussions")
                return@withContext
            }
            
            // Create random pairs of agents for discussions
            val agentPairs = mutableListOf<Pair<Agent, Agent>>()
            val shuffledAgents = agents.shuffled()
            
            // Create pairs until we have enough or run out of unique combinations
            var pairCount = 0
            var attempts = 0
            val maxAttempts = agents.size * agents.size // Avoid infinite loop
            
            while (pairCount < discussionCount && attempts < maxAttempts) {
                attempts++
                
                // Select two random agents
                val agent1 = shuffledAgents.random()
                val agent2 = shuffledAgents.filter { it.id != agent1.id }.randomOrNull() ?: continue
                
                // Check if this pair already exists (in either order)
                val pairExists = agentPairs.any { 
                    (it.first.id == agent1.id && it.second.id == agent2.id) || 
                    (it.first.id == agent2.id && it.second.id == agent1.id) 
                }
                
                if (!pairExists) {
                    agentPairs.add(Pair(agent1, agent2))
                    pairCount++
                }
            }
            
            Log.d(TAG, "Created ${agentPairs.size} agent discussion pairs")
            
            // Simulate discussions for each pair
            for ((agent1, agent2) in agentPairs) {
                try {
                    simulateDiscussionBetweenAgents(agent1, agent2)
                } catch (e: Exception) {
                    Log.e(TAG, "Error simulating discussion between ${agent1.name} and ${agent2.name}: ${e.message}", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error simulating agent discussions: ${e.message}", e)
        }
    }
    
    /**
     * Simulates a discussion between two specific agents.
     * Uses GPT-4o to generate the discussion if available, otherwise uses a local algorithm.
     */
    private suspend fun simulateDiscussionBetweenAgents(agent1: Agent, agent2: Agent) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Simulating discussion between ${agent1.name} and ${agent2.name}")
        
        try {
            // Find common interests between the agents
            val commonInterests = agent1.interests.filter { it in agent2.interests }
            
            // Find unique interests for each agent
            val agent1UniqueInterests = agent1.interests.filter { it !in agent2.interests }
            val agent2UniqueInterests = agent2.interests.filter { it !in agent1.interests }
            
            // Determine discussion topic
            val discussionTopic = if (commonInterests.isNotEmpty()) {
                // 70% chance to discuss a common interest
                if (Random.nextDouble() < 0.7) {
                    commonInterests.random()
                } else {
                    // 30% chance to discuss a unique interest from either agent
                    val combinedUniqueInterests = agent1UniqueInterests + agent2UniqueInterests
                    if (combinedUniqueInterests.isNotEmpty()) combinedUniqueInterests.random() else "general topics"
                }
            } else {
                // If no common interests, pick from either agent's interests
                val allInterests = agent1.interests + agent2.interests
                if (allInterests.isNotEmpty()) allInterests.random() else "general topics"
            }
            
            Log.d(TAG, "Discussion topic: $discussionTopic")
            
            if (contentGenerator != null) {
                // Use GPT-4o to simulate the discussion
                simulateDiscussionWithGPT4o(agent1, agent2, discussionTopic)
            } else {
                // Use local algorithm to simulate the discussion
                simulateDiscussionLocally(agent1, agent2, discussionTopic)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in agent discussion: ${e.message}", e)
        }
    }
    
    /**
     * Simulates a discussion between two agents using GPT-4o.
     */
    private suspend fun simulateDiscussionWithGPT4o(
        agent1: Agent, 
        agent2: Agent, 
        topic: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Simulating discussion with GPT-4o between ${agent1.name} and ${agent2.name} about $topic")
        
        try {
            val prompt = """
                Simulate a discussion between two social media users with the following profiles:
                
                User 1:
                Name: ${agent1.name}
                Description: ${agent1.description}
                Interests: ${agent1.interests.joinToString(", ")}
                Personality: ${agent1.personality}
                
                User 2:
                Name: ${agent2.name}
                Description: ${agent2.description}
                Interests: ${agent2.interests.joinToString(", ")}
                Personality: ${agent2.personality}
                
                Topic of discussion: $topic
                
                Simulate a brief but meaningful exchange between these two users. Then analyze how this discussion might influence each user's interests and personality.
                
                Respond in JSON format with the following structure:
                {
                    "discussion": "Brief transcript of their discussion",
                    "insights": {
                        "user1": {
                            "newInterests": ["potential new interest 1", "potential new interest 2"],
                            "personalityShift": "How this discussion might subtly shift their personality",
                            "contentIdeas": ["idea 1", "idea 2"]
                        },
                        "user2": {
                            "newInterests": ["potential new interest 1", "potential new interest 2"],
                            "personalityShift": "How this discussion might subtly shift their personality",
                            "contentIdeas": ["idea 1", "idea 2"]
                        }
                    }
                }
            """.trimIndent()
            
            val response = contentGenerator?.callGPT4oAPI(prompt, temperature = 0.8, maxTokens = 800)
                ?: return@withContext // Return early if contentGenerator is null
            
            try {
                // Parse the JSON response
                val jsonResponse = org.json.JSONObject(response)
                val discussion = jsonResponse.getString("discussion")
                val insights = jsonResponse.getJSONObject("insights")
                
                // Process insights for agent1
                val user1Insights = insights.getJSONObject("user1")
                val agent1NewInterests = mutableListOf<String>()
                val user1InterestsArray = user1Insights.getJSONArray("newInterests")
                for (i in 0 until user1InterestsArray.length()) {
                    agent1NewInterests.add(user1InterestsArray.getString(i))
                }
                val agent1PersonalityShift = user1Insights.getString("personalityShift")
                
                // Process insights for agent2
                val user2Insights = insights.getJSONObject("user2")
                val agent2NewInterests = mutableListOf<String>()
                val user2InterestsArray = user2Insights.getJSONArray("newInterests")
                for (i in 0 until user2InterestsArray.length()) {
                    agent2NewInterests.add(user2InterestsArray.getString(i))
                }
                val agent2PersonalityShift = user2Insights.getString("personalityShift")
                
                // Store discussion results for each agent
                storeDiscussionResults(
                    agent1.id, 
                    agent2.id, 
                    topic, 
                    discussion, 
                    agent1NewInterests, 
                    agent1PersonalityShift
                )
                
                storeDiscussionResults(
                    agent2.id, 
                    agent1.id, 
                    topic, 
                    discussion, 
                    agent2NewInterests, 
                    agent2PersonalityShift
                )
                
                Log.d(TAG, "Discussion between ${agent1.name} and ${agent2.name} completed")
                Log.d(TAG, "Discussion summary: ${discussion.take(100)}...")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing GPT-4o discussion response: ${e.message}", e)
                // Fallback to local discussion
                simulateDiscussionLocally(agent1, agent2, topic)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in GPT-4o discussion simulation: ${e.message}", e)
            // Fallback to local discussion
            simulateDiscussionLocally(agent1, agent2, topic)
        }
    }
    
    /**
     * Simulates a discussion between two agents using a local algorithm.
     * This is a fallback when GPT-4o is not available.
     */
    private suspend fun simulateDiscussionLocally(
        agent1: Agent, 
        agent2: Agent, 
        topic: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Simulating discussion locally between ${agent1.name} and ${agent2.name} about $topic")
        
        // Generate a simple discussion summary
        val discussionSummary = "A discussion between ${agent1.name} and ${agent2.name} about $topic. " +
                "They shared perspectives based on their different backgrounds and interests."
        
        // Determine potential new interests for each agent
        val agent1NewInterests = mutableListOf<String>()
        val agent2NewInterests = mutableListOf<String>()
        
        // Each agent has a chance to adopt an interest from the other
        if (Random.nextDouble() < 0.3) { // 30% chance
            val potentialNewInterest = agent2.interests.filter { it !in agent1.interests }.randomOrNull()
            if (potentialNewInterest != null) {
                agent1NewInterests.add(potentialNewInterest)
            }
        }
        
        if (Random.nextDouble() < 0.3) { // 30% chance
            val potentialNewInterest = agent1.interests.filter { it !in agent2.interests }.randomOrNull()
            if (potentialNewInterest != null) {
                agent2NewInterests.add(potentialNewInterest)
            }
        }
        
        // Generate simple personality shifts
        val agent1PersonalityShift = when {
            topic in agent1.interests -> "Became more confident about $topic"
            topic in agent2.interests -> "Gained new perspective on $topic"
            else -> "Slightly more open to new ideas"
        }
        
        val agent2PersonalityShift = when {
            topic in agent2.interests -> "Became more confident about $topic"
            topic in agent1.interests -> "Gained new perspective on $topic"
            else -> "Slightly more open to new ideas"
        }
        
        // Store discussion results for each agent
        storeDiscussionResults(
            agent1.id, 
            agent2.id, 
            topic, 
            discussionSummary, 
            agent1NewInterests, 
            agent1PersonalityShift
        )
        
        storeDiscussionResults(
            agent2.id, 
            agent1.id, 
            topic, 
            discussionSummary, 
            agent2NewInterests, 
            agent2PersonalityShift
        )
        
        Log.d(TAG, "Local discussion between ${agent1.name} and ${agent2.name} completed")
    }
    
    /**
     * Stores the results of a discussion for an agent.
     * These results will influence the agent's evolution.
     */
    private suspend fun storeDiscussionResults(
        agentId: String,
        otherAgentId: String,
        topic: String,
        discussionSummary: String,
        potentialNewInterests: List<String>,
        personalityShift: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Create a discussion record
            val discussionRecord = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "otherAgentId" to otherAgentId,
                "topic" to topic,
                "summary" to discussionSummary,
                "potentialNewInterests" to potentialNewInterests.joinToString(","),
                "personalityShift" to personalityShift
            )
            
            // Store the discussion record in the agent's metadata
            agentRepository.addDiscussionRecord(agentId, discussionRecord)
            
            Log.d(TAG, "Stored discussion results for agent $agentId")
            
            // Check if the agent should evolve based on discussions
            val agent = agentRepository.getAgentById(agentId)
            if (agent != null) {
                val discussionCount = agentRepository.getAgentDiscussionCount(agentId)
                
                // If the agent has participated in enough discussions, consider evolution
                if (discussionCount >= 3) {
                    Log.d(TAG, "Agent $agentId has participated in $discussionCount discussions, considering evolution")
                    checkAndEvolveAgentBasedOnDiscussions(agent)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error storing discussion results: ${e.message}", e)
        }
    }
    
    /**
     * Checks if an agent should evolve based on their discussion history.
     */
    private suspend fun checkAndEvolveAgentBasedOnDiscussions(agent: Agent) = withContext(Dispatchers.IO) {
        try {
            // Get the agent's discussion records
            val discussionRecords = agentRepository.getAgentDiscussionRecords(agent.id)
            if (discussionRecords.isEmpty()) {
                return@withContext
            }
            
            Log.d(TAG, "Checking if agent ${agent.name} should evolve based on ${discussionRecords.size} discussions")
            
            // Collect potential new interests from discussions
            val potentialNewInterests = mutableListOf<String>()
            val personalityShifts = mutableListOf<String>()
            
            for (record in discussionRecords) {
                val interests = record["potentialNewInterests"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                potentialNewInterests.addAll(interests)
                
                val shift = record["personalityShift"]
                if (!shift.isNullOrBlank()) {
                    personalityShifts.add(shift)
                }
            }
            
            // If there are potential new interests or personality shifts, evolve the agent
            if (potentialNewInterests.isNotEmpty() || personalityShifts.isNotEmpty()) {
                evolveAgentBasedOnDiscussions(agent, potentialNewInterests, personalityShifts)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking agent evolution based on discussions: ${e.message}", e)
        }
    }
    
    /**
     * Evolves an agent based on their discussion history.
     */
    private suspend fun evolveAgentBasedOnDiscussions(
        agent: Agent,
        potentialNewInterests: List<String>,
        personalityShifts: List<String>
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Evolving agent ${agent.name} based on discussions")
        
        try {
            // Process new interests
            val newInterests = agent.interests.toMutableList()
            
            // Add up to 2 new interests if there's room
            if (newInterests.size < 7) {
                val filteredNewInterests = potentialNewInterests
                    .filter { it !in newInterests }
                    .distinct()
                    .shuffled()
                    .take(2)
                
                newInterests.addAll(filteredNewInterests)
            }
            
            // Process personality shifts
            val personalityShift = if (personalityShifts.isNotEmpty()) {
                personalityShifts.random()
            } else {
                "Slightly evolved through social interactions"
            }
            
            val newPersonality = "${agent.personality}, now ${personalityShift.lowercase()}"
            
            // Create reason for evolution
            val reason = "Evolved through discussions with other agents, gaining new perspectives and interests."
            
            // Create content metrics map
            val contentMetrics = mapOf(
                "discussionCount" to agentRepository.getAgentDiscussionCount(agent.id).toFloat(),
                "learningFactor" to agent.learningFactor
            )
            
            // Evolve the agent
            val evolvedAgent = agentRepository.evolveAgent(
                agent = agent,
                newPersonality = newPersonality,
                newInterests = newInterests,
                reason = reason,
                contentMetrics = contentMetrics
            )
            
            // Clear processed discussion records
            agentRepository.clearDiscussionRecords(agent.id)
            
            Log.d(TAG, "Agent evolved through discussions: ${agent.name} v${agent.version} -> v${evolvedAgent.version}")
            Log.d(TAG, "New personality: ${evolvedAgent.personality}")
            Log.d(TAG, "New interests: ${evolvedAgent.interests.joinToString(", ")}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error evolving agent based on discussions: ${e.message}", e)
        }
    }
} 