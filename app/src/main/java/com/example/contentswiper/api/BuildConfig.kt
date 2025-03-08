package com.example.contentswiper.api

/**
 * API configuration class to safely access API keys.
 */
object ApiConfig {
    // Method to get API key from environment or BuildConfig
    fun getApiKey(): String {
        // First try to get from system environment
        val envKey = System.getenv("OPENAI_API_KEY")
        if (!envKey.isNullOrEmpty()) {
            return envKey
        }
        
        // Fallback to BuildConfig (which should be populated during build)
        return com.example.contentswiper.BuildConfig.OPENAI_API_KEY
    }
} 