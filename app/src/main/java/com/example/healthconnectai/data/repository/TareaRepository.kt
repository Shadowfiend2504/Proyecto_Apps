package com.example.healthconnectai.data.repository

import androidx.lifecycle.LiveData
import com.example.healthconnectai.data.local.TareaDao
import com.example.healthconnectai.data.model.Tarea
import com.example.healthconnectai.data.network.RetrofitClient
import com.example.healthconnectai.data.network.TareaDTO
import com.example.healthconnectai.data.network.toEntity
import com.example.healthconnectai.data.network.toNewDto
import com.example.healthconnectai.data.network.GoogleSheetCsvClient
import com.example.healthconnectai.data.network.GoogleSheetsSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class TareaRepository(private val tareaDao: TareaDao) {

    val tareas: LiveData<List<Tarea>> = tareaDao.getAllTareas()

    // Inserta la tarea localmente y la sincroniza con Google Sheets
    suspend fun insert(tarea: Tarea): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Primero guardamos localmente
                tareaDao.insert(tarea)
                android.util.Log.d("TareaRepository", "Tarea guardada localmente: ${tarea.titulo}")

                // Luego intentamos sincronizar con Google Sheets
                val syncSuccess = GoogleSheetsSync.appendRow(tarea)
                if (syncSuccess) {
                    android.util.Log.d("TareaRepository", "Tarea sincronizada con Google Sheets: ${tarea.titulo}")
                } else {
                    android.util.Log.w("TareaRepository", "No se pudo sincronizar con Google Sheets: ${tarea.titulo}")
                }
                syncSuccess
            } catch (e: Exception) {
                android.util.Log.e("TareaRepository", "Error al insertar tarea", e)
                false
            }
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            tareaDao.deleteAll()
        }
    }

    // Sincronizar desde una Google Sheet publicada como CSV
    suspend fun syncFromSheet(sheetUrl: String) {
        try {
            android.util.Log.d("TareaRepository", "Iniciando sincronización con CSV")
            
            // Extraer ID de la hoja de la URL
            val spreadsheetId = extractSpreadsheetId(sheetUrl)
            android.util.Log.d("TareaRepository", "ID de la hoja: $spreadsheetId")
            
            // Obtener datos CSV
            val rows: List<List<String>> = withContext(Dispatchers.IO) {
                GoogleSheetCsvClient.fetchCsvFromPublishedSheet(spreadsheetId)
            }
            
            if (rows.isEmpty() || rows.size == 1) {
                android.util.Log.w("TareaRepository", "La hoja está vacía o solo tiene encabezados")
                return
            }
            
            // Ignorar la primera fila (encabezados)
            val dataRows: List<List<String>> = rows.subList(1, rows.size)
            android.util.Log.d("TareaRepository", "Filas de datos encontradas: ${dataRows.size}")
            
            withContext(Dispatchers.IO) {
                // Primero, encontrar los índices correctos de las columnas
                val headers = rows[0]
                val tituloIndex = headers.indexOfFirst { it.equals("titulo", true) }
                val descripcionIndex = headers.indexOfFirst { it.equals("descripcion", true) }
                val fechaIndex = headers.indexOfFirst { it.equals("fecha", true) }
                
                if (tituloIndex == -1 || descripcionIndex == -1 || fechaIndex == -1) {
                    android.util.Log.e("TareaRepository", "Formato de CSV inválido. Encabezados necesarios no encontrados")
                    return@withContext
                }

                // Limpiar tareas existentes antes de insertar las nuevas
                tareaDao.deleteAll()
                
                dataRows.forEach { row ->
                    try {
                        if (row.size >= maxOf(tituloIndex, descripcionIndex, fechaIndex) + 1) {
                            val tarea = Tarea(
                                id = 0, // Room generará el ID
                                titulo = row[tituloIndex],
                                descripcion = row[descripcionIndex],
                                fecha = row[fechaIndex]
                            )
                            tareaDao.insert(tarea)
                            android.util.Log.d("TareaRepository", "Tarea insertada: ${tarea.titulo}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TareaRepository", "Error al procesar fila: $row", e)
                    }
                }
            }
            
            android.util.Log.d("TareaRepository", "Sincronización completada exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("TareaRepository", "Error en sincronización", e)
            throw e // Re-lanzar para que el ViewModel pueda manejarlo
        }
    }
    
    private fun extractSpreadsheetId(url: String): String {
        // Extraer ID de la URL de la hoja
        return when {
            url.contains("/d/") -> {
                val start = url.indexOf("/d/") + 3
                val end = url.indexOf("/", start).takeIf { it > -1 } ?: url.length
                url.substring(start, end)
            }
            else -> throw IllegalArgumentException("URL de hoja de cálculo inválida")
        }
    }
}
