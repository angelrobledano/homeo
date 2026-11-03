# Device Compatibility & Hardware Integration Matrix

[English](DEVICE_COMPATIBILITY.md) | [Español](../es/DEVICE_COMPATIBILITY.md)

This document provides technical documentation on supported hardware devices, communication protocols, authentication handshakes, and data ingestion methods in Homeo.

---

## Hardware Compatibility Matrix

| Manufacturer & Model | Source / Integration | Data Types | Direction | Permissions & Auth | Network Required | Frequency & Limits | Deduplication Key | Verified Constraints | Verification Date | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Amazfit GTR 3** (Model: A1971 / Huami 227) | BLE GATT (Zepp OS 2021 Protocol / `0xFEE0` & `0xFEE1`) | Live Heart Rate, Steps, Distance, Calories, Battery Status. | Bidirectional (Authenticated handshake & telemetry streaming) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`. 16-byte Auth Key (AES-128 + ECDH B-163). | **No** (100% offline, local Bluetooth) | Continuous / Reactive BLE connection. | Primary key / Unique index: `(source_device, timestamp)`. | Requires Auth Key extracted from Zepp / Gadgetbridge. Keys isolated in `secrets.properties`. | 2026-10-27 (Physical Pixel 7a) | **Implemented & Verified** |
| **RENPHO Elis 1** (Model: ES-CS20M / QN-Scale) | Android Health Connect (`androidx.health.connect:connect-client`) | Weight (`kg`), Body Fat (`%`). | Unidirectional (Health Connect -> Room) | `android.permission.health.READ_WEIGHT`, `android.permission.health.READ_BODY_FAT` | **No** (100% local Android API) | On-demand at app launch or sync button. | 3-minute timestamp window matching existing `WeightMeasurementEntity`. | Requires Android 14+ or Health Connect app installed. Body fat percentage is bioelectrical estimation. | 2026-11-02 (Physical Pixel 7a, 7 records imported) | **Implemented & Verified** |
| **Outdoor GPS Tracker** | Android Location Services (`ACCESS_FINE_LOCATION`) | GPS Coordinates, Altitude, Instant Pace, Trajectory. | Real-time sensor stream | `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE_LOCATION` | **No** (Offline cached OpenStreetMap tiles) | 1-second location updates during active workouts. | Continuous session segment list in `WorkoutSessionEntity`. | GPS accuracy threshold filtering (< 15 meters horizontal accuracy required). | 2026-11-03 (Physical Pixel 7a) | **Implemented & Verified** |
| **RENPHO Elis 1 (Direct BLE)** | BLE Broadcast (QN-Scale Protocol) | Raw Weight, Electrical Impedance. | Unidirectional Broadcast (Scale -> App) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`. No pairing key needed. | **No** (100% local BLE broadcast) | On-demand when stepping onto the scale. | `(source_device, timestamp_window_5s, weight)` | Bypasses Health Connect; planned for Phase 5. | 2026-10-27 (openScale reference driver) | **Viable / Planned** |

---

## Technical Protocol Specifications

### 1. Amazfit GTR 3 (Zepp OS / Huami 2021)

#### GATT Service & Characteristic UUIDs
- **Authentication Service (`0xFEE1`):**
  - Characteristic `00000009-0000-1000-8000-00805f9b34fb`: Auth control point for cryptographic challenge-response.
- **Telemetry Service (`0xFEE0`):**
  - Characteristic `00000007-0000-1000-8000-00805f9b34fb`: Real-time sensor and step streaming.
  - Characteristic `00000006-0000-1000-8000-00805f9b34fb`: Battery status notifications.
- **Heart Rate Service (`0x180D`):**
  - Characteristic `0x2A37`: Standard Heart Rate Measurement notification.

#### Cryptographic Handshake Sequence
1. **Challenge Request:** Homeo writes `[0x02, 0x00]` to the Auth control point.
2. **Challenge Response:** The watch returns a 16-byte random salt: `[0x10, 0x02, 0x01, ...16 bytes random salt...]`.
3. **Key Derivation & Encryption:**
   - Ephemeral ECDH keypair is computed over NIST Curve B-163.
   - The 16-byte random challenge is encrypted using AES-128 (ECB mode, zero padding) with the secret 16-byte `WATCH_AUTH_KEY`.
4. **Authorization Verification:** Homeo writes `[0x03, 0x00, ...16 bytes encrypted payload...]`.
5. **Session Established:** The watch validates the signature and emits `[0x10, 0x03, 0x01]`, transitioning connection state to `CONNECTED`.

---

### 2. Renpho Elis 1 & Android Health Connect

#### Integration Architecture
- Uses the official Android Jetpack Health Connect SDK (`androidx.health.connect:connect-client:1.1.0-alpha11`).
- Queries `WeightRecord` and `BodyFatRecord` instances populated by the manufacturer companion app.
- Ingestion filter automatically discards abnormal readings (< 35 kg or > 250 kg) to prevent accidental steps, pet steps, or tare calibration errors from corrupting baseline trends.

#### Deduplication Window
- Health Connect records within 3 minutes of an existing local Room record are evaluated: if weight matches within $\pm 0.05$ kg, duplicate insertion is suppressed.

---

### 3. OpenStreetMap GPS Workout Engine

- **Service Lifecycle:** Managed by `WalkTrackingService` as a persistent Android Foreground Service with type `location`.
- **Map Renderer:** osmdroid 6.1.18 rendering pre-cached vector tiles without requiring proprietary Google Play Services API keys.
- **Filtering:** Raw NMEA/GPS fixes with horizontal accuracy dilution $> 20$ meters or anomalous speeds $> 45$ km/h (for foot activities) are rejected to eliminate GPS drift spikes.
