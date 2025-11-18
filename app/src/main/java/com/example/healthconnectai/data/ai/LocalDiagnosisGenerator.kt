package com.example.healthconnectai.data.ai

import android.util.Log

/**
 * Generador local de diagnósticos basados en datos capturados (audio/imagen)
 * sin depender de APIs externas. Se usa como fallback cuando Gemini/Generative API falla.
 */
class LocalDiagnosisGenerator {

    /**
     * Genera un diagnóstico basado en métricas de audio
     */
    fun generateDiagnosisFromAudio(audio: AudioMetrics, profile: UserProfile? = null): DiagnosisResult {
        Log.d("LocalDiagnosisGenerator", "Generando diagnóstico local desde audio: duration=${audio.duration}ms, pitch=${audio.averagePitch}Hz, cough=${audio.coughDetected}")
        
        val conditions = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var urgency = "BAJA"
        var shouldConsult = false
        
        // Análisis de tos
        if (audio.coughDetected) {
            conditions.add("Posible tos o irritación de garganta")
            recommendations.add("Mantente hidratado")
            recommendations.add("Evita irritantes (humo, polvo)")
            urgency = "MEDIA"
        }
        
        // Análisis de respiración
        when (audio.breathingPattern) {
            "acelerada" -> {
                conditions.add("Respiración acelerada")
                recommendations.add("Intenta respirar lentamente")
                recommendations.add("Descansa y busca un lugar tranquilo")
                urgency = maxOf(urgency, "MEDIA", compareBy { urgencyOrder(it) })
                shouldConsult = true
            }
            "superficial" -> {
                conditions.add("Respiración superficial o débil")
                recommendations.add("Respira profundamente desde el abdomen")
                urgency = maxOf(urgency, "MEDIA", compareBy { urgencyOrder(it) })
                shouldConsult = true
            }
            else -> {
                if (!audio.coughDetected) {
                    conditions.add("Patrón de respiración dentro de los parámetros normales")
                    recommendations.add("Continúa monitoreando tu estado de salud")
                }
            }
        }
        
        // Análisis de voz
        when (audio.voiceQuality) {
            "ronca" -> {
                conditions.add("Voz ronca o disfonía")
                recommendations.add("Evita forzar la voz")
                recommendations.add("Toma bebidas templadas")
                urgency = maxOf(urgency, "MEDIA", compareBy { urgencyOrder(it) })
            }
            "débil" -> {
                conditions.add("Voz débil o entrecortada")
                recommendations.add("Descansa vocal")
                urgency = maxOf(urgency, "ALTA", compareBy { urgencyOrder(it) })
                shouldConsult = true
            }
        }
        
        // Si no hay síntomas claros
        if (conditions.isEmpty()) {
            conditions.add("Sin síntomas significativos detectados en el análisis de audio")
            recommendations.add("Continúa con tus rutinas diarias normales")
        } else {
            recommendations.add("Consulta a un profesional si los síntomas persisten por más de 1 semana")
        }
        
        val diagnosis = "Análisis de audio local: ${conditions.joinToString(", ")}. Estado general: $urgency"
        
        return DiagnosisResult(
            preliminaryDiagnosis = diagnosis,
            potentialConditions = conditions.take(5),
            urgencyLevel = urgency,
            recommendations = recommendations.take(5),
            shouldConsultDoctor = shouldConsult || urgency in listOf("ALTA", "CRÍTICA"),
            success = true,
            errorMessage = null
        )
    }
    
    /**
     * Genera un diagnóstico basado en métricas de imagen
     */
    fun generateDiagnosisFromImage(image: ImageMetrics, profile: UserProfile? = null): DiagnosisResult {
        Log.d("LocalDiagnosisGenerator", "Generando diagnóstico local desde imagen: bodyPart=${image.bodyPart}")
        
        val conditions = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var urgency = "BAJA"
        var shouldConsult = false
        
        // Análisis por parte del cuerpo
        when {
            image.bodyPart.contains("garganta", ignoreCase = true) -> {
                conditions.add("Garganta analizada")
                recommendations.add("Observa cambios de color o inflamación")
                recommendations.add("Hidratación abundante")
                if (image.description.contains("roja", ignoreCase = true) || 
                    image.description.contains("inflamada", ignoreCase = true)) {
                    conditions.add("Posible enrojecimiento o inflamación")
                    urgency = "MEDIA"
                    shouldConsult = true
                }
            }
            image.bodyPart.contains("pecho", ignoreCase = true) -> {
                conditions.add("Imagen de pecho analizada")
                recommendations.add("Monitorea síntomas respiratorios")
                if (image.description.contains("erupción", ignoreCase = true)) {
                    conditions.add("Posible erupción o irritación cutánea")
                    urgency = "MEDIA"
                    shouldConsult = true
                }
            }
            image.bodyPart.contains("piel", ignoreCase = true) -> {
                conditions.add("Imagen de piel analizada")
                recommendations.add("Mantén la zona limpia y seca")
                if (image.description.contains("rojo", ignoreCase = true) || 
                    image.description.contains("inflamado", ignoreCase = true)) {
                    conditions.add("Posible inflamación o irritación")
                    urgency = "MEDIA"
                }
            }
            else -> {
                conditions.add("Imagen de ${image.bodyPart} analizada")
                recommendations.add("Describe cualquier cambio que notes")
            }
        }
        
        // Recomendaciones generales
        if (urgency == "BAJA") {
            recommendations.add("Continúa monitoreando el área fotografiada")
        }
        if (urgency in listOf("MEDIA", "ALTA")) {
            recommendations.add("Consulta a un médico si no ves mejoría en 48 horas")
            shouldConsult = true
        }
        
        val diagnosis = "Análisis de imagen local: ${image.bodyPart}. ${image.description}. Observaciones: ${conditions.joinToString("; ")}"
        
        return DiagnosisResult(
            preliminaryDiagnosis = diagnosis,
            potentialConditions = conditions.take(5),
            urgencyLevel = urgency,
            recommendations = recommendations.take(5),
            shouldConsultDoctor = shouldConsult,
            success = true,
            errorMessage = null
        )
    }
    
    /**
     * Genera diagnóstico combinado de audio + imagen
     */
    fun generateCombinedDiagnosis(
        audio: AudioMetrics? = null,
        image: ImageMetrics? = null,
        profile: UserProfile? = null
    ): DiagnosisResult {
        if (audio == null && image == null) {
            return DiagnosisResult.error("Sin datos de audio o imagen disponibles")
        }
        
        val allConditions = mutableListOf<String>()
        val allRecommendations = mutableSetOf<String>()
        var maxUrgency = "BAJA"
        var shouldConsult = false
        
        if (audio != null) {
            val audioDiagnosis = generateDiagnosisFromAudio(audio, profile)
            allConditions.addAll(audioDiagnosis.potentialConditions)
            allRecommendations.addAll(audioDiagnosis.recommendations)
            if (urgencyOrder(audioDiagnosis.urgencyLevel) > urgencyOrder(maxUrgency)) {
                maxUrgency = audioDiagnosis.urgencyLevel
            }
            shouldConsult = shouldConsult || audioDiagnosis.shouldConsultDoctor
        }
        
        if (image != null) {
            val imageDiagnosis = generateDiagnosisFromImage(image, profile)
            allConditions.addAll(imageDiagnosis.potentialConditions)
            allRecommendations.addAll(imageDiagnosis.recommendations)
            if (urgencyOrder(imageDiagnosis.urgencyLevel) > urgencyOrder(maxUrgency)) {
                maxUrgency = imageDiagnosis.urgencyLevel
            }
            shouldConsult = shouldConsult || imageDiagnosis.shouldConsultDoctor
        }
        
        // Eliminar duplicados
        val uniqueConditions = allConditions.distinct().take(5)
        val uniqueRecommendations = allRecommendations.distinct().take(5)
        
        val diagnosis = "Análisis local combinado: Se analizaron ${listOfNotNull(audio, image).size} fuente(s) de datos. " +
                "Condiciones detectadas: ${uniqueConditions.joinToString(", ")}."
        
        return DiagnosisResult(
            preliminaryDiagnosis = diagnosis,
            potentialConditions = uniqueConditions,
            urgencyLevel = maxUrgency,
            recommendations = uniqueRecommendations,
            shouldConsultDoctor = shouldConsult,
            success = true,
            errorMessage = null
        )
    }
    
    private fun urgencyOrder(level: String): Int = when (level) {
        "BAJA" -> 0
        "MEDIA" -> 1
        "ALTA" -> 2
        "CRÍTICA" -> 3
        else -> 0
    }
}
