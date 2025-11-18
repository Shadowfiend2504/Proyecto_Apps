package com.example.healthconnectai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthconnectai.databinding.ActivityMapBinding
import com.example.healthconnectai.data.models.HospitalLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import com.example.healthconnectai.data.api.PlacesApiService

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST = 1001
    private val PLACES_API_KEY = BuildConfig.GOOGLE_PLACES_API_KEY
    
    private var currentLocation: Location? = null
    private val hospitales = mutableListOf<HospitalLocation>()
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
        
        // Verificación temprana de API Key de Google Places
        if (PLACES_API_KEY.isBlank()) {
            Toast.makeText(
                this,
                "Falta GOOGLE_PLACES_API_KEY en local.properties. Configure la clave para usar el mapa.",
                Toast.LENGTH_LONG
            ).show()
            binding.progressLayout.visibility = View.GONE
            return
        }

        // Obtener el MapFragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Eventos de botones
        binding.btnCloseCard.setOnClickListener {
            binding.hospitalInfoCard.visibility = View.GONE
        }

        binding.btnOpenInMaps.setOnClickListener {
            abrirEnGoogleMaps()
        }

        // Verificar permisos y obtener ubicación
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                currentLocation = it
                val latLng = LatLng(it.latitude, it.longitude)

                // Mover cámara del mapa a ubicación actual
                if (::googleMap.isInitialized) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                    
                    // Agregar marcador de ubicación actual
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Mi ubicación")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                }

                // Buscar hospitales cercanos
                buscarHospitalesCercanos(it.latitude, it.longitude)
            } ?: run {
                Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
                binding.progressLayout.visibility = View.GONE
            }
        }
    }

    private fun buscarHospitalesCercanos(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val location = "$latitude,$longitude"
                val response = placesApiService.getNearbyHospitals(
                    location = location,
                    radius = 5000,
                    apiKey = PLACES_API_KEY
                )

                if (response.status == "OK") {
                    hospitales.clear()
                    
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

                        // Agregar marcador al mapa
                        val markerLatLng = LatLng(hospital.latitude, hospital.longitude)
                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(markerLatLng)
                                .title(hospital.name)
                                .snippet(hospital.address)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )

                        // Guardar referencia del hospital en el marcador
                        marker?.tag = hospital
                    }

                    // Listener para clics en marcadores
                    googleMap.setOnMarkerClickListener { marker ->
                        val hospital = marker.tag as? HospitalLocation
                        hospital?.let { mostrarInfoHospital(it) }
                        true
                    }

                    binding.progressLayout.visibility = View.GONE
                } else {
                    Toast.makeText(this@MapActivity, "Error: ${response.status}", Toast.LENGTH_SHORT).show()
                    binding.progressLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MapActivity,
                    "Error al buscar hospitales: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressLayout.visibility = View.GONE
            }
        }
    }

    private fun mostrarInfoHospital(hospital: HospitalLocation) {
        binding.hospitalName.text = hospital.name
        binding.hospitalAddress.text = hospital.address
        binding.hospitalInfoCard.visibility = View.VISIBLE

        // Guardar hospital actual para usarlo en "Abrir en Maps"
        binding.btnOpenInMaps.tag = hospital
    }

    private fun abrirEnGoogleMaps() {
        val hospital = binding.btnOpenInMaps.tag as? HospitalLocation
        hospital?.let {
            val uri = Uri.parse("geo:${it.latitude},${it.longitude}?q=${Uri.encode(it.name)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback a navegador
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/${Uri.encode(it.name)}/@${it.latitude},${it.longitude},15z")
                )
                startActivity(browserIntent)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configurar el mapa
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        // Si ya tenemos ubicación, centrar el mapa
        currentLocation?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacion()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            binding.progressLayout.visibility = View.GONE
        }
    }
}
