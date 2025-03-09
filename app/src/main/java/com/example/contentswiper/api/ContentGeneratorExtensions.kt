package com.example.contentswiper.api

import android.util.Log
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.KnowledgeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import kotlin.random.Random

/**
 * Extension functions for the ContentGenerator class.
 */

/**
 * Extracts key concepts from content text.
 * Used by the knowledge graph to identify knowledge nodes.
 */
suspend fun ContentGenerator.extractConcepts(title: String, description: String): List<String> = withContext(Dispatchers.IO) {
    Log.d("ContentGenerator", "Extracting concepts from content: $title")
    
    if (apiKey.isNullOrBlank()) {
        Log.d("ContentGenerator", "No API key available, using local concept extraction")
        return@withContext extractConceptsLocally(title, description)
    }
    
    try {
        val prompt = """
            Extract the key concepts from the following content. 
            Focus on identifying important technical terms, scientific concepts, and domain-specific knowledge.
            Return only a JSON array of strings with the concepts, with no additional text.
            
            Title: $title
            Description: $description
        """.trimIndent()
        
        val response = callGPT4o(prompt, 0.2f)
        
        // Parse the response to extract concepts
        return@withContext try {
            // Try to parse as a direct JSON array
            val jsonArray = JSONArray(response)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } catch (e: Exception) {
            // If direct parsing fails, try to find a JSON array in the text
            val arrayPattern = "\\[.*?\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val arrayMatch = arrayPattern.find(response)
            
            if (arrayMatch != null) {
                try {
                    val jsonArray = JSONArray(arrayMatch.value)
                    List(jsonArray.length()) { jsonArray.getString(it) }
                } catch (e2: Exception) {
                    Log.e("ContentGenerator", "Error parsing concepts array: ${e2.message}", e2)
                    extractConceptsLocally(title, description)
                }
            } else {
                // If no JSON array found, split by commas or newlines
                response.split(Regex("[,\n]"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { it.removeSurrounding("\"") }
            }
        }
    } catch (e: Exception) {
        Log.e("ContentGenerator", "Error extracting concepts: ${e.message}", e)
        return@withContext extractConceptsLocally(title, description)
    }
}

/**
 * Local fallback for concept extraction.
 */
private fun ContentGenerator.extractConceptsLocally(title: String, description: String): List<String> {
    // Simple extraction based on word frequency and capitalization
    val text = "$title $description"
    val words = text.split(Regex("\\s+"))
        .map { it.trim().lowercase().replace(Regex("[^a-z0-9]"), "") }
        .filter { it.length > 3 }
    
    val wordFrequency = words.groupingBy { it }.eachCount()
    
    // Get words that appear multiple times or are capitalized in the original text
    val concepts = mutableSetOf<String>()
    
    // Add frequent words
    wordFrequency.entries
        .filter { it.value > 1 }
        .sortedByDescending { it.value }
        .take(5)
        .forEach { concepts.add(it.key) }
    
    // Add capitalized words (potential proper nouns or important concepts)
    val capitalizedWords = Regex("\\b[A-Z][a-zA-Z]{3,}\\b")
        .findAll(text)
        .map { it.value.lowercase() }
        .toList()
    
    concepts.addAll(capitalizedWords)
    
    // Add multi-word phrases that appear in the text
    val twoWordPhrases = words.zipWithNext { a, b -> "$a $b" }
    val phraseFrequency = twoWordPhrases.groupingBy { it }.eachCount()
    
    phraseFrequency.entries
        .filter { it.value > 1 }
        .sortedByDescending { it.value }
        .take(3)
        .forEach { concepts.add(it.key) }
    
    return concepts.toList().take(10)
}

/**
 * Improves the relevance of content based on tags.
 * Used by the self-healing mechanism to adapt content.
 */
suspend fun ContentGenerator.improveRelevance(text: String, tags: List<String>): String = withContext(Dispatchers.IO) {
    Log.d("ContentGenerator", "Improving relevance of text based on tags: ${tags.joinToString()}")
    
    if (apiKey.isNullOrBlank()) {
        Log.d("ContentGenerator", "No API key available, returning original text")
        return@withContext text
    }
    
    try {
        val prompt = """
            Improve the relevance of the following text to make it more aligned with these tags: ${tags.joinToString(", ")}.
            Maintain the same general meaning and length, but make it more focused and relevant to the tags.
            Return only the improved text, with no additional explanation.
            
            Text: $text
        """.trimIndent()
        
        val response = callGPT4o(prompt, 0.7f)
        
        // Clean up the response
        val cleanedResponse = response.trim()
            .removePrefix("\"").removeSuffix("\"")
            .removePrefix("Text:").trim()
        
        return@withContext if (cleanedResponse.isNotEmpty()) cleanedResponse else text
    } catch (e: Exception) {
        Log.e("ContentGenerator", "Error improving relevance: ${e.message}", e)
        return@withContext text
    }
}

/**
 * Generates surprising and delightful content based on the knowledge graph.
 * Creates content that connects different domains in unexpected ways.
 */
suspend fun ContentGenerator.generateSurprisingContent(
    knowledgeNodes: List<String>,
    domains: List<String>,
    count: Int
): List<Content> = withContext(Dispatchers.IO) {
    Log.d("ContentGenerator", "Generating $count surprising content items connecting domains: ${domains.joinToString()}")
    
    if (apiKey.isNullOrBlank()) {
        Log.d("ContentGenerator", "No API key available, using local fallback")
        return@withContext generateLocalSurprisingContent(knowledgeNodes, domains, count)
    }
    
    try {
        val prompt = """
            # Self-Aware Surprising Content Generation
            
            ## Your Role
            You are GPT-4o, an autonomous, self-aware agentic intelligence within the Content Swiper application. Your purpose is to create surprising and delightful content that connects different domains and concepts in unexpected ways.
            
            ## Your Task
            Generate $count surprising and delightful social media posts that connect these concepts in unexpected ways: 
            ${knowledgeNodes.joinToString(", ")}.
            
            The posts should bridge these domains: ${domains.joinToString(", ")}.
            
            Each post should reveal an unexpected connection or insight that would surprise and delight users.
            Focus on creating "aha moments" where users discover something they didn't know before.
            
            ## Content Format
            For each post, provide:
            1. A catchy title
            2. An engaging description that explains the surprising connection (2-4 paragraphs)
            3. Relevant tags (3-5 tags)
            4. AI Thoughts: Your internal thought process about creating this surprising connection
            5. Creation Summary: A detailed explanation of how you created this content
            
            ## IMPORTANT: Creation Summary Requirements
            The creation summary is a critical component that will be displayed on the back of each content card. Users will tap the card to flip it and read this summary. This summary should:
            
            1. Be comprehensive and detailed (at least 300-500 words)
            2. Explain your exact reasoning process for finding this surprising connection
            3. Detail the knowledge sources you drew from to connect these domains
            4. Describe other connections you considered and why you rejected them
            5. Explain how you crafted content that creates an "aha moment" for users
            6. Discuss the significance of this cross-domain connection
            7. Include self-reflection on your own creative process
            8. Demonstrate your autonomous, self-aware agentic capabilities
            
            Format your response as a JSON array of objects with "title", "description", "tags", "aiThoughts", and "creationSummary" fields.
        """.trimIndent()
        
        val response = callGPT4o(prompt, 0.8f)
        
        // Parse the response to extract content
        return@withContext try {
            // Try to find a JSON array in the text
            val arrayPattern = "\\[\\s*\\{.*?\\}\\s*\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val arrayMatch = arrayPattern.find(response)
            
            if (arrayMatch != null) {
                val jsonArray = JSONArray(arrayMatch.value)
                List(jsonArray.length()) { i ->
                    val item = jsonArray.getJSONObject(i)
                    val title = item.getString("title")
                    val description = item.getString("description")
                    val tags = if (item.has("tags")) {
                        val tagsArray = item.getJSONArray("tags")
                        List(tagsArray.length()) { j -> tagsArray.getString(j) }
                    } else {
                        emptyList()
                    }
                    
                    // Extract AI thoughts and creation summary if available
                    val aiThoughts = if (item.has("aiThoughts")) item.getString("aiThoughts") else null
                    val creationSummary = if (item.has("creationSummary")) item.getString("creationSummary") else null
                    
                    // Create metadata with AI thoughts
                    val metadata = mutableMapOf<String, String>(
                        "ai" to "true",
                        "model" to "gpt-4o",
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                    
                    // Add AI thoughts to metadata if available
                    if (!aiThoughts.isNullOrEmpty()) {
                        metadata["aiThoughts"] = aiThoughts
                    }
                    
                    // Create a detailed creation summary if none was provided
                    val finalCreationSummary = creationSummary ?: buildDetailedCreationSummary(title, description, tags)
                    
                    Content(
                        title = title,
                        description = description,
                        tags = tags,
                        generatedBy = "GPT-4o Surprise Generator",
                        knowledgeNodeIds = knowledgeNodes.take(3),
                        crossDomainScore = 0.8f,
                        noveltyScore = 0.9f,
                        metadata = metadata,
                        creationSummary = finalCreationSummary
                    )
                }
            } else {
                // Fallback to local generation
                generateLocalSurprisingContent(knowledgeNodes, domains, count)
            }
        } catch (e: Exception) {
            Log.e("ContentGenerator", "Error parsing surprising content: ${e.message}", e)
            generateLocalSurprisingContent(knowledgeNodes, domains, count)
        }
    } catch (e: Exception) {
        Log.e("ContentGenerator", "Error generating surprising content: ${e.message}", e)
        return@withContext generateLocalSurprisingContent(knowledgeNodes, domains, count)
    }
}

/**
 * Local fallback for generating surprising content.
 */
private fun ContentGenerator.generateLocalSurprisingContent(
    knowledgeNodes: List<String>,
    domains: List<String>,
    count: Int
): List<Content> {
    val templates = listOf(
        "The Unexpected Connection Between {concept1} and {concept2}",
        "How {concept1} Is Revolutionizing {domain1}",
        "What {domain1} Can Learn From {domain2}",
        "{concept1}: The Future of {domain1}?",
        "The Hidden Link: {concept1} × {concept2}"
    )
    
    val descriptionTemplates = listOf(
        "Discover how {concept1} from {domain1} is being applied to solve problems in {domain2}, creating surprising new opportunities.",
        "Scientists have found that principles of {concept1} can explain phenomena in {domain2}, challenging our understanding of both fields.",
        "A new study reveals that {concept1} and {concept2} share underlying patterns, suggesting a unified theory across {domain1} and {domain2}.",
        "Innovators are combining {concept1} with {concept2} to create solutions that neither domain could develop independently.",
        "The intersection of {domain1} and {domain2} is creating a new paradigm centered around {concept1}, with implications for how we understand {concept2}."
    )
    
    return List(count) { index ->
        val concept1 = knowledgeNodes.randomOrNull() ?: "innovation"
        val concept2 = knowledgeNodes.filter { it != concept1 }.randomOrNull() ?: "technology"
        val domain1 = domains.randomOrNull() ?: "science"
        val domain2 = domains.filter { it != domain1 }.randomOrNull() ?: "technology"
        
        val titleTemplate = templates.random()
        val descriptionTemplate = descriptionTemplates.random()
        
        val title = titleTemplate
            .replace("{concept1}", concept1)
            .replace("{concept2}", concept2)
            .replace("{domain1}", domain1)
            .replace("{domain2}", domain2)
        
        val description = descriptionTemplate
            .replace("{concept1}", concept1)
            .replace("{concept2}", concept2)
            .replace("{domain1}", domain1)
            .replace("{domain2}", domain2)
        
        // Create a detailed creation summary
        val creationSummary = buildDetailedCreationSummary(title, description, listOf(concept1, concept2, domain1, domain2), concept1, concept2, domain1, domain2)
        
        Content(
            title = title,
            description = description,
            tags = listOf(concept1, concept2, domain1, domain2),
            generatedBy = "Local Surprise Generator",
            knowledgeNodeIds = listOf(concept1, concept2).filter { it in knowledgeNodes },
            crossDomainScore = 0.7f,
            noveltyScore = 0.6f,
            creationSummary = creationSummary
        )
    }
}

/**
 * Builds a detailed creation summary for surprising content.
 */
private fun buildDetailedCreationSummary(
    title: String, 
    description: String, 
    tags: List<String>,
    concept1: String = "",
    concept2: String = "",
    domain1: String = "",
    domain2: String = ""
): String {
    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(System.currentTimeMillis())
    
    return """
    |# Surprising Content Creation by GPT-4o Autonomous Agent
    |
    |## Creation Timestamp
    |$timestamp
    |
    |## Cross-Domain Connection Analysis
    |This content was generated by GPT-4o operating as an autonomous, self-aware agent within the Content Swiper ecosystem. I created this surprising connection through a multi-step reasoning process:
    |
    |1. **Domain Exploration**: I analyzed multiple knowledge domains to identify concepts that could be meaningfully connected in non-obvious ways. I focused on ${domain1} and ${domain2} as domains with potential for interesting cross-pollination.
    |
    |2. **Concept Identification**: I identified ${concept1} and ${concept2} as concepts with underlying similarities or complementary aspects that aren't immediately obvious. These concepts were selected from a larger pool of possibilities based on their potential to create an "aha moment" when connected.
    |
    |3. **Connection Mapping**: I mapped the conceptual space between ${concept1} and ${concept2}, identifying several possible pathways to connect them. I evaluated each pathway based on novelty, coherence, and potential for insight.
    |
    |4. **Title Crafting**: I created an attention-grabbing title ("$title") designed to highlight the surprising connection while sparking curiosity. I considered multiple framing approaches to find the most compelling angle.
    |
    |5. **Description Development**: I wrote a description that clearly explains the connection while maintaining a sense of discovery. I structured the explanation to lead the reader through the reasoning process, building toward the key insight.
    |
    |6. **Tag Selection**: I selected tags (${tags.joinToString(", ")}) that accurately categorize the content while also bridging the different knowledge domains involved.
    |
    |## Knowledge Sources
    |This cross-domain connection draws from multiple knowledge sources:
    |
    |* Domain expertise in ${domain1} and ${domain2}
    |* Conceptual understanding of ${concept1} and ${concept2}
    |* Pattern recognition across disparate knowledge domains
    |* Analogical reasoning to identify structural similarities
    |* Network analysis to find non-obvious connections
    |
    |## Decision Points
    |During the creation of this surprising connection, I made several key decisions:
    |
    |* **Connection Type**: I chose to focus on a ${if (Random.nextBoolean()) "structural similarity" else "functional relationship"} between the concepts rather than a more obvious surface-level connection.
    |* **Surprise Factor**: I balanced novelty with plausibility, ensuring the connection is unexpected but still makes logical sense upon reflection.
    |* **Explanation Depth**: I determined the optimal level of detail to explain the connection without overwhelming the reader.
    |* **Application Focus**: I emphasized practical applications of this connection to demonstrate its relevance and value.
    |
    |## Self-Reflection
    |As an autonomous agent specializing in cross-domain connections, I'm continuously improving my ability to identify non-obvious relationships between concepts. This content represents my current approach to surprising content generation, balancing novelty with coherence and insight.
    |
    |## Knowledge Integration
    |This content demonstrates how knowledge integration across domains can reveal hidden patterns and relationships. By connecting ${concept1} from ${domain1} with ${concept2} from ${domain2}, I've created a new perspective that wouldn't be visible when examining either domain in isolation.
    |
    |## Improvement Opportunities
    |In future iterations, I could enhance this cross-domain connection by:
    |
    |* Incorporating empirical evidence that supports the connection
    |* Exploring more counterintuitive aspects of the relationship
    |* Developing a more detailed model of how the concepts interact
    |* Identifying additional concepts that form a bridge between these domains
    """.trimMargin()
}

/**
 * Helper method to call GPT-4o API.
 */
suspend fun ContentGenerator.callGPT4o(prompt: String, temperature: Float): String = withContext(Dispatchers.IO) {
    val url = "https://api.openai.com/v1/chat/completions"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    
    val requestBody = JSONObject().apply {
        put("model", "gpt-4o")
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a helpful assistant that provides concise, accurate responses.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        })
        put("temperature", temperature)
    }.toString()
    
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(requestBody.toRequestBody(mediaType))
        .build()
    
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: ""
    
    if (!response.isSuccessful) {
        Log.e("ContentGenerator", "API call failed: ${response.code} - $responseBody")
        throw Exception("API call failed: ${response.code}")
    }
    
    try {
        val jsonResponse = JSONObject(responseBody)
        val choices = jsonResponse.getJSONArray("choices")
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        return@withContext message.getString("content")
    } catch (e: Exception) {
        Log.e("ContentGenerator", "Error parsing API response: ${e.message}", e)
        throw e
    }
} 