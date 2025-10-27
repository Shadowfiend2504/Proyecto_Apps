package com.example.healthconnectai.data.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import com.example.healthconnectai.data.model.TareaDTO

object GoogleSheetClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://docs.google.com/")
        .client(client)
        .build()

    val api: GoogleSheetApi = retrofit.create(GoogleSheetApi::class.java)

    fun extractJsonFromGviz(raw: String): JSONObject {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val jsonStr = raw.substring(start, end + 1)
        return JSONObject(jsonStr)
    }

    fun parseSheetToTasks(json: JSONObject): List<TareaDTO> {
        val table = json.getJSONObject("table")
        val rows = table.getJSONArray("rows")
        val result = mutableListOf<TareaDTO>()

        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i).getJSONArray("c")
            val id = row.optJSONObject(0)?.optString("v", "") ?: ""
            val titulo = row.optJSONObject(1)?.optString("v", "") ?: ""
            val descripcion = row.optJSONObject(2)?.optString("v", "") ?: ""
            val fecha = row.optJSONObject(3)?.optString("v", "") ?: ""

            result.add(TareaDTO(id, titulo, descripcion, fecha))
        }
        return result
    }
}