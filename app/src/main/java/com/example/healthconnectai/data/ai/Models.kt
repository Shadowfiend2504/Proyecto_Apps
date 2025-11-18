package com.example.healthconnectai.data.ai

/**
 * Estructura consolidada de todas las métricas de salud del usuario
 */
data class HealthMetrics(
    val audioAnalysis: AudioMetrics? = null,
    val imageAnalysis: ImageMetrics? = null,
    val taskHistory: List<HealthTask> = emptyList(),
    val location: LocationData? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val userProfile: UserProfile? = null
)

/**
 * Métricas extraídas del análisis de audio
 */
data class AudioMetrics(
    val duration: Long,                    // Duración en ms
    val averagePitch: Float,              // Frecuencia fundamental en Hz
    val voiceQuality: String,             // "clara", "ronca", "débil"
    val coughDetected: Boolean,           // ¿Se detectó tos?
    val breathingPattern: String          // "normal", "acelerada", "superficial"
)

/**
 * Métricas extraídas del análisis de imagen
 */
data class ImageMetrics(
    val imagePath: String,
    val timestamp: Long,
    val bodyPart: String,                 // "garganta", "pecho", "piel", etc.
    val description: String               // Análisis descriptivo
)

/**
 * Tarea de salud / Síntoma reportado
 */
data class HealthTask(
    val symptom: String,
    val severity: Int,                    // 1-5
    val duration: String,                 // "2 días", "1 semana"
    val date: String,
    val notes: String = ""
)

/**
 * Información del usuario para contexto
 */
data class UserProfile(
    val age: Int = 0,
    val gender: String = "",
    val medicalHistory: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList()
)

/**
 * Datos de ubicación
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f
)

/**
 * Resultado final del diagnóstico
 */
data class DiagnosisResult(
    val preliminaryDiagnosis: String = "",
    val potentialConditions: List<String> = emptyList(),
    val urgencyLevel: String = "BAJA",
    val recommendations: List<String> = emptyList(),
    val shouldConsultDoctor: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val errorMessage: String? = null
) {
    companion object {
        fun error(message: String): DiagnosisResult {
            return DiagnosisResult(
                preliminaryDiagnosis = "Error en diagnóstico",
                errorMessage = message,
                success = false
            )
        }
    }
}
