package com.example.healthconnectai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.healthconnectai.databinding.ActivityImageAnalysisBinding

class ImageAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageAnalysisBinding
    private val CAMERA_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 200
    
    // Use Activity Result API para obtener la imagen (TakePicturePreview)
    private val takePicturePreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            binding.imgPreview.setImageBitmap(bitmap)
            Toast.makeText(this, "Foto capturada correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No se obtuvo imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        binding.btnAnalyzeImage.setOnClickListener {
            binding.txtResult.text =
                "Análisis: posible anomalía detectada.\nRecomendación: consultar al médico."
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        // Primero comprobar si el dispositivo tiene hardware de cámara
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Este dispositivo no dispone de cámara", Toast.LENGTH_SHORT).show()
            return
        }

        // Con TakePicturePreview no dependemos de una aplicación externa: lanzamos la captura directamente
        takePicturePreview.launch(null)
    }
    // Ya no usamos onActivityResult con la Activity Result API

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }
}

