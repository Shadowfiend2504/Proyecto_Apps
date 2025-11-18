
package com.example.healthconnectai.test

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityImageAnalysisBinding

class TestImageAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageAnalysisBinding

    // Launcher para pedir permiso de cámara
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCameraInternal()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher moderno para capturar imagen
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val imageBitmap = data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    binding.imagePreview.setImageBitmap(imageBitmap)
                    Toast.makeText(this, "Imagen capturada correctamente.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se recibió imagen.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Captura cancelada.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón para abrir cámara
        binding.btnTakePhoto.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraInternal() {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error abriendo la cámara: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
