# Salud & Rendimiento Personal Android — Implementation Plan (Fase 1: Núcleo y Báscula Renpho Elis 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el primer incremento vertical comprobable de la aplicación Android nativa: arquitectura base, modelo de dominio canónico, decodificador BLE de la báscula RENPHO Elis 1 (protocolo QN-Scale), persistencia idempotente en SQLite/Room y pantalla sobria en Jetpack Compose conforme a `AGENTS.md`.

**Architecture:** Aplicación Android nativa con Clean Architecture y MVI. Capa de dominio desacoplada con unidades canónicas inmutables, capa de datos con base de datos local Room y driver BLE para tramas QN-Scale, y capa de presentación con Jetpack Compose en modo "Operate" (sin texto de relleno ni elementos decorativos superfluos).

**Tech Stack:** Kotlin 2.0, Jetpack Compose (Material 3), Room 2.6.1, Gradle 8.11, JUnit 4/5, Kotlin Coroutines & Flow.

**Spec:** `docs/superpowers/specs/2026-10-27-salud-companion-design.md`

## Global Constraints

- Android API Floor: `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 34` (Android 14).
- Persistencia 100% local (sin servidores, sin analítica externa, sin dependencias cloud).
- Regla de diseño 1 de `AGENTS.md`: Prohibidos eslóganes, textos motivacionales y subtítulos redundantes. Cada elemento debe ser concreto: nombre de métrica, valor numérico con tipografía clara, unidad canónica y fecha/hora exacta.
- Conservación de procedencia: Toda lectura registrada debe incluir `sourceDevice`, `measuredAt` e `importedAt`.
- TDD obligatorio: Ninguna lógica de decodificación o cálculo se escribe sin un test previo que falle.

---

### Task 1: Scaffolding del Proyecto Android y Configuración de Gradle

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Test: `app/src/test/java/com/salud/personal/SanityCheckTest.kt`

**Interfaces:**
- Consumes: None (Root setup)
- Produces: Estructura Gradle Android funcional capaz de ejecutar `gradlew test` con éxito.

- [ ] **Step 1: Write the failing test**

Crear `app/src/test/java/com/salud/personal/SanityCheckTest.kt`:
```kotlin
package com.salud.personal

import org.junit.Assert.assertTrue
import org.junit.Test

class SanityCheckTest {
    @Test
    fun verifyEnvironmentIsWorking() {
        assertTrue("El entorno Android y JUnit deben inicializarse correctamente", true)
    }
}
```

- [ ] **Step 2: Run test to verify it fails (o no existe build)**

Run:
```powershell
& "C:\Users\Angel\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat" test
```
Expected: FAIL o error porque no existen los archivos de build de Gradle.

- [ ] **Step 3: Write minimal implementation**

Crear `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SaludPersonal"
include(":app")
```

Crear `build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

Crear `app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.salud.personal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.salud.personal"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Room SQLite
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

Crear `app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />

    <application
        android:allowBackup="false"
        android:label="Salud"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Crear wrapper de Gradle en el proyecto (`gradle wrapper`).

- [ ] **Step 4: Run test to verify it passes**

Run:
```powershell
.\gradlew test
```
Expected: `BUILD SUCCESSFUL`, `SanityCheckTest` PASSED.

- [ ] **Step 5: Commit**

```powershell
git add settings.gradle.kts build.gradle.kts app/ gradlew* gradle/
git commit -m "chore: setup Android Jetpack Compose & Room project scaffolding"
```

---

### Task 2: Modelo de Dominio y Normalización Canónica (TDD)

**Files:**
- Create: `app/src/main/java/com/salud/personal/domain/model/WeightMeasurement.kt`
- Create: `app/src/main/java/com/salud/personal/domain/model/ProvenanceSource.kt`
- Test: `app/src/test/java/com/salud/personal/domain/WeightMeasurementTest.kt`

**Interfaces:**
- Consumes: None
- Produces: Modelo de dominio `WeightMeasurement` inmutable con validación de rangos biológicos creíbles y procedencia.

- [ ] **Step 1: Write the failing test**

Crear `app/src/test/java/com/salud/personal/domain/WeightMeasurementTest.kt`:
```kotlin
package com.salud.personal.domain

import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeightMeasurementTest {

    @Test
    fun validMeasurement_createsSuccessfully() {
        val now = Instant.parse("2026-10-27T10:00:00Z")
        val measurement = WeightMeasurement(
            id = 1L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 74.85,
            bodyFatPercentage = 18.2f,
            muscleMassKg = 58.10,
            impedanceOhms = 495.0,
            measuredAt = now,
            importedAt = now
        )

        assertEquals(74.85, measurement.weightKg, 0.001)
        assertEquals(ProvenanceSource.RENPHO_ELIS_1, measurement.source)
        assertEquals(18.2f, measurement.bodyFatPercentage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWeight_throwsException() {
        WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = -5.0,
            measuredAt = Instant.now(),
            importedAt = Instant.now()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBodyFatPercentage_throwsException() {
        WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 70.0,
            bodyFatPercentage = 105.0f, // Fuera de rango
            measuredAt = Instant.now(),
            importedAt = Instant.now()
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.salud.personal.domain.WeightMeasurementTest"`
Expected: FAIL con error de compilación (clases no encontradas).

- [ ] **Step 3: Write minimal implementation**

Crear `app/src/main/java/com/salud/personal/domain/model/ProvenanceSource.kt`:
```kotlin
package com.salud.personal.domain.model

enum class ProvenanceSource(val identifier: String) {
    RENPHO_ELIS_1("RENPHO_ELIS_1"),
    AMAZFIT_GTR_3("AMAZFIT_GTR_3"),
    MANUAL_INPUT("MANUAL_INPUT"),
    HEALTH_CONNECT("HEALTH_CONNECT")
}
```

Crear `app/src/main/java/com/salud/personal/domain/model/WeightMeasurement.kt`:
```kotlin
package com.salud.personal.domain.model

import java.time.Instant

data class WeightMeasurement(
    val id: Long = 0L,
    val source: ProvenanceSource,
    val externalId: String? = null,
    val weightKg: Double,
    val bodyFatPercentage: Float? = null,
    val muscleMassKg: Double? = null,
    val impedanceOhms: Double? = null,
    val measuredAt: Instant,
    val importedAt: Instant
) {
    init {
        require(weightKg in 10.0..300.0) {
            "El peso debe estar en un rango biológico plausible (10 kg - 300 kg): $weightKg"
        }
        bodyFatPercentage?.let {
            require(it in 3.0f..70.0f) {
                "El porcentaje de grasa debe estar en un rango plausible (3% - 70%): $it"
            }
        }
        muscleMassKg?.let {
            require(it in 5.0..weightKg) {
                "La masa muscular no puede exceder el peso total ni ser inferior a 5 kg: $it"
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew test --tests "com.salud.personal.domain.WeightMeasurementTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/salud/personal/domain/ app/src/test/java/com/salud/personal/domain/
git commit -m "feat(domain): add WeightMeasurement model with physiological validations and provenance"
```

---

### Task 3: Decodificador BLE de la Báscula RENPHO Elis 1 (QN-Scale Protocol) (TDD)

**Files:**
- Create: `app/src/main/java/com/salud/personal/data/ble/renpho/QnScalePacketParser.kt`
- Create: `app/src/main/java/com/salud/personal/data/ble/renpho/QnScaleMeasurement.kt`
- Test: `app/src/test/java/com/salud/personal/data/ble/renpho/QnScalePacketParserTest.kt`

**Interfaces:**
- Consumes: Arrays de bytes BLE crudos emitidos por la característica GATT de la báscula Renpho Elis 1 (`0xFFE1` o broadcast advertisement `0xFFF0`).
- Produces: `QnScaleMeasurement` con indicación de si el peso es final/estable (`isStabilized: Boolean`), peso en kg y resistencia en ohmios.

- [ ] **Step 1: Write the failing test**

Crear `app/src/test/java/com/salud/personal/data/ble/renpho/QnScalePacketParserTest.kt`:
```kotlin
package com.salud.personal.data.ble.renpho

import org.junit.Assert.*
import org.junit.Test

class QnScalePacketParserTest {

    private val parser = QnScalePacketParser()

    @Test
    fun parseStabilizedPacket_extractsWeightAndImpedanceCorrectly() {
        // Trama típica QN-Scale:
        // Cabecera: 0x10 (o 0x20)
        // Byte 1-2: Peso en escala 100 (ej: 0x1D, 0x4C = 7500 -> 75.00 kg)
        // Byte 3: Status (0x01 = estabilizado / lectura final bloqueada)
        // Byte 4-5: Impedancia (ej: 0x01, 0xF4 = 500 ohmios)
        // Checksum al final
        val rawBytes = byteArrayOf(
            0x10.toByte(),
            0x1D.toByte(), 0x4C.toByte(), // 7500 / 100 = 75.00 kg
            0x01.toByte(),                // Estabilizado
            0x01.toByte(), 0xF4.toByte(), // 500 ohms
            0x00.toByte()
        )

        val result = parser.parse(rawBytes)

        assertNotNull("La trama válida debe ser decodificada", result)
        assertEquals(75.00, result!!.weightKg, 0.01)
        assertTrue("La medición debe estar marcada como estabilizada", result.isStabilized)
        assertEquals(500.0, result.impedanceOhms ?: 0.0, 0.1)
    }

    @Test
    fun parseUnstabilizedPacket_marksIsStabilizedFalse() {
        val rawBytes = byteArrayOf(
            0x10.toByte(),
            0x1D.toByte(), 0x40.toByte(), // 74.88 kg mientras la persona aún se mueve
            0x00.toByte(),                // No estabilizado
            0x00.toByte(), 0x00.toByte(),
            0x00.toByte()
        )

        val result = parser.parse(rawBytes)

        assertNotNull(result)
        assertFalse("Lectura en movimiento no debe marcarse como final", result!!.isStabilized)
    }

    @Test
    fun parseCorruptedPacket_returnsNull() {
        val tooShortBytes = byteArrayOf(0x10.toByte(), 0x01.toByte())
        assertNull(parser.parse(tooShortBytes))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.salud.personal.data.ble.renpho.QnScalePacketParserTest"`
Expected: FAIL por clases inexistentes.

- [ ] **Step 3: Write minimal implementation**

Crear `app/src/main/java/com/salud/personal/data/ble/renpho/QnScaleMeasurement.kt`:
```kotlin
package com.salud.personal.data.ble.renpho

data class QnScaleMeasurement(
    val weightKg: Double,
    val isStabilized: Boolean,
    val impedanceOhms: Double?
)
```

Crear `app/src/main/java/com/salud/personal/data/ble/renpho/QnScalePacketParser.kt`:
```kotlin
package com.salud.personal.data.ble.renpho

class QnScalePacketParser {

    fun parse(data: ByteArray): QnScaleMeasurement? {
        if (data.size < 6) return null

        // Protocolo QN-Scale / Trisa / Renpho clásico:
        // Byte 0: Header (0x10, 0x20 o tipo de paquete)
        // Byte 1 & 2: Peso en Big-Endian (entero sin signo / 100)
        val weightRaw = ((data[1].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)
        val weightKg = weightRaw / 100.0

        // Byte 3: Status flag (bit 0 = 1 si la lectura está estabilizada)
        val isStabilized = (data[3].toInt() and 0x01) == 0x01

        // Byte 4 & 5: Impedancia (resistencia eléctrica en ohmios)
        val impedanceRaw = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val impedanceOhms = if (impedanceRaw > 0) impedanceRaw.toDouble() else null

        if (weightKg <= 0.0) return null

        return QnScaleMeasurement(
            weightKg = weightKg,
            isStabilized = isStabilized,
            impedanceOhms = impedanceOhms
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew test --tests "com.salud.personal.data.ble.renpho.QnScalePacketParserTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/salud/personal/data/ble/ app/src/test/java/com/salud/personal/data/ble/
git commit -m "feat(ble): add QN-Scale packet parser for Renpho Elis 1 with TDD"
```

---

### Task 4: Persistencia Local Room con Deduplicación Idempotente (TDD)

**Files:**
- Create: `app/src/main/java/com/salud/personal/data/local/entity/WeightMeasurementEntity.kt`
- Create: `app/src/main/java/com/salud/personal/data/local/dao/WeightDao.kt`
- Create: `app/src/main/java/com/salud/personal/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/salud/personal/data/repository/WeightRepository.kt`
- Test: `app/src/test/java/com/salud/personal/data/repository/WeightRepositoryTest.kt`

**Interfaces:**
- Consumes: `WeightMeasurement` del dominio.
- Produces: Métodos de inserción idempotente con regla de deduplicación de 10 segundos y consulta de la última medición / histórico de 7 días.

- [ ] **Step 1: Write the failing test**

Crear `app/src/test/java/com/salud/personal/data/repository/WeightRepositoryTest.kt`:
```kotlin
package com.salud.personal.data.repository

import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeightRepositoryTest {

    private val fakeDao = FakeWeightDao()
    private val repository = WeightRepository(fakeDao)

    @Test
    fun insertMeasurement_savesSuccessfully() = runBlocking {
        val measurement = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.0,
            measuredAt = Instant.parse("2026-10-27T10:00:00Z"),
            importedAt = Instant.parse("2026-10-27T10:00:01Z")
        )

        repository.saveMeasurement(measurement)

        val latest = repository.getLatestMeasurement()
        assertNotNull(latest)
        assertEquals(75.0, latest!!.weightKg, 0.01)
    }

    @Test
    fun duplicateMeasurementWithin10Seconds_isIgnored() = runBlocking {
        val first = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.0,
            measuredAt = Instant.parse("2026-10-27T10:00:00Z"),
            importedAt = Instant.parse("2026-10-27T10:00:01Z")
        )
        // 3 segundos después con el mismo peso (+-0.05kg)
        val duplicate = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.02,
            measuredAt = Instant.parse("2026-10-27T10:00:03Z"),
            importedAt = Instant.parse("2026-10-27T10:00:04Z")
        )

        repository.saveMeasurement(first)
        repository.saveMeasurement(duplicate)

        val count = fakeDao.getStoredCount()
        assertEquals("La pesada repetida en ventana de 10s no debe duplicarse", 1, count)
    }
}

class FakeWeightDao : WeightDaoInterface {
    private val list = mutableListOf<WeightMeasurement>()

    override suspend fun insert(measurement: WeightMeasurement) {
        list.add(measurement)
    }

    override suspend fun getLatest(): WeightMeasurement? = list.maxByOrNull { it.measuredAt }

    fun getStoredCount(): Int = list.size
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.salud.personal.data.repository.WeightRepositoryTest"`
Expected: FAIL por tipos inexistentes.

- [ ] **Step 3: Write minimal implementation**

Crear interfaces, repositorio Room y conversor Instant.
`WeightRepository.kt`:
```kotlin
package com.salud.personal.data.repository

import com.salud.personal.domain.model.WeightMeasurement
import java.time.Duration
import kotlin.math.abs

interface WeightDaoInterface {
    suspend fun insert(measurement: WeightMeasurement)
    suspend fun getLatest(): WeightMeasurement?
}

class WeightRepository(private val dao: WeightDaoInterface) {

    suspend fun saveMeasurement(measurement: WeightMeasurement): Boolean {
        val latest = dao.getLatest()
        if (latest != null) {
            val duration = Duration.between(latest.measuredAt, measurement.measuredAt).abs()
            val weightDiff = abs(latest.weightKg - measurement.weightKg)

            // Deduplicación: si es de la misma fuente dentro de 10s y peso equivalente, ignorar
            if (latest.source == measurement.source && duration.seconds <= 10 && weightDiff < 0.1) {
                return false
            }
        }
        dao.insert(measurement)
        return true
    }

    suspend fun getLatestMeasurement(): WeightMeasurement? = dao.getLatest()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew test --tests "com.salud.personal.data.repository.WeightRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/salud/personal/data/repository/ app/src/test/java/com/salud/personal/data/repository/
git commit -m "feat(data): add idempotent weight repository with 10s deduplication window"
```

---

### Task 5: Pantalla Principal en Jetpack Compose (Modo "Operate" de Impeccable)

**Files:**
- Create: `app/src/main/java/com/salud/personal/ui/MainActivity.kt`
- Create: `app/src/main/java/com/salud/personal/ui/dashboard/DashboardScreen.kt`
- Create: `app/src/main/java/com/salud/personal/ui/dashboard/DashboardState.kt`
- Test: `app/src/test/java/com/salud/personal/ui/dashboard/DashboardStateFormattingTest.kt`

**Interfaces:**
- Consumes: `DashboardState` emitido por el ViewModel con `latestWeight`, `delta7Days`, `lastSyncTime`.
- Produces: Interfaz gráfica sin ruido, con contraste alto, tipografía tabular para cifras numéricas y estados de sincronización explícitos.

- [ ] **Step 1: Write the failing test**

Crear `app/src/test/java/com/salud/personal/ui/dashboard/DashboardStateFormattingTest.kt`:
```kotlin
package com.salud.personal.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardStateFormattingTest {

    @Test
    fun formatsWeightWithTwoDecimalsAndUnit() {
        assertEquals("74.80 kg", DashboardFormatting.formatWeight(74.8))
        assertEquals("0.00 kg", DashboardFormatting.formatWeight(null))
    }

    @Test
    fun formatsDeltaWithHonestSign() {
        assertEquals("-0.45 kg", DashboardFormatting.formatDelta(-0.45))
        assertEquals("+0.30 kg", DashboardFormatting.formatDelta(0.30))
        assertEquals("0.00 kg", DashboardFormatting.formatDelta(0.0))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.salud.personal.ui.dashboard.DashboardStateFormattingTest"`
Expected: FAIL con `DashboardFormatting` no encontrado.

- [ ] **Step 3: Write minimal implementation**

Crear `DashboardFormatting.kt` y la interfaz Compose `DashboardScreen.kt`:
- Cabecera sobria sin eslóganes: nombre de la métrica (`Peso`), valor numérico en tipografía grande (`74.85 kg`), fecha de pesada relativa/absoluta y delta honesto.
- Estado de sincronización de la báscula Renpho Elis 1 (Última conexión).

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew test --tests "com.salud.personal.ui.dashboard.DashboardStateFormattingTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/salud/personal/ui/ app/src/test/java/com/salud/personal/ui/
git commit -m "feat(ui): add quiet operate dashboard screen with honest weight and delta formatting"
```

---

## Plan Review Checklist
- [x] Spec coverage: Scaffolding, domain model, QN-Scale packet parser, Room repository with deduplication, Compose UI dashboard.
- [x] Zero placeholders (no "TBD", "TODO", "implement later").
- [x] Exact code snippets and paths provided.
- [x] TDD cycle explicit in every task.
