package com.example.contentswiper.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.Converters
import com.example.contentswiper.model.UserPreference

/**
 * Main database for the application, using Room persistence library.
 */
@Database(
    entities = [Content::class, UserPreference::class, Agent::class],
    version = 5,
    exportSchema = true // Changed to true to export schema for better debugging
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun contentDao(): ContentDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun agentDao(): AgentDao
    
    companion object {
        private const val TAG = "AppDatabase"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add your migration code here if needed
                    // Example: database.execSQL("ALTER TABLE agents ADD COLUMN learning_factor REAL NOT NULL DEFAULT 0.5")
                    Log.d(TAG, "Migration 1->2 executed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 1->2: ${e.message}", e)
                    throw e
                }
            }
        }
        
        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add your migration code here if needed
                    // Example: database.execSQL("ALTER TABLE content ADD COLUMN evolution_contribution REAL NOT NULL DEFAULT 0")
                    Log.d(TAG, "Migration 2->3 executed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 2->3: ${e.message}", e)
                    throw e
                }
            }
        }
        
        // Migration from version 3 to 4 for the new knowledge graph fields
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add new columns to Content table
                    database.execSQL("ALTER TABLE content ADD COLUMN knowledge_node_ids TEXT NOT NULL DEFAULT '[]'")
                    database.execSQL("ALTER TABLE content ADD COLUMN knowledge_contribution REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE content ADD COLUMN cross_domain_score REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE content ADD COLUMN novelty_score REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE content ADD COLUMN resilience_score REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE content ADD COLUMN adaptive_elements TEXT NOT NULL DEFAULT '[]'")
                    database.execSQL("ALTER TABLE content ADD COLUMN self_healing_triggers TEXT NOT NULL DEFAULT '[]'")
                    database.execSQL("ALTER TABLE content ADD COLUMN healing_history TEXT NOT NULL DEFAULT '[]'")
                    
                    // Add new columns to Agent table
                    database.execSQL("ALTER TABLE agents ADD COLUMN knowledge_nodes TEXT NOT NULL DEFAULT '[]'")
                    database.execSQL("ALTER TABLE agents ADD COLUMN connection_strength TEXT NOT NULL DEFAULT '{}'")
                    database.execSQL("ALTER TABLE agents ADD COLUMN bridge_score REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE agents ADD COLUMN hub_score REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE agents ADD COLUMN specialty_domains TEXT NOT NULL DEFAULT '[]'")
                    database.execSQL("ALTER TABLE agents ADD COLUMN adaptability_score REAL NOT NULL DEFAULT 0.5")
                    database.execSQL("ALTER TABLE agents ADD COLUMN self_repair_mechanisms TEXT NOT NULL DEFAULT '[]'")
                    database.execSQL("ALTER TABLE agents ADD COLUMN last_self_healing_time INTEGER NOT NULL DEFAULT 0")
                    
                    Log.d(TAG, "Migration 3->4 executed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 3->4: ${e.message}", e)
                    throw e
                }
            }
        }
        
        // Migration from version 4 to 5 to handle model changes
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    Log.d(TAG, "Starting migration 4->5")
                    
                    // Since we can't easily modify the database schema to match the new model classes,
                    // we'll recreate the tables with the new schema
                    
                    // Create temporary tables with the new schema
                    database.execSQL("CREATE TABLE agents_new (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "name TEXT NOT NULL, " +
                            "description TEXT NOT NULL, " +
                            "interests TEXT NOT NULL, " +
                            "personality TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL, " +
                            "isActive INTEGER NOT NULL, " +
                            "version INTEGER NOT NULL, " +
                            "lastEvolutionTime INTEGER NOT NULL, " +
                            "evolutionHistory TEXT NOT NULL, " +
                            "contentGenerationCount INTEGER NOT NULL, " +
                            "contentLikeRate REAL NOT NULL, " +
                            "contentEngagementScore REAL NOT NULL, " +
                            "learningFactor REAL NOT NULL, " +
                            "knowledgeNodes TEXT NOT NULL, " +
                            "connectionStrength TEXT NOT NULL, " +
                            "bridgeScore REAL NOT NULL, " +
                            "hubScore REAL NOT NULL, " +
                            "specialtyDomains TEXT NOT NULL, " +
                            "adaptabilityScore REAL NOT NULL, " +
                            "selfRepairMechanisms TEXT NOT NULL, " +
                            "lastSelfHealingTime INTEGER NOT NULL, " +
                            "metadata TEXT NOT NULL)")
                    
                    database.execSQL("CREATE TABLE content_new (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "title TEXT NOT NULL, " +
                            "description TEXT NOT NULL, " +
                            "imageUrl TEXT, " +
                            "imagePrompt TEXT, " +
                            "tags TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL, " +
                            "generatedBy TEXT NOT NULL, " +
                            "metadata TEXT NOT NULL, " +
                            "agentId TEXT, " +
                            "agentVersion INTEGER NOT NULL, " +
                            "likeCount INTEGER NOT NULL, " +
                            "dislikeCount INTEGER NOT NULL, " +
                            "engagementScore REAL NOT NULL, " +
                            "feedbackSummary TEXT, " +
                            "evolutionContribution REAL NOT NULL, " +
                            "creationSummary TEXT, " +
                            "knowledgeNodeIds TEXT NOT NULL, " +
                            "knowledgeContribution REAL NOT NULL, " +
                            "crossDomainScore REAL NOT NULL, " +
                            "noveltyScore REAL NOT NULL, " +
                            "resilienceScore REAL NOT NULL, " +
                            "adaptiveElements TEXT NOT NULL, " +
                            "selfHealingTriggers TEXT NOT NULL, " +
                            "healingHistory TEXT NOT NULL)")
                    
                    // Copy data from old tables to new tables with default values for new columns
                    database.execSQL("INSERT INTO agents_new " +
                            "SELECT id, name, description, interests, personality, createdAt, isActive, " +
                            "version, lastEvolutionTime, evolutionHistory, contentGenerationCount, " +
                            "contentLikeRate, contentEngagementScore, learningFactor, " +
                            "knowledge_nodes, connection_strength, bridge_score, hub_score, " +
                            "specialty_domains, adaptability_score, self_repair_mechanisms, " +
                            "last_self_healing_time, metadata FROM agents")
                    
                    database.execSQL("INSERT INTO content_new " +
                            "SELECT id, title, description, imageUrl, imagePrompt, tags, createdAt, " +
                            "generatedBy, metadata, agentId, agentVersion, likeCount, dislikeCount, " +
                            "engagementScore, feedbackSummary, evolutionContribution, creationSummary, " +
                            "knowledge_node_ids, knowledge_contribution, cross_domain_score, " +
                            "novelty_score, resilience_score, adaptive_elements, " +
                            "self_healing_triggers, healing_history FROM content")
                    
                    // Drop old tables
                    database.execSQL("DROP TABLE agents")
                    database.execSQL("DROP TABLE content")
                    
                    // Rename new tables to original names
                    database.execSQL("ALTER TABLE agents_new RENAME TO agents")
                    database.execSQL("ALTER TABLE content_new RENAME TO content")
                    
                    Log.d(TAG, "Migration 4->5 executed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 4->5: ${e.message}", e)
                    throw e
                }
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    Log.d(TAG, "Building Room database instance")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "content_swiper_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // Keep this as a last resort
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // Better performance
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d(TAG, "Database created successfully")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d(TAG, "Database opened successfully")
                        }
                    })
                    .build()
                    
                    INSTANCE = instance
                    Log.d(TAG, "Room database instance built successfully")
                    instance
                } catch (e: Exception) {
                    Log.e(TAG, "Error building Room database: ${e.message}", e)
                    throw RuntimeException("Database build error: ${e.message}", e)
                }
            }
        }
    }
} 