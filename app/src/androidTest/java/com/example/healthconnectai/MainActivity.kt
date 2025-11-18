package com.example.healthconnectai.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.healthconnectai.AudioAnalysisActivity
import com.example.healthconnectai.MapActivity
import com.example.healthconnectai.databinding.ActivityMainBinding

class TestMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botones que llevan a las otras pantallas (versión androidTest)
        binding.btnImageAnalysis.setOnClickListener {
            startActivity(Intent(this, ImageAnalysisActivity::class.java))
        }

        binding.btnAudioAnalysis.setOnClickListener {
            startActivity(Intent(this, AudioAnalysisActivity::class.java))
        }

        binding.btnMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}
