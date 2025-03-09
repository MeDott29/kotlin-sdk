package com.example.contentswiper.utils

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Utility class to help view and manage crash reports.
 */
object CrashReportViewer {
    private const val TAG = "CrashReportViewer"
    
    /**
     * Lists all crash reports in the app's files directory.
     * @return A list of crash report files
     */
    fun listCrashReports(context: Context): List<File> {
        val filesDir = context.filesDir
        return filesDir.listFiles { file -> file.name.startsWith("crash_") }?.toList() ?: emptyList()
    }
    
    /**
     * Gets the content of a specific crash report.
     * @param file The crash report file
     * @return The content of the crash report
     */
    fun getCrashReportContent(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading crash report: ${e.message}", e)
            "Error reading crash report: ${e.message}"
        }
    }
    
    /**
     * Gets the most recent crash report.
     * @return The most recent crash report file, or null if none exists
     */
    fun getMostRecentCrashReport(context: Context): File? {
        val reports = listCrashReports(context)
        return if (reports.isNotEmpty()) {
            reports.maxByOrNull { it.lastModified() }
        } else {
            null
        }
    }
    
    /**
     * Deletes a specific crash report.
     * @param file The crash report file to delete
     * @return True if the file was deleted successfully, false otherwise
     */
    fun deleteCrashReport(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting crash report: ${e.message}", e)
            false
        }
    }
    
    /**
     * Deletes all crash reports.
     * @return The number of files deleted
     */
    fun deleteAllCrashReports(context: Context): Int {
        val reports = listCrashReports(context)
        var deletedCount = 0
        
        for (report in reports) {
            if (deleteCrashReport(report)) {
                deletedCount++
            }
        }
        
        return deletedCount
    }
} 