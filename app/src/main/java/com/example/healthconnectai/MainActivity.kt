package com.example.healthconnectai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityMainBinding
import com.example.healthconnectai.ui.tasks.TareasActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón Analizar Audio
        binding.btnAudio.setOnClickListener {
            startActivity(Intent(this, AudioAnalysisActivity::class.java))
        }

        // Botón Analizar Imagen
        binding.btnImage.setOnClickListener {
            startActivity(Intent(this, ImageAnalysisActivity::class.java))
        }

        // Botón Ver Casos en el Mapa
        binding.btnMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // Botón Ver Resultados
        binding.btnResults.setOnClickListener {
            startActivity(Intent(this, ResultActivity::class.java))
        }

        // 🆕 Nuevo Botón: Ver tareas y sincronizar API REST
        binding.btnViewTasks.setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }
    }
}

