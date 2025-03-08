package com.example.contentswiper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.contentswiper.model.Agent
import com.example.contentswiper.model.Content
import com.example.contentswiper.model.Converters
import com.example.contentswiper.model.UserPreference

/**
 * Main database for the application, using Room persistence library.
 */
@Database(
    entities = [Content::class, UserPreference::class, Agent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun contentDao(): ContentDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun agentDao(): AgentDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "content_swiper_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 