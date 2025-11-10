package com.example.healthconnectai.ui.tasks

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthconnectai.databinding.ActivityTareasBinding
import android.app.AlertDialog
import android.widget.EditText
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import com.example.healthconnectai.data.model.Tarea
import android.widget.Toast

class TareasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTareasBinding
    private val viewModel: TareaViewModel by viewModels()
    private lateinit var adapter: TareaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TareaAdapter()
        binding.recyclerTareas.layoutManager = LinearLayoutManager(this)
        binding.recyclerTareas.adapter = adapter

        viewModel.tareas.observe(this) {
            adapter.submitList(it)
        }

        viewModel.postResult.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearPostResult()
            }
        }

        binding.btnAdd.setOnClickListener {
            showAddDialog()
        }

        binding.btnSyncSheet.setOnClickListener {
            // URL de la hoja publicada como CSV
            val sheetUrl = "https://docs.google.com/spreadsheets/d/1WLankjx3ktol-e162ZTPCFR2jAYmub1URDJ2XTXxBmM"
            viewModel.syncFromSheet(sheetUrl)
            
            // Log para debugging
            android.util.Log.d("TareasActivity", "Iniciando sincronización con Google Sheet CSV")
        }

        binding.btnBackHomeTareas.setOnClickListener {
            finish() // Regresar al inicio
        }
    }

    private fun showAddDialog() {
        // Usaremos EditText programáticamente para simplicidad
        val titleInput = EditText(this)
        titleInput.hint = "Título"
        val descInput = EditText(this)
        descInput.hint = "Descripción"

        val container = LinearLayoutCompat(this).apply {
            orientation = LinearLayoutCompat.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(titleInput)
            addView(descInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Agregar tarea")
            .setView(container)
            .setPositiveButton("Agregar") { _, _ ->
                val titulo = titleInput.text.toString().ifBlank { "Tarea sin título" }
                val descripcion = descInput.text.toString().ifBlank { "" }
                val tarea = Tarea(titulo = titulo, descripcion = descripcion, fecha = java.time.LocalDate.now().toString())
                viewModel.addTarea(tarea)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
