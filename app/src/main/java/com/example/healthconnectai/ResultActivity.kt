package com.example.healthconnectai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtFinalResult.text = """
            🧠 Diagnóstico preliminar: Sin anomalías detectadas.
            
            ✅ Salud general: Estable.
            ⚙️ Recomendación: Mantén tu rutina saludable y realiza chequeos periódicos.
        """.trimIndent()

        binding.btnBackHomeResults.setOnClickListener {
            finish() // Regresar al inicio
        }
    }
}
