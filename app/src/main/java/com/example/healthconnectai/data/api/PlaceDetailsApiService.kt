package com.example.healthconnectai.data.api

import com.example.healthconnectai.data.models.PlaceDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaceDetailsApiService {

    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,rating,formatted_phone_number,formatted_address,opening_hours,website",
        @Query("key") apiKey: String
    ): PlaceDetailsResponse
}
