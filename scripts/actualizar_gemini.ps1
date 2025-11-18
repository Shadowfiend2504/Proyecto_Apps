# Script para actualizar GeminiHealthClient.kt a Gemini 1.5 Pro

$filePath = "c:\GitHub\Android Studio\Proyecto_Apps\app\src\main\java\com\example\healthconnectai\data\ai\GeminiHealthClient.kt"

Write-Host "📝 Actualizando GeminiHealthClient.kt con 4 cambios restantes..." -ForegroundColor Cyan

# Cambio 2: callTextModel() - Actualizar URL y estructura del payload
$oldCallTextModel = @"
    // Llama al modelo de texto (sin imagen)
    private suspend fun callTextModel(prompt: String): String = withContext(Dispatchers.IO) {
        val url = "$'$'baseUrl/$'$'modelId:generateText?key=$'$'apiKey"
        // Construir payload con la forma esperada por la API (prompt.text)
        val json = com.google.gson.JsonObject()
        val promptObj = com.google.gson.JsonObject()
        promptObj.addProperty("text", prompt)
        json.add("prompt", promptObj)
        json.addProperty("temperature", 0.6)
        json.addProperty("maxOutputTokens", 800)
        val body = json.toString()
"@

$newCallTextModel = @"
    // Llama al modelo de texto (sin imagen)
    private suspend fun callTextModel(prompt: String): String = withContext(Dispatchers.IO) {
        val url = "$'$'baseUrl/$'$'modelId:generateContent?key=$'$'apiKey"
        // Construir payload para API v1 (estructura content + parts)
        val json = com.google.gson.JsonObject()
        val contentsArray = com.google.gson.JsonArray()
        val content = com.google.gson.JsonObject()
        val partsArray = com.google.gson.JsonArray()
        val part = com.google.gson.JsonObject()
        part.addProperty("text", prompt)
        partsArray.add(part)
        content.add("parts", partsArray)
        contentsArray.add(content)
        json.add("contents", contentsArray)
        json.addProperty("temperature", 0.7)
        json.addProperty("maxOutputTokens", 1024)
        val body = json.toString()
"@

$content = Get-Content $filePath -Raw

# Cambio 2
try {
    if ($content -match [regex]::Escape($oldCallTextModel)) {
        $content = $content -replace [regex]::Escape($oldCallTextModel), $newCallTextModel
        Write-Host "✅ Cambio 2: callTextModel() actualizado"
    } else {
        Write-Host "⚠️ Cambio 2: No se encontró el patrón exacto (método ya puede estar actualizado)"
    }
} catch {
    Write-Host "⚠️ Cambio 2: Error - $_"
}

# Cambio 3: Error 404 - Simplificar
$old404 = @"
                        404 -> {
                            // Intentar endpoint alternativo (v1) si usamos v1beta2
                            if (url.contains("v1beta2")) {
                                Log.w("GeminiHealthClient", "Modelo no encontrado en v1beta2, intentando v1")
                                val altUrl = url.replace("v1beta2", "v1")
                                val altResp = tryAlternateCall(altUrl, body)
                                return@withContext altResp
                            }
                            return@withContext "ERROR: Recurso no encontrado (404). Comprueba que el modelo 'text-bison-001' exista y que la API Generative Language esté habilitada en tu proyecto GCP. Detalle: $'$'detail"
                        }
"@

$new404 = @"
                        404 -> {
                            return@withContext "ERROR: Modelo no encontrado (404). Verifica que 'gemini-1.5-pro-latest' esté disponible en tu región y que hayas habilitado la API Generative Language. Detalle: $'$'detail"
                        }
"@

try {
    if ($content -match [regex]::Escape($old404)) {
        $content = $content -replace [regex]::Escape($old404), $new404
        Write-Host "✅ Cambio 3: Manejo de error 404 actualizado"
    } else {
        Write-Host "⚠️ Cambio 3: No se encontró el patrón exacto"
    }
} catch {
    Write-Host "⚠️ Cambio 3: Error - $_"
}

# Cambio 4: callTextModelWithImage()
$oldImage = @"
    // Llama al modelo incluyendo una imagen en base64 (se envía inline como campo adicional)
    private suspend fun callTextModelWithImage(prompt: String, base64Image: String): String = withContext(Dispatchers.IO) {
        val url = "$'$'baseUrl/$'$'modelId:generateText?key=$'$'apiKey"
        val json = com.google.gson.JsonObject()
        val promptObj = com.google.gson.JsonObject()
        promptObj.addProperty("text", prompt + "\n[ImageIncluded]")
        // Dependiendo de la API, las imágenes pueden requerir campos específicos; intentamos enviar inline
        json.add("prompt", promptObj)
        json.addProperty("image_base64", base64Image)
        json.addProperty("temperature", 0.6)
        json.addProperty("maxOutputTokens", 600)
"@

$newImage = @"
    // Llama al modelo incluyendo una imagen en base64
    private suspend fun callTextModelWithImage(prompt: String, base64Image: String): String = withContext(Dispatchers.IO) {
        val url = "$'$'baseUrl/$'$'modelId:generateContent?key=$'$'apiKey"
        val json = com.google.gson.JsonObject()
        val contentsArray = com.google.gson.JsonArray()
        val content = com.google.gson.JsonObject()
        val partsArray = com.google.gson.JsonArray()
        
        val textPart = com.google.gson.JsonObject()
        textPart.addProperty("text", prompt)
        partsArray.add(textPart)
        
        val imagePart = com.google.gson.JsonObject()
        val inlineData = com.google.gson.JsonObject()
        inlineData.addProperty("mimeType", "image/jpeg")
        inlineData.addProperty("data", base64Image)
        imagePart.add("inlineData", inlineData)
        partsArray.add(imagePart)
        
        content.add("parts", partsArray)
        contentsArray.add(content)
        json.add("contents", contentsArray)
        json.addProperty("temperature", 0.7)
        json.addProperty("maxOutputTokens", 800)
"@

try {
    if ($content -match [regex]::Escape($oldImage)) {
        $content = $content -replace [regex]::Escape($oldImage), $newImage
        Write-Host "✅ Cambio 4: callTextModelWithImage() actualizado"
    } else {
        Write-Host "⚠️ Cambio 4: No se encontró el patrón exacto"
    }
} catch {
    Write-Host "⚠️ Cambio 4: Error - $_"
}

# Cambio 5: jsonAsString()
$oldJsonAs = @"
    private fun jsonAsString(el: JsonElement): String? {
        // Intentos por distintas estructuras: "candidates" / "output" / "predictions"
        try {
            val obj = el.asJsonObject
            if (obj.has("candidates")) {
                val cand = obj.getAsJsonArray("candidates").get(0).asJsonObject
                if (cand.has("content")) return cand.getAsJsonArray("content").get(0).asJsonObject.get("text").asString
            }
            if (obj.has("predictions")) return obj.getAsJsonArray("predictions").get(0).asString
            if (obj.has("output")) return obj.get("output").asString
        } catch (_: Exception) {}
        return null
    }
"@

$newJsonAs = @"
    private fun jsonAsString(el: JsonElement): String? {
        // Estructura moderna de Gemini API v1: candidates[0].content.parts[0].text
        try {
            val obj = el.asJsonObject
            if (obj.has("candidates")) {
                val cand = obj.getAsJsonArray("candidates").get(0).asJsonObject
                if (cand.has("content")) {
                    val content = cand.getAsJsonObject("content")
                    if (content.has("parts")) {
                        val parts = content.getAsJsonArray("parts")
                        if (parts.size() > 0) {
                            val part = parts.get(0).asJsonObject
                            if (part.has("text")) return part.get("text").asString
                        }
                    }
                }
            }
            if (obj.has("predictions")) return obj.getAsJsonArray("predictions").get(0).asString
            if (obj.has("output")) return obj.get("output").asString
        } catch (_: Exception) {}
        return null
    }
"@

try {
    if ($content -match [regex]::Escape($oldJsonAs)) {
        $content = $content -replace [regex]::Escape($oldJsonAs), $newJsonAs
        Write-Host "✅ Cambio 5: jsonAsString() actualizado"
    } else {
        Write-Host "⚠️ Cambio 5: No se encontró el patrón exacto"
    }
} catch {
    Write-Host "⚠️ Cambio 5: Error - $_"
}

# Guardar los cambios
Set-Content $filePath $content

Write-Host ""
Write-Host "✅ Todos los cambios aplicados" -ForegroundColor Green
Write-Host ""
Write-Host "Próximo paso: Compilar el proyecto" -ForegroundColor Yellow
