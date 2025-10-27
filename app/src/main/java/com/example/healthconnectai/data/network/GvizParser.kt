package com.example.healthconnectai.data.network

import org.json.JSONObject
import com.example.healthconnectai.data.model.Tarea

fun extractJsonFromGviz(raw: String): JSONObject {
    android.util.Log.d("GvizParser", "Raw response: $raw")
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    val jsonStr = if (start >= 0 && end >= 0 && end >= start) raw.substring(start, end + 1) else raw
    android.util.Log.d("GvizParser", "Extracted JSON: $jsonStr")
    return JSONObject(jsonStr)
}

// Parsear la tabla gviz y convertir a lista de Tarea (mapear columnas comunes: titulo, descripcion, fecha)
fun parseSheetToTasks(json: JSONObject): List<Tarea> {
    val table = json.getJSONObject("table")
    val cols = table.getJSONArray("cols")
    val rows = table.getJSONArray("rows")

    val columnNames = mutableListOf<String>()
    for (i in 0 until cols.length()) {
        val c = cols.getJSONObject(i)
        // usar label si existe; sino id
        val label = if (c.has("label") && !c.isNull("label")) c.getString("label") else c.optString("id", "col$i")
        columnNames.add(label.lowercase())
    }
    android.util.Log.d("GvizParser", "Columnas detectadas: $columnNames")

    val result = mutableListOf<Tarea>()
    for (r in 0 until rows.length()) {
        val row = rows.getJSONObject(r)
        val cArray = row.getJSONArray("c")
        val map = mutableMapOf<String, String>()
        for (i in 0 until cArray.length()) {
            val cell = cArray.opt(i)
            if (cell == null || cell == JSONObject.NULL) {
                map[columnNames.getOrNull(i) ?: "col$i"] = ""
            } else {
                val cellObj = cArray.getJSONObject(i)
                map[columnNames.getOrNull(i) ?: "col$i"] = cellObj.optString("v", "")
            }
        }

        val titulo = map["titulo"] ?: map["title"] ?: "Sin título"
        val descripcion = map["descripcion"] ?: map["description"] ?: ""
        val fecha = map["fecha"] ?: map["date"] ?: java.time.LocalDate.now().toString()

        result.add(Tarea(titulo = titulo, descripcion = descripcion, fecha = fecha))
    }

    return result
}
