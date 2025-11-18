plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // ✅ Necesario para Room (procesamiento de anotaciones)
}

import java.util.Properties

// Cargar local.properties para obtener secretos locales (no se deben commitear)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.reader().use { this.load(it) }
    }
}
val geminiApiKeyFromLocal: String = localProps.getProperty("GEMINI_API_KEY") ?: ""
val googlePlacesApiKeyFromLocal: String = localProps.getProperty("GOOGLE_PLACES_API_KEY") ?: ""
val mapsApiKeyFromLocal: String = localProps.getProperty("GOOGLE_MAPS_API_KEY") ?: ""

android {
    namespace = "com.example.healthconnectai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.healthconnectai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKeyFromLocal\"")
            buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"$googlePlacesApiKeyFromLocal\"")
            manifestPlaceholders["MAPS_API_KEY"] = mapsApiKeyFromLocal
        }
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKeyFromLocal\"")
            buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"$googlePlacesApiKeyFromLocal\"")
            manifestPlaceholders["MAPS_API_KEY"] = mapsApiKeyFromLocal
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // --- DEPENDENCIAS BÁSICAS ---
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.7.2")

    // OkHttp para peticiones HTTP
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.7.2")

    // --- GOOGLE MAPS ---
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.2.0")

    // --- ROOM (BASE DE DATOS LOCAL) ---
    implementation("androidx.room:room-runtime:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    // --- RETROFIT (SERVICIOS REST) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp logging interceptor para depuración de peticiones
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // --- COROUTINES (para llamadas asíncronas) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel y LiveData (para MVVM)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    // --- VIEWMODEL + LIVEDATA (MVVM) ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    // --- RECYCLERVIEW (para listas de tareas) ---
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // --- GOOGLE GENERATIVE AI (GEMINI) - ✨ NUEVA ---
        // Usando REST API con OkHttp y Retrofit (ya incluidos arriba)
        // No necesita dependencias adicionales
    
    // Para manejo avanzado de imágenes
    implementation("androidx.graphics:graphics-core:1.0.0-alpha01")
    
    // Para parseo JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // --- TESTS ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

