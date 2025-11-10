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
            ⚠️ Mantente hidratado y evita exposición prolongada al sol.
            😷 Si tienes tos persistente o fiebre, consulta con tu médico.
            💊 Toma tus medicamentos a la hora indicada.
            🧘 Practica ejercicios de respiración si sientes estrés.
        """.trimIndent()

        binding.btnBackHomeAlerts.setOnClickListener { finish() }
    }
}
