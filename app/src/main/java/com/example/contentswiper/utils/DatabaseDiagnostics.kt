package com.example.contentswiper.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Utility class for diagnosing and fixing database issues.
 */
object DatabaseDiagnostics {
    private const val TAG = "DatabaseDiagnostics"
    
    /**
     * Checks if the database is valid and can be opened.
     * @return true if the database is valid, false otherwise
     */
    fun isDatabaseValid(context: Context, dbName: String): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            Log.w(TAG, "Database file does not exist: $dbName")
            return false
        }
        
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            Log.d(TAG, "Database $dbName is valid and can be opened")
            true
        } catch (e: SQLiteException) {
            Log.e(TAG, "Database $dbName is invalid: ${e.message}", e)
            ErrorLogger.logDatabaseError(context, "validation", e)
            false
        } finally {
            db?.close()
        }
    }
    
    /**
     * Performs a PRAGMA integrity_check on the database.
     * @return true if the database passed the integrity check, false otherwise
     */
    fun checkDatabaseIntegrity(context: Context, dbName: String): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            Log.w(TAG, "Database file does not exist: $dbName")
            return false
        }
        
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            val cursor = db.rawQuery("PRAGMA integrity_check", null)
            val result = if (cursor.moveToFirst()) {
                val integrityResult = cursor.getString(0)
                Log.d(TAG, "Database integrity check result: $integrityResult")
                integrityResult == "ok"
            } else {
                Log.w(TAG, "Database integrity check returned no results")
                false
            }
            cursor.close()
            
            result
        } catch (e: SQLiteException) {
            Log.e(TAG, "Database integrity check failed: ${e.message}", e)
            ErrorLogger.logDatabaseError(context, "integrity_check", e)
            false
        } finally {
            db?.close()
        }
    }
    
    /**
     * Backs up the database to a zip file in the app's files directory.
     * @return the path to the backup file, or null if the backup failed
     */
    fun backupDatabase(context: Context, dbName: String): String? {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            Log.w(TAG, "Database file does not exist: $dbName")
            return null
        }
        
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupDir, "${dbName}_backup_$timestamp.zip")
        
        return try {
            val zipOut = ZipOutputStream(FileOutputStream(backupFile))
            
            // Add the main database file
            addFileToZip(zipOut, dbFile, dbFile.name)
            
            // Add journal files if they exist
            val journalFile = File(dbFile.parentFile, "$dbName-journal")
            if (journalFile.exists()) {
                addFileToZip(zipOut, journalFile, journalFile.name)
            }
            
            val walFile = File(dbFile.parentFile, "$dbName-wal")
            if (walFile.exists()) {
                addFileToZip(zipOut, walFile, walFile.name)
            }
            
            val shmFile = File(dbFile.parentFile, "$dbName-shm")
            if (shmFile.exists()) {
                addFileToZip(zipOut, shmFile, shmFile.name)
            }
            
            zipOut.close()
            Log.d(TAG, "Database backup created at ${backupFile.absolutePath}")
            backupFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Database backup failed: ${e.message}", e)
            ErrorLogger.logException(context, "database_backup", e)
            null
        }
    }
    
    /**
     * Adds a file to a zip output stream.
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        val fis = FileInputStream(file)
        val zipEntry = ZipEntry(entryName)
        zipOut.putNextEntry(zipEntry)
        
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zipOut.write(bytes, 0, length)
        }
        
        fis.close()
        zipOut.closeEntry()
    }
    
    /**
     * Deletes the database and its associated files.
     * @return true if the database was successfully deleted, false otherwise
     */
    fun deleteDatabase(context: Context, dbName: String): Boolean {
        try {
            // Backup the database before deleting
            backupDatabase(context, dbName)
            
            // Delete the database
            val deleted = context.deleteDatabase(dbName)
            Log.d(TAG, "Database $dbName deleted: $deleted")
            return deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting database: ${e.message}", e)
            ErrorLogger.logException(context, "database_deletion", e)
            return false
        }
    }
    
    /**
     * Gets information about the database.
     * @return a string containing information about the database
     */
    fun getDatabaseInfo(context: Context, dbName: String): String {
        val dbFile = context.getDatabasePath(dbName)
        val sb = StringBuilder()
        
        sb.appendLine("Database: $dbName")
        sb.appendLine("Exists: ${dbFile.exists()}")
        
        if (dbFile.exists()) {
            sb.appendLine("Path: ${dbFile.absolutePath}")
            sb.appendLine("Size: ${dbFile.length() / 1024} KB")
            sb.appendLine("Last modified: ${java.util.Date(dbFile.lastModified())}")
            
            // Check for journal files
            val journalFile = File(dbFile.parentFile, "$dbName-journal")
            sb.appendLine("Journal exists: ${journalFile.exists()}")
            if (journalFile.exists()) {
                sb.appendLine("Journal size: ${journalFile.length() / 1024} KB")
            }
            
            val walFile = File(dbFile.parentFile, "$dbName-wal")
            sb.appendLine("WAL exists: ${walFile.exists()}")
            if (walFile.exists()) {
                sb.appendLine("WAL size: ${walFile.length() / 1024} KB")
            }
            
            val shmFile = File(dbFile.parentFile, "$dbName-shm")
            sb.appendLine("SHM exists: ${shmFile.exists()}")
            if (shmFile.exists()) {
                sb.appendLine("SHM size: ${shmFile.length() / 1024} KB")
            }
            
            // Check database validity
            sb.appendLine("Valid: ${isDatabaseValid(context, dbName)}")
            sb.appendLine("Integrity check: ${checkDatabaseIntegrity(context, dbName)}")
            
            // Get table info if possible
            try {
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                
                // Get list of tables
                val tablesCursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'",
                    null
                )
                
                sb.appendLine("\nTables:")
                while (tablesCursor.moveToNext()) {
                    val tableName = tablesCursor.getString(0)
                    sb.appendLine("- $tableName")
                    
                    // Get row count for each table
                    try {
                        val countCursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
                        if (countCursor.moveToFirst()) {
                            val count = countCursor.getInt(0)
                            sb.appendLine("  Rows: $count")
                        }
                        countCursor.close()
                    } catch (e: Exception) {
                        sb.appendLine("  Error getting row count: ${e.message}")
                    }
                }
                
                tablesCursor.close()
                db.close()
            } catch (e: Exception) {
                sb.appendLine("\nError getting table info: ${e.message}")
            }
        }
        
        return sb.toString()
    }
} 