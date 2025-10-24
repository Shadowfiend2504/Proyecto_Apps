package com.example.proyectoapps.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.proyectoapps.data.Tarea
import com.example.proyectoapps.network.RetrofitClient
import com.example.proyectoapps.repository.TareaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TareaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TareaRepository(RetrofitClient.api, application.applicationContext)

    val tareas = repository.getTareasFlow().asLiveData()

    // Estado para notificar a la UI que la sincronización terminó (true una sola vez)
    private val _syncFinished = MutableStateFlow(false)
    val syncFinished: StateFlow<Boolean> = _syncFinished.asStateFlow()

    fun sync() {
        viewModelScope.launch {
            _syncFinished.value = false
            repository.syncFromRemote()
            // Notificar a la UI que finalizó (la UI puede resetear si lo necesita)
            _syncFinished.value = true
        }
    }

    // Estado para notificar a la UI que el push terminó
    private val _pushFinished = MutableStateFlow(false)
    val pushFinished: StateFlow<Boolean> = _pushFinished.asStateFlow()

    /**
     * Enviar todas las tareas locales al webApp proporcionado.
     */
    fun push(webAppUrl: String) {
        viewModelScope.launch {
            _pushFinished.value = false
            val ok = withContext(Dispatchers.IO) { repository.pushToRemote(webAppUrl) }
            _pushFinished.value = ok
        }
    }

    fun resetPushFinished() {
        _pushFinished.value = false
    }

    /**
     * Resetear indicador de sincronización (si la UI necesita observar otra vez)
     */
    fun resetSyncFinished() {
        _syncFinished.value = false
    }

    fun addTarea(titulo: String, descripcion: String?) {
        viewModelScope.launch {
            val tarea = Tarea(titulo = titulo, descripcion = descripcion, fecha = System.currentTimeMillis())
            repository.insertTarea(tarea)
        }
    }

    /**
     * Enviar todas las tareas locales al webApp proporcionado.
     */
    fun pushToWebApp(webAppUrl: String) {
        viewModelScope.launch {
            _pushFinished.value = false
            val ok = withContext(Dispatchers.IO) { repository.pushToRemote(webAppUrl) }
            _pushFinished.value = ok
        }
    }
}
