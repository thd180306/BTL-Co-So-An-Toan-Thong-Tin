# ARCHITECTURE - Detailed Design

Module dependencies, contracts, and state machine.

## Module Dependency Graph

    +-----------------------------------------------------------+
    |  Pixel Chaser (Java Swing - existing game)                |
    |  - Game logic, UI, lifecycle events                       |
    |  - Emits GAME_CLOSED via GameExitNotifier                 |
    +-----------------------------+-----------------------------+
                                  |
                          POST /api/game/closed
                                  |
                                  v
    +-----------------------------------------------------------+
    |  Demo Controller :5000 (ransomware.controller)            |
    |  - Owns sessions.db                                       |
    |  - Orchestrates: SessionManager, SandboxManager,          |
    |    CryptoAdapter, LockScreen, CountdownService            |
    +-----------------------------+-----------------------------+
                                  |
             +--------------------+--------------------+
             |                    |                    |
             v                    v                    v
    +-----------------+  +-----------------+  +-----------------+
    | SessionManager  |  | SandboxManager  |  | CryptoAdapter   |
    | (session)       |  | (sandbox)       |  | (crypto)        |
    |                 |  |                 |  |                 |
    | Repositories:   |  | Path validation |  | Wraps existing  |
    | - sessions      |  | Workspace mgmt  |  | CryptoUtils:    |
    | - recovery_keys |  | Baseline copy   |  | - encrypt()     |
    | - file_manifest |  |                 |  | - decrypt()     |
    | - payments      |  | HashService     |  |                 |
    | - audit_events  |  | (SHA-256)       |  | (no rewrite)    |
    +-----------------+  +-----------------+  +-----------------+
                                  |
                                  v
    +-----------------------------------------------------------+
    |  RecoveryBackend :8080 (ransomware.web)                   |
    |  - HTTP proxy to :5000                                    |
    |  - Serves static Recovery Portal                          |
    +-----------------------------+-----------------------------+
                                  |
                                  v
    +-----------------------------------------------------------+
    |  Recovery Portal (HTML/CSS/JS)                            |
    |  - Session view                                           |
    |  - DEMO PAYMENT button                                    |
    +-----------------------------------------------------------+

## State Machine

    CREATED
       |
       | after encrypt
       v
    LOCKED
       |
       | LockScreen displayed
       v
    PAYMENT_PENDING
       |
       +-------+
       |       |
       |       | timeout
       v       v
    PAYMENT_   EXPIRED
    CONFIRMED     |
       |          v
       |      DATA_UNAVAILABLE
       |      (terminal)
       v
    RECOVERY_AVAILABLE
       |
       | user submits key
       v
    RESTORING
       |
       | decrypt done
       v
    VERIFYING
       |
       | hashes match
       v
    COMPLETED
    (terminal)

## API Contract

### Demo Controller (:5000)

POST   /api/game/closed       {game, instanceId, timestamp}
                              -> {success, recoveryId, status, paymentStatus}
GET    /api/session/latest    -> session or {found:false}
GET    /api/session/{rid}     -> session or 404
POST   /api/payment/demo      {recoveryId} -> {success, status, paymentStatus}
GET    /api/recovery/status/{rid} -> session

### Web Backend (:8080)

Proxy all /api/* to :5000. Plus:
GET    /              -> index.html
GET    /style.css     -> CSS
GET    /app.js        -> JS

## Database Schema

### sessions

    session_id      TEXT PK       SES-NNNNNN
    recovery_id     TEXT UNIQUE   RCV-YYYYMMDD-XXXXXX
    instance_id     TEXT          UUID v4 from game
    created_at      INTEGER       Unix epoch
    expires_at      INTEGER       Unix epoch
    status          TEXT          SessionStatus enum
    payment_status  TEXT          PaymentStatus enum

### recovery_keys

    recovery_id     TEXT PK       FK to sessions
    key_id          TEXT          UUID
    private_key_b64 TEXT          Base64-encoded PKCS8 RSA key
    status          TEXT          AVAILABLE / USED
    created_at      INTEGER       Unix epoch
    released_at     INTEGER       Null until used

### file_manifest

    id              INTEGER PK    Auto
    recovery_id     TEXT          FK to sessions
    original_name   TEXT          From baseline
    original_sha256 TEXT          SHA-256 hex
    current_name    TEXT          After encryption
    current_sha256  TEXT          Null until restore
    restore_status  TEXT          PENDING / VERIFIED / FAILED

### payment_events

    payment_id      TEXT PK       UUID
    recovery_id     TEXT          FK to sessions
    amount_demo     INTEGER       e.g. 100
    status          TEXT          PaymentStatus
    created_at      INTEGER       Unix epoch
    confirmed_at    INTEGER       Null until confirmed

### audit_events

    id              INTEGER PK    Auto
    timestamp       INTEGER       Unix epoch
    recovery_id     TEXT          Nullable
    event_type      TEXT          Canonical event
    status          TEXT          OK / ERROR / WARN
    metadata_json   TEXT          Extra data

## Crypto Flow - File by File

### Encrypt

    plaintext file
          |
          v
    +------------------+
    | AES-256 key gen  |  per file
    | IV-12 random     |
    +--------+---------+
             |
             v
    +------------------+
    | AES-GCM encrypt  |  -> ciphertext + 128-bit tag
    +--------+---------+
             |
             v
    +------------------+
    | RSA-OAEP-4096    |  wrap AES key
    | encrypt AES key  |
    +--------+---------+
             |
             v
    [4-byte len]
    [encrypted AES key]
    [12-byte IV]
    [ciphertext + tag]
             |
             v
    .locked file
    Delete original plaintext

### Decrypt

Reverse process. RSA-OAEP unwraps AES key, AES-GCM decrypts ciphertext,
GCM tag verifies integrity.

### Hash Verification

After decrypt:
- Compute SHA-256 of restored file
- Compare against file_manifest.original_sha256
- If match -> mark VERIFIED
- If mismatch -> RECOVERY_FAILED

## Safety Boundaries

### 1. Filesystem

    Path resolved = sandboxRoot.resolve(input).normalize().toAbsolutePath();
    if (!resolved.startsWith(sandboxRoot)) {
        throw new SecurityException("Path outside sandbox: " + resolved);
    }

### 2. Baseline Immutability

baseline is copied to working before encryption. Original never modified.

### 3. Config Hard-Coding

Config.TARGET_DIR hard-coded to Lab_sandbox\working\.
No way to redirect at runtime.

### 4. No Persistence

No registry writes, no startup folders, no services.
Only sessions.db (owned by controller).

### 5. No Network

All HTTP bound to 127.0.0.1. No external calls.

## Testing Strategy

    SandboxTest          8 tests   Module 4 - Sandbox guard
    CryptoAdapterTest   12 tests   Module 6 - Encrypt/decrypt
    RecoverySystemTest  10 tests   Module 10 - Restore + expiration
    ControllerSmokeTest  3 tests   Module 8 - HTTP endpoints
    Total               33 tests

All run without external dependencies.

## Design Decisions

### Why wrap CryptoUtils instead of rewriting?

- Preserve team knowledge - Crypto team algorithms are correct
- Separation of concerns - Controller doesn't know crypto details
- Testability - Adapter is mockable
- Safety - Adapter enforces sandbox boundaries

### Why Swing for LockScreen?

- Consistency - Pixel Chaser uses Swing
- Bundle size - JavaFX needs separate runtime
- Cross-version compatibility - Swing works in JDK 8-26

### Why SQLite?

- Zero config - Single file, no server
- ACID - Transaction-safe
- Java-friendly - sqlite-jdbc driver is lightweight
- Inspectable - Can open with any SQLite client

### Why no watchdog respawn?

- Safety - Hard to kill in lab
- Debugging - Easier to test without respawn
- Honesty - Demo doesn't need malware-grade persistence

## Known Limitations

1. Windows-only - Hard-coded D:\ paths in Config
2. Unicode path issue - JDK tools on Windows mangle Unicode paths;
   workaround = run from C:\PixelChaserDemo\
3. No real payment integration - Mock only
4. No TLS - HTTP only (localhost, acceptable for lab)
5. Single-session UI - Portal shows latest session only

## Future Work

- Multi-platform support (path abstraction)
- REST API authentication
- WebSocket for real-time updates
- Installer NSIS packaging
- Docker containerization
