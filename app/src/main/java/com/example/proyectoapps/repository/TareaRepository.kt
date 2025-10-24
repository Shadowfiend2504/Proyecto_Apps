package com.example.proyectoapps.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.proyectoapps.data.AppDatabase
import com.example.proyectoapps.data.Tarea
import com.example.proyectoapps.network.ApiService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.proyectoapps.network.PushTarea
import com.google.gson.JsonParser
import java.lang.Exception

class TareaRepository(
    private val api: ApiService,
    private val context: Context
) {

    private val TAG = "TareaRepository"

    private val db = AppDatabase.getInstance(context)
    private val dao = db.tareaDao()

    fun getTareasFlow() = dao.getAllFlow()

    suspend fun insertTarea(tarea: Tarea) {
        dao.insert(tarea)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    // Sincroniza desde la API remota y guarda en Room
    suspend fun syncFromRemote() {
        withContext(Dispatchers.IO) {
            // Comprobar conexión antes de intentar sincronizar
            if (!isNetworkAvailable(context)) {
                return@withContext
            }

            try {
                // Intentar obtener desde Google Sheet público (gviz JSON)
                val sheetId = "1WLankjx3ktol-e162ZTPCFR2jAYmub1URDJ2XTXxBmM"
                val url = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:json"
                val body = try {
                    api.getSheet(url).string()
                } catch (e: Exception) {
                    // Fallback a jsonplaceholder si falla
                    null
                }

                if (body != null) {
                    // El endpoint gviz devuelve un wrapper JS; extraer el objeto JSON
                    val jsonStart = body.indexOf('{')
                    val jsonEnd = body.lastIndexOf('}')
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        val json = body.substring(jsonStart, jsonEnd + 1)
                        val root = JsonParser.parseString(json).asJsonObject
                        val table = root.getAsJsonObject("table")
                        val cols = table.getAsJsonArray("cols")
                        val rows = table.getAsJsonArray("rows")

                        // Determinar índices de columnas a partir de encabezados (si existen)
                        val headers = mutableListOf<String?>()
                        for (i in 0 until cols.size()) {
                            val col = cols[i].asJsonObject
                            val label = if (col.has("label") && !col.get("label").isJsonNull) col.get("label").asString.trim().lowercase() else null
                            headers.add(label)
                        }

                        fun findIndex(names: List<String>, defaultIndex: Int): Int {
                            for (i in headers.indices) {
                                val h = headers[i]
                                if (h != null) {
                                    for (n in names) if (h.contains(n)) return i
                                }
                            }
                            return defaultIndex
                        }

                        val titleIndex = findIndex(listOf("title", "titulo", "tarea", "task", "name"), 0)
                        val descIndex = findIndex(listOf("description", "descripcion", "desc", "details", "detalle"), 1)
                        val dateIndex = findIndex(listOf("fecha", "date", "created", "timestamp"), -1)

                        // Limpiar y guardar en BD
                        dao.clearAll()

                        // Tomar hasta 100 filas para evitar sobrecarga
                        rows.take(100).forEach { r ->
                            try {
                                val row = r.asJsonObject
                                val cells = row.getAsJsonArray("c")

                                // Obtener valor seguro de celda
                                fun cellValue(i: Int): String? {
                                    return if (i >= 0 && i < cells.size() && !cells[i].isJsonNull) {
                                        val obj = cells[i].asJsonObject
                                        if (obj.has("v") && !obj.get("v").isJsonNull) obj.get("v").asString else null
                                    } else null
                                }

                                val title = cellValue(titleIndex) ?: cellValue(0) ?: "Sin título"
                                val desc = cellValue(descIndex)

                                // Manejar fecha si existe
                                val fechaVal = if (dateIndex >= 0) {
                                    val v = cellValue(dateIndex)
                                    v?.let {
                                        try {
                                            // Intentar parseo como número (milis) o ISO
                                            val asLong = it.toLongOrNull()
                                            if (asLong != null && asLong > 0) return@let asLong
                                            try {
                                                return@let java.time.Instant.parse(it).toEpochMilli()
                                            } catch (_: Exception) {
                                                null
                                            }
                                        } catch (_: Exception) { null }
                                    }
                                } else null

                                val tarea = Tarea(
                                    titulo = title,
                                    descripcion = desc,
                                    fecha = fechaVal ?: System.currentTimeMillis()
                                )
                                dao.insert(tarea)
                            } catch (inner: Exception) {
                                // ignorar fila problemática
                                inner.printStackTrace()
                            }
                        }
                        return@withContext
                    }
                }

                // Si no se pudo usar Google Sheets, fallback a ejemplo jsonplaceholder
                val remote = api.getTodos()
                dao.clearAll()
                remote.take(50).forEach { dto ->
                    val tarea = Tarea(
                        titulo = dto.title ?: "Sin título",
                        descripcion = "Remoto: completed=${dto.completed}",
                        fecha = System.currentTimeMillis()
                    )
                    dao.insert(tarea)
                }
            } catch (e: Exception) {
                // Falló la sincronización, dejar los datos locales como están
                e.printStackTrace()
            }
        }
    }

    /**
     * Enviar (push) todas las tareas locales al WebApp de Google Apps Script.
     * @param webAppUrl URL del WebApp (debe ser la URL completa desplegada)
     * @return true si la petición terminó correctamente (no garantiza que la hoja cambie si el script lo ignora)
     */
    suspend fun pushToRemote(webAppUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                Log.w(TAG, "No network available for push")
                return@withContext false
            }

            try {
                val local = dao.getAll()
                val payload = local.map { t -> PushTarea(t.titulo, t.descripcion, t.fecha) }
                Log.i(TAG, "Pushing ${payload.size} tareas to $webAppUrl")
                val resp = api.pushTasks(webAppUrl, payload)
                val respBody = try { resp.string() } catch (e: Exception) { "<no-body>" }
                Log.i(TAG, "Push response body: $respBody")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Push failed", e)
                return@withContext false
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val n = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(n) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val ni = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            ni != null && ni.isConnected
        }
    }
}
