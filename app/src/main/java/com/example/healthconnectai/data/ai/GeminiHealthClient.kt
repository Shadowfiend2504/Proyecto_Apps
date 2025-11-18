package com.example.healthconnectai.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

/**
 * Cliente REST para Google Generative Language API (Gemini)
 * Implementación ligera basada en OkHttp + Gson para evitar SDKs no disponibles.
 */
class GeminiHealthClient(private val apiKey: String) {
    private val gson = Gson()

    // Cliente OkHttp con timeouts y logging para depuración.
    private val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()
    }
    // Usamos el modelo por defecto que actualmente está disponible.
    private val baseUrl = "https://generativelanguage.googleapis.com/v1/models"
    // Se fija a gemini-2.5-flash ya que es el modelo disponible en el entorno actual.
    private val modelId = "gemini-2.5-flash"

    suspend fun analyzeHealthData(metrics: HealthMetrics): DiagnosisResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            val errorMsg = "❌ GEMINI_API_KEY está vacío o no configurado.\n\n" +
                    "Para usar análisis de IA:\n" +
                    "1. Ve a: local.properties\n" +
                    "2. Agrega: GEMINI_API_KEY=tu_clave_aqui\n" +
                    "3. Recompila el proyecto\n\n" +
                    "Mientras tanto, se usará análisis local fallback."
            Log.w("GeminiHealthClient", errorMsg)
            // Usar generador local como fallback
            return@withContext DiagnosisResult.error(errorMsg)
        }

        val prompt = buildJsonHealthPrompt(metrics)
        try {
            val apiResp = callTextModel(prompt)
            // Si la respuesta indica un error, convertir a DiagnosisResult.error
            if (apiResp.startsWith("ERROR:")) {
                return@withContext DiagnosisResult.error(apiResp.removePrefix("ERROR:").trim())
            }
            // Prefer JSON parsing to save tokens; fall back to text parsing if needed
            val jsonParsed = try { parseJsonDiagnosis(apiResp) } catch (_: Exception) { null }
            if (jsonParsed != null) return@withContext jsonParsed
            parseHealthDiagnosis(apiResp)
        } catch (t: Throwable) {
            Log.e("GeminiHealthClient", "analyzeHealthData failed", t)
            DiagnosisResult.error("Error en análisis de IA: ${t.message}")
        }
    }

    suspend fun analyzeHealthImage(bitmap: Bitmap, bodyPart: String, additionalContext: String = ""): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "ERROR: GEMINI_API_KEY está vacío. Añade tu clave en local.properties y recompila."

        try {
            val b64 = bitmapToBase64(bitmap)
            val prompt = "Analiza esta imagen del cuerpo: $bodyPart. $additionalContext"
            val resp = callTextModelWithImage(prompt, b64)
            if (resp.startsWith("ERROR:")) resp else resp
        } catch (t: Throwable) {
            Log.e("GeminiHealthClient", "analyzeHealthImage failed", t)
            "Error al analizar imagen: ${t.message}"
        }
    }

    suspend fun analyzeVoiceMetrics(duration: Long, pitchVariation: Float, breathingPattern: String, coughDetected: Boolean): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "ERROR: GEMINI_API_KEY está vacío. Añade tu clave en local.properties y recompila."

        val prompt = "Analiza métricas de voz:\n- Duración: ${duration}ms\n- PitchVar: ${pitchVariation}\n- Respiración: $breathingPattern\n- Tos: $coughDetected"
        try {
            val resp = callTextModel(prompt)
            if (resp.startsWith("ERROR:")) resp else resp
        } catch (t: Throwable) {
            Log.e("GeminiHealthClient", "analyzeVoiceMetrics failed", t)
            "Error al analizar audio: ${t.message}"
        }
    }

    private fun buildComprehensiveHealthPrompt(metrics: HealthMetrics): String {
        val profile = metrics.userProfile
        val tasks = if (metrics.taskHistory.isNotEmpty()) metrics.taskHistory.joinToString("\n") { "- ${it.symptom} (sev:${it.severity}, dur:${it.duration})" } else "Sin síntomas reportados"
        val audio = metrics.audioAnalysis
        val image = metrics.imageAnalysis
        val location = metrics.location

        val age = profile?.age ?: 0
        val gender = profile?.gender ?: "N/D"
        val history = profile?.medicalHistory?.joinToString(", ") ?: "N/D"

        return """
            DIAGNÓSTICO PRELIMINAR
            Timestamp: ${metrics.timestamp}

            PERFIL: edad=$age, género=$gender, antecedentes=$history

            SÍNTOMAS:
            $tasks

            AUDIO: ${audio?.let { "dur=${it.duration}ms, pitch=${it.averagePitch}, cough=${it.coughDetected}" } ?: "N/D"}
            IMAGEN: ${image?.let { "parte=${it.bodyPart}, desc=${it.description}" } ?: "N/D"}
            UBICACIÓN: ${location?.let { "lat=${it.latitude}, lon=${it.longitude}" } ?: "N/D"}

            Instrucciones: responde con un bloque claro que incluya: DIAGNÓSTICO PRELIMINAR, POSIBLES CONDICIONES (3), URGENCIA (BAJA/MEDIA/ALTA/CRÍTICA), RECOMENDACIONES (3-5) y si debe consultar a un médico. Añade disclaimer.
        """.trimIndent()
    }

    // Build a compact JSON prompt asking the model to return a strict JSON object.
    private fun buildJsonHealthPrompt(metrics: HealthMetrics): String {
        val profile = metrics.userProfile
        val tasks = if (metrics.taskHistory.isNotEmpty()) metrics.taskHistory.joinToString("\\n") { "- ${it.symptom} (sev:${it.severity}, dur:${it.duration})" } else "Sin síntomas reportados"
        val audio = metrics.audioAnalysis
        val image = metrics.imageAnalysis
        val location = metrics.location

        val age = profile?.age ?: 0
        val gender = profile?.gender ?: "N/D"
        val history = profile?.medicalHistory?.joinToString(", ") ?: "N/D"

        // Instructions: return ONLY a compact JSON object with exact keys.
        return """
        Responde SOLO con un JSON compacto (sin texto adicional) que contenga las claves:
        - preliminaryDiagnosis: string
        - potentialConditions: array of strings
        - urgencyLevel: one of [BAJA, MEDIA, ALTA, CRÍTICA]
        - recommendations: array of strings
        - shouldConsultDoctor: boolean
        - disclaimer: string (muy corto)

        Proporciona valores concisos. Usa el siguiente contexto para generar el JSON:
        perfil: {"age": $age, "gender": "$gender", "history": "$history"}
        sintomas:\n$tasks
        audio: ${audio?.let { "{duration:${it.duration}, pitch:${it.averagePitch}, cough:${it.coughDetected}}" } ?: "N/D"}
        imagen: ${image?.let { "{bodyPart:'${it.bodyPart}', desc:'${it.description}'}" } ?: "N/D"}
        ubicacion: ${location?.let { "lat=${it.latitude}, lon=${it.longitude}" } ?: "N/D"}

        Devuelve únicamente el JSON en una sola línea compacta. No añadas explicaciones ni etiquetas.
        """.trimIndent()
    }

    // Llama al modelo de texto (sin imagen)
    private suspend fun callTextModel(prompt: String): String = withContext(Dispatchers.IO) {
        val url = "$baseUrl/$modelId:generateContent?key=$apiKey"
        // Construir payload para v1 API: contents[].parts[].text
        val json = com.google.gson.JsonObject()
        val contentsArray = com.google.gson.JsonArray()
        val contentObj = com.google.gson.JsonObject()
        val partsArray = com.google.gson.JsonArray()
        val partObj = com.google.gson.JsonObject()
        partObj.addProperty("text", prompt)
        partsArray.add(partObj)
        contentObj.add("parts", partsArray)
        contentsArray.add(contentObj)
        json.add("contents", contentsArray)
        
        // Agregar generationConfig correctamente
        val genConfig = com.google.gson.JsonObject()
        genConfig.addProperty("temperature", 0.6)
        // Incrementamos maxOutputTokens para evitar truncados por MAX_TOKENS
        genConfig.addProperty("maxOutputTokens", 1400)
        json.add("generationConfig", genConfig)
        val body = json.toString()

        val req = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string()
                if (!resp.isSuccessful) {
                    val detail = respBody?.takeIf { it.isNotBlank() } ?: "(no body)"
                    // Manejo específico para códigos comunes
                    when (resp.code) {
                        401, 403 -> return@withContext "ERROR: Autenticación fallida (código ${resp.code}). Verifica GEMINI_API_KEY y restricciones de la clave. Detalle: $detail"
                        429 -> return@withContext "ERROR: Límite de peticiones alcanzado (429). Intenta más tarde."
                        404 -> {
                            // Intentar obtener un modelo alternativo disponible y reintentar
                            val alt = fetchAvailableModel()
                            if (!alt.isNullOrBlank()) {
                                Log.w("GeminiHealthClient", "Modelo ${modelId} no disponible, reintentando con: $alt")
                                val retryUrl = "$baseUrl/$alt:generateContent?key=$apiKey"
                                val retryReq = Request.Builder()
                                    .url(retryUrl)
                                    .post(body.toRequestBody("application/json".toMediaType()))
                                    .build()
                                client.newCall(retryReq).execute().use { r2 ->
                                    val b2 = r2.body?.string()
                                    if (!r2.isSuccessful) return@withContext "ERROR: API error ${r2.code}: ${b2 ?: "(no body)"}"
                                    val extracted2 = b2?.let { extractTextFromResponse(it) }
                                    return@withContext extracted2 ?: "(sin texto extraído)"
                                }
                            }
                            return@withContext "ERROR: Modelo o endpoint no encontrado (404). Asegúrate de que el modelo configurado está disponible. Detalle: $detail"
                        }
                        else -> return@withContext "ERROR: API error ${resp.code}: $detail"
                    }
                }
                val s = respBody ?: return@withContext "ERROR: Respuesta vacía del servicio"
                val extracted = extractTextFromResponse(s)

                // Si la respuesta fue truncada por max tokens, intentar una continuación automática
                try {
                    val root = gson.fromJson(s, JsonElement::class.java).asJsonObject
                    if (root.has("candidates")) {
                        val cand = root.getAsJsonArray("candidates").get(0).asJsonObject
                        if (cand.has("finishReason") && cand.get("finishReason").asString == "MAX_TOKENS") {
                            Log.w("GeminiHealthClient", "Respuesta truncada por MAX_TOKENS — intentando continuación automática")
                            val partial = extracted ?: ""
                            val contPrompt = "Continúa la respuesta anterior y complétala coherentemente. Texto actual:\n" + partial + "\n\nContinúa desde aquí sin repetir lo anterior. Mantén el formato de DIAGNÓSTICO PRELIMINAR solicitado."

                            // Reintento simple: una sola continuación
                            val contUrl = "$baseUrl/$modelId:generateContent?key=$apiKey"
                            val contJson = com.google.gson.JsonObject()
                            val contentsArray2 = com.google.gson.JsonArray()
                            val contentObj2 = com.google.gson.JsonObject()
                            val partsArray2 = com.google.gson.JsonArray()
                            val part2 = com.google.gson.JsonObject()
                            part2.addProperty("text", contPrompt)
                            partsArray2.add(part2)
                            contentObj2.add("parts", partsArray2)
                            contentsArray2.add(contentObj2)
                            contJson.add("contents", contentsArray2)
                            val genConfig2 = com.google.gson.JsonObject()
                            genConfig2.addProperty("temperature", 0.6)
                            genConfig2.addProperty("maxOutputTokens", 800)
                            contJson.add("generationConfig", genConfig2)

                            val contReq = Request.Builder()
                                .url(contUrl)
                                .post(contJson.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            client.newCall(contReq).execute().use { rCont ->
                                val bCont = rCont.body?.string()
                                if (rCont.isSuccessful && !bCont.isNullOrBlank()) {
                                    val extra = extractTextFromResponse(bCont)
                                    val combined = (partial + "\n" + (extra ?: "")).trim()
                                    return@withContext if (combined.isNotBlank()) combined else (extracted ?: "(sin texto extraído)")
                                } else {
                                    Log.w("GeminiHealthClient", "Continuación fallida: code=${rCont.code}")
                                }
                            }
                        }
                    }
                } catch (t: Exception) {
                    Log.w("GeminiHealthClient", "Error comprobando finishReason", t)
                }

                return@withContext extracted ?: "(sin texto extraído)"
            }
        } catch (t: SocketTimeoutException) {
            Log.e("GeminiHealthClient", "Timeout calling Gemini API", t)
            return@withContext "ERROR: Tiempo de espera agotado al contactar la API de IA"
        } catch (t: UnknownHostException) {
            Log.e("GeminiHealthClient", "UnknownHostException", t)
            return@withContext "ERROR: No se puede resolver el host de la API. Comprueba tu conexión a internet"
        } catch (t: Exception) {
            Log.e("GeminiHealthClient", "callTextModel exception", t)
            return@withContext "ERROR: ${t.message}"
        }
    }


    // Llama al modelo incluyendo una imagen en base64
    private suspend fun callTextModelWithImage(prompt: String, base64Image: String): String = withContext(Dispatchers.IO) {
        val url = "$baseUrl/$modelId:generateContent?key=$apiKey"
        val json = com.google.gson.JsonObject()
        val contentsArray = com.google.gson.JsonArray()
        val contentObj = com.google.gson.JsonObject()
        val partsArray = com.google.gson.JsonArray()
        
        // Parte de texto
        val textPart = com.google.gson.JsonObject()
        textPart.addProperty("text", prompt)
        partsArray.add(textPart)
        
        // Parte de imagen en base64
        val imagePart = com.google.gson.JsonObject()
        val imageBlobObj = com.google.gson.JsonObject()
        imageBlobObj.addProperty("mimeType", "image/jpeg")
        imageBlobObj.addProperty("data", base64Image)
        imagePart.add("inlineData", imageBlobObj)
        partsArray.add(imagePart)
        
        contentObj.add("parts", partsArray)
        contentsArray.add(contentObj)
        json.add("contents", contentsArray)
        
        // Agregar generationConfig correctamente
        val genConfig = com.google.gson.JsonObject()
        genConfig.addProperty("temperature", 0.6)
        // Incrementamos maxOutputTokens para evitar truncados por MAX_TOKENS
        genConfig.addProperty("maxOutputTokens", 1400)
        json.add("generationConfig", genConfig)

        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string()
                if (!resp.isSuccessful) {
                    val detail = respBody?.takeIf { it.isNotBlank() } ?: "(no body)"
                    if (resp.code == 404) {
                        val alt = fetchAvailableModel()
                        if (!alt.isNullOrBlank()) {
                            Log.w("GeminiHealthClient", "Modelo ${modelId} no disponible, reintentando con: $alt")
                            val retryUrl = "$baseUrl/$alt:generateContent?key=$apiKey"
                            val retryReq = Request.Builder()
                                .url(retryUrl)
                                .post(json.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            client.newCall(retryReq).execute().use { r2 ->
                                val b2 = r2.body?.string()
                                if (!r2.isSuccessful) return@withContext "ERROR: API error ${r2.code}: ${b2 ?: "(no body)"}"
                                return@withContext b2 ?: "(no body)"
                            }
                        }
                    }
                    return@withContext "ERROR: API error ${resp.code}: $detail"
                }
                val s = respBody ?: return@withContext "(no body)"
                // Intentar detectar truncado y continuar si procede
                try {
                    val root = gson.fromJson(s, JsonElement::class.java).asJsonObject
                    if (root.has("candidates")) {
                        val cand = root.getAsJsonArray("candidates").get(0).asJsonObject
                        if (cand.has("finishReason") && cand.get("finishReason").asString == "MAX_TOKENS") {
                            Log.w("GeminiHealthClient", "Imagen: respuesta truncada por MAX_TOKENS — intentando continuación automática")
                            val partial = extractTextFromResponse(s) ?: ""
                            val contPrompt = "Continúa la respuesta anterior y complétala coherentemente. Texto actual:\n" + partial + "\n\nContinúa desde aquí sin repetir lo anterior."
                            val contUrl = "$baseUrl/$modelId:generateContent?key=$apiKey"
                            val contJson = com.google.gson.JsonObject()
                            val contentsArray2 = com.google.gson.JsonArray()
                            val contentObj2 = com.google.gson.JsonObject()
                            val partsArray2 = com.google.gson.JsonArray()
                            val textPart = com.google.gson.JsonObject()
                            textPart.addProperty("text", contPrompt)
                            partsArray2.add(textPart)
                            contentObj2.add("parts", partsArray2)
                            contentsArray2.add(contentObj2)
                            contJson.add("contents", contentsArray2)
                            val genConfig2 = com.google.gson.JsonObject()
                            genConfig2.addProperty("temperature", 0.6)
                            genConfig2.addProperty("maxOutputTokens", 800)
                            contJson.add("generationConfig", genConfig2)

                            val contReq = Request.Builder()
                                .url(contUrl)
                                .post(contJson.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            client.newCall(contReq).execute().use { rCont ->
                                val bCont = rCont.body?.string()
                                if (rCont.isSuccessful && !bCont.isNullOrBlank()) {
                                    val extra = extractTextFromResponse(bCont)
                                    val combined = (partial + "\n" + (extra ?: "")).trim()
                                    return@withContext if (combined.isNotBlank()) combined else s
                                }
                            }
                        }
                    }
                } catch (t: Exception) {
                    Log.w("GeminiHealthClient", "Error comprobando finishReason en imagen", t)
                }

                return@withContext s
            }
        } catch (t: Exception) {
            Log.e("GeminiHealthClient", "callTextModelWithImage exception", t)
            return@withContext "ERROR: ${t.message}"
        }
    }

    private fun extractTextFromResponse(jsonString: String): String {
        return try {
            val json = gson.fromJson(jsonString, JsonElement::class.java)
            // Navegar de forma segura según estructura esperada
            val content = jsonAsString(json)
            content ?: jsonString
        } catch (e: Exception) {
            Log.e("GeminiHealthClient", "extractTextFromResponse parse error", e)
            jsonString
        }
    }

    private fun jsonAsString(el: JsonElement): String? {
        // Estructura moderna de Gemini API v1: candidates[0].content.parts[0].text
        try {
            val obj = el.asJsonObject
            if (obj.has("candidates")) {
                val cand = obj.getAsJsonArray("candidates").get(0).asJsonObject
                if (cand.has("content")) {
                    val content = cand.getAsJsonObject("content")
                    if (content.has("parts")) {
                        val parts = content.getAsJsonArray("parts")
                        if (parts.size() > 0) {
                            val part = parts.get(0).asJsonObject
                            if (part.has("text")) return part.get("text").asString
                        }
                    }
                }
            }
            if (obj.has("predictions")) return obj.getAsJsonArray("predictions").get(0).asString
            if (obj.has("output")) return obj.get("output").asString
        } catch (_: Exception) {}
        return null
    }

    // Intenta obtener un modelo disponible desde la API ListModels.
    // Devuelve el identificador corto del modelo (p.ej. "gemini-1.5-pro-latest") o null si no se encuentra ninguno.
    private fun fetchAvailableModel(): String? {
        try {
            val url = "$baseUrl?key=$apiKey"
            val req = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!resp.isSuccessful || body.isNullOrBlank()) {
                    Log.w("GeminiHealthClient", "ListModels failed: code=${resp.code}, body=${body ?: "(empty)"}")
                    return null
                }
                try {
                    val el = gson.fromJson(body, JsonElement::class.java)
                    val obj = el.asJsonObject
                    // Buscar array 'models' o 'model' según la respuesta
                    if (obj.has("models")) {
                        val arr = obj.getAsJsonArray("models")
                        for (i in 0 until arr.size()) {
                            val m = arr.get(i).asJsonObject
                            // Nombre puede venir como 'name' o 'model'
                            val fullName = when {
                                m.has("name") -> m.get("name").asString
                                m.has("model") -> m.get("model").asString
                                else -> null
                            } ?: continue

                            // Chequear si soporta generateContent (o variantes)
                            if (m.has("supportedMethods")) {
                                try {
                                    val methods = m.getAsJsonArray("supportedMethods")
                                    for (j in 0 until methods.size()) {
                                        val meth = methods.get(j).asString
                                        if (meth.contains("generateContent", ignoreCase = true) || meth.contains("generate", ignoreCase = true)) {
                                            val parts = fullName.split('/')
                                            return parts.lastOrNull()
                                        }
                                    }
                                } catch (_: Exception) { }
                            }

                            // Fallback: si no hay supportedMethods devolver el primer modelo nominado
                            val parts = fullName.split('/')
                            return parts.lastOrNull()
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GeminiHealthClient", "Failed parsing ListModels response", e)
                    return null
                }
            }
        } catch (t: Exception) {
            Log.w("GeminiHealthClient", "fetchAvailableModel error: ${t.message}")
        }
        return null
    }

    private fun parseHealthDiagnosis(responseText: String): DiagnosisResult {
        try {
            val urgency = when {
                responseText.contains("CRÍTICA", ignoreCase = true) -> "CRÍTICA"
                responseText.contains("ALTA", ignoreCase = true) -> "ALTA"
                responseText.contains("MEDIA", ignoreCase = true) -> "MEDIA"
                else -> "BAJA"
            }

            val conditions = Regex("(?i)posibles condiciones:?(.*?)(?=urgencia|recomend|$)", RegexOption.DOT_MATCHES_ALL)
                .find(responseText)?.groupValues?.get(1)?.lines()?.map { it.trim().trimStart('-', '•', '0', '1', '2', '3', '.') }?.filter { it.isNotBlank() } ?: emptyList()

            val recs = Regex("(?i)recomendaciones:?(.*?)(?=¿consultar|consultar|$)", RegexOption.DOT_MATCHES_ALL)
                .find(responseText)?.groupValues?.get(1)?.lines()?.map { it.trim().trimStart('-', '•', '0', '1', '2', '.') }?.filter { it.isNotBlank() } ?: emptyList()

            val diag = Regex("(?i)diagn[oó]stico preliminar:?(.*?)(?=posibles|urgencia|$)", RegexOption.DOT_MATCHES_ALL).find(responseText)?.groupValues?.get(1)?.trim() ?: responseText.take(200)

            val shouldConsult = urgency == "ALTA" || urgency == "CRÍTICA"

            return DiagnosisResult(
                preliminaryDiagnosis = diag,
                potentialConditions = conditions.take(5),
                urgencyLevel = urgency,
                recommendations = if (recs.isNotEmpty()) recs.take(5) else listOf("Consultar a un profesional de salud"),
                shouldConsultDoctor = shouldConsult,
                success = true,
                errorMessage = null
            )
        } catch (t: Throwable) {
            return DiagnosisResult.error(t.message ?: "Error parsing response")
        }
    }

    // Intenta extraer y parsear un JSON compacto desde la respuesta del modelo.
    // Devuelve null si no contiene JSON válido.
    private fun parseJsonDiagnosis(responseText: String): DiagnosisResult? {
        try {
            // Buscar el primer bloque JSON (desde la primera llave hasta la última llave) para tolerar texto extra alrededor
            val start = responseText.indexOf('{')
            val end = responseText.lastIndexOf('}')
            if (start < 0 || end < 0 || end <= start) return null
            val jsonSub = responseText.substring(start, end + 1)
            val obj = gson.fromJson(jsonSub, com.google.gson.JsonObject::class.java)

            val preliminary = if (obj.has("preliminaryDiagnosis")) obj.get("preliminaryDiagnosis").asString else ""
            val urgency = if (obj.has("urgencyLevel")) obj.get("urgencyLevel").asString else "BAJA"
            val conditions = mutableListOf<String>()
            if (obj.has("potentialConditions") && obj.get("potentialConditions").isJsonArray) {
                val arr = obj.getAsJsonArray("potentialConditions")
                for (i in 0 until arr.size()) {
                    try { conditions.add(arr.get(i).asString) } catch (_: Exception) {}
                }
            }
            val recs = mutableListOf<String>()
            if (obj.has("recommendations") && obj.get("recommendations").isJsonArray) {
                val arr = obj.getAsJsonArray("recommendations")
                for (i in 0 until arr.size()) {
                    try { recs.add(arr.get(i).asString) } catch (_: Exception) {}
                }
            }
            val should = if (obj.has("shouldConsultDoctor")) obj.get("shouldConsultDoctor").asBoolean else (urgency == "ALTA" || urgency == "CRÍTICA")
            val disclaimer = if (obj.has("disclaimer")) obj.get("disclaimer").asString else ""

            return DiagnosisResult(
                preliminaryDiagnosis = preliminary,
                potentialConditions = conditions.take(5),
                urgencyLevel = urgency,
                recommendations = if (recs.isNotEmpty()) recs.take(5) else listOf("Consultar a un profesional de salud"),
                shouldConsultDoctor = should,
                success = true,
                errorMessage = null
            )
        } catch (t: Throwable) {
            Log.w("GeminiHealthClient", "parseJsonDiagnosis failed", t)
            return null
        }
    }

    // Exponer parsing para que ViewModel pueda convertir texto crudo en DiagnosisResult
    fun parseResponseToDiagnosis(responseText: String): DiagnosisResult {
        return try {
            parseHealthDiagnosis(responseText)
        } catch (t: Throwable) {
            DiagnosisResult.error("Error parsing IA response: ${t.message}")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

// Helper pequeño para construir JSON manualmente sin depender de clases extra
private class JsonBuilder {
    private val map = com.google.gson.JsonObject()
    fun add(name: String, value: String) { map.addProperty(name, value) }
    fun add(name: String, value: Number) { map.addProperty(name, value) }
    fun add(name: String, value: Boolean) { map.addProperty(name, value) }
    fun build(): String = map.toString()
}






