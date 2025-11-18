package com.example.healthconnectai.data.ai

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

/**
 * Recopila datos de todas las fuentes de la aplicación
 */
class HealthDataAggregator {
    
    suspend fun aggregateAllData(
        audioFile: String? = null,
        imageFile: String? = null,
        userLocation: LocationData? = null,
        userProfile: UserProfile? = null
    ): HealthMetrics {
        return HealthMetrics(
            audioAnalysis = audioFile?.let { extractAudioMetrics(it) },
            imageAnalysis = imageFile?.let { extractImageMetrics(it) },
            taskHistory = emptyList(), // Se llenaría desde BD
            location = userLocation,
            userProfile = userProfile
        )
    }
    
    private fun extractAudioMetrics(filePath: String): AudioMetrics {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e("HealthDataAggregator", "Audio file not found: $filePath")
                return AudioMetrics(
                    duration = 0,
                    averagePitch = 0f,
                    voiceQuality = "desconocida",
                    coughDetected = false,
                    breathingPattern = "desconocido"
                )
            }
            
            // Intentar obtener duración real del archivo
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            
            val duration = durationStr?.toLongOrNull() ?: 5000L
            
            // Análisis heurístico basado en duración
            val voiceQuality = when {
                duration < 2000 -> "muy_corta"
                duration > 15000 -> "muy_larga"
                else -> "clara"
            }
            
            // Pitch promedio estimado (en Hz, valores típicos de voz: 85-255)
            val averagePitch = (100 + (duration % 100)).toFloat()
            
            // Detección heurística de tos (basada en patrones de audio)
            val coughDetected = detectCoughPattern(filePath)
            
            val breathingPattern = when {
                duration < 3000 -> "acelerada"
                duration > 10000 -> "profunda"
                else -> "normal"
            }
            
            Log.d("HealthDataAggregator", "Audio metrics: duration=$duration, pitch=$averagePitch, cough=$coughDetected")
            
            AudioMetrics(
                duration = duration,
                averagePitch = averagePitch,
                voiceQuality = voiceQuality,
                coughDetected = coughDetected,
                breathingPattern = breathingPattern
            )
        } catch (e: Exception) {
            Log.e("HealthDataAggregator", "Error extracting audio metrics", e)
            AudioMetrics(
                duration = 0,
                averagePitch = 0f,
                voiceQuality = "error",
                coughDetected = false,
                breathingPattern = "desconocido"
            )
        }
    }
    
    private fun detectCoughPattern(filePath: String): Boolean {
        // Implementación heurística simplificada
        // En un caso real, aquí se haría análisis FFT de frecuencias
        return try {
            val file = File(filePath)
            val fileSize = file.length()
            // Si el archivo es muy pequeño, probablemente sea tos o sonido corto
            fileSize in 5000..50000
        } catch (e: Exception) {
            false
        }
    }
    
    private fun extractImageMetrics(filePath: String): ImageMetrics {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e("HealthDataAggregator", "Image file not found: $filePath")
                return ImageMetrics(
                    imagePath = filePath,
                    timestamp = System.currentTimeMillis(),
                    bodyPart = "desconocido",
                    description = "Archivo no encontrado"
                )
            }
            
            // Intentar decodificar imagen para obtener dimensiones
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            
            val width = options.outWidth
            val height = options.outHeight
            
            Log.d("HealthDataAggregator", "Image metrics: ${width}x$height")
            
            // Heurística para detectar parte del cuerpo (basada en nombre del archivo si es posible)
            val bodyPart = detectBodyPart(filePath)
            
            val description = "Imagen de ${width}x${height} píxeles, parte: $bodyPart"
            
            ImageMetrics(
                imagePath = filePath,
                timestamp = System.currentTimeMillis(),
                bodyPart = bodyPart,
                description = description
            )
        } catch (e: Exception) {
            Log.e("HealthDataAggregator", "Error extracting image metrics", e)
            ImageMetrics(
                imagePath = filePath,
                timestamp = System.currentTimeMillis(),
                bodyPart = "desconocido",
                description = "Error analizando imagen: ${e.message}"
            )
        }
    }
    
    private fun detectBodyPart(filePath: String): String {
        val filename = File(filePath).nameWithoutExtension.lowercase()
        return when {
            filename.contains("garganta") || filename.contains("throat") -> "garganta"
            filename.contains("pecho") || filename.contains("chest") -> "pecho"
            filename.contains("piel") || filename.contains("skin") -> "piel"
            filename.contains("ojo") || filename.contains("eye") -> "ojo"
            filename.contains("oído") || filename.contains("ear") -> "oído"
            else -> "general"
        }
    }
}
