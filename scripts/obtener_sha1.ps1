#!/usr/bin/env powershell
# Script para obtener SHA-1 fingerprint para Google API

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SHA-1 Fingerprint Generator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Ruta del debug keystore
$keystorePath = "$env:USERPROFILE\.android\debug.keystore"

if (Test-Path $keystorePath) {
    Write-Host "✓ Keystore encontrado: $keystorePath" -ForegroundColor Green
    Write-Host ""
    Write-Host "Generando SHA-1 fingerprint..." -ForegroundColor Yellow
    Write-Host ""
    
    # Ejecutar keytool
    keytool -list -v -keystore $keystorePath -alias androiddebugkey -storepass android -keypass android | findstr "SHA1"
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ SHA-1 Fingerprint generado arriba" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Próximos pasos:" -ForegroundColor Cyan
    Write-Host "1. Copia el SHA-1 fingerprint de arriba"
    Write-Host "2. Ve a https://console.cloud.google.com/"
    Write-Host "3. Crea o selecciona tu proyecto"
    Write-Host "4. Ve a Credenciales > Crear credencial > API Key"
    Write-Host "5. Configura restricciones:"
    Write-Host "   - Tipo: Android apps"
    Write-Host "   - SHA-1: [pega el SHA-1 de arriba]"
    Write-Host "   - Package name: com.example.healthconnectai"
    Write-Host "6. Copia la API Key"
    Write-Host "7. Añade a local.properties:"
    Write-Host "   GOOGLE_PLACES_API_KEY=AIzaSy..."
    Write-Host ""
} else {
    Write-Host "✗ Keystore NO encontrado en: $keystorePath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Asegúrate de haber ejecutado la app en el emulador o dispositivo primero." -ForegroundColor Yellow
}

pause
