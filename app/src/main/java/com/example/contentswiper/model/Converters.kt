package com.example.contentswiper.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database to handle complex data types.
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromFloatMap(value: Map<String, Float>?): String {
        return gson.toJson(value ?: emptyMap<String, Float>())
    }
    
    @TypeConverter
    fun toFloatMap(value: String): Map<String, Float> {
        val mapType = object : TypeToken<Map<String, Float>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromEvolutionRecordList(value: List<EvolutionRecord>?): String {
        return gson.toJson(value ?: emptyList<EvolutionRecord>())
    }
    
    @TypeConverter
    fun toEvolutionRecordList(value: String): List<EvolutionRecord> {
        val listType = object : TypeToken<List<EvolutionRecord>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromAnyMap(value: Map<String, Any>?): String {
        return gson.toJson(value ?: emptyMap<String, Any>())
    }
    
    @TypeConverter
    fun toAnyMap(value: String): Map<String, Any> {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    // New converters for KnowledgeNode
    @TypeConverter
    fun fromKnowledgeNodeList(value: List<KnowledgeNode>?): String {
        return gson.toJson(value ?: emptyList<KnowledgeNode>())
    }
    
    @TypeConverter
    fun toKnowledgeNodeList(value: String): List<KnowledgeNode> {
        val listType = object : TypeToken<List<KnowledgeNode>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    // New converters for AdaptiveElement
    @TypeConverter
    fun fromAdaptiveElementList(value: List<AdaptiveElement>?): String {
        return gson.toJson(value ?: emptyList<AdaptiveElement>())
    }
    
    @TypeConverter
    fun toAdaptiveElementList(value: String): List<AdaptiveElement> {
        val listType = object : TypeToken<List<AdaptiveElement>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    // New converters for HealingEvent
    @TypeConverter
    fun fromHealingEventList(value: List<HealingEvent>?): String {
        return gson.toJson(value ?: emptyList<HealingEvent>())
    }
    
    @TypeConverter
    fun toHealingEventList(value: String): List<HealingEvent> {
        val listType = object : TypeToken<List<HealingEvent>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    // New converter for KnowledgeGraphDelta
    @TypeConverter
    fun fromKnowledgeGraphDelta(value: KnowledgeGraphDelta?): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toKnowledgeGraphDelta(value: String): KnowledgeGraphDelta? {
        return gson.fromJson(value, KnowledgeGraphDelta::class.java)
    }
} 