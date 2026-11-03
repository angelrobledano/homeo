# Development & Contribution Guide

[English](DEVELOPMENT.md) | [Español](../es/DEVELOPMENT.md)

This guide provides technical instructions for setting up the development environment, configuring local hardware credentials, executing automated tests, and building Homeo from source.

---

## 1. Prerequisites & Tooling

To build and run Homeo, your workstation requires:

- **Operating System:** Windows, macOS, or Linux.
- **JDK:** OpenJDK 17 (recommended: Eclipse Temurin or Android Studio bundled OpenJDK).
- **Android Studio:** Hedgehog (2023.1.1) or Ladybug (2024.2+).
- **Android SDK:**
  - `compileSdk = 34` (Android 14)
  - `minSdk = 26` (Android 8.0 Oreo)
  - `targetSdk = 34`
  - Build Tools: `34.0.0`
- **Physical Test Device:** Android 14 device with Bluetooth 5.0+ (Google Pixel 7a recommended).

---

## 2. Project Setup & Cloning

```bash
# Clone the repository
git clone https://github.com/your-username/homeo.git
cd homeo

# Verify Gradle wrapper integrity
./gradlew --version
```

---

## 3. Configuring Local Hardware Credentials (`secrets.properties`)

Direct Bluetooth LE connection to Amazfit GTR 3 smartwatches requires a unique 16-byte cryptographic authentication key paired to the device MAC address.

1. In the repository root, create a file named `secrets.properties` (this file is excluded from Git via `.gitignore`):
   ```properties
   # Amazfit GTR 3 Bluetooth MAC address
   WATCH_MAC=XX:XX:XX:XX:XX:XX

   # 16-byte (32-character hex) Auth Key extracted from Zepp / Gadgetbridge
   WATCH_AUTH_KEY=0123456789abcdef0123456789abcdef
   ```

2. If `secrets.properties` is omitted, the app will run in fallback simulation mode, allowing complete inspection of UI, GPS tracking, and Health Connect without a live watch connected.

---

## 4. Gradle Build & Test Commands

### Running Unit Tests

Homeo adheres to Test-Driven Development (TDD). The test suite exercises all domain models, Room DAO operations, BLE packet decoders, and ViewModels:

```bash
# Execute all local JVM unit tests
./gradlew testDebugUnitTest

# View HTML test report
# app/build/reports/tests/testDebugUnitTest/index.html
```

### Compiling Debug Binary

```bash
# Build the debug APK
./gradlew assembleDebug

# Output artifact location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Code Formatting & Static Analysis

```bash
# Run Android Lint
./gradlew lintDebug
```

---

## 5. Deploying to Physical Device (ADB)

```bash
# 1. Verify ADB connection
adb devices

# 2. Install the debug build with overwrite (-r)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Launch Homeo directly into MainActivity
adb shell am start -n com.salud.personal/.MainActivity

# 4. Stream application logcat output filtered by Homeo
adb logcat -v time | grep "com.salud.personal"
```

---

## 6. Development Workflow & TDD Guidelines

### Implementing New Domain Logic
1. Create unit tests first in `app/src/test/java/com/salud/personal/domain/`.
2. Implement deterministic business rules in `app/src/main/java/com/salud/personal/domain/`.
3. Verify all tests pass before wiring into ViewModels.

### Modifying Room Database Schemas
1. Add new fields or tables to `@Entity` classes under `data/local/entity/`.
2. Increment `databaseVersion` in `AppDatabase.kt`.
3. Provide an explicit `Migration(from, to)` to protect historical data integrity.
4. Add migration verification tests.

### Composable UI Guidelines
1. Adhere to **Rule 1**: Avoid redundant explanatory paragraphs or decorative filler copy.
2. Rely exclusively on tokens from `WhoopColors` and `MaterialTheme.typography`.
3. Ensure interactive elements maintain a minimum 48x48dp touch target.
4. Never hardcode user-facing strings in Composables; reference `stringResource(R.string.<id>)`.

---

## 7. Internationalization & Adding Translations (i18n)

Homeo is engineered as an open-source global project where adding a new language requires **zero Kotlin code modifications**:

1. **Default Language (English):** Located in `app/src/main/res/values/strings.xml`.
2. **Spanish Localization:** Located in `app/src/main/res/values-es/strings.xml`.
3. **To contribute a new language:**
   - Create a directory `app/src/main/res/values-<locale>/` (e.g., `values-fr/`, `values-de/`, `values-it/`, `values-pt/`, `values-ja/`).
   - Copy `app/src/main/res/values/strings.xml` into your directory.
   - Translate the string values within the `<string name="...">...</string>` tags.
   - Run `./gradlew assembleDebug` to verify XML validity.
   - Submit your Pull Request!
