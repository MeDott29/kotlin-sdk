package com.example.contentswiper.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for handling errors and logging throughout the app.
 */
object ErrorLogger {
    private const val TAG = "ErrorLogger"
    private const val MAX_LOG_FILES = 20
    
    /**
     * Logs an exception with detailed information.
     */
    fun logException(context: Context, source: String, exception: Throwable, showToast: Boolean = false) {
        // Log to Logcat
        Log.e(TAG, "Exception in $source: ${exception.message}")
        Log.e(TAG, "Exception type: ${exception.javaClass.name}")
        
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        Log.e(TAG, "Stack trace: $sw")
        
        exception.cause?.let {
            Log.e(TAG, "Caused by: ${it.message}")
        }
        
        // Write to error log file
        writeErrorToFile(context, source, exception)
        
        // Show toast if requested
        if (showToast) {
            showToast(context, "Error in $source: ${exception.message}")
        }
        
        // Clean up old log files
        cleanupOldLogFiles(context)
    }
    
    /**
     * Logs a Room database error with detailed information.
     */
    fun logDatabaseError(context: Context, operation: String, exception: Throwable) {
        Log.e(TAG, "Database error during $operation: ${exception.message}")
        logException(context, "Database:$operation", exception, true)
        
        // Additional database-specific logging could be added here
    }
    
    /**
     * Writes error information to a log file.
     */
    private fun writeErrorToFile(context: Context, source: String, exception: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val filename = "error_${timestamp}_${source.replace(":", "_")}.txt"
            val file = File(context.filesDir, "logs")
            if (!file.exists()) {
                file.mkdirs()
            }
            
            val logFile = File(file, filename)
            
            logFile.printWriter().use { writer ->
                writer.println("Timestamp: $timestamp")
                writer.println("Source: $source")
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
                
                // Add device and app info
                writer.println("\nDevice info:")
                writer.println("Model: ${android.os.Build.MODEL}")
                writer.println("Android version: ${android.os.Build.VERSION.RELEASE}")
                writer.println("SDK: ${android.os.Build.VERSION.SDK_INT}")
                
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    writer.println("\nApp info:")
                    writer.println("Version: ${packageInfo.versionName}")
                    writer.println("Version code: ${packageInfo.longVersionCode}")
                } catch (e: Exception) {
                    writer.println("\nCould not get app info: ${e.message}")
                }
                
                // Add memory info
                val runtime = Runtime.getRuntime()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val maxMemory = runtime.maxMemory() / (1024 * 1024)
                writer.println("\nMemory info:")
                writer.println("Used memory: $usedMemory MB")
                writer.println("Max memory: $maxMemory MB")
            }
            
            Log.i(TAG, "Error log written to $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write error log: ${e.message}", e)
        }
    }
    
    /**
     * Cleans up old log files to prevent excessive storage usage.
     */
    private fun cleanupOldLogFiles(context: Context) {
        try {
            val logsDir = File(context.filesDir, "logs")
            if (!logsDir.exists() || !logsDir.isDirectory) return
            
            val logFiles = logsDir.listFiles { file -> file.name.startsWith("error_") }
                ?.sortedBy { it.lastModified() } ?: return
                
            if (logFiles.size > MAX_LOG_FILES) {
                // Delete oldest files
                for (i in 0 until logFiles.size - MAX_LOG_FILES) {
                    val deleted = logFiles[i].delete()
                    if (deleted) {
                        Log.d(TAG, "Deleted old log file: ${logFiles[i].name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up log files: ${e.message}", e)
        }
    }
    
    /**
     * Shows a toast message on the main thread.
     */
    private fun showToast(context: Context, message: String) {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Gets all error logs as a formatted string for debugging.
     */
    fun getErrorLogs(context: Context): String {
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists() || !logsDir.isDirectory) {
            return "No error logs found"
        }
        
        val logFiles = logsDir.listFiles { file -> file.name.startsWith("error_") }
            ?.sortedByDescending { it.lastModified() } ?: return "No error logs found"
            
        val sb = StringBuilder()
        sb.appendLine("Error Logs (${logFiles.size} files):")
        
        for (file in logFiles) {
            sb.appendLine("\n${file.name} (${file.length() / 1024} KB)")
            try {
                // Just include the first few lines of each log
                val lines = file.readLines().take(5)
                for (line in lines) {
                    sb.appendLine("  $line")
                }
                sb.appendLine("  ...")
            } catch (e: Exception) {
                sb.appendLine("  Error reading log: ${e.message}")
            }
        }
        
        return sb.toString()
    }
} 