# build-installer.ps1 — Build fresh installer with latest JARs, Web, and Docs
$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$BuildDir    = Join-Path $ProjectRoot "build"
$PayloadApp  = Join-Path $ProjectRoot "Installer\Payload\app"
$NsisScript  = Join-Path $ProjectRoot "Installer\NSIS\installer.nsi"
$OutputDir   = Join-Path $ProjectRoot "Installer\Output"

Write-Host "============================================================"
Write-Host "Building Pixel Chaser Installer (PixelChaser_Setup.exe)"
Write-Host "============================================================"

# Step 1: Ensure project is built
if (-not (Test-Path (Join-Path $BuildDir "Launcher.jar"))) {
    Write-Host "[1/4] Build not found. Compiling project first..."
    & powershell -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot "build.ps1")
} else {
    Write-Host "[1/4] Project build artifacts verified."
}

# Step 2: Sync latest JARs and config into Payload
Write-Host "[2/4] Synchronizing latest JARs and configs into Payload..."
Copy-Item (Join-Path $BuildDir "Launcher.jar")    (Join-Path $PayloadApp "lib\Launcher.jar") -Force
Copy-Item (Join-Path $BuildDir "sqlite-jdbc.jar") (Join-Path $PayloadApp "lib\sqlite-jdbc.jar") -Force
Copy-Item (Join-Path $BuildDir "Helper.jar")      (Join-Path $PayloadApp "extras\Helper.jar") -Force
Copy-Item (Join-Path $BuildDir "PixelChaser.jar") (Join-Path $PayloadApp "game\PixelChaser.jar") -Force

if (Test-Path (Join-Path $BuildDir "app.properties")) {
    Copy-Item (Join-Path $BuildDir "app.properties") (Join-Path $PayloadApp "app.properties") -Force
}

# Step 3: Sync latest Docs and Web Portal
Write-Host "[3/4] Synchronizing Docs and Web Portal into Payload..."
Copy-Item (Join-Path $ProjectRoot "DEMO_SCRIPT.md")   (Join-Path $PayloadApp "docs\DEMO_SCRIPT.md") -Force
Copy-Item (Join-Path $ProjectRoot "README.md")        (Join-Path $PayloadApp "docs\README.md") -Force
Copy-Item (Join-Path $ProjectRoot "ARCHITECTURE.md")  (Join-Path $PayloadApp "docs\ARCHITECTURE.md") -Force

robocopy (Join-Path $ProjectRoot "Web_server\Recovery_Portal") (Join-Path $PayloadApp "web\Recovery_Portal") /E /NFL /NDL /NJH /NJS /NC /NS | Out-Null
robocopy (Join-Path $ProjectRoot "Web_server\Landing_Page")    (Join-Path $PayloadApp "web\Landing_Page")    /E /NFL /NDL /NJH /NJS /NC /NS | Out-Null
robocopy (Join-Path $ProjectRoot "Web_server\Email_Template") (Join-Path $PayloadApp "web\Email_Template") /E /NFL /NDL /NJH /NJS /NC /NS | Out-Null

# Ensure runtime directory exists and has bundled Java runtime
$RuntimeDir = Join-Path $ProjectRoot "Installer\Payload\runtime"
if (-not (Test-Path (Join-Path $RuntimeDir "bin\java.exe"))) {
    Write-Host "  [INFO] Bundling portable Java runtime via jlink..."
    $jdkPath = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
    if (Test-Path "$jdkPath\bin\jlink.exe") {
        $tempRuntime = "C:\Temp\pixel_runtime"
        if (-not (Test-Path "$tempRuntime\bin\java.exe")) {
            $modules = "java.base,java.desktop,java.sql,java.net.http,jdk.httpserver,java.naming,java.management"
            & "$jdkPath\bin\jlink.exe" --module-path "$jdkPath\jmods" --add-modules $modules --strip-debug --no-man-pages --no-header-files --output $tempRuntime
        }
        if (-not (Test-Path $RuntimeDir)) { New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null }
        robocopy $tempRuntime $RuntimeDir /E /NFL /NDL /NJH /NJS /NC /NS | Out-Null
        Write-Host "  [OK] Portable Java runtime bundled into Payload\runtime."
    }
} else {
    Write-Host "  [OK] Portable Java runtime already present in Payload\runtime."
}

# Step 4: Locate makensis and compile
Write-Host "[4/4] Compiling NSIS Installer..."

$Makensis = $null
$Candidates = @(
    "C:\Program Files (x86)\NSIS\makensis.exe",
    "C:\Program Files\NSIS\makensis.exe"
)
foreach ($c in $Candidates) {
    if (Test-Path $c) { $Makensis = $c; break }
}
if (-not $Makensis) {
    $cmd = Get-Command makensis -ErrorAction SilentlyContinue
    if ($cmd) { $Makensis = $cmd.Source }
}

if (-not $Makensis) {
    Write-Host "[ERROR] makensis.exe not found on this machine."
    Write-Host "Please install NSIS from https://nsis.sourceforge.io/ or add it to PATH."
    exit 1
}

Push-Location (Join-Path $ProjectRoot "Installer\NSIS")
try {
    & $Makensis $NsisScript
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] makensis compilation failed."
        exit 1
    }
} finally {
    Pop-Location
}

$SetupExe = Join-Path $OutputDir "PixelChaser_Setup.exe"
Write-Host ""
Write-Host "============================================================"
Write-Host "INSTALLER BUILD SUCCESSFUL!"
Write-Host "Output: $SetupExe"
if (Test-Path $SetupExe) {
    $fileInfo = Get-Item $SetupExe
    Write-Host "Size  : $([math]::Round($fileInfo.Length / 1MB, 2)) MB"
    Write-Host "Time  : $($fileInfo.LastWriteTime)"
}
Write-Host "============================================================"
