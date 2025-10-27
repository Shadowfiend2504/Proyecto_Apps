package com.example.healthconnectai.ui.tasks

import android.app.Application
import androidx.lifecycle.*
import com.example.healthconnectai.data.local.AppDatabase
import com.example.healthconnectai.data.model.Tarea
import com.example.healthconnectai.data.repository.TareaRepository
import kotlinx.coroutines.launch

class TareaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TareaRepository
    val tareas: LiveData<List<Tarea>>

    init {
        val dao = AppDatabase.getDatabase(application).tareaDao()
        repository = TareaRepository(dao)
        tareas = repository.tareas
    }

    // LiveData para notificar resultado del POST remoto
    private val _postResult = MutableLiveData<String?>(null)
    val postResult: LiveData<String?> = _postResult

    fun addTarea(tarea: Tarea) = viewModelScope.launch {
        val syncSuccess = repository.insert(tarea)
        if (syncSuccess) {
            _postResult.postValue("Tarea guardada y sincronizada con Google Sheets")
        } else {
            _postResult.postValue("Tarea guardada localmente (sin sincronizar con Google Sheets)")
        }
    }

    // Limpiar notificación después de mostrarla
    fun clearPostResult() {
        _postResult.value = null
    }

    fun syncFromSheet(url: String) = viewModelScope.launch {
        repository.syncFromSheet(url)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}
