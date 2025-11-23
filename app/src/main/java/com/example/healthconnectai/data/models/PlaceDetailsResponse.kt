package com.example.healthconnectai.data.models

data class PlaceDetailsResponse(
    val result: PlaceDetailResult? = null,
    val status: String? = null
)

data class PlaceDetailResult(
    val name: String? = null,
    val rating: Float? = null,
    val formatted_address: String? = null,
    val formatted_phone_number: String? = null,
    val website: String? = null,
    val opening_hours: OpeningHours? = null
)

