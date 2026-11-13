# Arranque en Windows (PowerShell 5.1 o 7+). También funciona con PowerShell en macOS y Linux.
#
#   .\scripts\iniciar.ps1            base de datos + interfaz gráfica
#   .\scripts\iniciar.ps1 -Modo cli  base de datos + consola interactiva
#   .\scripts\iniciar.ps1 -Modo db   solo la base de datos (MySQL :3307 y phpMyAdmin :8090)
#   .\scripts\iniciar.ps1 -Modo parar
#   .\scripts\iniciar.ps1 -Modo check
param(
    [ValidateSet('gui', 'cli', 'db', 'parar', 'check')]
    [string]$Modo = 'gui'
)
# Continue: los comandos nativos (java, docker) escriben en stderr sin que sea un error;
# los fallos se detectan con $LASTEXITCODE y se informan con Fallo.
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location (Split-Path -Parent $PSScriptRoot)
$ComposeFile = 'docker/docker-compose.yml'

function Info($m)  { Write-Host "› $m" -ForegroundColor Cyan }
function Ok($m)    { Write-Host "✔ $m" -ForegroundColor Green }
function Fallo($m) { Write-Host "✘ $m" -ForegroundColor Red; exit 1 }

$EsWindows = [System.Environment]::OSVersion.Platform -eq 'Win32NT'
$Mvnw = if ($EsWindows) { '.\mvnw.cmd' } else { './mvnw' }

function Requisitos-Java {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Fallo 'No se encuentra Java. Instala el JDK 21 (ver docs/MULTIPLATAFORMA.md).'
    }
    $linea = (& java -version 2>&1 | Select-Object -First 1 | Out-String)
    $version = if ($linea -match '"(\d+)') { [int]$Matches[1] } else { 0 }
    if ($version -lt 21) { Fallo "Se necesita Java 21 o superior (encontrado: $version)." }
    Ok "Java $version"
}

# Función simple (sin param): en una avanzada, «-d» se tomaría como el parámetro común -Debug.
function Compose {
    $resto = $args
    & docker compose version *> $null
    if ($LASTEXITCODE -eq 0) { & docker compose -f $ComposeFile @resto }
    elseif (Get-Command docker-compose -ErrorAction SilentlyContinue) { & docker-compose -f $ComposeFile @resto }
    else { Fallo 'No se encuentra Docker Compose (ni "docker compose" ni "docker-compose").' }
    if ($LASTEXITCODE -ne 0) { Fallo 'Docker Compose devolvió un error.' }
}

function Requisitos-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Fallo 'No se encuentra Docker. Instala Docker Desktop.' }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) { Fallo 'Docker no está en marcha. Abre Docker Desktop y reintenta.' }
    Ok 'Docker en marcha'
}

function Base-De-Datos {
    Info 'Levantando MySQL y phpMyAdmin…'
    Compose up -d
    Info 'Esperando a que MySQL esté listo…'
    for ($i = 0; $i -lt 60; $i++) {
        $estado = (& docker inspect -f '{{.State.Health.Status}}' jardineria-mysql 2>$null)
        if ($estado -eq 'healthy') {
            Ok 'MySQL listo en localhost:3307 · phpMyAdmin en http://localhost:8090 (root / password)'
            return
        }
        Start-Sleep -Seconds 2
    }
    Fallo 'MySQL no respondió en 2 minutos. Revisa "docker logs jardineria-mysql".'
}

Info "Sistema: $([System.Runtime.InteropServices.RuntimeInformation]::OSDescription) ($([System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture))"
switch ($Modo) {
    'check' { Requisitos-Java; Requisitos-Docker; Ok 'Todo listo.' }
    'parar' { Requisitos-Docker; Compose stop; Ok 'Contenedores detenidos.' }
    'db'    { Requisitos-Docker; Base-De-Datos }
    'cli'   { Requisitos-Java; Requisitos-Docker; Base-De-Datos; & $Mvnw -q compile exec:java '-Dexec.args=--cli' }
    'gui'   {
        Requisitos-Java; Requisitos-Docker; Base-De-Datos
        Info 'Abriendo la interfaz gráfica (la primera vez Maven descarga dependencias)…'
        & $Mvnw -q javafx:run
    }
}
