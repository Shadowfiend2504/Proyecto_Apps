package com.example.healthconnectai

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.healthconnectai.data.ai.DiagnosisResult
import com.example.healthconnectai.databinding.ActivityResultBinding
import com.example.healthconnectai.ui.viewmodel.HealthDiagnosisViewModel

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: HealthDiagnosisViewModel by viewModels()
    private var currentErrorDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observar carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar resultado
        viewModel.diagnosisResult.observe(this) { result ->
            displayDiagnosis(result)
        }

        // Botón atrás
        binding.btnBackHomeResults.setOnClickListener {
            finish()
        }

        // Generar diagnóstico de inmediato con los extras (sin pedir síntomas por texto)
        generateDiagnosis()
    }

    private fun generateDiagnosis() {
        // Leer extras (si vienen) y pasarlas al ViewModel
        val imageFile = intent.getStringExtra("imageFile")
        val audioFile = intent.getStringExtra("audioFile")
        // Nota: userLocation y userProfile pueden pasarse en extras si se requiere
        viewModel.generateDiagnosis(
            audioFile = audioFile,
            imageFile = imageFile
        )
    }

    private fun displayDiagnosis(result: DiagnosisResult) {
        if (!result.success) {
            binding.txtFinalResult.text = """
                ⚠️ Error: ${result.errorMessage}
                
                Por favor, intenta nuevamente.
            """.trimIndent()
            binding.txtFinalResult.setTextColor(Color.RED)
            // Mostrar diálogo con opciones (Reintentar / Cerrar)
            showErrorDialog(result.errorMessage ?: "Error desconocido")
            return
        }

        // Diagnóstico principal
        binding.txtFinalResult.text = """
            🧠 DIAGNÓSTICO PRELIMINAR
            ═══════════════════════════
            
            ${result.preliminaryDiagnosis}
            
            ⚠️ NIVEL DE URGENCIA: ${result.urgencyLevel}
            
            📋 CONDICIONES POTENCIALES:
            ${result.potentialConditions.mapIndexed { i, c -> 
                "${i + 1}. $c" 
            }.joinToString("\n")}
            
            💡 RECOMENDACIONES:
            ${result.recommendations.mapIndexed { i, r -> 
                "${i + 1}. $r" 
            }.joinToString("\n")}
            
            ${if (result.shouldConsultDoctor) {
                """
                ⛔ CONSULTA A UN MÉDICO PROFESIONAL
                Este análisis es preliminar y no reemplaza atención médica real.
                """.trimIndent()
            } else {
                "✅ Mantén un seguimiento de tus síntomas."
            }}
        """.trimIndent()

        // Color según urgencia
        val color = when (result.urgencyLevel) {
            "CRÍTICA" -> Color.RED
            "ALTA" -> Color.parseColor("#FF6B6B")
            "MEDIA" -> Color.parseColor("#FFA500")
            else -> Color.GREEN
        }
        binding.txtFinalResult.setTextColor(color)
    }

    private fun showErrorDialog(message: String) {
        // Evitar múltiples diálogos
        currentErrorDialog?.dismiss()
        val builder = AlertDialog.Builder(this)
            .setTitle("Error en el análisis")
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Reintentar") { _, _ ->
                // Volver a generar diagnóstico con los mismos extras
                generateDiagnosis()
            }
            .setNegativeButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }

        currentErrorDialog = builder.create()
        currentErrorDialog?.show()
    }

    override fun onDestroy() {
        currentErrorDialog?.dismiss()
        super.onDestroy()
    }
}
