plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

import java.util.Properties

// Cargar local.properties
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.reader().use { this.load(it) }
    }
}

// Solo una clave para Maps y Places
val geminiApiKeyFromLocal = localProps.getProperty("GEMINI_API_KEY") ?: ""
val googleApiKeyFromLocal = localProps.getProperty("GOOGLE_API_KEY") ?: "" // <- clave unificada

// Generar google_maps_api.xml dinámicamente desde local.properties
val generateGoogleMapsApiTask = tasks.register("generateGoogleMapsApi") {
    doLast {
        val outputDir = project.file("src/main/res/values")
        outputDir.mkdirs()
        val outputFile = file("$outputDir/google_maps_api.xml")
        outputFile.writeText("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!--
        Clave de API para Google Maps.
        Se obtiene desde local.properties (GOOGLE_API_KEY) en tiempo de compilación.
        Esta clave solo debe usarse en desarrollo y pruebas.
        Para producción, recuerda restringir la clave en Google Cloud.
    -->
    <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">
        $googleApiKeyFromLocal
    </string>
</resources>
""")
    }
}

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
            buildConfigField("String", "GOOGLE_API_KEY", "\"$googleApiKeyFromLocal\"") // <- unificada
            manifestPlaceholders["MAPS_API_KEY"] = googleApiKeyFromLocal
        }
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKeyFromLocal\"")
            buildConfigField("String", "GOOGLE_API_KEY", "\"$googleApiKeyFromLocal\"") // <- unificada
            manifestPlaceholders["MAPS_API_KEY"] = googleApiKeyFromLocal

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

// Agregar la tarea como dependencia del preBuild (fuera del bloque android)
tasks.named("preBuild").configure {
    dependsOn(generateGoogleMapsApiTask)
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.7.2")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.2.0")

    implementation("androidx.room:room-runtime:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    implementation("androidx.recyclerview:recyclerview:1.3.1")

    implementation("androidx.graphics:graphics-core:1.0.0-alpha01")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
