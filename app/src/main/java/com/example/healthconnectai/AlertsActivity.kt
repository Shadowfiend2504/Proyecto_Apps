package com.example.healthconnectai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityAlertsBinding
import android.content.Context
import org.json.JSONObject

class AlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intentar leer el último aviso persistido
        val prefs = getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
        val last = prefs.getString("last_alert", null)

        if (!last.isNullOrEmpty()) {
            try {
                val obj = JSONObject(last)
                val urgency = obj.optString("urgency", "--")
                val preliminary = obj.optString("preliminary", "Sin diagnóstico disponible")
                val recs = obj.optJSONArray("recommendations")
                val sb = StringBuilder()
                sb.append("⚠️ NIVEL DE URGENCIA: $urgency\n\n")
                sb.append("🧠 DIAGNÓSTICO PRELIMINAR:\n")
                sb.append(preliminary)
                sb.append("\n\n💡 RECOMENDACIONES:\n")
                if (recs != null) {
                    for (i in 0 until recs.length()) {
                        sb.append("${i + 1}. ${recs.optString(i)}\n")
                    }
                }
                binding.txtAlerts.text = sb.toString()
            } catch (e: Exception) {
                binding.txtAlerts.text = "Sin alertas recientes."
            }
        } else {
            binding.txtAlerts.text = "Sin alertas recientes."
        }

        binding.btnBackHomeAlerts.setOnClickListener { finish() }
    }
}
