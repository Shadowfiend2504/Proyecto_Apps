package com.example.healthconnectai

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.healthconnectai.databinding.ActivityAudioAnalysisBinding
import java.io.File
import java.io.IOException

class AudioAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioAnalysisBinding
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private lateinit var audioFile: File

    // Lanzador moderno para pedir permiso RECORD_AUDIO
    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startRecordingInternal()
            } else {
                Toast.makeText(this, "Permiso de micrófono denegado.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Crear archivo temporal en cacheDir para no necesitar WRITE_EXTERNAL_STORAGE
        audioFile = File.createTempFile("audio_record_", ".3gp", cacheDir)

        binding.btnStart.setOnClickListener {
            // Pide permiso (si ya está concedido, el launcher responde inmediatamente con true)
            requestAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }

        binding.btnStop.setOnClickListener {
            stopRecording()
        }

        binding.btnPlay.setOnClickListener {
            playAudioSafe()
        }
        binding.btnBackHomeAudio.setOnClickListener { finish() }
    }

    private fun startRecordingInternal() {
        // Configura y arranca MediaRecorder (solo se llama si permission granted)
        recorder = MediaRecorder()
        try {
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            Toast.makeText(this, "Grabando... archivo: ${audioFile.name}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error iniciando la grabación: ${e.message}", Toast.LENGTH_LONG).show()
            try {
                recorder?.release()
            } catch (ignored: Exception) { }
            recorder = null
        } catch (e: RuntimeException) {
            // Puede ocurrir si el micrófono está ocupado o falta permiso
            e.printStackTrace()
            Toast.makeText(this, "No se pudo iniciar la grabación (mic posiblemente ocupado o permiso faltante).", Toast.LENGTH_LONG).show()
            try {
                recorder?.release()
            } catch (ignored: Exception) { }
            recorder = null
        }
    }

    private fun stopRecording() {
        recorder?.let {
            try {
                it.stop()
            } catch (e: RuntimeException) {
                // stop puede lanzar si start no se llamó correctamente
                e.printStackTrace()
            } finally {
                try { it.release() } catch (ignored: Exception) {}
                recorder = null
                Toast.makeText(this, "Grabación detenida. Guardada en: ${audioFile.name}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No hay grabación activa.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudioSafe() {
        if (!::audioFile.isInitialized || !audioFile.exists()) {
            Toast.makeText(this, "No hay archivo de audio para reproducir.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    Toast.makeText(this@AudioAnalysisActivity, "Reproducción finalizada.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error reproduciendo audio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Si la app se va a segundo plano, aseguramos liberar recursos
        try {
            recorder?.let {
                try { it.stop() } catch (ignored: Exception) {}
                it.release()
            }
        } catch (ignored: Exception) {}
        recorder = null

        player?.let {
            try { it.stop() } catch (ignored: Exception) {}
            it.release()
        }
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        try { recorder?.release() } catch (ignored: Exception) {}
        try { player?.release() } catch (ignored: Exception) {}
        recorder = null
        player = null
    }

}
