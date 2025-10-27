package com.example.healthconnectai.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedReader
import java.io.StringReader
import android.util.Log

object GoogleSheetCsvClient {
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("GoogleSheetCsvClient", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    suspend fun fetchCsvFromPublishedSheet(spreadsheetId: String, gid: String = "0"): List<List<String>> {
        val url = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$gid"
        Log.d("GoogleSheetCsvClient", "Fetching CSV from URL: $url")
        
        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GoogleSheetCsvClient", "Error HTTP ${response.code}")
                    throw Exception("HTTP ${response.code}")
                }
                
                val body = response.body?.string() ?: ""
                Log.d("GoogleSheetCsvClient", "Received CSV body: ${body.take(200)}...")
                parseCsv(body)
            }
        }
    }

    private fun parseCsv(csvText: String): List<List<String>> {
        val reader = BufferedReader(StringReader(csvText))
        val rows = mutableListOf<List<String>>()
        
        reader.forEachLine { line ->
            val cols = line.split(",").map { it.trim().trim('"') }
            Log.d("GoogleSheetCsvClient", "Parsed CSV row: $cols")
            rows.add(cols)
        }
        
        return rows
    }
}