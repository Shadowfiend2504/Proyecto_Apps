package com.example.healthconnectai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthconnectai.databinding.ActivityMapBinding
import com.example.healthconnectai.data.api.PlacesApiService
import com.example.healthconnectai.data.models.HospitalLocation
import com.example.healthconnectai.data.models.PlacesApiResponse
import com.example.healthconnectai.data.models.PlaceResult
import com.example.healthconnectai.data.models.PlaceDetailsResponse
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    private val LOCATION_PERMISSION_REQUEST = 1001
    private val GOOGLE_API_KEY = BuildConfig.GOOGLE_API_KEY.trim()

    private var currentLocation: Location? = null
    private val hospitales = mutableListOf<HospitalLocation>()

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val placesApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (GOOGLE_API_KEY.isBlank()) {
            Toast.makeText(
                this,
                "ERROR: Falta GOOGLE_API_KEY en local.properties",
                Toast.LENGTH_LONG
            ).show()
            binding.progressLayout.visibility = View.GONE
            return
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.btnCloseCard.setOnClickListener { binding.hospitalInfoCard.visibility = View.GONE }
        binding.btnOpenInMaps.setOnClickListener { abrirEnGoogleMaps() }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            obtenerUbicacion()
        }
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    val latLng = LatLng(location.latitude, location.longitude)

                    if (::googleMap.isInitialized) {
                        googleMap.isMyLocationEnabled = true
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Mi ubicación")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }

                    buscarHospitalesCercanos(location.latitude, location.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun buscarHospitalesCercanos(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val response: PlacesApiResponse = placesApiService.getNearbyHealthcare(
                    location = "$lat,$lng",
                    radius = 5000,
                    keyword = "hospital",
                    apiKey = GOOGLE_API_KEY
                )

                if (response.results.isNotEmpty()) {
                    hospitales.clear()
                    googleMap.clear()

                    response.results.forEach { result ->
                        val hospital = HospitalLocation(
                            placeId = result.place_id,
                            name = result.name,
                            address = result.formatted_address ?: "Dirección no disponible",
                            latitude = result.geometry.location.lat,
                            longitude = result.geometry.location.lng,
                            rating = result.rating ?: 0f
                        )
                        hospitales.add(hospital)

                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(hospital.latitude, hospital.longitude))
                                .title(hospital.name)
                                .snippet(hospital.address)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                        marker?.tag = hospital
                    }

                    googleMap.setOnMarkerClickListener { marker ->
                        val hospital = marker.tag as? HospitalLocation
                        hospital?.let { mostrarInfoHospital(it) }
                        true
                    }

                    binding.progressLayout.visibility = View.GONE
                } else {
                    binding.progressLayout.visibility = View.GONE
                    Log.w("MapActivity", "No se encontraron hospitales cercanos")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressLayout.visibility = View.GONE
            }
        }
    }

    private fun mostrarInfoHospital(hospital: HospitalLocation) {
        // Mostramos la tarjeta y el progreso
        binding.progressLayout.visibility = View.VISIBLE
        binding.hospitalInfoCard.visibility = View.VISIBLE

        // Título y calificación
        binding.hospitalName.text = "🏥 ${hospital.name}"
        binding.hospitalRating.text = "⭐ Calificación: ${hospital.rating}"

        // Inicializamos campos mientras cargamos detalles
        binding.hospitalAddress.text = "Cargando dirección..."
        binding.hospitalPhone.text = "Cargando teléfono..."
        binding.hospitalWebsite.text = "Cargando sitio web..."
        binding.hospitalOpening.text = "Cargando horario..."

        lifecycleScope.launch {
            try {
                val detailsResponse: PlaceDetailsResponse = placesApiService.getPlaceDetails(
                    placeId = hospital.placeId,
                    apiKey = GOOGLE_API_KEY
                )
                val details = detailsResponse.result

                // Formato bonito con iconos
                val address = details?.formatted_address?.let { "📍 $it" } ?: "Dirección no disponible"
                val phone = details?.formatted_phone_number?.let { "📞 $it" } ?: "Teléfono no disponible"
                val website = details?.website?.let { "🌐 $it" } ?: "Sitio web no disponible"
                val opening = details?.opening_hours?.open_now?.let { if (it) "🟢 Abierto ahora" else "🔴 Cerrado ahora" }
                    ?: "Horario no disponible"

                // Asignamos al binding
                binding.hospitalAddress.text = address
                binding.hospitalPhone.text = phone
                binding.hospitalWebsite.text = website
                binding.hospitalOpening.text = opening

                // Guardamos referencia para abrir en Google Maps
                binding.btnOpenInMaps.tag = hospital

            } catch (e: Exception) {
                e.printStackTrace()
                binding.hospitalAddress.text = "Información no disponible"
                binding.hospitalPhone.text = "-"
                binding.hospitalWebsite.text = "-"
                binding.hospitalOpening.text = "-"
            } finally {
                // Ocultamos el loader
                binding.progressLayout.visibility = View.GONE
            }
        }
    }
    private fun abrirEnGoogleMaps() {
        val hospital = binding.btnOpenInMaps.tag as? HospitalLocation ?: return
        val uri = Uri.parse("geo:${hospital.latitude},${hospital.longitude}?q=${Uri.encode(hospital.name)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/${Uri.encode(hospital.name)}/@${hospital.latitude},${hospital.longitude},15z")))
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        currentLocation?.let {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            binding.progressLayout.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
