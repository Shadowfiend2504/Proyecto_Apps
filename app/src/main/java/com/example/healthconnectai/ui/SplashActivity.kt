package com.example.healthconnectai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java
import kotlin.text.substring

class SplashActivity : AppCompatActivity() {

    private lateinit var txtDescription: TextView
    private lateinit var btnStart: Button
    private val fullText = "Tu asistente inteligente para el bienestar y el diagnóstico rápido."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imgLogo)
        val title = findViewById<TextView>(R.id.txtAppName)
        txtDescription = findViewById(R.id.txtDescription)
        btnStart = findViewById(R.id.btnStart)

        // Animaciones
        val fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeInDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed)

        logo.startAnimation(fadeInUp)
        title.startAnimation(fadeIn)

        // Efecto de tipeo para la descripción
        startTypingAnimation()

        // Mostrar el botón "Comenzar" después del tipeo
        Handler(Looper.getMainLooper()).postDelayed({
            btnStart.visibility = Button.VISIBLE
            btnStart.startAnimation(fadeInDelayed)
        }, 3500)

        // Acción del botón "Comenzar"
        btnStart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun startTypingAnimation() {
        val handler = Handler(Looper.getMainLooper())
        var index = 0

        val typingRunnable = object : Runnable {
            override fun run() {
                if (index <= fullText.length) {
                    txtDescription.text = fullText.substring(0, index)
                    index++
                    handler.postDelayed(this, 40) // velocidad (40ms por letra)
                }
            }
        }
        handler.post(typingRunnable)
    }
}
