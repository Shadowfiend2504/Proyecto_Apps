package com.example.proyectoapps.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectoapps.data.Tarea
import com.example.proyectoapps.viewmodel.TareaViewModel

@Composable
fun TasksScreen(viewModel: TareaViewModel = viewModel()) {
    val tareas by viewModel.tareas.observeAsState(emptyList())
    val syncFinished by viewModel.syncFinished.collectAsState(initial = false)
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var webAppUrl by remember { mutableStateOf("") }
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current
    val pushFinished by viewModel.pushFinished.collectAsState(initial = false)

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = {
                if (titulo.isNotBlank()) {
                    viewModel.addTarea(titulo, descripcion.ifBlank { null })
                    titulo = ""
                    descripcion = ""
                }
            }) {
                Text("Agregar")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { viewModel.sync() }) {
                Text("Sincronizar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            OutlinedTextField(
                value = webAppUrl,
                onValueChange = { webAppUrl = it },
                label = { Text("WebApp URL (Apps Script)") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (webAppUrl.isNotBlank()) {
                    viewModel.push(webAppUrl.trim())
                } else {
                    Toast.makeText(context, "Introduce la URL del WebApp", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Subir")
            }
        }

        // Mostrar resultado del push
        LaunchedEffect(pushFinished) {
            if (pushFinished) {
                Toast.makeText(context, "Push finalizó correctamente", Toast.LENGTH_SHORT).show()
                viewModel.resetPushFinished()
            }
        }

        // Si la sincronización finalizó, cerrar la Activity (comportamiento pedido por el usuario)
        LaunchedEffect(syncFinished) {
            if (syncFinished) {
                activity?.finish()
                // resetear indicador en el ViewModel para posteriores sincronizaciones
                viewModel.resetSyncFinished()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Tareas", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tareas) { tarea: Tarea ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(tarea.titulo, style = MaterialTheme.typography.titleSmall)
                        tarea.descripcion?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
