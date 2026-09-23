# build.ps1 - ASCII-safe build (build to C:\Temp, then copy back)
$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$Ransomware  = Join-Path $ProjectRoot "Ransomware"
$Pixel       = Join-Path $ProjectRoot "Pixel Chaser"
$Lib         = Join-Path $Ransomware "lib"
$BuildFinal  = Join-Path $ProjectRoot "build"

# ASCII-only build workspace
$AsciiRoot = "C:\Temp\pixel_build"
$AsciiOut  = Join-Path $AsciiRoot "out"
$AsciiLib  = Join-Path $AsciiRoot "lib"
$AsciiRwSrc = Join-Path $AsciiRoot "src_ransomware"
$AsciiPxSrc = Join-Path $AsciiRoot "src_pixel"

$Javac = "javac"
$Jar   = "jar"

Write-Host "============================================================"
Write-Host "Building Pixel Chaser Demo (ASCII-safe)"
Write-Host "============================================================"

# Clean
if (Test-Path $AsciiRoot) { Remove-Item -Recurse -Force $AsciiRoot }
New-Item -ItemType Directory -Force -Path $AsciiOut | Out-Null
New-Item -ItemType Directory -Force -Path $AsciiLib | Out-Null

# Copy sources to ASCII paths
robocopy (Join-Path $Ransomware "src") $AsciiRwSrc /E /NFL /NDL /NJH /NJS /NC /NS | Out-Null
robocopy (Join-Path $Pixel "src") $AsciiPxSrc /E /NFL /NDL /NJH /NJS /NC /NS | Out-Null

# Copy sqlite jar to ASCII path
$SqliteSrc = Join-Path $Lib "sqlite-jdbc-3.47.0.0.jar"
if (-not (Test-Path $SqliteSrc)) {
    Write-Host "[ERROR] Missing: $SqliteSrc"
    exit 1
}
$SqliteAscii = Join-Path $AsciiLib "sqlite.jar"
Copy-Item $SqliteSrc $SqliteAscii -Force

# ============================================================
# 1. Ransomware
# ============================================================
Write-Host ""
Write-Host "[1/3] Building Ransomware..."

$RwSources = @(Get-ChildItem -Path $AsciiRwSrc -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
& $Javac -encoding UTF-8 -cp $SqliteAscii -d $AsciiOut $RwSources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Ransomware compile failed."
    exit 1
}
Write-Host "  Compile OK"

# ============================================================
# 2. JARs (manifest + jar from ASCII paths)
# ============================================================
$RwManifest = Join-Path $AsciiRoot "rw_manifest.txt"
Set-Content -Path $RwManifest -Value "Main-Class: ransomware.controller.DemoController"

$WebManifest = Join-Path $AsciiRoot "web_manifest.txt"
Set-Content -Path $WebManifest -Value "Main-Class: ransomware.web.RecoveryBackend"

if (Test-Path (Join-Path $AsciiRwSrc "icon.png")) {
    Copy-Item (Join-Path $AsciiRwSrc "icon.png") $AsciiOut -Force
}

Push-Location $AsciiOut
try {
    & $Jar cfm (Join-Path $AsciiRoot "Launcher.jar") $RwManifest .
    if ($LASTEXITCODE -ne 0) { Write-Host "[ERROR] Ransomware.jar failed"; exit 1 }
    Write-Host "  [OK] Ransomware.jar"

    & $Jar cfm (Join-Path $AsciiRoot "Helper.jar") $WebManifest .
    if ($LASTEXITCODE -ne 0) { Write-Host "[ERROR] WebServer.jar failed"; exit 1 }
    Write-Host "  [OK] WebServer.jar"
} finally {
    Pop-Location
}

# ============================================================
# 3. Pixel Chaser
# ============================================================
Write-Host ""
Write-Host "[2/3] Building PixelChaser..."

$PxOut = Join-Path $AsciiRoot "out_pixel"
New-Item -ItemType Directory -Force -Path $PxOut | Out-Null

$PxSources = @(Get-ChildItem -Path $AsciiPxSrc -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
& $Javac -encoding UTF-8 -d $PxOut $PxSources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Pixel Chaser compile failed."
    exit 1
}
Write-Host "  Compile OK"

if (Test-Path (Join-Path $AsciiPxSrc "icon.png")) {
    Copy-Item (Join-Path $AsciiPxSrc "icon.png") $PxOut -Force
}

$PxManifest = Join-Path $AsciiRoot "px_manifest.txt"
Set-Content -Path $PxManifest -Value "Main-Class: javaapplication1.JavaApplication1"

Push-Location $PxOut
try {
    & $Jar cfm (Join-Path $AsciiRoot "PixelChaser.jar") $PxManifest .
    if ($LASTEXITCODE -ne 0) { Write-Host "[ERROR] PixelChaser.jar failed"; exit 1 }
    Write-Host "  [OK] PixelChaser.jar"
} finally {
    Pop-Location
}

# ============================================================
# 4. Copy all jars back to project build\
# ============================================================
Write-Host ""
Write-Host "[3/3] Copying jars to project..."

if (Test-Path $BuildFinal) { Remove-Item -Recurse -Force $BuildFinal }
New-Item -ItemType Directory -Force -Path $BuildFinal | Out-Null

Copy-Item (Join-Path $AsciiRoot "Launcher.jar")    $BuildFinal -Force
Copy-Item (Join-Path $AsciiRoot "Helper.jar")      $BuildFinal -Force
Copy-Item (Join-Path $AsciiRoot "PixelChaser.jar") $BuildFinal -Force
Copy-Item $SqliteSrc (Join-Path $BuildFinal "sqlite-jdbc.jar") -Force

# Copy app.properties into build/ so AppConfig finds it next to the JARs
$AppProps = Join-Path $ProjectRoot "Installer\Payload\app\app.properties"
if (Test-Path $AppProps) {
    Copy-Item $AppProps $BuildFinal -Force
    Write-Host "  [OK] app.properties"
} else {
    Write-Host "  [WARN] app.properties not found at $AppProps - using built-in defaults"
}

Write-Host ""
Write-Host "============================================================"
Write-Host "BUILD COMPLETE"
Write-Host "============================================================"
Get-ChildItem "$BuildFinal\*" | Select-Object -ExpandProperty Name
Write-Host "============================================================"
