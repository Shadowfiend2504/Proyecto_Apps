package com.example.healthconnectai.data.api

import com.example.healthconnectai.data.models.PlacesApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    
    @GET("place/nearbysearch/json")
    suspend fun getNearbyHospitals(
        @Query("location") location: String, // "latitude,longitude"
        @Query("radius") radius: Int = 5000, // 5 km
        @Query("type") type: String = "hospital",
        @Query("key") apiKey: String
    ): PlacesApiResponse

    @GET("place/nearbysearch/json")
    suspend fun getNearbyHealthcare(
        @Query("location") location: String,
        @Query("radius") radius: Int = 5000,
        @Query("keyword") keyword: String = "hospital|clinica|centro medico",
        @Query("key") apiKey: String
    ): PlacesApiResponse
}
