$kLine = Select-String -Path .\local.properties -Pattern '^GEMINI_API_KEY='
if ($kLine) { $k = $kLine.Line.Split('=')[1].Trim() } else { Write-Error 'No GEMINI_API_KEY found in local.properties'; exit 1 }

Write-Output "Usando key desde local.properties (oculta). Iniciando pruebas..." | Out-Host

function Try-Post($uri, $jsonBody) {
    try {
        # Intentar con Invoke-WebRequest para capturar status y body
        $resp = Invoke-WebRequest -Uri $uri -Method Post -Body $jsonBody -ContentType 'application/json' -UseBasicParsing -ErrorAction Stop
        return @{ success = $true; status = $resp.StatusCode; body = $resp.Content }
    } catch {
        $err = $_
        $status = $null
        $body = $null
        try {
            $response = $err.Exception.Response
            if ($response -ne $null) {
                $status = $response.StatusCode.Value__
                $stream = $response.GetResponseStream()
                if ($stream) { $body = (New-Object System.IO.StreamReader($stream)).ReadToEnd() }
            }
        } catch { }
        return @{ success = $false; status = $status; body = $body; message = $err.Exception.Message }
    }
}

try {
    # Listar modelos y extraer nombres que contengan 'gemini' o tomar primeros si falla
    Write-Output "Listando modelos disponibles..." | Out-Host
    $models = $null
    try { $models = Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1/models?key=$k" -Method Get -ErrorAction Stop } catch { Write-Output "No se pudo listar modelos: $($_.Exception.Message)" | Out-Host }
    $modelNames = @()
    if ($models -and $models.models) {
        foreach ($m in $models.models) { $modelNames += $m.name }
    }
    if ($modelNames.Count -eq 0) {
        # fallback común
        $modelNames = @('models/gemini-2.5-flash','models/gemini-2.5-pro','models/gemini-2.0-flash')
    }
    Write-Output "Modelos candidates: $($modelNames -join ', ')" | Out-Host

    # Endpoints a probar (varias variantes conocidas)
    $endpointSuffixes = @(':generateContent', ':generateText', ':predict')

    $promptText = 'Prueba breve en español: responde con DIAGNÓSTICO PRELIMINAR, POSIBLES CONDICIONES, URGENCIA, RECOMENDACIONES'

    # Payload variants to try (hash tables converted to JSON)
    $payloads = @()
    $payloads += (@{ prompt = @{ text = $promptText }; temperature = 0.2; maxOutputTokens = 200 } | ConvertTo-Json -Compress)
    $payloads += (@{ input = $promptText } | ConvertTo-Json -Compress)
    $payloads += (@{ instances = @(@{ input = $promptText }) } | ConvertTo-Json -Compress)
    $payloads += (@{ text = $promptText; temperature = 0.2 } | ConvertTo-Json -Compress)
    $payloads += (@{ prompt = $promptText } | ConvertTo-Json -Compress)
    $payloads += (@{ messages = @(@{ author = 'user'; content = @(@{ type = 'text'; text = $promptText }) }); temperature = 0.2 } | ConvertTo-Json -Compress)
    $payloads += (@{ prompt = @{ messages = @(@{ role = 'user'; content = $promptText }) } ; temperature = 0.2 } | ConvertTo-Json -Compress)

    # Variantes globales que aceptan el nombre del modelo en el body (algunas versiones usan esta forma)
    $globalEndpoints = @(
        "https://generativelanguage.googleapis.com/v1/models:generateText?key=$k",
        "https://generativelanguage.googleapis.com/v1/models:generateContent?key=$k",
        "https://generativelanguage.googleapis.com/v1beta2/models:predict?key=$k"
    )

    # Probar endpoints globales primero (con body que incluye el campo model)
    foreach ($guri in $globalEndpoints) {
        Write-Output "Probando endpoint global: $guri" | Out-Host
        # payloads que incluyen el campo model
        foreach ($model in $modelNames) {
            # normalizar modelId (sin el prefijo 'models/')
            $modelId = $model.Split('/')[-1]
            foreach ($pbTemplate in $payloads) {
                # Insertar model field en el JSON: convertir a objeto, agregar model, reconvertir
                try {
                    $obj = ConvertFrom-Json $pbTemplate -ErrorAction Stop
                    $obj | Add-Member -NotePropertyName model -NotePropertyValue $model -Force
                    $fullJson = $obj | ConvertTo-Json -Compress
                } catch {
                    # si falla la conversión, construir manual (usar modelId)
                    $fullJson = "{`"model`":`"$modelId`",`"input`":`"$promptText`"}"
                }
                Write-Output "- Probando (global) model=$model payload=$fullJson" | Out-Host
                $gres = Try-Post -uri $guri -jsonBody $fullJson
                if ($gres.success) { Write-Output "--> Éxito global: $guri model=$model status=$($gres.status)"; Write-Output $gres.body; $found = $true; break }
                else { Write-Output "--> Falló global: status=$($gres.status) message=$($gres.message)"; if ($gres.body) { Write-Output "---- Cuerpo: $($gres.body)" } }
            }
            if ($found) { break }
        }
        if ($found) { break }
    }
    if ($found) { Write-Output "Encontrado endpoint global válido; terminando pruebas." | Out-Host }

    $found = $false
    foreach ($model in $modelNames) {
        foreach ($suf in $endpointSuffixes) {
            $uri = "https://generativelanguage.googleapis.com/v1/$model$suf?key=$k"
            Write-Output "Probando endpoint: $uri" | Out-Host
            foreach ($pb in $payloads) {
                Write-Output "- Probando payload: $pb" | Out-Host
                $res = Try-Post -uri $uri -jsonBody $pb
                if ($res.success -eq $true) {
                    Write-Output "--> Éxito: status=$($res.status)" | Out-Host
                    Write-Output "Respuesta body: $($res.body)" | Out-Host
                    $found = $true
                    break
                } else {
                    Write-Output "--> Falló: status=$($res.status) message=$($res.message)" | Out-Host
                    if ($res.body) { Write-Output "---- Cuerpo: $($res.body)" | Out-Host }
                }
            }
            if ($found) { break }
        }
        if ($found) { break }
    }
    if (-not $found) { Write-Error "No se encontró un payload/endpoint exitoso con los modelos probados." }
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
