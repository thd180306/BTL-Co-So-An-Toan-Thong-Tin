# INTEGRATION ANALYSIS REPORT
> Generated: 2026-09-23 | Status: INSPECTION ONLY — No source files modified.

---

## 1. Existing Project Structure

```
D:\Bài tập lớn Cơ sở an toàn thông tin\
│
├── Pixel Chaser\
│   ├── jre\                          (bundled JRE — Windows DLLs present)
│   └── src\
│       └── JavaApplication1.java     (SINGLE FILE — 1066 lines, decompiled)
│
└── Ransomware\
    ├── Ransomware.iml                (IntelliJ module, inheritedJdk)
    ├── src\
    │   ├── META-INF\
    │   │   └── MANIFEST.MF           (Main-Class: ransomware.Ransomware)
    │   └── ransomware\
    │       ├── Config.java           (74 lines)
    │       ├── CryptoUtils.java      (192 lines)
    │       ├── Ransomware.java       (217 lines)
    │       └── Decryption.java       (75 lines)
    └── out\
        ├── artifacts\Ransomware_jar\Ransomware.jar
        └── production\Ransomware\ransomware\
            ├── Config.class
            ├── CryptoUtils.class
            ├── Decryption.class
            └── Ransomware.class
```

**PATH MISMATCH — REPORTED:**
- Task spec referenced `Pixel Chaser\src\javaapplication1\JavaApplication1.java`
- Actual path found: `Pixel Chaser\src\JavaApplication1.java`
- Package declaration in file: `package javaapplication1;`
- The source file sits directly in `src\` without a subdirectory matching the package name. This is consistent with a CFR decompile output (see line 2: `Decompiled with CFR 0.152.`).

---

## 2. Pixel Chaser Lifecycle

### Main Class
- **Class name:** `JavaApplication1` (package `javaapplication1`)
- **Extends:** `JPanel`
- **Implements:** `Runnable`, `KeyListener`, `MouseListener`, `MouseMotionListener`

### JFrame Creation
- **Method:** `main(String[] args)` — **line 974**
- **Exact sequence (lines 975–983):**
  ```java
  JFrame window = new JFrame("Pixel Chaser");   // line 975
  window.setDefaultCloseOperation(3);            // line 976
  window.setResizable(false);                    // line 977
  JavaApplication1 game = new JavaApplication1();// line 978
  window.add(game);                              // line 979
  window.pack();                                 // line 980
  window.setLocationRelativeTo(null);            // line 981
  window.setVisible(true);                       // line 982
  game.start();                                  // line 983
  ```

### setDefaultCloseOperation Value
- **Exact integer literal:** `3`
- **Constant name:** `JFrame.EXIT_ON_CLOSE` (value 3 per javax.swing.WindowConstants)
- **⚠ PROJECT_RULES violation:** `EXIT_ON_CLOSE` is explicitly forbidden in PROJECT_RULES.md §SAFETY BOUNDARY.

### All Exit Paths

| Path | Method | Line | Effect |
|------|--------|------|--------|
| **Menu → EXIT button (mouse click)** | `executeMenuAction(int index)` | **line 934** | `System.exit(0)` |
| **Menu → EXIT button (keyboard ENTER)** | `keyPressed(KeyEvent)` → `executeMenuAction(3)` | **line 853** dispatches to **line 934** | `System.exit(0)` |
| **Pause → EXIT button (mouse click)** | `executePauseAction(int index)` | **line 946** | `System.exit(0)` |
| **Pause → EXIT button (keyboard ENTER)** | `keyPressed(KeyEvent)` → `executePauseAction(2)` | **line 871** dispatches to **line 946** | `System.exit(0)` |
| **Window X button / Alt+F4** | No `WindowListener` registered. Falls through to `setDefaultCloseOperation(3)` = `EXIT_ON_CLOSE` | **line 976** | JVM-level `System.exit` via Swing EDT |

**WindowListener:** **NOT PRESENT.** No `addWindowListener(...)` call exists anywhere in the 1066-line file.

**Alt+F4 / windowClosing:** Handled implicitly by `EXIT_ON_CLOSE` (line 976). No custom `windowClosing` hook.

**Summary of exit signals emitted:** `System.exit(0)` — **hard kills the JVM**. No event, no callback, no hook is fired before termination. This is the primary integration blocker.

### Game Loop Thread
- **Line 122–126:** `start()` creates `new Thread(this)` (assigns to `this.gameThread`), sets `this.running = true`, calls `gameThread.start()`.
- **Line 217–233:** `run()` is the game loop — runs in a **separate daemon-style thread** (not explicitly marked daemon, but lifecycle tied to `running` flag).
- The game loop **does not terminate cleanly** — `running` is never set to `false` from any exit path; `System.exit(0)` kills everything.

### Build Method
- No `pom.xml`, `build.gradle`, or `build.xml` found in `Pixel Chaser\`.
- The single source file `src\JavaApplication1.java` is consistent with a **NetBeans / plain `javac`** project (CFR decompile note on line 1–3 suggests the `.java` file is itself a decompile artifact, not the original source).
- Build: **`javac` / IDE (likely NetBeans or IntelliJ)**.

### Java Version Compatibility
- Source uses `var` (line 126 in `Ransomware.java`, not Pixel Chaser) — not applicable here.
- Pixel Chaser source: No language features beyond Java 8 observed. Uses standard Swing, `java.awt`, `javax.sound.sampled`.
- A bundled `jre\` directory is present under `Pixel Chaser\`, suggesting the game was shipped with a specific JRE (likely Java 8 era based on JRE directory structure with `bin\WindowsAccessBridge-32.dll`).
- **Bytecode version marker:** `.class` files not inspected (decompile artifact). Bundled JRE is 32-bit (WindowsAccessBridge-**32**.dll).

---

## 3. Existing Ransomware Architecture

**Package:** `ransomware`  
**Build:** IntelliJ IDEA (`Ransomware.iml`, `out/` directory, pre-built `Ransomware.jar`)  
**JDK config:** `<orderEntry type="inheritedJdk" />` — JDK resolved from IntelliJ project SDK, **not hardcoded**.  
**Entry point (MANIFEST.MF):** `Main-Class: ransomware.Ransomware`

### main() Step Sequence — `Ransomware.java`

| Step | Method called | Line(s) | Description |
|------|--------------|---------|-------------|
| 0 | `checkSafety()` | 29 | Kill-switch check, target dir validation, root-path guard, interactive `YES` prompt |
| 1 | `initLog()` | 31 | Opens `FileWriter` to `Config.LOG_FILE` (append mode) |
| 2 | `CryptoUtils.generateRSAKeyPair()` | 41 | Generates RSA-4096 key pair |
| 3 | `CryptoUtils.savePublicKey()` | 48 | Saves `public.key` to `Config.KEY_DIR` |
| 4 | `CryptoUtils.savePrivateKey()` | 49 | Saves `private.key` to `Config.KEY_DIR` |
| 5 | *(conditional)* `File.delete()` | 60 | Deletes private key if `Config.DELETE_PRIVATE_KEY == true` (currently `false`) |
| 6 | `encryptRecursive(targetDir, pub)` | 70 | **Triggers all file encryption** (recursive walk of `Config.TARGET_DIR`) |
| 7 | `createRansomNote()` | 75 | Writes ransom note to `Config.RANSOM_NOTE` |
| 8 | `logWriter.close()` | 89 | Closes log file |

### Encryption Trigger
- **Method:** `encryptRecursive(File file, PublicKey pub)` — **line 152**
- Recursively walks `Config.TARGET_DIR`, skips files via `Config.shouldSkip()`, calls `CryptoUtils.encryptFile(file, pub)` per file — **line 159**.

### `checkSafety()` guards (lines 97–132)
1. Kill-switch file exists → abort (line 98–101)
2. Target dir doesn't exist or isn't directory → abort (line 103–107)
3. Target is root path (C:/, D:/, E:/, F:/, /) → abort (line 109–114)
4. Interactive console prompt: must type `YES` → abort if not (line 123–131)

---

## 4. Existing Crypto API

**File:** `CryptoUtils.java` (192 lines)  
**Class:** `public final class CryptoUtils` — utility class, private constructor, all static methods.

### Algorithms
| Layer | Algorithm | Details |
|-------|-----------|---------|
| Symmetric | **AES/GCM/NoPadding** | Key size: **256-bit** (`Config.AES_KEY_SIZE=256`), IV: **12 bytes** (`Config.IV_SIZE=12`), GCM tag: **128-bit** (`Config.TAG_BITS=128`) |
| Asymmetric | **RSA** | Key size: **4096-bit** (`Config.RSA_KEY_SIZE=4096`), Padding: **OAEP with SHA-256 and MGF1** (`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`) |

### Envelope Format (byte layout)
```
[ 4 bytes: int keyLen ][ keyLen bytes: RSA-encrypted AES key ][ 12 bytes: IV ][ N bytes: AES-GCM ciphertext+tag ]
```
- `encryptWithEnvelope()` builds via `ByteBuffer` (lines 126–132).
- `decryptWithEnvelope()` reads via `ByteBuffer.wrap()` (lines 141–158).

### All Public Methods

| Signature | Line | Description |
|-----------|------|-------------|
| `public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException` | 36 | Generates RSA-4096 key pair |
| `public static void savePublicKey(PublicKey key, String path) throws IOException` | 46 | Writes X.509 public key bytes to file |
| `public static void savePrivateKey(PrivateKey key, String path) throws IOException` | 54 | Writes PKCS8 private key bytes to file |
| `public static PublicKey loadPublicKey(String path) throws Exception` | 62 | Reads file → X509EncodedKeySpec → RSA PublicKey |
| `public static PrivateKey loadPrivateKey(String path) throws Exception` | 68 | Reads file → PKCS8EncodedKeySpec → RSA PrivateKey |
| `public static SecretKey generateAESKey() throws NoSuchAlgorithmException` | 78 | Generates AES-256 key |
| `public static byte[] encryptAESKeyWithRSA(SecretKey aesKey, PublicKey rsaPublic) throws GeneralSecurityException` | 88 | RSA-OAEP encrypts raw AES key bytes |
| `public static SecretKey decryptAESKeyWithRSA(byte[] encryptedAesKey, PrivateKey rsaPrivate) throws GeneralSecurityException` | 99 | RSA-OAEP decrypts → reconstructs AES SecretKey |
| `public static byte[] encryptWithEnvelope(byte[] data, PublicKey rsaPublic) throws GeneralSecurityException, NoSuchAlgorithmException` | 112 | Full envelope encrypt: plaintext bytes → envelope bytes |
| `public static byte[] decryptWithEnvelope(byte[] data, PrivateKey rsaPrivate) throws GeneralSecurityException` | 139 | Full envelope decrypt: envelope bytes → plaintext bytes |
| `public static void encryptFile(File input, PublicKey rsaPublic) throws IOException, GeneralSecurityException, NoSuchAlgorithmException` | 165 | Read file → encryptWithEnvelope → write `.locked` → delete original |
| `public static void decryptFile(File encrypted, PrivateKey rsaPrivate) throws IOException, GeneralSecurityException` | 180 | Read `.locked` → decryptWithEnvelope → write original → delete `.locked` |

---

## 5. Existing Decryption API

**File:** `Decryption.java` (75 lines)  
**Class:** `public class Decryption` — standalone `main()` entry point.

### main() Step Sequence

| Step | Line(s) | Description |
|------|---------|-------------|
| 1 | 26–30 | Checks `Config.KEY_DIR + "private.key"` exists; exits if not |
| 2 | 33 | `CryptoUtils.loadPrivateKey(privPath)` — loads RSA private key |
| 3 | 38–45 | `Files.walk(Config.TARGET_DIR)` — collects all `*.locked` files into a `List<File>` |
| 4 | 51–60 | Iterates list, calls `CryptoUtils.decryptFile(f, priv)` per file |
| 5 | 62–68 | Prints summary: files decrypted, errors |

### Decryption Trigger
- **Method:** `CryptoUtils.decryptFile(File encrypted, PrivateKey rsaPrivate)` — called at **line 53** of `Decryption.java`.

### Hash Verification
- **NOT PRESENT.** No checksum, SHA, MD5, or any hash verification exists in `Decryption.java`, `CryptoUtils.decryptFile()`, or `decryptWithEnvelope()`. AES-GCM authentication tag provides **implicit integrity** (tag mismatch throws `AEADBadTagException`) but there is no explicit file-level hash check.

---

## 6. Current File Deletion Behavior (with line numbers)

### `Files.delete()` call sites — EXACT LINE NUMBERS

| File | Line | Statement | Context |
|------|------|-----------|---------|
| `CryptoUtils.java` | **173** | `Files.delete(input.toPath());` | Inside `encryptFile()` — deletes the **original plaintext file** after writing the `.locked` version |
| `CryptoUtils.java` | **190** | `Files.delete(encrypted.toPath());` | Inside `decryptFile()` — deletes the **`.locked` ciphertext file** after writing the restored plaintext |

### Deletion Behavior Details

**`encryptFile()` (lines 165–174):**
1. Read all bytes from `input` → plaintext
2. `encryptWithEnvelope(data, rsaPublic)` → ciphertext
3. Write ciphertext to `input.getPath() + ".locked"`
4. **`Files.delete(input.toPath())`** — original is immediately deleted

**`decryptFile()` (lines 180–191):**
1. Read all bytes from `.locked` file → envelope bytes
2. `decryptWithEnvelope(data, rsaPrivate)` → plaintext
3. Write plaintext to path with `.locked` suffix stripped
4. **`Files.delete(encrypted.toPath())`** — `.locked` file is deleted after restore

**⚠ Integration Risk:** When adapting for sandbox use, `encryptFile()` **MUST NOT** delete originals from `baseline\` (immutable). The adapter must redirect `input` to `working\` copies only.

---

## 7. Current JDK Requirement

### Ransomware Module
- **`Ransomware.iml`:** `<orderEntry type="inheritedJdk" />` — JDK version inherited from IntelliJ project SDK. **Not hardcoded.**
- **Language feature evidence in source:**
  - `Ransomware.java` line 126: `var br = new java.io.BufferedReader(...)` — **`var` requires Java 10+**
  - `Ransomware.java` line 182–211: Text block (`"""..."""`) — **Text blocks require Java 15+**
  - Therefore minimum: **Java 15**; recommended per PROJECT_RULES: **JDK 26**
- **Pre-built artifact:** `out/production/Ransomware/*.class` — bytecode version not inspected (no `javap` run per inspection-only constraint), but compatible with existing compiled output.
- **External dependencies:** None. All crypto via `javax.crypto`, `java.security` (standard JDK).

### Pixel Chaser Module
- Bundled `jre\` present (32-bit JRE, Windows). Source uses no features beyond Java 8.
- File is noted as **decompiled** (CFR 0.152), so original bytecode version unknown.
- No `pom.xml`, `build.gradle`, or `.iml` found — build method unclear, likely plain `javac`.

---

## 8. Integration Points

### What exists that CAN be used directly (without rewriting)

| Component | Integration Surface | Notes |
|-----------|-------------------|-------|
| `CryptoUtils.encryptFile(File, PublicKey)` | **Primary adapter target** — wrap in sandbox path guard | All crypto logic intact |
| `CryptoUtils.decryptFile(File, PrivateKey)` | **Primary adapter target** — wrap in sandbox path guard | All decrypt logic intact |
| `CryptoUtils.encryptWithEnvelope(byte[], PublicKey)` | Lower-level, usable if adapter needs byte control | Avoids direct file I/O |
| `CryptoUtils.decryptWithEnvelope(byte[], PrivateKey)` | Lower-level, usable for restore with custom file handling | Avoids direct file I/O |
| `Config.shouldSkip(String fileName)` | Reusable as-is for exclusion logic | Public static method |
| `Ransomware.checkSafety()` | **NOT directly usable** — reads from hardcoded paths, requires console input | Must be bypassed/replaced by controller |

### Pixel Chaser → Controller Signal
- **Current state:** No event is emitted before `System.exit(0)`. The JVM terminates before any listener could act.
- **Potential hook points:**
  - `executeMenuAction(3)` at **line 933–935** — the only in-process point before `System.exit(0)` for Menu Exit
  - `executePauseAction(2)` at **line 945–947** — the only in-process point before `System.exit(0)` for Pause Exit
  - Window close via `setDefaultCloseOperation(EXIT_ON_CLOSE)` at **line 976** — no in-process hook at all

---

## 9. Missing Components

The following components **do not exist** in either codebase and must be built new:

| Missing Component | Required For |
|------------------|-------------|
| **`GameLifecycleHook`** | Intercepts exit before `System.exit()` fires; emits `GAME_CLOSED` signal |
| **`SandboxGuard`** | Validates all file paths stay inside `Lab_sandbox\working\`; rejects anything outside |
| **`CryptoAdapter`** | Wraps `CryptoUtils` methods with sandbox-safe path logic; copies `baseline\` → `working\` before encrypting |
| **`SessionManager`** | Manages session state machine (`CREATED→…→COMPLETED`); owns `sessions.db` |
| **`RansomwareController`** (Flask/Python or Java HTTP) | Listens on `127.0.0.1:5000`; receives `GAME_CLOSED`, triggers simulated encryption via adapter |
| **`Web_Server`** backend | Listens on `127.0.0.1:8080`; payment UI, recovery key validation; queries Controller for session state |
| **`Installer`** | Sets up `Lab_sandbox\baseline\` and `working\` structure; copies test data |
| **`recoveryId` / `recoveryKey` generator** | Produces `RCV-YYYYMMDD-XXXXXX` and `DEMO-XXXX-XXXX` identifiers |
| **Hash verification in Decryption** | Post-restore integrity check (currently absent) |
| **Ransom note with dynamic `recoveryId`** | Current note is static; needs `recoveryId` injection |

---

## 10. Risks / Incompatibilities

### CRITICAL

| Risk | Severity | Detail |
|------|----------|--------|
| **`EXIT_ON_CLOSE` on JFrame** | 🔴 CRITICAL | Line 976: `window.setDefaultCloseOperation(3)` violates PROJECT_RULES. Window X / Alt+F4 kills JVM immediately — no hook possible. |
| **`System.exit(0)` in game logic** | 🔴 CRITICAL | Lines 934, 946: Hard exit — no event emitted. Cannot intercept from outside without modifying the file. |
| **`Config.TARGET_DIR = "D:\\test\\ban-sao\\"` is outside sandbox** | 🔴 CRITICAL | Hardcoded to `D:\test\ban-sao\` — completely outside `Lab_sandbox\`. Would encrypt arbitrary system data. |
| **`Config.KEY_DIR = "D:\\keys\\"` is outside sandbox** | 🔴 CRITICAL | Hardcoded to `D:\keys\` — outside sandbox boundary. |
| **`Config.KILL_SWITCH = "D:\\STOP.txt"` is outside sandbox** | 🔴 HIGH | Outside sandbox boundary per PROJECT_RULES. |
| **`Config.LOG_FILE = "D:\\ransomware.log"` is outside sandbox** | 🟠 HIGH | Outside sandbox boundary. |
| **`Config.RANSOM_NOTE` is outside sandbox** | 🟠 HIGH | `D:\test\ban-sao\README_RANSOM.txt` — outside sandbox. |

### HIGH

| Risk | Severity | Detail |
|------|----------|--------|
| **No `GAME_CLOSED` event** | 🟠 HIGH | No event/callback/hook exists in Pixel Chaser to signal closure to external process. |
| **Interactive `YES` prompt in `checkSafety()`** | 🟠 HIGH | `Ransomware.main()` blocks on `System.in` — incompatible with controller-driven automation. |
| **`Files.delete()` deletes originals immediately** | 🟠 HIGH | No staging, no backup verification before delete. If adapter feeds wrong path, data is lost. |
| **Pixel Chaser package path mismatch** | 🟡 MEDIUM | Source is in `src\JavaApplication1.java` but declares `package javaapplication1;`. A proper build requires `src\javaapplication1\` subdirectory or classpath override. |
| **Pixel Chaser is a decompile** | 🟡 MEDIUM | `Decompiled with CFR 0.152` (line 2). Original source may differ. Modifying this file may introduce subtle bugs. |
| **`Config.DELETE_PRIVATE_KEY = false`** | 🟢 LOW | Currently safe — private key is preserved for lab use. |
| **No hash verification in Decryption** | 🟡 MEDIUM | AES-GCM tag provides byte-level integrity but no file-level verification after restore. |
| **32-bit bundled JRE in Pixel Chaser** | 🟡 MEDIUM | May conflict with 64-bit JDK 26 used for Ransomware module. |

---

## 11. Recommended Minimal Changes

> **Rule:** Only what is strictly necessary. Do NOT rewrite crypto. Do NOT rewrite Pixel Chaser gameplay.

### A. `Config.java` — Minimum 5 constant changes

All 5 path constants must be redirected into `Lab_sandbox\`. The smallest viable change:

```
TARGET_DIR  → Lab_sandbox\working\
KEY_DIR     → Lab_sandbox\manifest\keys\
KILL_SWITCH → Lab_sandbox\STOP.txt
LOG_FILE    → Lab_sandbox\manifest\ransomware.log
RANSOM_NOTE → Lab_sandbox\working\README_RANSOM.txt
```

These are **string literal** changes only — no logic changes in `Config.java`. Zero risk to crypto correctness.

### B. `Pixel Chaser\src\JavaApplication1.java` — Minimum 3 changes (or adapter pattern)

Option A — **Direct (smallest diff):**
1. **Line 976:** Change `window.setDefaultCloseOperation(3)` → `window.setDefaultCloseOperation(0)` (`DO_NOTHING_ON_CLOSE`)
2. **Line 975 area:** Add `window.addWindowListener(...)` with `windowClosing()` that signals the controller (e.g., HTTP GET to `127.0.0.1:5000/game-closed`) then calls `running = false` to stop game loop cleanly.
3. **Lines 934, 946:** Replace `System.exit(0)` with `running = false; window.dispose()` and emit the same signal.

Option B — **Shim process (zero source changes to Pixel Chaser):**
- Launch Pixel Chaser via `ProcessBuilder`, monitor for process exit via `Process.waitFor()`, then trigger controller. Simpler but less reliable (no distinction between normal exit vs. crash).

### C. New `CryptoAdapter` class — wraps CryptoUtils

```
Ransomware\src\ransomware\CryptoAdapter.java  [NEW]
```
- Takes `instanceId`, `sessionId` as constructor parameters.
- Validates all paths against `Lab_sandbox\working\` before delegating to `CryptoUtils.encryptFile()` / `decryptFile()`.
- Uses `Config` constants (after fix in §A) — no additional hardcoding.
- Reports file count back to `SessionManager`.

### D. No changes to CryptoUtils.java

The existing crypto API is **correct, complete, and wrappable**. Do not modify it.

### E. Build order for next modules

```
1. Fix Config.java paths       (prerequisite for everything)
2. Fix Pixel Chaser exit paths (prerequisite for event-driven flow)
3. Build CryptoAdapter         (prerequisite for Lab_sandbox encryption)
4. Build SessionManager + DB   (prerequisite for Web_Server)
5. Build RansomwareController  (5000) — integrates Adapter + SessionManager
6. Build Web_Server            (8080) — integrates SessionManager (read-only)
7. Build Installer             — sets up Lab_sandbox structure
```

---

*End of INTEGRATION_ANALYSIS.md — INSPECTION ONLY. No source files were modified.*
