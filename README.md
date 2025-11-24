# 🏥 HealthConnectAI - Asistente de Diagnóstico Inteligente

<div align="center">

https://deepwiki.com/badge-maker?url=https%3A%2F%2Fdeepwiki.com%2FShadowfiend2504%2FProyecto_Apps 
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Google_Gemini-8E75B6?style=for-the-badge&logo=google&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**Un proyecto universitario que combina IA y análisis multimodal para diagnósticos preliminares de salud**

</div>

---

## 📋 Contenido

- [Descripción del Proyecto](#-descripción-del-proyecto)
- [Características Principales](#-características-principales)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Tecnologías Utilizadas](#-tecnologías-utilizadas)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Uso](#-uso)
- [Integrantes del Grupo](#-integrantes-del-grupo)
- [Documentación](#-documentación)
- [Información Legal](#-información-legal)

---

## 🎯 Descripción del Proyecto

**HealthConnectAI** es una aplicación móvil de Android que proporciona **diagnósticos preliminares inteligentes** utilizando inteligencia artificial. 

Este es un **proyecto universitario** desarrollado como parte de un curso/trabajo final que demuestra la integración de tecnologías modernas como:
- 🤖 **Google Gemini AI** para análisis inteligente
- 📱 **Android SDK** con arquitectura moderna
- 🎤 **Análisis de audio** para detección de síntomas
- 📸 **Análisis de imágenes** con visión por computadora
- 📍 **Integración de mapas** para localizar hospitales cercanos

### Objetivo
Proporcionar a los usuarios una herramienta de **autoevaluación preliminar de salud** que les ayude a entender sus síntomas y les sugiera recursos médicos cercanos, **sin pretender reemplazar la consulta médica profesional**.

---

## ✨ Características Principales

### 🎤 Análisis de Audio
- Captura de grabaciones de voz (3-5 segundos)
- Análisis de tono y características acústicas
- Detección de síntomas auditivos (tos, respiración, etc.)

### 📸 Análisis de Imágenes
- Captura de fotos de zonas afectadas
- Análisis inteligente con Gemini Vision API
- Descripción de hallazgos visuales

### 📝 Reporte de Síntomas
- Registro manual de síntomas experimentados
- Historial completo de reportes
- Almacenamiento local con Room Database

### 🤖 Diagnóstico Inteligente
- Análisis multimodal (audio + imagen + síntomas)
- Generación de diagnósticos preliminares con IA
- Recomendaciones personalizadas
- Estimación de urgencia médica

### 📍 Búsqueda de Hospitales
- Integración con Google Maps
- Búsqueda de hospitales cercanos
- Información de ubicación y distancia
- Direcciones y contacto

### 📱 Interfaz de Usuario
- Diseño intuitivo y fácil de usar
- Navegación clara entre módulos
- Hub central con acceso a todas las funciones

---

## 🏗️ Estructura del Proyecto

```
Proyecto_Apps/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/                 # Código Kotlin/Java
│   │   │   │   ├── activities/       # Activities principales
│   │   │   │   ├── fragments/        # Fragments
│   │   │   │   ├── viewmodels/       # ViewModels
│   │   │   │   ├── data/             # Modelos de datos
│   │   │   │   │   ├── api/          # Servicios de API (Retrofit)
│   │   │   │   │   └── models/       # Clases de datos
│   │   │   │   ├── database/         # Room Database
│   │   │   │   ├── utils/            # Utilidades
│   │   │   │   └── repository/       # Repositorios
│   │   │   ├── res/                  # Recursos (layouts, strings, etc.)
│   │   │   │   ├── drawable/         # Imágenes y vectores
│   │   │   │   ├── layout/           # Layouts XML
│   │   │   │   └── values/           # Strings, colores, estilos
│   │   │   └── AndroidManifest.xml   # Configuración de app
│   │   ├── androidTest/              # Pruebas de instrumentación
│   │   └── test/                     # Pruebas unitarias
│   └── build.gradle.kts              # Configuración de Gradle
├── gradle/                           # Configuración de Gradle
├── scripts/                          # Scripts auxiliares
└── build.gradle.kts                  # Build principal
```

---

## 🔧 Tecnologías Utilizadas

### Framework & Lenguaje
- **Kotlin** - Lenguaje de programación principal
- **Android SDK 34+** - Framework de Android
- **AndroidX** - Bibliotecas de compatibilidad

### Arquitectura & Patrones
- **MVVM (Model-View-ViewModel)** - Patrón arquitectónico
- **Repository Pattern** - Acceso a datos
- **Coroutines** - Programación asincrónica

### APIs y Servicios
- **Google Gemini AI** - IA generativa para diagnósticos
- **Google Places API** - Búsqueda de hospitales
- **Google Maps SDK** - Visualización de mapa
- **Retrofit** - Cliente HTTP

### Base de Datos
- **Room Database** - Base de datos local SQLite
- **Gson** - Serialización JSON

### Librerías Adicionales
- **Material Design 3** - Componentes UI modernos
- **CameraX** - Captura de imágenes
- **MediaRecorder** - Grabación de audio
- **FusedLocationProvider** - Ubicación GPS

---

## 📦 Requisitos Previos

### Software Requerido
- **Android Studio** 2023.1 o superior
- **JDK 11** o superior
- **Android SDK** nivel 34 o superior
- **Gradle** 8.0+ (incluido con Android Studio)

### Cuentas y APIs Requeridas
- **Google Cloud Project** con acceso a:
  - Google Gemini API
  - Google Places API
  - Google Maps SDK
  - (Instrucciones en `local.properties.example`)

### Dispositivo/Emulador
- **Android 11+** (API 30+)
- Mínimo 4 GB de RAM
- Conexión a internet
- Permisos: Cámara, Micrófono, Ubicación

---

## 🚀 Instalación

### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/Shadowfiend2504/Proyecto_Apps.git
cd Proyecto_Apps
```

### Paso 2: Configurar las API Keys

1. Copia el archivo `local.properties.example` a `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```

2. Obtén tus API keys en [Google Cloud Console](https://console.cloud.google.com/)

3. Edita `local.properties` y agrega tus keys:
   ```properties
   GEMINI_API_KEY=tu_clave_aqui
   GOOGLE_PLACES_API_KEY=tu_clave_aqui
   GOOGLE_MAPS_API_KEY=tu_clave_aqui
   ```

### Paso 3: Abrir en Android Studio

1. Abre Android Studio
2. Selecciona `File` → `Open` → ve a la carpeta del proyecto
3. Android Studio descargará automáticamente dependencias

### Paso 4: Compilar y Ejecutar

```bash
# Compilar (usando Gradle)
./gradlew build

# O ejecutar directamente en Android Studio
# Presiona Shift + F10 (Windows/Linux) o Ctrl + R (Mac)
```

---

## 💻 Uso

### Flujo Principal de la Aplicación

```
┌─────────────────────────────────────┐
│    PANTALLA PRINCIPAL (Hub)         │
└─────────────────────────────────────┘
          ↓
    Elige una opción:
    │
    ├─► 🎤 Análisis de Audio
    │   └─ Graba síntomas auditivos
    │
    ├─► 📸 Análisis de Imagen
    │   └─ Captura zona afectada
    │
    ├─► 📝 Reportar Síntomas
    │   └─ Lista manual de síntomas
    │
    ├─► 🤖 Ver Diagnóstico
    │   └─ Obtén análisis inteligente
    │
    └─► 📍 Buscar Hospitales
        └─ Encuentra recursos médicos
```

### Ejemplo de Uso

1. **Iniciar la app** → Ves el menú principal
2. **Grabar audio** → Presiona el botón de micrófono
3. **Capturar imagen** → Toma foto de la zona afectada
4. **Reportar síntomas** → Ingresa síntomas manualmente
5. **Ver resultado** → Presiona "Ver Diagnóstico" para análisis IA
6. **Buscar hospitales** → Abre mapa con recursos cercanos

---

## 👥 Integrantes del Grupo

Este es un proyecto académico desarrollado por:

| Nombre | Rol |
|--------|-----|
| **Juan Carvajal** | Desarrollador |
| **Karol Zapata** | Desarrolladora |
| **Naren Cipagauta** | Desarrollador |

---

## 📚 Documentación

Se incluyen documentos detallados en el repositorio:

- **README_MAPA.md** - Guía específica para el módulo de mapas
- **QUICK_START_MAPA.md** - Inicio rápido (5 minutos)
- **RESUMEN_EJECUTIVO.md** - Resumen del proyecto completo
- **RESUMEN_FINAL_MAPA.md** - Detalles de integración de mapas
- **CHECKLIST_MAPA.md** - Lista de verificación

### Archivos de Configuración

- **local.properties.example** - Plantilla para variables sensibles
- **build.gradle.kts** - Configuración de Gradle
- **settings.gradle.kts** - Configuración de módulos

---

## ⚠️ Información Legal

### Descargo de Responsabilidad Médica

**IMPORTANTE:** HealthConnectAI proporciona **únicamente análisis preliminares generados por IA**. 

```
⛔ ESTO NO ES DIAGNÓSTICO MÉDICO
```

- Los resultados NO reemplazan la consulta con profesionales médicos
- En caso de emergencia, llama a servicios de emergencia locales
- Consulta siempre a un médico certificado para diagnósticos definitivos
- El proyecto es solo educativo/demostrativo

### Privacidad y Datos

- Los datos se almacenan localmente en el dispositivo
- Las API keys se guardan en `local.properties` (no commiteadas)
- Se recomienda usar HTTPS para todas las conexiones
- El usuario es responsable de sus datos

### Licencia

Este proyecto se distribuye bajo licencia **MIT**. Ver archivo `LICENSE` para detalles.

---

## 🤝 Contribuciones

Como proyecto académico, está cerrado a contribuciones externas. 

Para reportar bugs o sugerencias, contacta a los integrantes del grupo.

---

## 📞 Contacto

Para preguntas sobre el proyecto:
- 📧 Contacta a través de GitHub Issues
- 🐙 Visita el repositorio: [Shadowfiend2504/Proyecto_Apps](https://github.com/Shadowfiend2504/Proyecto_Apps)

---

## 🎓 Contexto Académico

**HealthConnectAI** es un proyecto desarrollado como parte de un curso universitario con el objetivo de:

✅ Demostrar integración de IA en aplicaciones móviles
✅ Aplicar patrones arquitectónicos modernos (MVVM)
✅ Integrar múltiples APIs de Google Cloud
✅ Crear una experiencia de usuario intuitiva
✅ Documentar el proceso de desarrollo

---

## 📊 Estado del Proyecto

| Aspecto | Estado |
|---------|--------|
| Compilación | ✅ Sin errores |
| Documentación | ✅ Completa |
| Funcionalidades Core | ✅ Implementadas |
| Integración IA | ✅ Funcional |
| Mapas | ✅ Integrados |
| Testing | 🔄 En progreso |
| Deployment | ⏳ Listo |

---

## 🚀 Próximas Mejoras

- [ ] Historial de diagnósticos más detallado
- [ ] Sincronización con médicos
- [ ] Integración con wearables
- [ ] Predicción de tendencias de salud
- [ ] Modo offline mejorado
- [ ] Multi-idioma

---

<div align="center">

**Desarrollado con ❤️ por Juan Carvajal, Karol Zapata y Naren Cipagauta**

Proyecto Universitario - 2025

</div>
