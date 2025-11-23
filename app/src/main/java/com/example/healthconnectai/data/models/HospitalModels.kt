package com.example.healthconnectai.data.models

data class HospitalLocation(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Float = 0f,
    val distance: Float = 0f
)
