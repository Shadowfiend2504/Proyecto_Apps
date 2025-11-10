package com.example.healthconnectai

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityMainBinding
import com.example.healthconnectai.ui.tasks.TareasActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🌟 Animación de entrada (fade + desplazamiento)
        val fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        val cards = listOf(
            binding.cardAudio,
            binding.cardImage,
            binding.cardMap,
            binding.cardResults,
            binding.cardTasks,
            binding.cardAlerts
        )

        cards.forEachIndexed { index, card ->
            card.startAnimation(fadeInUp)
            card.animation.startOffset = (index * 150).toLong()
        }

        // 💥 Animación de rebote (bounce) al tocar tarjetas
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        fun applyBounceEffect(vararg views: android.view.View) {
            views.forEach { view ->
                view.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        v.startAnimation(bounce)
                        v.animate().translationZ(12f).setDuration(100).start() // efecto 3D al presionar
                    } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        v.animate().translationZ(0f).setDuration(150).start() // vuelve al nivel original
                    }
                    false
                }
            }
        }

        applyBounceEffect(
            binding.cardAudio,
            binding.cardImage,
            binding.cardMap,
            binding.cardResults,
            binding.cardTasks,
            binding.cardAlerts
        )

        // 🎤 Card Analizar Audio
        binding.cardAudio.setOnClickListener {
            startActivity(Intent(this, AudioAnalysisActivity::class.java))
        }

        // 📷 Card Analizar Imagen
        binding.cardImage.setOnClickListener {
            startActivity(Intent(this, ImageAnalysisActivity::class.java))
        }

        // 🗺️ Card Ver Casos en el Mapa
        binding.cardMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // 📊 Card Ver Resultados
        binding.cardResults.setOnClickListener {
            startActivity(Intent(this, ResultActivity::class.java))
        }

        // 📋 Card Ver Tareas (API REST)
        binding.cardTasks.setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }

        // ⚠️ Card Ver Alertas y Recomendaciones
        binding.cardAlerts.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }
    }
}
