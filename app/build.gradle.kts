plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

import java.io.FileInputStream
import java.util.Properties

// Function to safely get environment variables with a default value
fun getEnvOrDefault(key: String, defaultValue: String): String {
    // First try to get from system environment
    val envValue = System.getenv(key)
    if (!envValue.isNullOrEmpty()) {
        println("Found $key in environment variables (length: ${envValue.length})")
        return envValue
    } else {
        println("No $key found in environment variables")
    }
    
    // Then try to load from .env file
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        println(".env file exists at: ${envFile.absolutePath}")
        try {
            val properties = Properties()
            FileInputStream(envFile).use { stream ->
                properties.load(stream)
            }
            val propValue = properties.getProperty(key)
            if (!propValue.isNullOrEmpty()) {
                println("Found $key in .env file (length: ${propValue.length})")
                return propValue
            } else {
                println("No $key found in .env file or it's empty")
            }
        } catch (e: Exception) {
            println("Error reading .env file: ${e.message}")
        }
    } else {
        println(".env file does not exist at: ${envFile.absolutePath}")
    }
    
    // Fallback to default
    println("Using default value for $key")
    return defaultValue
}

// Function to read the .env file content for debugging
fun readEnvFileContent(): String {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        try {
            return envFile.readText()
        } catch (e: Exception) {
            return "Error reading .env file: ${e.message}"
        }
    }
    return ".env file does not exist"
}

android {
    namespace = "com.example.contentswiper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.contentswiper"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Room schema export location
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
        
        // Print .env file content for debugging
        println("==== .env file content ====")
        println(readEnvFileContent())
        println("==========================")
        
        // Add API key from environment variable or .env file
        val apiKey = getEnvOrDefault("OPENAI_API_KEY", "")
        buildConfigField("String", "OPENAI_API_KEY", "\"$apiKey\"")
        
        // Log the API key being used (first 5 chars only for security)
        val keyPreview = if (apiKey.length > 5) apiKey.substring(0, 5) + "..." else "not found"
        println("Using API key: $keyPreview")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true // Enable BuildConfig generation
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Room for local database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Card stack view for swipeable cards
    implementation("com.github.yuyakaido:cardstackview:2.3.4")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
} 