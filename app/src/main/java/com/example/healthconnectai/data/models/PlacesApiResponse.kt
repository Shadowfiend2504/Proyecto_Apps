package com.example.healthconnectai.data.models

data class PlacesApiResponse(
    val results: List<PlaceResult>,
    val next_page_token: String? = null,
    val status: String
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val formatted_address: String? = null,
    val geometry: Geometry,
    val rating: Float? = null,
    val opening_hours: OpeningHours? = null,
    val types: List<String>? = null
)

data class Geometry(
    val location: Location,
    val viewport: Viewport? = null
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class Viewport(
    val northeast: Location,
    val southwest: Location
)

data class OpeningHours(
    val open_now: Boolean? = null,
    val weekday_text: List<String>? = null
)
