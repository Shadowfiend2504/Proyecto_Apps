package com.example.healthconnectai.data.network

import com.example.healthconnectai.data.model.Tarea
import com.google.gson.annotations.SerializedName

// DTO para la sincronización con Google Sheets
data class TareaDTO(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    @SerializedName("completed") val completed: Boolean = false
)

// Mapeo sencillo a la entidad Room `Tarea`
fun TareaDTO.toEntity(): Tarea {
    return Tarea(
        id = 0, // Dejar que Room genere el ID
        titulo = this.titulo,
        descripcion = this.descripcion,
        fecha = this.fecha
    )
}
