package com.example.contentswiper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.contentswiper.databinding.ActivityMainBinding
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.ui.CardStackAdapter
import com.example.contentswiper.ui.MainViewModel
import com.example.contentswiper.ui.MainViewModelFactory
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeableMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import com.example.contentswiper.utils.CrashReportViewer
import com.example.contentswiper.utils.DatabaseDiagnostics
import com.example.contentswiper.utils.ErrorLogger
import android.widget.Button
import android.widget.EditText
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CardStackListener {
    
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var cardStackAdapter: CardStackAdapter
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    
    private val viewModel: MainViewModel by viewModels {
        val app = application as ContentSwiperApp
        MainViewModelFactory(
            app.contentRepository,
            app.userPreferenceRepository,
            app.agentRepository,
            app.contentGenerator,
            app.agentManager,
            app.knowledgeGraphManager
        )
    }
    
    private val contentRefreshHandler = Handler(Looper.getMainLooper())
    private val agentSimulationHandler = Handler(Looper.getMainLooper())
    private val CONTENT_REFRESH_INTERVAL = 60000L // 1 minute
    private val AGENT_SIMULATION_INTERVAL = 300000L // 5 minutes
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "Starting MainActivity onCreate")
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Check for crash reports
            checkForCrashReports()
            
            try {
                setupCardStackView()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up card stack view: ${e.message}", e)
                (application as ContentSwiperApp).logException("setupCardStackView", e)
                showErrorView("Error setting up UI: ${e.message}")
                return
            }
            
            try {
                setupObservers()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up observers: ${e.message}", e)
                (application as ContentSwiperApp).logException("setupObservers", e)
                showErrorView("Error setting up data observers: ${e.message}")
                return
            }
            
            try {
                setupListeners()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up listeners: ${e.message}", e)
                (application as ContentSwiperApp).logException("setupListeners", e)
                showErrorView("Error setting up button listeners: ${e.message}")
                return
            }
            
            // Initialize default agents if needed
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = application as ContentSwiperApp
                    val agentCount = app.agentRepository.getActiveAgentCount()
                    if (agentCount == 0) {
                        app.agentManager?.createDefaultAgents()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing agents: ${e.message}", e)
                    (application as ContentSwiperApp).logException("initializeAgents", e)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error initializing agents: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            // Load content with a slight delay to ensure everything is initialized
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            binding.root.postDelayed({
                try {
                    loadContent()
                    
                    // Start automatic content refresh and agent simulation
                    startAutomaticContentRefresh()
                    startAutomaticAgentSimulation()
                    
                    // Update the AI status text
                    updateAiStatusText("AI network active and generating content")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading initial content: ${e.message}", e)
                    (application as ContentSwiperApp).logException("loadInitialContent", e)
                    showErrorView("Error loading content: ${e.message}")
                }
            }, 500)
            
            Log.d(TAG, "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in MainActivity.onCreate: ${e.message}", e)
            (application as ContentSwiperApp).logException("MainActivity.onCreate", e)
            showErrorView("Fatal error initializing app: ${e.message}")
        }
    }
    
    /**
     * Checks for crash reports from previous sessions.
     */
    private fun checkForCrashReports() {
        try {
            val filesDir = application.filesDir
            val crashFiles = filesDir.listFiles { file -> file.name.startsWith("crash_") }
            
            if (crashFiles != null && crashFiles.isNotEmpty()) {
                // Sort by modification time, newest first
                crashFiles.sortByDescending { it.lastModified() }
                
                // Read the most recent crash report
                val latestCrash = crashFiles[0]
                val crashContent = latestCrash.readText()
                
                // Show a dialog with the crash information
                AlertDialog.Builder(this)
                    .setTitle("Previous Crash Detected")
                    .setMessage("The app crashed previously. Would you like to see the crash report?")
                    .setPositiveButton("View Report") { _, _ ->
                        AlertDialog.Builder(this)
                            .setTitle("Crash Report")
                            .setMessage(crashContent)
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Delete Report") { _, _ ->
                                latestCrash.delete()
                                Toast.makeText(this, "Crash report deleted", Toast.LENGTH_SHORT).show()
                            }
                            .show()
                    }
                    .setNegativeButton("Ignore", null)
                    .setNeutralButton("Delete All Reports") { _, _ ->
                        crashFiles.forEach { it.delete() }
                        Toast.makeText(this, "All crash reports deleted", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for crash reports: ${e.message}", e)
        }
    }
    
    /**
     * Shows an error view when something goes wrong.
     */
    private fun showErrorView(errorMessage: String) {
        try {
            binding.progressBar.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
            binding.emptyStateText.text = errorMessage
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error view: ${e.message}", e)
            Toast.makeText(this, "Multiple errors occurred. Check logs.", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupCardStackView() {
        Log.d(TAG, "Setting up card stack view")
        cardStackLayoutManager = CardStackLayoutManager(this, this).apply {
            setStackFrom(StackFrom.Top)
            setVisibleCount(3)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(20.0f)
            setDirections(Direction.HORIZONTAL)
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
        }
        
        cardStackAdapter = CardStackAdapter(this, emptyList())
        binding.cardStackView.layoutManager = cardStackLayoutManager
        binding.cardStackView.adapter = cardStackAdapter
        Log.d(TAG, "Card stack view setup completed")
    }
    
    private fun setupObservers() {
        Log.d(TAG, "Setting up observers")
        viewModel.contentList.observe(this, Observer { contents ->
            Log.d(TAG, "Content list updated with ${contents.size} items")
            try {
                cardStackAdapter.setItems(contents)
                binding.progressBar.visibility = View.GONE
                
                if (contents.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating content list: ${e.message}", e)
                (application as ContentSwiperApp).logException("updateContentList", e)
                showErrorView("Error updating content: ${e.message}")
            }
        })
        
        viewModel.error.observe(this, Observer { error ->
            if (error.isNotEmpty()) {
                Log.e(TAG, "Error from ViewModel: $error")
                binding.progressBar.visibility = View.GONE
                
                // If we have an error and no content, show empty state
                if (cardStackAdapter.itemCount == 0) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.emptyStateText.text = "Error loading content: $error"
                }
                
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        })
        Log.d(TAG, "Observers setup completed")
    }
    
    private fun setupListeners() {
        Log.d(TAG, "Setting up button listeners")
        
        binding.refreshButton.setOnClickListener {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.emptyStateText.visibility = View.GONE
                loadContent()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing content: ${e.message}", e)
                (application as ContentSwiperApp).logException("refreshContent", e)
                showErrorView("Error refreshing content: ${e.message}")
            }
        }
        
        binding.likeButton.setOnClickListener {
            try {
                val currentPosition = cardStackLayoutManager.topPosition
                if (currentPosition < cardStackAdapter.itemCount) {
                    val content = cardStackAdapter.getContentAt(currentPosition)
                    viewModel.recordUserPreference(content, true)
                    binding.cardStackView.swipe()
                    Toast.makeText(this, "Liked: ${content.title}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error liking content: ${e.message}", e)
                (application as ContentSwiperApp).logException("likeContent", e)
                Toast.makeText(this, "Error liking content: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.dislikeButton.setOnClickListener {
            try {
                val currentPosition = cardStackLayoutManager.topPosition
                if (currentPosition < cardStackAdapter.itemCount) {
                    val content = cardStackAdapter.getContentAt(currentPosition)
                    viewModel.recordUserPreference(content, false)
                    binding.cardStackView.swipe()
                    Toast.makeText(this, "Disliked: ${content.title}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error disliking content: ${e.message}", e)
                (application as ContentSwiperApp).logException("dislikeContent", e)
                Toast.makeText(this, "Error disliking content: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.agentButton.setOnClickListener {
            try {
                showAgentNetworkDialog()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing agent network: ${e.message}", e)
                (application as ContentSwiperApp).logException("showAgentNetwork", e)
                Toast.makeText(this, "Error showing agent network: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadContent() {
        Log.d(TAG, "Loading content")
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE
        
        // Clear any previous errors
        viewModel.clearError()
        
        // Load content
        viewModel.loadContent()
    }
    
    // CardStackListener implementation
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    
    override fun onCardSwiped(direction: Direction) {
        Log.d(TAG, "Card swiped: $direction")
        
        try {
            // Get the content that was just swiped
            val position = cardStackLayoutManager.topPosition - 1
            if (position >= 0 && position < cardStackAdapter.itemCount) {
                val content = cardStackAdapter.getContentAt(position)
                
                // Record the user preference based on swipe direction
                when (direction) {
                    Direction.Right -> {
                        viewModel.recordUserPreference(content, true)
                        Toast.makeText(this, "Liked: ${content.title}", Toast.LENGTH_SHORT).show()
                    }
                    Direction.Left -> {
                        viewModel.recordUserPreference(content, false)
                        Toast.makeText(this, "Disliked: ${content.title}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Do nothing for other directions
                    }
                }
            }
            
            // Refresh content if needed based on current position
            viewModel.refreshContentIfNeeded(cardStackLayoutManager.topPosition)
            
            // Check if we need to load more content
            if (cardStackLayoutManager.topPosition >= cardStackAdapter.itemCount - 2) {
                Log.d(TAG, "Running low on content, loading more...")
                loadContent()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling card swipe: ${e.message}", e)
            (application as ContentSwiperApp).logException("onCardSwiped", e)
        }
    }
    
    override fun onCardRewound() {}
    
    override fun onCardCanceled() {}
    
    override fun onCardAppeared(view: View?, position: Int) {
        Log.d(TAG, "Card appeared at position: $position")
        
        try {
            // Refresh content if needed based on current position
            viewModel.refreshContentIfNeeded(position)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCardAppeared: ${e.message}", e)
            (application as ContentSwiperApp).logException("onCardAppeared", e)
        }
    }
    
    override fun onCardDisappeared(view: View?, position: Int) {}
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Add a diagnostics option
        menu.add(Menu.NONE, MENU_DIAGNOSTICS, Menu.NONE, "Diagnostics")
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_crash_reports -> {
                showCrashReportsList()
                true
            }
            R.id.action_delete_crash_reports -> {
                deleteAllCrashReports()
                true
            }
            MENU_DIAGNOSTICS -> {
                showDiagnosticsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Shows a list of all crash reports.
     */
    private fun showCrashReportsList() {
        val crashReports = CrashReportViewer.listCrashReports(this)
        
        if (crashReports.isEmpty()) {
            Toast.makeText(this, "No crash reports found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Sort by modification time, newest first
        val sortedReports = crashReports.sortedByDescending { it.lastModified() }
        
        // Create a list of report names with timestamps
        val reportItems = sortedReports.map { file ->
            val timestamp = file.name.removePrefix("crash_").removeSuffix(".txt").replace("_", " ")
            "Crash report: $timestamp"
        }.toTypedArray()
        
        // Show a dialog with the list of reports
        AlertDialog.Builder(this)
            .setTitle("Crash Reports")
            .setItems(reportItems) { _, which ->
                val selectedReport = sortedReports[which]
                showCrashReportContent(selectedReport)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Shows the content of a specific crash report.
     */
    private fun showCrashReportContent(file: File) {
        val content = CrashReportViewer.getCrashReportContent(file)
        
        AlertDialog.Builder(this)
            .setTitle("Crash Report: ${file.name}")
            .setMessage(content)
            .setPositiveButton("OK", null)
            .setNeutralButton("Delete") { _, _ ->
                if (CrashReportViewer.deleteCrashReport(file)) {
                    Toast.makeText(this, "Crash report deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete crash report", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Share") { _, _ ->
                shareCrashReport(file.name, content)
            }
            .show()
    }
    
    /**
     * Shares a crash report via intent.
     */
    private fun shareCrashReport(fileName: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report: $fileName")
        intent.putExtra(Intent.EXTRA_TEXT, content)
        
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Share Crash Report"))
        } else {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Deletes all crash reports.
     */
    private fun deleteAllCrashReports() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Crash Reports")
            .setMessage("Are you sure you want to delete all crash reports? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val count = CrashReportViewer.deleteAllCrashReports(this)
                Toast.makeText(this, "Deleted $count crash reports", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Shows a dialog with information about the agent network.
     */
    private fun showAgentNetworkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agent_network, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.agentRecyclerView)
        val simulateButton = dialogView.findViewById<android.widget.Button>(R.id.simulateButton)
        val discussButton = dialogView.findViewById<android.widget.Button>(R.id.discussButton)
        val lastActivityText = dialogView.findViewById<android.widget.TextView>(R.id.lastActivityText)
        
        // Set up the RecyclerView
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        // Update last activity text with automatic schedule and recent activities
        val app = application as ContentSwiperApp
        lifecycleScope.launch {
            try {
                // Get recent content and activities
                val recentContent = app.contentRepository.getRecentContentFromLastHour(5).first()
                val recentContentInfo = if (recentContent.isNotEmpty()) {
                    "Recent content: ${recentContent.size} items in the last hour"
                } else {
                    "No recent content generated yet"
                }
                
                // Format the automatic schedule information
                val activityInfo = """
                    |Automatic Schedule:
                    |• Content generation: Every ${CONTENT_REFRESH_INTERVAL / 1000 / 60} minute(s)
                    |• Agent interactions: Every ${AGENT_SIMULATION_INTERVAL / 1000 / 60} minute(s)
                    |
                    |$recentContentInfo
                """.trimMargin()
                
                lastActivityText.text = activityInfo
            } catch (e: Exception) {
                Log.e(TAG, "Error getting activity information: ${e.message}", e)
                lastActivityText.text = "Content generation: Every ${CONTENT_REFRESH_INTERVAL / 1000 / 60} minute(s)\nAgent interactions: Every ${AGENT_SIMULATION_INTERVAL / 1000 / 60} minute(s)"
            }
        }
        
        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("AI Social Network")
            .setMessage("The AI agents below are continuously generating content and interacting with each other in the background.")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()
        
        // Load agents
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val agents = (application as ContentSwiperApp).agentRepository.allActiveAgents.first()
                val adapter = AgentAdapter(agents)
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Log.e(TAG, "Error loading agents: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error loading agents: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up the simulate button
        simulateButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Toast.makeText(this@MainActivity, "Simulating agent network activity...", Toast.LENGTH_SHORT).show()
                    (application as ContentSwiperApp).agentManager?.simulateSocialNetworkCycle(10)
                    Toast.makeText(this@MainActivity, "Simulation complete! Pull to refresh for new content.", Toast.LENGTH_SHORT).show()
                    updateAiStatusText("AI agents shared and interacted with content")
                    
                    // Update the last activity text
                    lastActivityText.text = "Last manual activity: Network simulation just now\n" + lastActivityText.text
                } catch (e: Exception) {
                    Log.e(TAG, "Error simulating agent network: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error simulating agent network: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Set up the discuss button
        discussButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Toast.makeText(this@MainActivity, "Simulating agent discussions...", Toast.LENGTH_SHORT).show()
                    (application as ContentSwiperApp).agentManager?.simulateAgentDiscussions(5)
                    Toast.makeText(this@MainActivity, "Agent discussions complete! Their profiles and future content will reflect these interactions.", Toast.LENGTH_LONG).show()
                    updateAiStatusText("AI agents engaged in discussions")
                    
                    // Update the last activity text
                    lastActivityText.text = "Last manual activity: Agent discussions just now\n" + lastActivityText.text
                } catch (e: Exception) {
                    Log.e(TAG, "Error simulating agent discussions: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error simulating agent discussions: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        dialog.show()
    }
    
    /**
     * Adapter for displaying agents in the agent network dialog.
     */
    private inner class AgentAdapter(private val agents: List<Agent>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<AgentAdapter.AgentViewHolder>() {
        
        inner class AgentViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val nameTextView: android.widget.TextView = itemView.findViewById(R.id.agentNameTextView)
            val descriptionTextView: android.widget.TextView = itemView.findViewById(R.id.agentDescriptionTextView)
            val interestsTextView: android.widget.TextView = itemView.findViewById(R.id.agentInterestsTextView)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentViewHolder {
            val view = layoutInflater.inflate(R.layout.item_agent, parent, false)
            return AgentViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: AgentViewHolder, position: Int) {
            val agent = agents[position]
            holder.nameTextView.text = agent.name
            holder.descriptionTextView.text = agent.description
            holder.interestsTextView.text = "Interests: ${agent.interests.joinToString(", ")}"
            
            // Set up click listener to show agent's content
            holder.itemView.setOnClickListener {
                showAgentContentDialog(agent)
            }
        }
        
        override fun getItemCount() = agents.size
    }
    
    /**
     * Shows a dialog with content generated by a specific agent.
     */
    private fun showAgentContentDialog(agent: Agent) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val content = (application as ContentSwiperApp).contentRepository.getContentByGenerator(agent.name, 10)
                
                if (content.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No content from this agent yet", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val contentItems = content.joinToString("\n\n") { 
                    "• ${it.title}\n  ${it.tags.joinToString(", ")}" 
                }
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Content by ${agent.name}")
                    .setMessage(contentItems)
                    .setPositiveButton("Close", null)
                    .setNeutralButton("Generate More") { _, _ ->
                        generateAgentContent(agent)
                    }
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing agent content: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error showing agent content: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Generates more content from a specific agent.
     */
    private fun generateAgentContent(agent: Agent) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(this@MainActivity, "Generating content from ${agent.name}...", Toast.LENGTH_SHORT).show()
                
                val content = (application as ContentSwiperApp).contentGenerator?.generateAgentContent(agent, 3)
                if (content != null && content.isNotEmpty()) {
                    (application as ContentSwiperApp).contentRepository.insertAllContent(content)
                    
                    // Simulate other agents interacting with this content
                    for (item in content) {
                        (application as ContentSwiperApp).agentManager?.simulateAgentInteractions(item)
                    }
                    
                    Toast.makeText(this@MainActivity, "Generated ${content.size} new posts from ${agent.name}", Toast.LENGTH_SHORT).show()
                    
                    // Show the new content
                    showAgentContentDialog(agent)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to generate content", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating agent content: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error generating agent content: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Shows a diagnostics dialog with database information and repair options.
     */
    private fun showDiagnosticsDialog() {
        val app = application as ContentSwiperApp
        
        // Create a dialog with database diagnostics
        val builder = AlertDialog.Builder(this)
        builder.setTitle("App Diagnostics")
        
        // Show loading message initially
        builder.setMessage("Loading diagnostics information...")
        
        val dialog = builder.create()
        dialog.show()
        
        // Run diagnostics in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dbInfo = DatabaseDiagnostics.getDatabaseInfo(this@MainActivity, "content_swiper_database")
                val isValid = DatabaseDiagnostics.isDatabaseValid(this@MainActivity, "content_swiper_database")
                val integrityCheck = DatabaseDiagnostics.checkDatabaseIntegrity(this@MainActivity, "content_swiper_database")
                
                // Update UI on main thread
                runOnUiThread {
                    val message = StringBuilder()
                    message.appendLine("Database Diagnostics:")
                    message.appendLine("Valid: $isValid")
                    message.appendLine("Integrity Check: $integrityCheck")
                    message.appendLine("\n$dbInfo")
                    
                    // Update dialog message
                    dialog.setMessage(message.toString())
                    
                    // Add buttons based on diagnostics
                    if (!isValid || !integrityCheck) {
                        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Repair Database") { _, _ ->
                            repairDatabase()
                        }
                    }
                    
                    dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Backup Database") { _, _ ->
                        backupDatabase()
                    }
                    
                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close") { _, _ ->
                        dialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running diagnostics: ${e.message}", e)
                ErrorLogger.logException(this@MainActivity, "diagnostics", e)
                
                // Update UI on main thread
                runOnUiThread {
                    dialog.setMessage("Error running diagnostics: ${e.message}")
                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close") { _, _ ->
                        dialog.dismiss()
                    }
                }
            }
        }
    }
    
    /**
     * Repairs the database by deleting it and recreating it.
     */
    private fun repairDatabase() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Repair Database")
        builder.setMessage("This will delete and recreate the database. All data will be lost. Are you sure?")
        
        builder.setPositiveButton("Yes") { _, _ ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Backup before deleting
                    val backupPath = DatabaseDiagnostics.backupDatabase(this@MainActivity, "content_swiper_database")
                    
                    // Delete the database
                    val deleted = DatabaseDiagnostics.deleteDatabase(this@MainActivity, "content_swiper_database")
                    
                    runOnUiThread {
                        if (deleted) {
                            Toast.makeText(this@MainActivity, "Database repaired. Backup saved at: $backupPath", Toast.LENGTH_LONG).show()
                            // Restart the app to recreate the database
                            restartApp()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to repair database", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error repairing database: ${e.message}", e)
                    ErrorLogger.logException(this@MainActivity, "database_repair", e)
                    
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error repairing database: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    /**
     * Backs up the database to a zip file.
     */
    private fun backupDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupPath = DatabaseDiagnostics.backupDatabase(this@MainActivity, "content_swiper_database")
                
                runOnUiThread {
                    if (backupPath != null) {
                        Toast.makeText(this@MainActivity, "Database backed up to: $backupPath", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to backup database", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error backing up database: ${e.message}", e)
                ErrorLogger.logException(this@MainActivity, "database_backup", e)
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error backing up database: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Restarts the app.
     */
    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    
    /**
     * Starts automatic content refresh at regular intervals.
     */
    private fun startAutomaticContentRefresh() {
        Log.d(TAG, "Starting automatic content refresh")
        
        // Schedule the first refresh
        contentRefreshHandler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    Log.d(TAG, "Automatic content refresh triggered")
                    
                    // Generate a mix of content types
                    lifecycleScope.launch {
                        try {
                            val app = application as ContentSwiperApp
                            
                            // Randomly choose between surprising content and recursive reasoning
                            if (Math.random() < 0.5) {
                                // Generate surprising content
                                val seedConcepts = app.knowledgeGraphManager?.getRandomConcepts(3) ?: listOf("technology", "nature", "society")
                                val content = app.generateContentWithRecursiveReasoning(seedConcepts, 2)
                                
                                if (content.isNotEmpty()) {
                                    viewModel.addContent(content)
                                    updateAiStatusText("Generated new content through deep reasoning")
                                }
                            } else {
                                // Generate surprising content
                                val surprisingContent = app.generateSurprisingContent(2)
                                
                                if (surprisingContent.isNotEmpty()) {
                                    viewModel.addContent(surprisingContent)
                                    updateAiStatusText("Generated surprising content connections")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in automatic content refresh: ${e.message}", e)
                            (application as ContentSwiperApp).logException("automaticContentRefresh", e)
                        }
                    }
                    
                    // Schedule the next refresh
                    contentRefreshHandler.postDelayed(this, CONTENT_REFRESH_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling content refresh: ${e.message}", e)
                    (application as ContentSwiperApp).logException("scheduleContentRefresh", e)
                    
                    // Try to reschedule even if there was an error
                    contentRefreshHandler.postDelayed(this, CONTENT_REFRESH_INTERVAL)
                }
            }
        }, CONTENT_REFRESH_INTERVAL)
    }
    
    /**
     * Starts automatic agent simulation at regular intervals.
     */
    private fun startAutomaticAgentSimulation() {
        Log.d(TAG, "Starting automatic agent simulation")
        
        // Schedule the first simulation
        agentSimulationHandler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    Log.d(TAG, "Automatic agent simulation triggered")
                    
                    // Run agent simulations
                    lifecycleScope.launch {
                        try {
                            val app = application as ContentSwiperApp
                            
                            // Randomly choose between network cycle and discussions
                            if (Math.random() < 0.7) {
                                // Simulate network cycle
                                app.agentManager?.simulateSocialNetworkCycle(5)
                                updateAiStatusText("AI agents shared and interacted with content")
                            } else {
                                // Simulate discussions
                                app.agentManager?.simulateAgentDiscussions(3)
                                updateAiStatusText("AI agents engaged in discussions")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in automatic agent simulation: ${e.message}", e)
                            (application as ContentSwiperApp).logException("automaticAgentSimulation", e)
                        }
                    }
                    
                    // Schedule the next simulation
                    agentSimulationHandler.postDelayed(this, AGENT_SIMULATION_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling agent simulation: ${e.message}", e)
                    (application as ContentSwiperApp).logException("scheduleAgentSimulation", e)
                    
                    // Try to reschedule even if there was an error
                    agentSimulationHandler.postDelayed(this, AGENT_SIMULATION_INTERVAL)
                }
            }
        }, AGENT_SIMULATION_INTERVAL)
    }
    
    /**
     * Updates the AI status text with animation.
     */
    private fun updateAiStatusText(status: String) {
        binding.aiStatusText.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.aiStatusText.text = status
                binding.aiStatusText.animate()
                    .alpha(0.7f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Remove callbacks to prevent memory leaks
        contentRefreshHandler.removeCallbacksAndMessages(null)
        agentSimulationHandler.removeCallbacksAndMessages(null)
    }
    
    companion object {
        private const val MENU_DIAGNOSTICS = 1003
    }
} 