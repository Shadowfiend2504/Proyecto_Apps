package com.example.proyectoapps.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    // Endpoint para obtener Google Sheet público (gviz/tq?tqx=out:json)
    @GET
    suspend fun getSheet(@Url url: String): ResponseBody

    // Endpoint para subir tareas a un WebApp de Google Apps Script
    @POST
    suspend fun pushTasks(@Url url: String, @Body tasks: List<PushTarea>): ResponseBody

    // Mantener el endpoint de ejemplo por compatibilidad (opcional)
    @GET("todos")
    suspend fun getTodos(): List<TareaDTO>
}
