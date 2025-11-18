package com.example.healthconnectai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.healthconnectai.BuildConfig
import com.example.healthconnectai.data.ai.DiagnosisResult
import com.example.healthconnectai.data.ai.GeminiHealthClient
import com.example.healthconnectai.data.ai.HealthDataAggregator
import com.example.healthconnectai.data.ai.HealthMetrics
import com.example.healthconnectai.data.ai.LocationData
import com.example.healthconnectai.data.ai.UserProfile
import com.example.healthconnectai.data.ai.LocalDiagnosisGenerator
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.BitmapFactory
import java.io.File

class HealthDiagnosisViewModel(application: Application) : AndroidViewModel(application) {
    
    private val geminiClient by lazy {
        GeminiHealthClient(BuildConfig.GEMINI_API_KEY)
    }
    
    private val dataAggregator = HealthDataAggregator()
    private val localGenerator = LocalDiagnosisGenerator()
    
    private val _diagnosisResult = MutableLiveData<DiagnosisResult>()
    val diagnosisResult: LiveData<DiagnosisResult> = _diagnosisResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Genera diagnóstico consolidado
     */
    fun generateDiagnosis(
        audioFile: String? = null,
        imageFile: String? = null,
        userLocation: LocationData? = null,
        userProfile: UserProfile? = null
    ) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                // Recopilar datos
                val metrics = dataAggregator.aggregateAllData(
                    audioFile = audioFile,
                    imageFile = imageFile,
                    userLocation = userLocation,
                    userProfile = userProfile
                )
                
                // Enviar a IA
                val diagnosis = geminiClient.analyzeHealthData(metrics)

                // Si la respuesta viene vacía/no body, usar generador local
                if (!diagnosis.success) {
                    // Usar generador local para diagnósticos de fallback
                    val audio = metrics.audioAnalysis
                    val image = metrics.imageAnalysis
                    
                    if (audio != null || image != null) {
                        val localDiagnosis = localGenerator.generateCombinedDiagnosis(audio, image, metrics.userProfile)
                        _diagnosisResult.postValue(localDiagnosis)
                        _isLoading.postValue(false)
                        return@launch
                    } else {
                        // Sin datos locales: mostrar error
                        _diagnosisResult.postValue(diagnosis)
                        _isLoading.postValue(false)
                        return@launch
                    }
                }

                _diagnosisResult.postValue(diagnosis)

                // Persistir último aviso/recomendación compacto en SharedPreferences
                if (diagnosis.success) {
                    try {
                        val prefs = getApplication<Application>().getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
                        val json = JSONObject()
                        json.put("urgency", diagnosis.urgencyLevel)
                        json.put("preliminary", diagnosis.preliminaryDiagnosis)
                        val arr = JSONArray()
                        diagnosis.recommendations.forEach { arr.put(it) }
                        json.put("recommendations", arr)
                        json.put("timestamp", diagnosis.timestamp)
                        prefs.edit().putString("last_alert", json.toString()).apply()
                    } catch (e: Exception) {
                        // No bloquear el flujo si falla el guardado
                    }
                }
            } catch (e: Exception) {
                _diagnosisResult.postValue(
                    DiagnosisResult.error("Error: ${e.message}")
                )
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
