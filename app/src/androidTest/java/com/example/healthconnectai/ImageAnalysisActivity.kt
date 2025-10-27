package com.healthconnectai

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.healthconnectai.databinding.ActivityImageAnalysisBinding

class ImageAnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageAnalysisBinding
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCaptureImage.setOnClickListener {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }

        binding.btnAnalyzeImage.setOnClickListener {
            binding.txtImageResult.text = "Posibles signos visibles de enfermedad detectados.\nRecomendación: consulte un centro médico."
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            binding.imgPreview.setImageBitmap(imageBitmap)
        }
    }
}
