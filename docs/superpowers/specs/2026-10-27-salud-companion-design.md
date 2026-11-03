# Especificación de Diseño: Salud & Rendimiento Personal Android

- **Fecha:** 2026-10-27
- **Estado:** Propuesta aprobada en Brainstorming
- **Referencia:** Conforme a `AGENTS.md` y `docs/adr/0001-stack-selection.md`

---

## 1. Misión y Propósito del Producto
Construir una aplicación Android nativa de uso personal para centralizar, consultar y analizar datos de salud y rendimiento físico procedentes de un smartwatch **Amazfit GTR 3** (A1971) y una báscula inteligente **RENPHO Elis 1** (ES-CS20M), prescindiendo por completo de las aplicaciones oficiales y su telemetría.

### Principios Fundamentales (de `AGENTS.md`):
- **Privacidad estricta:** Funcionamiento 100% local (offline-first). Base de datos SQLite interna. Sin servidores propios, sin analítica ni trackers.
- **Regla de diseño 1 (Cero ruido):** Sin eslóganes, textos motivacionales ni etiquetas redundantes. Jerarquía visual autoexplicativa basada en valores, unidades y periodos comparables.
- **Trazabilidad:** Cada medición preserva su fuente de origen, valor original, valor normalizado canónico, fecha/hora exacta y estado de sincronización.
- **Rigor en la interpretación:** Diferenciar mediciones directas (frecuencia cardíaca, peso) de estimaciones indirectas (grasa por bioimpedancia) y cálculos derivados (TRIMP, ACWR).

---

## 2. Dispositivos y Fuentes de Datos

### 2.1. Amazfit GTR 3 (Modelo A1971 — Zepp OS)
- **Protocolo:** Bluetooth Low Energy (BLE) GATT con protocolo Huami/Zepp OS.
- **Autenticación (Handshake):**
  - Requiere intercambio criptográfico AES con una **Auth Key** de 16 bytes.
  - La clave se extrae una sola vez de la cuenta de Zepp y se almacena en el `EncryptedSharedPreferences` / `Android KeyStore` de la app.
- **Capacidades utilizadas:**
  - Lectura de frecuencia cardíaca continua y por sesión de entreno.
  - Descarga de sesiones de actividad con desglose por zonas cardíacas (Z1 a Z5).
  - Estado de batería y fecha de última sincronización.
  - Escritura de alertas y notificaciones locales hacia la pantalla del reloj (avisos de carga de entrenamiento y recuperación).

### 2.2. RENPHO Elis 1 (Serie ES-CS20M / QN-Scale)
- **Protocolo:** Bluetooth Low Energy (BLE) GATT con decodificación de tramas QN-Scale.
- **Autenticación:** Abierta (no requiere cifrado de emparejamiento).
- **Capacidades utilizadas:**
  - Transmisión en tiempo real al pisar la báscula.
  - Captura del peso bloqueado (en kg con resolución de 0.05 kg).
  - Lectura de resistencia/impedancia eléctrica y cálculo de grasa corporal (%), masa muscular (kg) y agua (%).

### 2.3. Android Health Connect (Google Fit / Ecosistema de Salud)
- **Integración:** API nativa `androidx.health.connect:connect-client`.
- **Propósito:** Sincronización local en el dispositivo hacia Health Connect, permitiendo que apps como Google Fit lean las mediciones de peso y entrenamientos sin depender de la nube.

---

## 3. Arquitectura del Sistema

La aplicación sigue los principios de **Clean Architecture** estructurada en capas:

```
┌────────────────────────────────────────────────────────┐
│ Presentation Layer (Jetpack Compose, Material 3)       │
│ - Pantalla Principal (Métricas clave, Estado de carga) │
│ - Pantalla de Historial (Gráficos honestos, Tendencias)│
│ - Pantalla de Dispositivos (Estado BLE, Batería, Sync) │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Domain Layer (Kotlin puro / Casos de uso)              │
│ - CalculateTrimpUseCase & CalculateAcwrUseCase         │
│ - NormalizeMeasurementUseCase                          │
│ - GenerateRecoveryRecommendationUseCase                │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Data Layer (Repositorios y Fuentes de Datos)           │
│ - LocalDatabase (Room SQLite, esquema versionado)      │
│ - BleDeviceManager (Foreground Service BLE)            │
│   ├── AmazfitZeppOsBleDriver (GTR 3 Handshake & Sync)  │
│   └── RenphoQnScaleBleDriver (Elis 1 BLE Parser)       │
│ - HealthConnectSyncManager (Exportación local)         │
└────────────────────────────────────────────────────────┘
```

---

## 4. Modelo de Datos y Normalización

### 4.1. Unidades Canónicas
- **Masa / Peso:** Kilogramos (`kg`) con precisión de 2 decimales en almacenamiento (`Double`).
- **Porcentajes:** `Float` en rango `[0.0, 100.0]`.
- **Frecuencia Cardíaca:** Entero positivo en pulsaciones por minuto (`bpm`).
- **Tiempo / Marcas temporales:** `java.time.Instant` en UTC en persistencia; visualización en zona horaria local del dispositivo.

### 4.2. Entidades Principales (Room)

```kotlin
// Medición de Peso y Composición Corporal
@Entity(
    tableName = "weight_measurements",
    indices = [Index(value = ["source_device", "measured_at"], unique = true)]
)
data class WeightMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceDevice: String,            // "RENPHO_ELIS_1"
    val externalId: String?,             // Identificador si el protocolo lo expone
    val weightKg: Double,                // Valor canónico
    val bodyFatPercentage: Float?,       // Estimación bioimpedancia (%)
    val muscleMassKg: Double?,           // Estimación masa muscular (kg)
    val impedanceOhms: Double?,          // Valor eléctrico directo
    val measuredAt: Instant,             // Hora de la medición
    val importedAt: Instant,             // Hora de importación local
    val isRawEstimate: Boolean           // Flag: true para valores bioeléctricos
)

// Sesión de Entrenamiento
@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["source_device", "started_at"], unique = true)]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceDevice: String,            // "AMAZFIT_GTR_3"
    val sportType: String,               // "RUNNING", "STRENGTH", "CYCLING", etc.
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val timeInZone1Seconds: Long,
    val timeInZone2Seconds: Long,
    val timeInZone3Seconds: Long,
    val timeInZone4Seconds: Long,
    val timeInZone5Seconds: Long,
    val calculatedTrimp: Double,         // Bannister TRIMP
    val importedAt: Instant
)
```

### 4.3. Reglas de Deduplicación e Idempotencia
- **Renpho Elis 1:** Dos lecturas sucesivas dentro de una ventana de 10 segundos con una diferencia de peso inferior a 0.1 kg se consideran la misma pesada (evita registrar el tanteo al subirse). Solo se consolida el registro con el peso final estabilizado.
- **Amazfit GTR 3:** Las sesiones y registros se identifican unívocamente por `(source_device, started_at)`. Una sincronización repetida no duplica ni modifica sesiones ya consolidadas.

---

## 5. Motor de Rendimiento Deportivo (Training Load)

### 5.1. Bannister TRIMP (Training Impulse)
Calcula el estrés cardiovascular por sesión en función de la reserva de frecuencia cardíaca ($HR_{res}$):
$$\Delta HR = \frac{HR_{mean} - HR_{rest}}{HR_{max} - HR_{rest}}$$
$$TRIMP = \text{Duración (min)} \times \Delta HR \times 0.64 \times e^{1.92 \times \Delta HR}$$

### 5.2. ACWR (Acute to Chronic Workload Ratio)
- **Carga Aguda (Fatiga a corto plazo):** Media ponderada exponencial o suma de la carga de los últimos **7 días**.
- **Carga Crónica (Forma física / Aptitud a medio plazo):** Media ponderada o suma acumulada de los últimos **28 días**.
- **Ratio ACWR:** $\text{Ratio} = \frac{\text{Carga Aguda}}{\text{Carga Crónica}}$.

**Estados de Recomendación:**
| Rango ACWR | Estado Fisiológico | Recomendación / Mensaje al GTR 3 |
| :--- | :--- | :--- |
| **< 0.8** | Subentrenamiento / Pérdida de forma | Estímulo bajo. Incremento progresivo recomendado. |
| **0.8 - 1.3** | Carga Óptima / Rendimiento | Carga en equilibrio. Proseguir según plan. |
| **1.3 - 1.5** | Sobrecarga funcional controlada | Carga alta pero asumible. Priorizar recuperación activa. |
| **> 1.5** | **Alerta de Sobrecarga / Riesgo de Lesión** | Carga excesiva. Se recomienda descanso total o sesión regenerativa. |

---

## 6. Diseño Visual e Interfaz (Impeccable "Operate")

Conforme a la **Regla de diseño 1** y las directrices de `AGENTS.md`:

1. **Pantalla Principal (Resumen Diario de Estado):**
   - **Indicador de Rendimiento:** Barra o valor sobrio de estado (ej: *"Óptimo (ACWR 1.12)"*), fecha de última actualización del GTR 3.
   - **Último Peso:** Valor en kg grande y tipografía clara (ej: `74.8 kg`), fecha y hora de la pesada, diferencia honesta respecto a la media de 7 días.
   - **Estado de Batería de Dispositivos:** Batería del GTR 3 en % discreto.
2. **Sin adornos:** Cero ilustraciones genéricas, cero tarjetas flotantes con sombras desmedidas, cero emojis en etiquetas. Tipografía monoespaciada para valores numéricos para facilitar comparación vertical.
3. **Estados del Sistema:**
   - Visualización explícita si la báscula o el reloj no se han sincronizado en más de 24 horas.
   - Estado de escaneo BLE visible sólo cuando se esté conectando activamente.

---

## 7. Privacidad y Seguridad

- **Almacenamiento Local:** La base de datos SQLite se almacena en el directorio protegido de la aplicación (`Context.filesDir`).
- **Auth Key de Huami:** Cifrada mediante `EncryptedSharedPreferences` respaldado por `Android KeyStore` (hardware-backed cuando esté disponible).
- **Permisos de Red:** No se solicitará el permiso `android.permission.INTERNET` en la app final (100% aislada de red).
- **Exportación:** Solo a través del almacenamiento local del usuario o Health Connect autorizado explícitamente.

---

## 8. Estrategia de Pruebas y TDD

Siguiendo la Sección 14 de `AGENTS.md`:
1. **Tests unitarios deterministas:**
   - Parser de bytes de la trama de Renpho Elis 1 (QN-Scale) con payloads sintéticos conocidos.
   - Algoritmo criptográfico de autenticación de Huami BLE (AES-128 con claves y challenges de prueba).
   - Cálculo de TRIMP y ACWR con matrices de sesiones de prueba.
   - Reglas de deduplicación de pesadas repetidas.
2. **Evaluaciones repetibles (`evals/`):**
   - Consistencia de importación de datos.
   - Ausencia de duplicados en sincronizaciones repetidas.

---

## 9. Desglose en Fases para Ejecución SDD

- **Fase 1 (Flujo Vertical Inicial — Renpho Elis 1):**
  - Configuración del proyecto Android (Gradle, Kotlin, Compose, Room).
  - TDD del parser BLE QN-Scale.
  - Persistencia de `WeightMeasurement` en Room.
  - Pantalla inicial con lectura de peso real y comparativa.
- **Fase 2 (Smartwatch Amazfit GTR 3):**
  - Utilidad de extracción / configuración de la Auth Key.
  - TDD del handshake AES y protocolo Huami/Zepp OS.
  - Sincronización de batería y sesiones de entrenamiento.
- **Fase 3 (Motor de Entrenamiento y Notificaciones al Reloj):**
  - TDD del cálculo TRIMP y ACWR.
  - Disparador de notificaciones y recomendaciones hacia el GTR 3.
- **Fase 4 (Health Connect & Pulido Impeccable):**
  - Integración con Android Health Connect.
  - Auditoría de diseño visual (revisión de contraste, tipografía y eliminación de texto innecesario).
