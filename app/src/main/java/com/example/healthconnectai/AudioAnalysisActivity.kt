package com.example.healthconnectai

import android.media.MediaPlayer
import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.healthconnectai.databinding.ActivityAudioAnalysisBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AudioAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioAnalysisBinding
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private lateinit var audioFile: File
    private val audioDirectory by lazy { File(cacheDir, "audio_history") }
    private val audioFilesList = mutableListOf<File>()

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

        // Crear directorio de historial
        if (!audioDirectory.exists()) {
            audioDirectory.mkdirs()
        }

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
        
        // Botón para analizar audio actual
        binding.btnAnalyzeAudio.setOnClickListener {
            if (::audioFile.isInitialized && audioFile.exists()) {
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("audioFile", audioFile.absolutePath)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay audio para analizar. Graba primero.", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnBackHomeAudio.setOnClickListener { finish() }
        
        // Botón para ver historial
        binding.btnViewHistory.setOnClickListener {
            showAudioHistory()
        }
        
        loadAudioHistory()
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
                
                // Guardar archivo en directorio de historial
                val savedFile = File(audioDirectory, "audio_${System.currentTimeMillis()}.3gp")
                try {
                    audioFile.copyTo(savedFile, overwrite = true)
                    loadAudioHistory()
                    // Lanzar ResultActivity para analizar audio
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra("audioFile", savedFile.absolutePath)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

    private fun loadAudioHistory() {
        audioFilesList.clear()
        if (audioDirectory.exists()) {
            audioFilesList.addAll(audioDirectory.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList())
        }
    }

    private fun showAudioHistory() {
        loadAudioHistory()
        if (audioFilesList.isEmpty()) {
            Toast.makeText(this, "Sin grabaciones en el historial", Toast.LENGTH_SHORT).show()
            return
        }

        val displayNames = audioFilesList.map { file ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = sdf.format(Date(file.lastModified()))
            "${file.name} - $date (${file.length() / 1024}KB)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Historial de Grabaciones (${audioFilesList.size})")
            .setItems(displayNames) { _, which ->
                showAudioOptions(audioFilesList[which], displayNames[which])
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showAudioOptions(file: File, displayName: String) {
        AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setMessage(displayName)
            .setPositiveButton("Reproducir") { _, _ ->
                playAudioFile(file)
            }
            .setNeutralButton("Analizar") { _, _ ->
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("audioFile", file.absolutePath)
                startActivity(intent)
            }
            .setNegativeButton("Borrar") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Confirmar borrado")
                    .setMessage("¿Eliminar $displayName?")
                    .setPositiveButton("Sí, borrar") { _, _ ->
                        file.delete()
                        loadAudioHistory()
                        Toast.makeText(this, "Archivo eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .show()
    }

    private fun playAudioFile(file: File) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    Toast.makeText(this@AudioAnalysisActivity, "Reproducción finalizada.", Toast.LENGTH_SHORT).show()
                }
            }
            Toast.makeText(this, "Reproduciendo: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error reproduciendo audio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
