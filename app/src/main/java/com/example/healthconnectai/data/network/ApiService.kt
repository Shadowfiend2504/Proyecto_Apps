package com.example.healthconnectai.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Usamos DTOs para mapear correctamente la respuesta JSON de la API
import com.example.healthconnectai.data.network.TareaDTO
import com.example.healthconnectai.data.network.NewTareaDTO

interface ApiService {
    @GET("todos")
    suspend fun getTareas(): Response<List<TareaDTO>>

    @POST("todos")
    suspend fun createTarea(@Body nueva: NewTareaDTO): Response<TareaDTO>
}
