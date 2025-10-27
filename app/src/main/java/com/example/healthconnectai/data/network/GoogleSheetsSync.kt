package com.example.healthconnectai.data.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.healthconnectai.data.model.Tarea
import java.io.IOException

object GoogleSheetsSync {
    private val client = OkHttpClient()
    private const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyMgjxtdWDN3HgyfCj0vFipvARnYynILODQ4lDN2RVLHwm3HIgoMlNuoVRqt7mgSEtc/exec"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun appendRow(tarea: Tarea): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("titulo", tarea.titulo)
                put("descripcion", tarea.descripcion)
                put("fecha", tarea.fecha)
            }

            val jsonStr = json.toString()
            Log.d("GoogleSheetsSync", "Enviando tarea a Google Sheets: $jsonStr")

            val request = Request.Builder()
                .url(SCRIPT_URL)
                .post(jsonStr.toRequestBody(JSON))
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("GoogleSheetsSync", "Código de respuesta: ${response.code}")
                Log.d("GoogleSheetsSync", "Respuesta completa: $responseBody")

                if (!response.isSuccessful) {
                    Log.e("GoogleSheetsSync", "Error HTTP ${response.code}: $responseBody")
                    return@withContext false
                }

                try {
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    val success = jsonResponse.optBoolean("success", false)
                    val message = jsonResponse.optString("message")
                    
                    Log.d("GoogleSheetsSync", "Success: $success, Message: $message")
                    success
                } catch (e: Exception) {
                    Log.e("GoogleSheetsSync", "Error al procesar respuesta JSON", e)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsSync", "Error al sincronizar con Google Sheets", e)
            e.printStackTrace()
            false
        }
    }
}