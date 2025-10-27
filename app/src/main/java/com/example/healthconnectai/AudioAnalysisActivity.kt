package com.example.healthconnectai

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.healthconnectai.databinding.ActivityAudioAnalysisBinding
import java.io.File

class AudioAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioAnalysisBinding
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private lateinit var audioFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRecordAudio.setOnClickListener {
            checkAudioPermissionAndRecord()
        }

        binding.btnStopRecording.setOnClickListener {
            stopRecording()
        }

        // 🟢 Nuevo botón: Reproducir audio grabado
        binding.btnPlayAudio.setOnClickListener {
            playAudio()
        }

        binding.btnAnalyze.setOnClickListener {
            binding.txtAudioResult.text =
                "Análisis: posible tos detectada.\nRecomendación: acudir al médico."
        }
    }

    private fun checkAudioPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                200
            )
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        audioFile = File.createTempFile("record_", ".3gp", cacheDir)
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFile.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
            start()
        }
        Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        Toast.makeText(this, "Grabación detenida.", Toast.LENGTH_SHORT).show()
    }

    private fun playAudio() {
        if (::audioFile.isInitialized && audioFile.exists()) {
            player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
            }
            Toast.makeText(this, "Reproduciendo audio...", Toast.LENGTH_SHORT).show()

            // Cuando termine la reproducción
            player?.setOnCompletionListener {
                Toast.makeText(this, "Reproducción finalizada", Toast.LENGTH_SHORT).show()
                player?.release()
                player = null
            }
        } else {
            Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        player?.release()
    }
}

