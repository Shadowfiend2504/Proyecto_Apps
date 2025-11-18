package com.example.healthconnectai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import android.widget.ArrayAdapter
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.healthconnectai.databinding.ActivityImageAnalysisBinding
import java.text.SimpleDateFormat
import java.util.*

class ImageAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageAnalysisBinding
    private val photoDirectory by lazy { File(cacheDir, "photo_history") }
    private val photoFilesList = mutableListOf<File>()

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
                    // Guardar la imagen en cache y abrir ResultActivity para análisis
                    try {
                        val imgFile = File(photoDirectory, "photo_${System.currentTimeMillis()}.jpg")
                        val fos = imgFile.outputStream()
                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                        fos.flush()
                        fos.close()
                        loadPhotoHistory()
                        val intent = Intent(this, ResultActivity::class.java)
                        intent.putExtra("imageFile", imgFile.absolutePath)
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error guardando imagen: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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

        // Crear directorio de historial
        if (!photoDirectory.exists()) {
            photoDirectory.mkdirs()
        }

        // Botón para abrir cámara
        binding.btnTakePhoto.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
        
        // Botón para analizar imagen actual
        binding.btnAnalyzeImage.setOnClickListener {
            loadPhotoHistory()
            if (photoFilesList.isNotEmpty()) {
                val lastPhoto = photoFilesList.maxByOrNull { it.lastModified() }
                if (lastPhoto != null) {
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra("imageFile", lastPhoto.absolutePath)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No hay imagen para analizar. Toma una foto primero.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No hay imagen para analizar. Toma una foto primero.", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnBackHomeImage.setOnClickListener {
            finish()
        }
        
        // Botón para ver historial
        binding.btnViewHistory.setOnClickListener {
            showPhotoHistory()
        }
        
        loadPhotoHistory()
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

    private fun loadPhotoHistory() {
        photoFilesList.clear()
        if (photoDirectory.exists()) {
            val files = photoDirectory.listFiles() ?: arrayOf()
            photoFilesList.addAll(files.sortedByDescending { it.lastModified() })
        }
    }

    private fun showPhotoHistory() {
        loadPhotoHistory()
        if (photoFilesList.isEmpty()) {
            Toast.makeText(this, "No hay fotos en el historial.", Toast.LENGTH_SHORT).show()
            return
        }
        val displayNames = photoFilesList.map { file ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            "${file.name} - ${sdf.format(Date(file.lastModified()))} (${file.length()/1024}KB)"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Historial de Fotos (${photoFilesList.size})")
            .setItems(displayNames) { _, which -> showPhotoOptions(photoFilesList[which], displayNames[which]) }
            .show()
    }

    private fun showPhotoOptions(file: File, displayName: String) {
        val options = arrayOf("Reproducir", "Analizar", "Borrar")
        AlertDialog.Builder(this)
            .setTitle(displayName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> playPhotoFile(file)
                    1 -> analyzePhotoFile(file)
                    2 -> confirmDeletePhoto(file)
                }
            }
            .show()
    }

    private fun playPhotoFile(file: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                binding.imagePreview.setImageBitmap(bitmap)
                Toast.makeText(this, "Foto: ${file.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo cargar la foto.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzePhotoFile(file: File) {
        try {
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("imageFile", file.absolutePath)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error analizando foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDeletePhoto(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Deseas eliminar ${file.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (file.delete()) {
                    Toast.makeText(this, "Foto eliminada.", Toast.LENGTH_SHORT).show()
                    loadPhotoHistory()
                } else {
                    Toast.makeText(this, "Error al eliminar la foto.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
