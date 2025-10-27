package com.example.healthconnectai.data.network

import com.example.healthconnectai.data.model.Tarea
import com.google.gson.annotations.SerializedName

data class NewTareaDTO(
    @SerializedName("userId") val userId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("completed") val completed: Boolean = false
)

fun Tarea.toNewDto(userId: Int = 1): NewTareaDTO {
    return NewTareaDTO(
        userId = userId,
        title = this.titulo,
        completed = false
    )
}
