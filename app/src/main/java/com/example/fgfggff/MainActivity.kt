package com.example.proyectoapps

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.proyectoapps.ui.camera.CameraCaptureScreen
import com.example.proyectoapps.ui.theme.HealthConnectAITheme
import com.example.proyectoapps.ui.tasks.TasksScreen
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


private val CAMERA_PERMISSION = Manifest.permission.CAMERA

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permiso si no está concedido
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), 0)
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun App() {
    HealthConnectAITheme {
        val navController = rememberNavController()
        AppNavHost(navController)
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("camera") { CameraCaptureScreen() }
        composable("home") { HomeScreen(navController) }
        composable("tasks") { TasksScreen() }
        composable("capture") { CaptureScreen(navController) }
        composable("map") { MapScreen(navController) }
        composable("alerts") { AlertsScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("HealthConnect AI") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Button(onClick = { navController.navigate("camera") }) {
                Text("Abrir cámara")
            }
            Button(onClick = { navController.navigate("tasks") }) {
                Text("Ver tareas")
            }
            Button(onClick = { navController.navigate("capture") }) {
                Text("Registrar síntomas")
            }
            Button(onClick = { navController.navigate("map") }) {
                Text("Ver mapa epidemiológico")
            }
            Button(onClick = { navController.navigate("alerts") }) {
                Text("Ver alertas y recomendaciones")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Captura de Síntomas") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Button(onClick = { /* TODO: Abrir cámara */ }) {
                Text("Capturar imagen")
            }
            Button(onClick = { /* TODO: Grabar audio */ }) {
                Text("Grabar audio")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mapa Epidemiológico") }) }
    ) { padding ->
        Text("Aquí irá el mapa con Google Maps o Mapbox",
            modifier = Modifier.padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Alertas y Recomendaciones") }) }
    ) { padding ->
        Text("Aquí se mostrarán las recomendaciones de la IA",
            modifier = Modifier.padding(padding))
    }
}
