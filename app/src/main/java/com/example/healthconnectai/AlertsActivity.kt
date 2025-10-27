package com.example.healthconnectai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityAlertsBinding

class AlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtAlerts.text = """
            ⚠️ Alertas y Recomendaciones:

            - Mantente hidratado si presentas tos o fiebre.
            - Usa tapabocas en lugares cerrados.
            - Consulta a un médico si tus síntomas persisten.
            - Verifica alertas sanitarias locales en el mapa.
        """.trimIndent()
    }
}
