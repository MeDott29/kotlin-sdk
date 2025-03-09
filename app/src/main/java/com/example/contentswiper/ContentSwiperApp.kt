package com.example.contentswiper

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import com.example.contentswiper.api.AgentManager
import com.example.contentswiper.api.ContentGenerator
import com.example.contentswiper.api.KnowledgeGraphManager
import com.example.contentswiper.api.RecursiveGraphReasoner
import com.example.contentswiper.api.SelfAwarePromptManager
import com.example.contentswiper.api.generateSurprisingContent
import com.example.contentswiper.data.AppDatabase
import com.example.contentswiper.repository.AgentRepository
import com.example.contentswiper.repository.ContentRepository
import com.example.contentswiper.repository.UserPreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

/**
 * Application class for the Content Swiper app.
 * Enhanced with self-aware prompting and recursive graph reasoning
 * based on the AgenticKnowledgeGraph paper.
 */
class ContentSwiperApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val TAG = "ContentSwiperApp"
    
    // Database
    val database by lazy { 
        try {
            Log.d(TAG, "Initializing Room database")
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error initializing database: ${e.message}", e)
            logException("Database initialization", e)
            showToast("Critical database error: ${e.message}")
            
            // Try to recover by deleting the database and recreating it
            applicationScope.launch(Dispatchers.IO) {
                try {
                    recoverFromDatabaseError()
                } catch (recoveryException: Exception) {
                    Log.e(TAG, "Failed to recover from database error: ${recoveryException.message}", recoveryException)
                    logException("Database recovery", recoveryException)
                }
            }
            
            // Return a fallback database instance
            createFallbackDatabase()
        }
    }
    
    // DAOs
    val contentDao by lazy { 
        try {
            database.contentDao()
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing contentDao: ${e.message}", e)
            logException("DAO access", e)
            throw e
        }
    }
    
    val userPreferenceDao by lazy { 
        try {
            database.userPreferenceDao()
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing userPreferenceDao: ${e.message}", e)
            logException("DAO access", e)
            throw e
        }
    }
    
    val agentDao by lazy { 
        try {
            database.agentDao()
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing agentDao: ${e.message}", e)
            logException("DAO access", e)
            throw e
        }
    }
    
    // Repositories
    val contentRepository by lazy { ContentRepository(contentDao) }
    val userPreferenceRepository by lazy { UserPreferenceRepository(userPreferenceDao) }
    val agentRepository by lazy { AgentRepository(agentDao) }
    
    // API Key
    private val apiKey by lazy { loadApiKey() }
    
    // Self-aware prompting
    val selfAwarePromptManager by lazy {
        try {
            SelfAwarePromptManager()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SelfAwarePromptManager: ${e.message}", e)
            logException("SelfAwarePromptManager initialization", e)
            null
        }
    }
    
    // API
    val contentGenerator by lazy { 
        try {
            ContentGenerator(apiKey, selfAwarePromptManager ?: SelfAwarePromptManager())
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ContentGenerator: ${e.message}", e)
            logException("ContentGenerator initialization", e)
            null
        }
    }
    
    val agentManager by lazy {
        try {
            val cg = contentGenerator
            val kgm = knowledgeGraphManager
            if (cg != null && kgm != null) {
                AgentManager(agentRepository, contentRepository, userPreferenceRepository, cg, kgm)
            } else {
                Log.w(TAG, "Cannot initialize AgentManager: dependencies not available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AgentManager: ${e.message}", e)
            logException("AgentManager initialization", e)
            null
        }
    }
    
    // Knowledge Graph Manager
    val knowledgeGraphManager by lazy {
        try {
            KnowledgeGraphManager(agentRepository, contentRepository, contentGenerator)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing KnowledgeGraphManager: ${e.message}", e)
            logException("KnowledgeGraphManager initialization", e)
            null
        }
    }
    
    // Recursive Graph Reasoner
    val recursiveGraphReasoner by lazy {
        try {
            val cg = contentGenerator
            val kgm = knowledgeGraphManager
            if (cg != null && kgm != null) {
                RecursiveGraphReasoner(cg, kgm)
            } else {
                Log.w(TAG, "Cannot initialize RecursiveGraphReasoner: dependencies not available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing RecursiveGraphReasoner: ${e.message}", e)
            logException("RecursiveGraphReasoner initialization", e)
            null
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize components
        initializeComponents()
    }
    
    private fun initializeComponents() {
        applicationScope.launch {
            try {
                // Initialize database and repositories
                database
                contentRepository
                userPreferenceRepository
                agentRepository
                
                // Initialize self-aware prompting
                selfAwarePromptManager
                
                // Initialize API components
                contentGenerator
                knowledgeGraphManager
                agentManager
                
                // Initialize recursive graph reasoner
                recursiveGraphReasoner
                
                // Initialize agents if needed
                initializeAgents()
                
                // Initialize knowledge graph
                initializeKnowledgeGraph()
                
                Log.d(TAG, "Application components initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during component initialization: ${e.message}", e)
                logException("Component initialization", e)
            }
        }
    }
    
    private suspend fun initializeAgents() {
        try {
            // Check if agents exist
            val agentCount = agentRepository.getAgentCount()
            
            if (agentCount == 0) {
                Log.d(TAG, "No agents found, creating default agents")
                agentManager?.createDefaultAgents()
            } else {
                Log.d(TAG, "Found $agentCount existing agents")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing agents: ${e.message}", e)
            logException("Agent initialization", e)
        }
    }
    
    private suspend fun initializeKnowledgeGraph() {
        try {
            // Initialize knowledge graph with seed concepts
            knowledgeGraphManager?.initializeKnowledgeGraph()
            Log.d(TAG, "Knowledge graph initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing knowledge graph: ${e.message}", e)
            logException("Knowledge graph initialization", e)
        }
    }
    
    /**
     * Generates surprising content based on the knowledge graph.
     * This method connects different domains to create unexpected insights.
     */
    suspend fun generateSurprisingContent(count: Int): List<com.example.contentswiper.model.Content> {
        try {
            // Get all agents
            val agents = agentRepository.getAllAgents().first()
            if (agents.isEmpty()) {
                Log.w(TAG, "No agents available for generating surprising content")
                return emptyList()
            }
            
            // Collect knowledge nodes from all agents
            val allKnowledgeNodes = agents.flatMap { it.knowledgeNodes }
            if (allKnowledgeNodes.isEmpty()) {
                Log.w(TAG, "No knowledge nodes available for generating surprising content")
                return emptyList()
            }
            
            // Get unique concepts and domains
            val concepts = allKnowledgeNodes.map { it.concept }.distinct().shuffled().take(10)
            val domains = allKnowledgeNodes.map { it.domain }.distinct().shuffled()
            
            // Generate surprising content
            return contentGenerator?.generateSurprisingContent(concepts, domains, count) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating surprising content: ${e.message}", e)
            logException("Surprising content generation", e)
            return emptyList()
        }
    }
    
    /**
     * Applies self-healing to content based on user feedback.
     */
    suspend fun applySelfHealingToContent(
        contentId: String,
        feedback: Map<String, Float>
    ): Boolean {
        try {
            // Get content
            val content = contentRepository.getContentById(contentId) ?: return false
            
            // Apply self-healing
            knowledgeGraphManager?.applySelfHealing(content, feedback)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying self-healing to content: ${e.message}", e)
            logException("Content self-healing", e)
            return false
        }
    }
    
    /**
     * Updates the knowledge graph with new content.
     */
    suspend fun updateKnowledgeGraph(contentId: String): Boolean {
        try {
            // Get content
            val content = contentRepository.getContentById(contentId) ?: return false
            
            // Update knowledge graph
            knowledgeGraphManager?.updateKnowledgeGraph(content)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating knowledge graph: ${e.message}", e)
            logException("Knowledge graph update", e)
            return false
        }
    }
    
    /**
     * Creates a fallback in-memory database for emergency use.
     */
    private fun createFallbackDatabase(): AppDatabase {
        Log.w(TAG, "Creating fallback in-memory database")
        return Room.inMemoryDatabaseBuilder(
            applicationContext,
            AppDatabase::class.java
        ).build()
    }
    
    /**
     * Attempts to recover from a database error by deleting and recreating the database.
     */
    private fun recoverFromDatabaseError() {
        Log.w(TAG, "Attempting to recover from database error")
        
        // Get database file
        val dbFile = getDatabasePath("content_swiper_database")
        if (dbFile.exists()) {
            // Backup the corrupted database for debugging
            val backupFile = File(filesDir, "corrupted_db_${System.currentTimeMillis()}.db")
            dbFile.copyTo(backupFile, overwrite = true)
            Log.d(TAG, "Backed up corrupted database to ${backupFile.absolutePath}")
            
            // Delete the database files
            for (file in dbFile.parentFile?.listFiles() ?: emptyArray()) {
                if (file.name.startsWith("content_swiper_database")) {
                    val deleted = file.delete()
                    Log.d(TAG, "Deleted database file ${file.name}: $deleted")
                }
            }
            
            Log.d(TAG, "Database files deleted, will be recreated on next access")
            showToast("Database has been reset due to corruption. Some data may be lost.")
        } else {
            Log.w(TAG, "Database file not found, cannot recover")
        }
    }
    
    /**
     * Loads the OpenAI API key from the .env file.
     */
    private fun loadApiKey(): String? {
        try {
            // Try to load from environment variable first
            val envApiKey = System.getenv("OPENAI_API_KEY")
            if (!envApiKey.isNullOrBlank()) {
                Log.d(TAG, "Loaded API key from environment variable")
                return envApiKey
            }
            
            // Try to load from .env file
            val envFile = File(".env")
            if (envFile.exists()) {
                Log.d(TAG, ".env file exists at: ${envFile.absolutePath}")
                val properties = Properties()
                envFile.inputStream().use { properties.load(it) }
                val fileApiKey = properties.getProperty("OPENAI_API_KEY")
                if (!fileApiKey.isNullOrBlank()) {
                    Log.d(TAG, "Found OPENAI_API_KEY in .env file (length: ${fileApiKey.length})")
                    return fileApiKey
                }
            }
            
            Log.w(TAG, "No API key found in environment or .env file")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading API key: ${e.message}", e)
            logException("API key loading", e)
            return null
        }
    }
    
    /**
     * Sets up a global uncaught exception handler to log crashes.
     */
    private fun setupExceptionHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
                logException("Uncaught exception", throwable)
                
                // Show a toast with the error message
                showToast("App crashed: ${throwable.message}")
                
                // Write crash info to a file
                writeCrashToFile(throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Error in exception handler: ${e.message}", e)
            } finally {
                // Call the default handler
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
        }
    }
    
    /**
     * Writes crash information to a file in the app's files directory.
     */
    private fun writeCrashToFile(throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val filename = "crash_$timestamp.txt"
            val file = File(filesDir, filename)
            
            file.printWriter().use { writer ->
                writer.println("Timestamp: $timestamp")
                writer.println("Exception: ${throwable.javaClass.name}")
                writer.println("Message: ${throwable.message}")
                writer.println("\nStack trace:")
                
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                writer.println(sw.toString())
                
                writer.println("\nDevice info:")
                writer.println("Model: ${android.os.Build.MODEL}")
                writer.println("Android version: ${android.os.Build.VERSION.RELEASE}")
                writer.println("SDK: ${android.os.Build.VERSION.SDK_INT}")
                
                writer.println("\nApp info:")
                writer.println("Version: ${packageManager.getPackageInfo(packageName, 0).versionName}")
                
                // Add database info
                writer.println("\nDatabase info:")
                val dbFile = getDatabasePath("content_swiper_database")
                writer.println("Database exists: ${dbFile.exists()}")
                if (dbFile.exists()) {
                    writer.println("Database size: ${dbFile.length()} bytes")
                    writer.println("Last modified: ${Date(dbFile.lastModified())}")
                }
            }
            
            Log.i(TAG, "Crash report written to $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash report: ${e.message}", e)
        }
    }
    
    /**
     * Logs an exception with detailed information.
     */
    fun logException(context: String, exception: Throwable) {
        Log.e(TAG, "Exception in $context: ${exception.message}")
        Log.e(TAG, "Exception type: ${exception.javaClass.name}")
        
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        Log.e(TAG, "Stack trace: $sw")
        
        exception.cause?.let {
            Log.e(TAG, "Caused by: ${it.message}")
        }
        
        // Write to error log file
        writeErrorToFile(context, exception)
    }
    
    /**
     * Writes error information to a log file.
     */
    private fun writeErrorToFile(context: String, exception: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val filename = "error_log_$timestamp.txt"
            val file = File(filesDir, filename)
            
            file.printWriter().use { writer ->
                writer.println("Timestamp: $timestamp")
                writer.println("Context: $context")
                writer.println("Exception: ${exception.javaClass.name}")
                writer.println("Message: ${exception.message}")
                
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                exception.printStackTrace(pw)
                writer.println("\nStack trace:")
                writer.println(sw.toString())
                
                exception.cause?.let {
                    writer.println("\nCaused by: ${it.message}")
                    it.printStackTrace(pw)
                    writer.println(sw.toString())
                }
            }
            
            Log.i(TAG, "Error log written to $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write error log: ${e.message}", e)
        }
    }
    
    /**
     * Shows a toast message on the main thread.
     */
    private fun showToast(message: String) {
        android.os.Handler(mainLooper).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Generates content using recursive graph reasoning.
     * This implements the core concept from the AgenticKnowledgeGraph paper.
     */
    suspend fun generateContentWithRecursiveReasoning(
        seedConcepts: List<String>,
        count: Int = 1
    ): List<com.example.contentswiper.model.Content> {
        try {
            Log.d(TAG, "Generating content with recursive reasoning from ${seedConcepts.size} seed concepts")
            
            val reasoner = recursiveGraphReasoner
            if (reasoner == null) {
                Log.w(TAG, "RecursiveGraphReasoner not available, falling back to standard content generation")
                return contentGenerator?.generateRandomContent(count) ?: emptyList()
            }
            
            // Get a random agent to use as the context for generation
            val agents = agentRepository.getAllAgents().first()
            if (agents.isEmpty()) {
                Log.w(TAG, "No agents available for recursive reasoning")
                return contentGenerator?.generateRandomContent(count) ?: emptyList()
            }
            
            val agent = agents.random()
            
            // Expand the knowledge graph with seed concepts
            val graphDelta = reasoner.expandKnowledgeGraph(
                agent = agent,
                seedConcepts = seedConcepts,
                maxIterations = 5
            )
            
            // Generate content based on the expanded graph
            val content = reasoner.generateContentFromExpandedGraph(
                agent = agent,
                graphDelta = graphDelta,
                count = count
            )
            
            // Update the agent with the expanded knowledge graph
            val updatedAgent = agent.copy(
                knowledgeNodes = agent.knowledgeNodes + graphDelta.addedNodes,
                connectionStrength = agent.connectionStrength + graphDelta.strengthenedConnections
            )
            agentRepository.updateAgent(updatedAgent)
            
            return content
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content with recursive reasoning: ${e.message}", e)
            logException("Recursive reasoning content generation", e)
            return contentGenerator?.generateRandomContent(count) ?: emptyList()
        }
    }
} 