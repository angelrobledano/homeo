# Homeo

[English](README.md) | [Español](README.es.md)

[![Android](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2034)-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVI%20%2F%20Clean%20Architecture-blue.svg?style=flat)]()
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Local%20%2F%20Zero%20Cloud-00E676.svg?style=flat)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg?style=flat)](LICENSE)

> **Dynamic Physiological Homeostasis & Biometric Telemetry Cockpit**  
> An offline-first, scientific health and athletic performance app built for Android. Direct BLE connection to wearables (Amazfit GTR 3), Android Health Connect integration (Renpho scales), deterministic sports physiology algorithms (Gabbett ACWR, Banister TRIMP, MacroFactor-style EMA weight trend), and an OLED dark-mode design inspired by WHOOP.

---

## Table of Contents

- [Core Principles](#core-principles)
- [Key Features](#key-features)
- [Scientific & Physiological Foundation](#scientific--physiological-foundation)
- [Device Integration Matrix](#device-integration-matrix)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Project Structure](#project-structure)
- [Getting Started & Development](#getting-started--development)
- [Privacy & Security](#privacy--security)
- [Documentation Index](#documentation-index)
- [Contributing](#contributing)
- [License](#license)

---

## Core Principles

1. **Zero Fluff, Zero Pseudo-Science:** No motivational slogans, no ungrounded arbitrary health scores, and no clinical diagnoses masquerading as consumer data. Only deterministic calculations based on peer-reviewed sports physiology.
2. **Personal Baseline Over Population Averages:** Your biometric standards are measured against your own historical baseline (30-day resting heart rate, 28-day chronic workload, and exponential moving average weight).
3. **100% Offline-First & Private:** All data is processed and stored locally on the device in Room SQLite. Zero external cloud servers, zero analytics, zero trackers.
4. **Resilient Hardware Integrations:** Direct BLE GATT handshake using reverse-engineered protocol drivers (Huami 2021 ECDH/AES-128 cryptographic challenge-response) and local Android Health Connect synchronization.

---

## Key Features

- **Daily Biometric Cockpit (`DashboardScreen`):**
  - **Dynamic Recovery Ring:** Calculated from resting heart rate deviations against your 30-day baseline and sleep duration.
  - **Daily Strain Target:** Dynamic daily exertion target based on morning recovery score.
  - **Vitals Baseline Card:** Live evaluation of whether Resting Heart Rate, Sleep, and Weight fall within your individual baseline range.
  - **Biological Weight Card:** Filters scale calibration errors and accidental steps (< 35 kg or > 250 kg) to track true 7-day weight delta.
  - **Smartwatch Connection Status:** Real-time BLE connection state and battery telemetry for Amazfit GTR 3.

- **Athletic Load & Trends (`ProgressScreen`):**
  - **Temporal Granularity Selectors (7D / 30D / 90D):** Instantaneous comparison of training load (TRIMP), steps, sleep, and resting heart rate against the preceding identical window (7d vs 7d, 30d vs 30d, 90d vs 90d).
  - **Acute:Chronic Workload Ratio (ACWR):** Visual 4-zone gauge (<0.8 Under-training, 0.8–1.3 Sweet Spot, 1.3–1.5 Caution, >1.5 Danger Zone) based on Tim Gabbett's workload model.
  - **4-Week Workload History:** Bar chart illustrating chronic capacity vs. acute spikes.
  - **30-Day Resting Heart Rate Trend:** Moving point curve plotted against your personal physiological baseline.
  - **Trend Weight (EMA Filter):** Exponential Moving Average ($\alpha = 0.10$) isolating water retention, sodium, and glycogen shifts from actual adipose/lean mass changes.
  - **Habit Impact Matrix (Journal):** Statistical correlation analysis measuring how habits (e.g., late hydration, sauna, alcohol, magnesium) affect next-morning recovery and sleep debt.

- **Workout Analysis & GPS Tracking (`WorkoutScreen`):**
  - Real-time GPS outdoor activity tracking with local OpenStreetMap caching via osmdroid.
  - Manual workout logging with RPE (Borg CR10) load estimation.
  - Physiological heart rate zone distribution (Zone 1 Recovery to Zone 5 Anaerobic/Max).

- **Rule-Based Physiological Coach (`CoachScreen`):**
  - Deterministic recommendations for workout volume, sleep schedule, and recovery protocols.
  - Sleep debt calculator tracking accumulated sleep deficit over rolling 7-day windows.

- **Closed Period Summaries (`PeriodSummaryScreen`):**
  - Structured retrospectives across Daily, Weekly, Monthly, and Annual periods with closed balance sheets.

- **Local Data Portability & App Widget:**
  - One-tap encrypted/sanitized JSON export and import for offline backup.
  - Glanceable home screen widget (`HomeoAppWidget`) displaying recovery, strain, and steps.

---

## Scientific & Physiological Foundation

| Metric | Scientific Algorithm | Purpose |
| :--- | :--- | :--- |
| **ACWR** | Tim Gabbett's Acute:Chronic Workload Ratio | Compares 7-day fatigue against 28-day chronic fitness capacity to minimize injury risk. |
| **TRIMP** | Eric Banister's Training Impulse | Quantifies session exertion using duration, heart rate reserve, and exponential gender weighting. |
| **Trend Weight** | Exponential Moving Average ($\alpha = 0.10$) | Smooths osmotic water fluctuations (glycogen/sodium) to extract true biological tissue delta. |
| **RHR Baseline** | Rolling 30-day trimmed mean & standard deviation | Detects systemic fatigue, illness, or sympathetic nervous system stress. |
| **Sleep Debt** | Rolling 7-day sleep need deficit accumulation | Calculates circadian sleep pressure and ideal bedtime adjustments. |

---

## Device Integration Matrix

| Device / Source | Protocol / API | Telemetry Data | Sync Type | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Amazfit GTR 3** | BLE GATT (`0xFEE0`/`0xFEE1`), Huami 2021 ECDH B-163 + AES-128 | Heart rate, steps, distance, calories, battery. | Direct BLE (Local) | **Verified on Pixel 7a** |
| **Renpho Elis 1** | Android Health Connect (`androidx.health.connect:connect-client`) | Weight (`kg`), Body Fat (`%`). | On-demand / Startup | **Verified on Pixel 7a** |
| **GPS Tracking** | Android Location Services + osmdroid | Route coordinates, pace, elevation, distance. | Real-time foreground | **Verified on Pixel 7a** |

For deep integration notes, authentication flows, and BLE packet specs, see [docs/en/DEVICE_COMPATIBILITY.md](docs/en/DEVICE_COMPATIBILITY.md).

---

## Architecture & Tech Stack

Homeo is engineered as a modern, reactive, modular Android application:

- **Language:** Kotlin 2.0 with strict static typing and null safety.
- **UI Framework:** Jetpack Compose with Material 3 design tokens.
- **State Pattern:** MVI (Model-View-Intent) backed by Kotlin Coroutines and `StateFlow`.
- **Local Persistence:** Room SQLite 2.6 with indexed schemas and migrations.
- **Health Platform:** Android Health Connect Client API.
- **Styling:** Custom `WhoopColors` dark OLED palette (`#0A0B0D`) adhering to WCAG AA accessibility standards.

Detailed architectural diagrams and data flows are documented in [docs/en/ARCHITECTURE.md](docs/en/ARCHITECTURE.md).

---

## Project Structure

```text
app/src/main/java/com/salud/personal/
├── data/
│   ├── ble/            # BLE GATT manager, Huami cryptographic handshake & packet parsers
│   ├── healthconnect/  # Health Connect sync client & permissions manager
│   ├── local/          # Room Database, TypeConverters, DAOs & Entities
│   ├── repository/     # Repository layer bridging BLE, Room, and Health Connect
│   └── service/        # Android Foreground Service for GPS workout tracking
├── domain/
│   ├── coach/          # Deterministic heuristic engine for workout & recovery guidance
│   ├── habits/         # Habit-to-recovery correlation matrix calculator
│   ├── model/          # Clean domain models independent of database/hardware representations
│   ├── recovery/       # Multi-factor recovery score algorithm (RHR + Sleep + Subjective)
│   ├── sleep/          # Sleep debt and circadian timing engine
│   ├── strain/         # TRIMP and manual workout load estimator
│   ├── summary/        # Daily, weekly, monthly, and yearly retrospective balance generator
│   ├── trends/         # ACWR (Gabbett), EMA weight trend, and temporal comparisons
│   └── vitals/         # Personal biological baseline evaluation engine
└── ui/
    ├── activity/       # GPS tracking screen & map views
    ├── coach/          # Recommendations & recovery guidance UI
    ├── dashboard/      # Daily cockpit, rings, vital status & watch telemetry
    ├── easyui/         # Reusable design system primitives (Spotlight cards, tactile buttons)
    ├── navigation/     # Jetpack Navigation host & bottom navigation bar
    ├── recovery/       # Recovery score breakdown & morning check-in
    ├── settings/       # Device pairing, secrets configuration & data management
    ├── summary/        # Closed period retrospective views
    ├── theme/          # OLED color palette, typography & shapes
    ├── trends/         # Long-term progress, ACWR gauges, workload & weight charts
    ├── widget/         # Android Glance/AppWidget provider
    └── workout/        # Workout history, heart rate zones & manual entry dialog
```

---

## Getting Started & Development

### Prerequisites

- **Android Studio:** Hedgehog (2023.1.1) or Ladybug (2024.2+).
- **JDK:** OpenJDK 17 or higher.
- **Android SDK:** API 34 (Android 14) platform and build-tools installed.
- **Test Device:** Physical Android device with Bluetooth 5.0+ (Pixel 7a recommended) or Android 14 Emulator.

### Build and Run

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/homeo.git
   cd homeo
   ```
2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run complete test suite:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Install on connected device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

For detailed setup instructions, pairing credentials (`secrets.properties`), and debugging guides, consult [docs/en/DEVELOPMENT.md](docs/en/DEVELOPMENT.md).

---

## Privacy & Security

Health data is exceptionally sensitive. Homeo implements privacy as an architectural constraint:
- **Zero Third-Party Telemetry:** No Google Analytics, Firebase Analytics, Crashlytics, or remote logging.
- **Secrets Isolation:** BLE MAC addresses and authentication keys reside in local `secrets.properties` and private Room database storage, never checked into version control.
- **Export Control:** Users own 100% of their data through human-readable, sanitized JSON export.

For full disclosure on permissions and data retention, read [docs/en/PRIVACY.md](docs/en/PRIVACY.md).

---

## Documentation Index

| English Document | Spanish Document | Description |
| :--- | :--- | :--- |
| [ARCHITECTURE.md](docs/en/ARCHITECTURE.md) | [ARCHITECTURE.md](docs/es/ARCHITECTURE.md) | System layers, data flows, Room schema, and state management. |
| [DEVELOPMENT.md](docs/en/DEVELOPMENT.md) | [DEVELOPMENT.md](docs/es/DEVELOPMENT.md) | Environment setup, testing guidelines, and build commands. |
| [DEVICE_COMPATIBILITY.md](docs/en/DEVICE_COMPATIBILITY.md) | [DEVICE_COMPATIBILITY.md](docs/es/DEVICE_COMPATIBILITY.md) | Hardware matrix, BLE protocols, and Health Connect integration. |
| [PRIVACY.md](docs/en/PRIVACY.md) | [PRIVACY.md](docs/es/PRIVACY.md) | Data protection policy, permissions audit, and local storage. |
| [FAQ.md](docs/en/FAQ.md) | [FAQ.md](docs/es/FAQ.md) | Frequently asked questions, troubleshooting, and algorithms. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | [CONTRIBUTING.es.md](CONTRIBUTING.es.md) | Guidelines for contributing code and maintaining bilingual documentation. |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | [CODE_OF_CONDUCT.es.md](CODE_OF_CONDUCT.es.md) | Community standards and expectations. |
| [SECURITY.md](SECURITY.md) | [SECURITY.es.md](SECURITY.es.md) | Vulnerability disclosure and security architecture. |
| [ROADMAP.md](ROADMAP.md) | [ROADMAP.es.md](ROADMAP.es.md) | Development roadmap and upcoming milestones. |

---

## Contributing

We welcome contributions from international and Spanish-speaking developers alike! Issues and Pull Requests are accepted in both English and Spanish.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) (or [CONTRIBUTING.es.md](CONTRIBUTING.es.md)) before submitting code. **Note:** Any changes to documentation must be applied to both English and Spanish versions simultaneously.

---

## License

Homeo is released under the [Apache License 2.0](LICENSE).
