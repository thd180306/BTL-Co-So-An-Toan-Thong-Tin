# Pixel Chaser - Controlled Ransomware Simulator

University Capstone Project - Co so An toan Thong tin
Controlled lab demonstration of a ransomware attack-chain in a sandboxed environment.

## Important Notice

This is a controlled academic simulation, NOT real ransomware.

- All operations localhost-only (127.0.0.1)
- File processing restricted to Lab_sandbox only
- Mock payment only (no real money)
- No credential theft, no persistence, no system-wide encryption
- Baseline data preserved (no destructive behavior)

## Project Structure

    D:\Bai tap lon Co so an toan thong tin\
    |
    +-- Pixel Chaser\              Java Swing game (existing)
    |
    +-- Ransomware\                Java crypto + simulation core
    |   +-- src\ransomware\
    |   |   +-- Config.java        5 sandbox paths
    |   |   +-- CryptoUtils.java   AES-GCM + RSA-OAEP (original team code)
    |   |   +-- Ransomware.java    (original team code)
    |   |   +-- Decryption.java    (original team code)
    |   |   +-- sandbox\           Module 4 - Path validation
    |   |   +-- session\           Module 5 - SessionManager
    |   |   +-- crypto\            Module 6 - CryptoAdapter
    |   |   +-- controller\        Module 8 - HTTP orchestrator :5000
    |   |   +-- ui\                Module 9 - LockScreen (Swing)
    |   |   +-- recovery\          Module 10 - RestoreService
    |   |   +-- web\               Module 11 - RecoveryBackend :8080
    |   +-- lib\                   sqlite-jdbc-3.47.0.0.jar
    |   +-- sessions.db            SQLite database (owned by controller)
    |
    +-- Lab_sandbox\               Controlled data scope
    |   +-- baseline\              IMMUTABLE test files
    |   +-- working\               Per-session workspace
    |   +-- restored\              Decryption output
    |   +-- manifest\              SHA-256 hashes + RSA keys
    |
    +-- Web_Server\
    |   +-- Email_Template\        Phishing email demo
    |   +-- Landing_Page\          Fake indie dev page
    |   +-- Recovery_Portal\       HTML/JS recovery UI
    |
    +-- build\                     Compiled JARs
    |   +-- Ransomware.jar
    |   +-- WebServer.jar
    |   +-- PixelChaser.jar
    |   +-- sqlite-jdbc.jar
    |
    +-- build.ps1                  Build script
    +-- run-controller.bat         Start Demo Controller
    +-- run-web.bat                Start Recovery Backend
    +-- run-game.bat               Start game
    +-- launch-demo.bat            Start all
    +-- reset-demo.bat             Reset demo state

## Quick Start

### Prerequisites

- Windows 10/11 (64-bit)
- JDK 17+ (Java 21 recommended)
- PowerShell 5.1+

### 1. Download SQLite JDBC

Download sqlite-jdbc-3.47.0.0.jar from Maven Central:
    https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.47.0.0/

Place at: Ransomware\lib\sqlite-jdbc-3.47.0.0.jar

### 2. Build

    cd "D:\Bai tap lon Co so an toan thong tin"
    .\build.ps1

### 3. Copy JARs to ASCII path

Windows console cannot pass Unicode paths to java tools.
Workaround: copy JARs to ASCII-only path.

    $Ascii = "C:\PixelChaserDemo\build"
    New-Item -ItemType Directory -Force -Path $Ascii | Out-Null
    Copy-Item "build\*.jar" $Ascii -Force

### 4. Run Demo

Terminal 1 - Controller:

    cd C:\PixelChaserDemo\build
    java -cp "Ransomware.jar;sqlite-jdbc.jar" ransomware.controller.DemoController

Terminal 2 - Web Backend:

    cd C:\PixelChaserDemo\build
    java -cp "WebServer.jar;sqlite-jdbc.jar" ransomware.web.RecoveryBackend

Terminal 3 - Browser:

    Open http://127.0.0.1:8080/

Terminal 4 - Game:

    cd C:\PixelChaserDemo\build
    java -jar PixelChaser.jar

Then follow DEMO_SCRIPT.md

## Architecture Overview

PHASE 1 - Lure Delivery (Static Artifacts)
    Email -> Landing Page -> Installer download

PHASE 2 - Game Trap
    User plays Pixel Chaser -> closes game (X / Alt+F4)
    -> GameExitNotifier POSTs GAME_CLOSED to :5000

PHASE 3 - Demo Controller (orchestrator :5000)
    1. SessionManager.createSession()
    2. SandboxManager.prepareWorkspace()
    3. CryptoAdapter.encrypt() - AES-GCM + RSA-OAEP
    4. Store private key in manifest\keys\
    5. Launch LockScreen (Swing, fullscreen, red)
    6. Start 36-min countdown

PHASE 4 - Recovery Portal (:8080)
    Browser shows session -> DEMO PAYMENT -> Key release

See ARCHITECTURE.md for detailed diagrams.

## Security Boundary

Filesystem  - All I/O restricted to Lab_sandbox via SandboxGuard
Baseline    - baseline\ is read-only; never modified
Network     - Localhost only (127.0.0.1); no external calls
Payment     - Mock only; no real payment API
Crypto      - AES-256-GCM + RSA-4096-OAEP; existing team code
Persistence - No registry, no startup, no services

## Cryptographic Flow

Symmetric encryption  - AES/GCM/NoPadding           - 256-bit
IV                    - Random per file             - 12 bytes
GCM auth tag          - Built-in integrity          - 128-bit
Key wrapping          - RSA/ECB/OAEP-SHA256-MGF1    - 4096-bit
File integrity        - SHA-256                     - 256-bit

Envelope format per file:
    [4-byte keyLen][encrypted AES key][12-byte IV][AES-GCM ciphertext+tag]

## Session Lifecycle

    CREATED -> LOCKED -> PAYMENT_PENDING -> PAYMENT_CONFIRMED
            -> RECOVERY_AVAILABLE -> RESTORING -> VERIFYING -> COMPLETED

    Timeout: PAYMENT_PENDING -> EXPIRED -> DATA_UNAVAILABLE

Each game launch produces a unique instanceId, sessionId, recoveryId,
and recoveryKey.

## Documentation

- DEMO_SCRIPT.md   - Step-by-step presentation script
- ARCHITECTURE.md  - Detailed architecture + diagrams
- CONTRACTS.md     - API + state machine contracts
- INTEGRATION_ANALYSIS.md - Module 0 findings

## License & Use

Academic use only. Do not use outside the controlled lab environment.
