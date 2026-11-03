# Amazfit GTR 3 BLE Integration & Synchronization Implementation Plan

Implement native Bluetooth Low Energy (BLE) integration for the Amazfit GTR 3 (Model A1971, Huami device code 227) into the `com.salud.personal` Android app. This enables 100% local cryptographic handshake (Huami 2021 ECDH B-163 + AES-128), real-time battery status reporting, heart rate reception, activity sync, and persistent local storage without cloud dependencies or telemetry.

---

## User Review Checkpoint
> [!NOTE]
> Device Identity Verified:
> - Model: Amazfit GTR 3 (A1971 / Huami 227)
> - Bluetooth MAC: Configurada y aislada en `secrets.properties`
> - Auth Key: Configurada y aislada en `secrets.properties`
> - Credentials securely isolated in `secrets.properties` (ignored in `.gitignore`).

---

## Proposed Changes & File Decomposition

### 1. Cryptography & BLE Protocol Core (`data/ble/huami/`)
- [`EcdhB163.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/ble/huami/crypto/EcdhB163.kt): Self-contained Kotlin implementation of NIST B-163 binary field elliptic-curve Diffie-Hellman key exchange (port of the field-tested Gadgetbridge B-163 engine).
- [`Huami2021AuthHandshake.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/ble/huami/auth/Huami2021AuthHandshake.kt): Pure state-machine for Zepp OS authentication handshake (generating initial public key command, processing challenge, generating double-AES response).
- [`Huami2021ChunkedCodec.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/ble/huami/codec/Huami2021ChunkedCodec.kt): Encoder and decoder for Zepp OS chunked BLE packets (`0x03` framing, CRC32, AES encryption with rolling session key).

### 2. Domain Models & Protocols (`domain/model/`)
- [`WatchBatteryStatus.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/domain/model/WatchBatteryStatus.kt): Battery percentage, charging status, timestamp.
- [`HeartRateSample.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/domain/model/HeartRateSample.kt): Timestamp, bpm, source device.
- [`DailyActivitySummary.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/domain/model/DailyActivitySummary.kt): Date, steps, distanceMeters, caloriesBurned.
- [`DeviceConnectionState.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/domain/model/DeviceConnectionState.kt): Disconnected, Scanning, Connecting, Authenticating, Connected, AuthFailed.

### 3. Local Persistence (`data/local/`)
- [`HeartRateSampleEntity.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/local/entity/HeartRateSampleEntity.kt): SQLite table with unique constraint on `(source_device, timestamp)`.
- [`HeartRateDao.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/local/dao/HeartRateDao.kt): Idempotent insert, latest sample flow, range query for training load.
- [`DailyActivityEntity.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/local/entity/DailyActivityEntity.kt): SQLite table for daily step aggregates.
- [`DailyActivityDao.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/local/dao/DailyActivityDao.kt): Daily step counts and calories.
- [`AppDatabase.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/local/AppDatabase.kt): Upgrade version from 1 to 2 with migration `MIGRATION_1_2`.

### 4. BLE Connection Manager (`data/ble/huami/`)
- [`AmazfitGtr3BleManager.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/ble/huami/AmazfitGtr3BleManager.kt): Manages `BluetoothGatt` lifecycle, MTU negotiation (request 512), service discovery, CCCD notifications on `00000017-...`, writing chunks to `00000016-...`, driving the auth handshake and emitting state flows.
- [`WatchRepository.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/data/repository/WatchRepository.kt): Repository coordinating BLE manager with Room database and exposing reactive domain flows.

### 5. UI Integration (`ui/dashboard/`)
- [`DashboardUiState.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/ui/dashboard/DashboardUiState.kt): Add `watchState: DeviceConnectionState`, `watchBattery: WatchBatteryStatus?`, `todaySteps: Int?`, `latestHeartRate: Int?`.
- [`DashboardScreen.kt`](file:///c:/Users/Angel/Documents/reloj/app/src/main/java/com/salud/personal/ui/dashboard/DashboardScreen.kt): Device status badge in top app bar / header, watch activity metric cards (sober, clean, no motivational filler text).
- Android Permissions in [`AndroidManifest.xml`](file:///c:/Users/Angel/Documents/reloj/app/src/main/AndroidManifest.xml): Ensure runtime permissions check for BLE on Android 12+ (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`).

---

## Bite-Sized Implementation Tasks

### Task 1: Elliptic Curve NIST B-163 & Auth Handshake Core (TDD)
- **Files:**
  - Create: `app/src/main/java/com/salud/personal/data/ble/huami/crypto/EcdhB163.kt`
  - Create: `app/src/main/java/com/salud/personal/data/ble/huami/auth/Huami2021AuthHandshake.kt`
  - Create: `app/src/test/java/com/salud/personal/data/ble/huami/crypto/EcdhB163Test.kt`
  - Create: `app/src/test/java/com/salud/personal/data/ble/huami/auth/Huami2021AuthHandshakeTest.kt`
- **Steps:**
  1. Write unit tests in `EcdhB163Test` verifying:
     - Public key derivation produces 48 bytes.
     - Key exchange between Alice and Bob produces the exact same 24-byte shared secret.
  2. Implement `EcdhB163.kt`.
  3. Write unit tests in `Huami2021AuthHandshakeTest`:
     - Test step 1: `buildInitCommand()` outputs 52 bytes (`[0x04, 0x02, 0x00, 0x02, <48 bytes pubKey>]`).
     - Test step 2: `handleServerChallenge()` with known test vector challenge + known 16-byte AuthKey generates valid 33-byte `[0x05, <16 bytes enc1>, <16 bytes enc2>]` and valid session key.
     - Test step 3: `handleServerResponse()` handles success (`[0x10, 0x05, 0x01]`) and failure (`[0x10, 0x05, 0x25]`).
  4. Implement `Huami2021AuthHandshake.kt`.
  5. Run tests: `.\gradlew.bat test --tests "com.salud.personal.data.ble.huami.*"`.
  6. Git commit: `feat(ble): implement ECDH B-163 and Huami 2021 auth handshake with TDD`.

### Task 2: Huami 2021 Chunked Packet Codec (TDD)
- **Files:**
  - Create: `app/src/main/java/com/salud/personal/data/ble/huami/codec/Huami2021ChunkedCodec.kt`
  - Create: `app/src/test/java/com/salud/personal/data/ble/huami/codec/Huami2021ChunkedCodecTest.kt`
- **Steps:**
  1. Write failing tests in `Huami2021ChunkedCodecTest`:
     - Encoding single-chunk payload (type `0x0082`, small data).
     - Encoding multi-chunk payload with MTU splitting.
     - Decoding single and multi-chunk packets into reassembled payloads.
     - CRC32 verification and payload integrity.
  2. Implement `Huami2021ChunkedCodec.kt` with `encode()` and `decode()`.
  3. Run tests: `.\gradlew.bat test --tests "com.salud.personal.data.ble.huami.codec.*"`.
  4. Git commit: `feat(ble): implement Huami 2021 chunked encoder and decoder with TDD`.

### Task 3: Room Database Persistence for Heart Rate & Daily Activity
- **Files:**
  - Create: `app/src/main/java/com/salud/personal/data/local/entity/HeartRateSampleEntity.kt`
  - Create: `app/src/main/java/com/salud/personal/data/local/dao/HeartRateDao.kt`
  - Create: `app/src/main/java/com/salud/personal/data/local/entity/DailyActivityEntity.kt`
  - Create: `app/src/main/java/com/salud/personal/data/local/dao/DailyActivityDao.kt`
  - Modify: `app/src/main/java/com/salud/personal/data/local/AppDatabase.kt`
  - Create: `app/src/test/java/com/salud/personal/data/local/HeartRateDaoTest.kt`
- **Steps:**
  1. Define entities and DAOs with indices on timestamp and source.
  2. Add `MIGRATION_1_2` to `AppDatabase.kt` and increment version to 2.
  3. Write Robolectric / unit tests verifying idempotent insertion and range queries.
  4. Run tests: `.\gradlew.bat test`.
  5. Git commit: `feat(db): add HeartRateSample and DailyActivity entities with Room migration`.

### Task 4: BLE Manager & Watch Repository
- **Files:**
  - Create: `app/src/main/java/com/salud/personal/data/ble/huami/AmazfitGtr3BleManager.kt`
  - Create: `app/src/main/java/com/salud/personal/data/repository/WatchRepository.kt`
  - Modify: `app/src/main/AndroidManifest.xml` (BLE permissions and features)
  - Create: `app/src/test/java/com/salud/personal/data/repository/WatchRepositoryTest.kt`
- **Steps:**
  1. Implement `WatchRepository` interface and implementation.
  2. Implement `AmazfitGtr3BleManager` with `BluetoothGattCallback` state-machine.
  3. Parse endpoint `0x0082` (Auth), endpoint `0x0010` / standard battery service `0x180F` (Battery), and endpoint `0x0007` (Steps).
  4. Write tests with mocked BLE manager verifying repository state propagation and persistence.
  5. Run tests: `.\gradlew.bat test`.
  6. Git commit: `feat(ble): add Amazfit GTR 3 BLE connection manager and repository`.

### Task 5: UI Integration & Physical Device Verification
- **Files:**
  - Modify: `app/src/main/java/com/salud/personal/ui/dashboard/DashboardUiState.kt`
  - Modify: `app/src/main/java/com/salud/personal/ui/dashboard/DashboardViewModel.kt`
  - Modify: `app/src/main/java/com/salud/personal/ui/dashboard/DashboardScreen.kt`
  - Modify: `app/src/main/java/com/salud/personal/MainActivity.kt`
  - Create: `app/src/test/java/com/salud/personal/ui/dashboard/DashboardViewModelTest.kt`
- **Steps:**
  1. Update `DashboardUiState` with watch connection and battery state.
  2. Update `DashboardViewModel` to combine weight and watch flows.
  3. Design sober Compose components for Amazfit GTR 3 status following `AGENTS.md` (no motivational text, high visual craft, crisp numbers and badges).
  4. Build APK: `.\gradlew.bat assembleDebug`.
  5. Install and launch on connected Google Pixel 7a via ADB.
  6. Verify UI and logcat output on physical device.
  7. Git commit: `feat(ui): display Amazfit GTR 3 connection, battery, and activity on dashboard`.

---

## Verification Plan
1. All automated tests pass: `.\gradlew.bat test`.
2. Static analysis / build pass: `.\gradlew.bat assembleDebug`.
3. Installation on user's Pixel 7a: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
4. Verification of BLE scan and connection handshake with Amazfit GTR 3 (configured via secrets.properties).
