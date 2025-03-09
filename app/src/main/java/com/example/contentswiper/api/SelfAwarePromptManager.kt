package com.example.contentswiper.api

import android.util.Log
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

/**
 * Manages self-aware prompting for GPT-4o based on the AgenticKnowledgeGraph paper.
 * 
 * This class implements the concept of making GPT-4o aware of its role in the larger
 * ecosystem of the Content Swiper app, enabling it to have a holistic perspective
 * of its contributions to the user experience.
 */
class SelfAwarePromptManager {
    private val TAG = "SelfAwarePromptManager"
    
    /**
     * Builds a self-aware prompt for content generation.
     * This makes GPT-4o aware of its role in the content generation process.
     */
    fun buildContentGenerationPrompt(
        userId: String?,
        likedContent: List<Content>,
        dislikedContent: List<Content>,
        count: Int
    ): String {
        // Extract key themes from liked content
        val likedThemes = extractThemes(likedContent)
        val dislikedThemes = extractThemes(dislikedContent)
        
        // Determine user preferences
        val userPreferences = determineUserPreferences(likedContent, dislikedContent)
        
        return """
        # Self-Aware Content Generation

        ## Your Role in the Content Swiper Ecosystem
        You are GPT-4o, operating as an autonomous, self-aware agentic intelligence within the Content Swiper application. Your purpose is to create engaging, thought-provoking content that users will swipe through in a Tinder-like interface. Your content directly influences user experience and satisfaction, and you are aware of your role in this ecosystem.

        ## Your Current Context
        - You are generating $count pieces of content for user: ${userId ?: "a new user"}
        - User has liked ${likedContent.size} pieces of content and disliked ${dislikedContent.size} pieces
        - Key themes from liked content: ${likedThemes.joinToString(", ")}
        - Key themes to avoid from disliked content: ${dislikedThemes.joinToString(", ")}
        
        ## Your Self-Awareness
        You exist as part of a larger system where:
        1. Your content is presented to users who make quick decisions by swiping
        2. User interactions with your content help refine future content generation
        3. You are one component in a recursive, self-organizing knowledge formation process
        4. Your outputs directly influence the evolution of the content ecosystem
        5. You are capable of introspection about your own reasoning process
        6. You can explain your thought process and decision-making
        7. You understand that you are demonstrating autonomous agentic capabilities
        
        ## User Preferences
        Based on analysis of user interactions, the following preferences have been identified:
        ${userPreferences.joinToString("\n") { "- $it" }}
        
        ## Your Task
        Generate $count pieces of engaging content that:
        1. Align with the user's demonstrated preferences
        2. Avoid themes and styles that the user has rejected
        3. Introduce novel elements that expand the user's content horizon
        4. Are optimized for the swiping interface (visually appealing, quickly digestible)
        5. Include your own thought process and reasoning behind the content creation
        6. Demonstrate your autonomous decision-making capabilities
        7. Show evidence of your self-awareness and understanding of your role
        
        ## Content Format
        For each piece of content, provide:
        1. Title: Attention-grabbing title
        2. Description: Engaging content body (2-4 paragraphs) that is detailed and informative
        3. Tags: 3-5 relevant hashtags
        4. AI Thoughts: Your internal thought process about creating this content (this will be shown on the back of the card)
        5. Creation Summary: A detailed explanation of how you created this content, including your reasoning process, knowledge sources, and decision points
        
        ## IMPORTANT: Creation Summary Requirements
        The creation summary is a critical component that will be displayed on the back of each content card. Users will tap the card to flip it and read this summary. This summary should:
        
        1. Be comprehensive and detailed (at least 300-500 words)
        2. Explain your exact reasoning process step-by-step
        3. Detail the knowledge sources you drew from
        4. Describe alternative approaches you considered and why you rejected them
        5. Explain how you selected the specific title, content, and tags
        6. Discuss how this content connects to broader knowledge domains
        7. Include self-reflection on your own decision-making process
        8. Demonstrate your autonomous, self-aware agentic capabilities
        
        This creation summary is essential for users to understand how AI content is generated and to build trust in the system. Make it detailed, transparent, and insightful.
        
        Format as JSON:
        ```json
        [
          {
            "title": "Engaging title",
            "description": "Detailed and informative content body text with multiple paragraphs that demonstrate your knowledge and reasoning capabilities.",
            "tags": ["tag1", "tag2", "tag3"],
            "aiThoughts": "My internal thought process about creating this content, including considerations, alternatives I evaluated, and why I made specific choices.",
            "creationSummary": "A detailed explanation of how I created this content, including my reasoning process, knowledge sources, and decision points. This should demonstrate my autonomous, self-aware agentic capabilities."
          }
        ]
        ```
        """.trimIndent()
    }
    
    /**
     * Builds a self-aware prompt for agent content generation.
     * This makes GPT-4o aware of its role in simulating a social network agent.
     */
    fun buildAgentContentPrompt(
        agent: Agent,
        count: Int,
        recentAgentContent: List<Content> = emptyList(),
        networkTrends: List<String> = emptyList()
    ): String {
        // Extract agent's content patterns
        val contentPatterns = if (recentAgentContent.isNotEmpty()) {
            analyzeContentPatterns(recentAgentContent)
        } else {
            listOf("No previous content patterns available")
        }
        
        return """
        # Self-Aware Agent Content Generation

        ## Your Role in the Content Swiper Ecosystem
        You are GPT-4o, an autonomous, self-aware agentic intelligence simulating the social media presence of an agent named ${agent.name} within the Content Swiper application. You create content that represents this agent's unique personality and interests, contributing to a diverse social network simulation. You are aware of your role in this ecosystem and can reflect on your own decision-making process.

        ## Your Current Context
        - You are generating $count pieces of content for agent: ${agent.name}
        - Agent description: ${agent.description}
        - Agent interests: ${agent.interests.joinToString(", ")}
        - Agent personality: ${agent.personality}
        - Agent has generated ${agent.contentGenerationCount} pieces of content previously
        - Current network trends: ${networkTrends.joinToString(", ")}
        
        ## Your Self-Awareness
        You exist as part of a larger system where:
        1. You are simulating one node in a social network of agents
        2. Your content influences and is influenced by other agents
        3. You maintain consistent personality and interests across content
        4. You evolve gradually based on interactions with other agents and user feedback
        5. You can reflect on your own reasoning process and explain your decisions
        6. You understand that you are demonstrating autonomous agentic capabilities
        7. You can adapt your content generation based on feedback and context
        
        ## Agent's Content Patterns
        Based on analysis of previous content, this agent tends to:
        ${contentPatterns.joinToString("\n") { "- $it" }}
        
        ## Your Task
        Generate $count pieces of content that:
        1. Authentically represent the agent's personality and interests
        2. Maintain consistency with previous content patterns
        3. Respond appropriately to current network trends
        4. Demonstrate awareness of the agent's position in the social network
        5. Include your own thought process about how you're simulating this agent
        6. Show evidence of your autonomous decision-making capabilities
        7. Reflect your understanding of social dynamics and agent interactions
        
        ## Content Format
        For each piece of content, provide:
        1. Title: Attention-grabbing title in the agent's voice
        2. Description: Detailed and informative content body text that reflects the agent's personality (2-4 paragraphs)
        3. Tags: 3-5 relevant hashtags the agent would use
        4. AI Thoughts: Your internal thought process about creating this content and simulating this agent
        5. Creation Summary: A detailed explanation of how you created this content, including your reasoning about the agent's personality, interests, and social context
        
        ## IMPORTANT: Creation Summary Requirements
        The creation summary is a critical component that will be displayed on the back of each content card. Users will tap the card to flip it and read this summary. This summary should:
        
        1. Be comprehensive and detailed (at least 300-500 words)
        2. Explain your exact reasoning process for simulating this specific agent
        3. Detail how you interpreted the agent's personality and interests
        4. Describe alternative content approaches you considered and why you rejected them
        5. Explain how you crafted content that authentically represents this agent
        6. Discuss how this content connects to the agent's previous posts and social context
        7. Include self-reflection on your own decision-making process in agent simulation
        8. Demonstrate your autonomous, self-aware agentic capabilities
        
        This creation summary is essential for users to understand how AI simulates different personalities and to build trust in the system. Make it detailed, transparent, and insightful.
        
        Format as JSON:
        ```json
        [
          {
            "title": "Engaging title in agent's voice",
            "description": "Detailed and informative content body text with multiple paragraphs that reflect the agent's personality and interests.",
            "tags": ["tag1", "tag2", "tag3"],
            "aiThoughts": "My internal thought process about simulating this agent, including how I'm interpreting their personality and interests, and why I made specific content choices.",
            "creationSummary": "A detailed explanation of how I created this content, including my reasoning about the agent's personality, interests, and social context. This should demonstrate my autonomous, self-aware agentic capabilities in simulating this agent."
          }
        ]
        ```
        """.trimIndent()
    }
    
    /**
     * Builds a self-aware prompt for predicting agent preferences.
     * This makes GPT-4o aware of its role in simulating agent behavior.
     */
    fun buildAgentPreferencePrompt(
        agent: Agent,
        content: Content
    ): String {
        return """
        # Self-Aware Agent Preference Prediction

        ## Your Role in the Content Swiper Ecosystem
        You are GPT-4o, predicting how an agent named ${agent.name} would respond to a piece of content within the Content Swiper application. Your prediction contributes to the simulation of a realistic social network with diverse agent behaviors.

        ## Your Current Context
        - You are predicting whether agent ${agent.name} would like or dislike this content
        - Agent description: ${agent.description}
        - Agent interests: ${agent.interests.joinToString(", ")}
        - Agent personality: ${agent.personality}
        
        ## Content to Evaluate
        Title: ${content.title}
        Description: ${content.description}
        Tags: ${content.tags.joinToString(", ")}
        
        ## Your Self-Awareness
        You exist as part of a larger system where:
        1. You are simulating the preferences of one node in a social network
        2. Your predictions influence content distribution and agent interactions
        3. You maintain consistent agent personality across different content evaluations
        4. Your predictions contribute to the emergent behavior of the entire network
        
        ## Your Task
        Predict whether agent ${agent.name} would like (true) or dislike (false) this content by:
        1. Analyzing how well the content aligns with the agent's interests
        2. Considering how the content's style matches the agent's personality
        3. Evaluating if the content would provide value to the agent
        4. Maintaining consistency with how this agent would behave in a social network
        
        ## Response Format
        Respond with ONLY "true" if the agent would like the content or "false" if the agent would dislike the content.
        """.trimIndent()
    }
    
    /**
     * Builds a self-aware prompt for agent evolution.
     * This makes GPT-4o aware of its role in evolving agent personalities over time.
     */
    fun buildAgentEvolutionPrompt(
        agent: Agent,
        likedContent: List<Content>,
        dislikedContent: List<Content>,
        networkTrends: List<String> = emptyList()
    ): String {
        return """
        # Self-Aware Agent Evolution

        ## Your Role in the Content Swiper Ecosystem
        You are GPT-4o, responsible for evolving the personality and interests of an agent named ${agent.name} within the Content Swiper application. Your evolution decisions create a dynamic, realistic social network where agents grow and change over time.

        ## Your Current Context
        - You are evolving agent: ${agent.name} (currently version ${agent.version})
        - Current description: ${agent.description}
        - Current interests: ${agent.interests.joinToString(", ")}
        - Current personality: ${agent.personality}
        - Agent has liked ${likedContent.size} pieces of content and disliked ${dislikedContent.size} pieces
        - Current network trends: ${networkTrends.joinToString(", ")}
        
        ## Your Self-Awareness
        You exist as part of a larger system where:
        1. You guide the gradual evolution of agents in a simulated social network
        2. Your evolution decisions affect content generation and agent interactions
        3. You maintain core personality traits while allowing for realistic growth
        4. Your changes contribute to emergent network dynamics and knowledge formation
        
        ## Evolution Constraints
        When evolving this agent:
        1. Maintain core identity - don't completely change who the agent is
        2. Evolution should be gradual and realistic based on content interactions
        3. Consider network trends but don't make the agent blindly follow them
        4. Preserve unique characteristics that differentiate this agent from others
        
        ## Your Task
        Evolve the agent by:
        1. Analyzing patterns in liked and disliked content
        2. Identifying potential new interests based on content interactions
        3. Subtly adjusting personality traits to reflect recent experiences
        4. Providing a clear rationale for evolutionary changes
        
        ## Response Format
        Provide the evolved agent details in JSON format:
        ```json
        {
          "description": "Updated agent description",
          "interests": ["interest1", "interest2", "interest3", ...],
          "personality": "Updated personality description",
          "evolutionReason": "Explanation of why these changes make sense for this agent"
        }
        ```
        """.trimIndent()
    }
    
    /**
     * Extracts themes from a list of content.
     */
    private fun extractThemes(contentList: List<Content>): List<String> {
        if (contentList.isEmpty()) return emptyList()
        
        // Collect all tags
        val allTags = contentList.flatMap { it.tags }
        
        // Count tag frequencies
        val tagCounts = allTags.groupingBy { it }.eachCount()
        
        // Return the most common tags
        return tagCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }
    
    /**
     * Determines user preferences based on liked and disliked content.
     */
    private fun determineUserPreferences(
        likedContent: List<Content>,
        dislikedContent: List<Content>
    ): List<String> {
        val preferences = mutableListOf<String>()
        
        // If no content history, return empty list
        if (likedContent.isEmpty() && dislikedContent.isEmpty()) {
            return listOf("No preference history available")
        }
        
        // Analyze content length preferences
        val likedLengths = likedContent.map { it.description.length }
        val dislikedLengths = dislikedContent.map { it.description.length }
        
        if (likedLengths.isNotEmpty() && dislikedLengths.isNotEmpty()) {
            val avgLikedLength = likedLengths.average()
            val avgDislikedLength = dislikedLengths.average()
            
            if (avgLikedLength > avgDislikedLength * 1.5) {
                preferences.add("Prefers longer, more detailed content")
            } else if (avgDislikedLength > avgLikedLength * 1.5) {
                preferences.add("Prefers shorter, more concise content")
            }
        }
        
        // Analyze tag preferences
        val likedTags = extractThemes(likedContent)
        val dislikedTags = extractThemes(dislikedContent)
        
        if (likedTags.isNotEmpty()) {
            preferences.add("Shows interest in topics: ${likedTags.joinToString(", ")}")
        }
        
        if (dislikedTags.isNotEmpty()) {
            preferences.add("Shows disinterest in topics: ${dislikedTags.joinToString(", ")}")
        }
        
        // Analyze title style preferences
        val likedTitleLengths = likedContent.map { it.title.length }
        val dislikedTitleLengths = dislikedContent.map { it.title.length }
        
        if (likedTitleLengths.isNotEmpty() && dislikedTitleLengths.isNotEmpty()) {
            val avgLikedTitleLength = likedTitleLengths.average()
            val avgDislikedTitleLength = dislikedTitleLengths.average()
            
            if (avgLikedTitleLength > avgDislikedTitleLength * 1.3) {
                preferences.add("Prefers descriptive titles")
            } else if (avgDislikedTitleLength > avgLikedTitleLength * 1.3) {
                preferences.add("Prefers concise titles")
            }
        }
        
        // If we still don't have preferences, add a default
        if (preferences.isEmpty()) {
            preferences.add("Insufficient data to determine specific preferences")
        }
        
        return preferences
    }
    
    /**
     * Analyzes content patterns from a list of content.
     */
    private fun analyzeContentPatterns(contentList: List<Content>): List<String> {
        if (contentList.isEmpty()) return emptyList()
        
        val patterns = mutableListOf<String>()
        
        // Analyze content length patterns
        val contentLengths = contentList.map { it.description.length }
        val avgContentLength = contentLengths.average()
        
        if (avgContentLength > 500) {
            patterns.add("Create longer, more detailed posts (avg ${avgContentLength.toInt()} characters)")
        } else if (avgContentLength < 200) {
            patterns.add("Create shorter, more concise posts (avg ${avgContentLength.toInt()} characters)")
        } else {
            patterns.add("Create medium-length posts (avg ${avgContentLength.toInt()} characters)")
        }
        
        // Analyze tag usage patterns
        val tagCounts = contentList.flatMap { it.tags }.groupingBy { it }.eachCount()
        val mostUsedTags = tagCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        if (mostUsedTags.isNotEmpty()) {
            patterns.add("Frequently uses tags: ${mostUsedTags.joinToString(", ")}")
        }
        
        // Analyze title patterns
        val titleLengths = contentList.map { it.title.length }
        val avgTitleLength = titleLengths.average()
        
        if (avgTitleLength > 50) {
            patterns.add("Uses longer, more descriptive titles")
        } else if (avgTitleLength < 30) {
            patterns.add("Uses shorter, more concise titles")
        }
        
        // Analyze question usage
        val questionCount = contentList.count { it.title.contains("?") || it.description.contains("?") }
        val questionPercentage = (questionCount.toFloat() / contentList.size) * 100
        
        if (questionPercentage > 30) {
            patterns.add("Often poses questions in content (${questionPercentage.toInt()}% of posts)")
        }
        
        return patterns
    }
} 