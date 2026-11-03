# Matriz de Compatibilidad de Dispositivos e Integraciones de Hardware

[English](../en/DEVICE_COMPATIBILITY.md) | [Español](DEVICE_COMPATIBILITY.md)

Este documento detalla la información técnica sobre dispositivos compatibles, protocolos de comunicación, handshakes criptográficos y métodos de ingesta de datos en Homeo.

---

## Matriz de Compatibilidad de Hardware

| Fabricante y Modelo | Fuente / Integración | Tipos de Datos | Dirección | Permisos y Autenticación | Red Requerida | Frecuencia y Límites | Clave de Deduplicación | Restricciones Verificadas | Fecha de Verificación | Estado |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Amazfit GTR 3** (Modelo: A1971 / Huami 227) | BLE GATT (Protocolo Zepp OS 2021 / `0xFEE0` y `0xFEE1`) | Frecuencia cardíaca en vivo, pasos, distancia, calorías, batería. | Bidireccional (Handshake autenticado y telemetría continua) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`. Clave Auth Key de 16 bytes (AES-128 + ECDH B-163). | **No** (100% offline, Bluetooth local) | Conexión BLE continua y reactiva. | Clave primaria / Índice único: `(source_device, timestamp)`. | Requiere Auth Key extraída de Zepp / Gadgetbridge. Claves aisladas en `secrets.properties`. | 2026-10-27 (Pixel 7a físico) | **Implementado y Verificado** |
| **RENPHO Elis 1** (Modelo: ES-CS20M / QN-Scale) | Android Health Connect (`androidx.health.connect:connect-client`) | Peso (`kg`), Grasa corporal (`%`). | Unidireccional (Health Connect -> Room) | `android.permission.health.READ_WEIGHT`, `android.permission.health.READ_BODY_FAT` | **No** (API local de Android al 100%) | Bajo demanda al abrir la app o pulsar sincronizar. | Ventana temporal de 3 minutos respecto a registros en `WeightMeasurementEntity`. | Requiere Android 14+ o app Health Connect instalada. El % de grasa es estimación bioeléctrica. | 2026-11-02 (Pixel 7a físico, 7 registros importados) | **Implementado y Verificado** |
| **Tracker GPS Exterior** | Servicios de Ubicación de Android (`ACCESS_FINE_LOCATION`) | Coordenadas GPS, altitud, ritmo instantáneo, trazado de ruta. | Flujo continuo de sensores | `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE_LOCATION` | **No** (Mosaicos cacheados en OpenStreetMap) | Actualizaciones cada segundo durante entrenamientos activos. | Lista de segmentos de sesión en `WorkoutSessionEntity`. | Filtrado de precisión horizontal (< 15 metros de error requerido). | 2026-11-03 (Pixel 7a físico) | **Implementado y Verificado** |
| **RENPHO Elis 1 (BLE Directo)** | Emisión BLE Broadcast (Protocolo QN-Scale) | Peso bruto, impedancia eléctrica. | Broadcast unidireccional (Báscula -> App) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`. No requiere clave de emparejamiento. | **No** (Emisión BLE 100% local) | Bajo demanda al subirse a la báscula. | `(source_device, timestamp_ventana_5s, peso)` | Prescinde de Health Connect; planificado para Fase 5. | 2026-10-27 (Controlador de referencia openScale) | **Viable / Planificado** |

---

## Especificaciones Técnicas de Protocolos

### 1. Amazfit GTR 3 (Zepp OS / Huami 2021)

#### UUIDs de Servicios y Características GATT
- **Servicio de Autenticación (`0xFEE1`):**
  - Característica `00000009-0000-1000-8000-00805f9b34fb`: Punto de control de autenticación para reto-respuesta criptográfico.
- **Servicio de Telemetría (`0xFEE0`):**
  - Característica `00000007-0000-1000-8000-00805f9b34fb`: Flujo continuo de sensores y pasos en tiempo real.
  - Característica `00000006-0000-1000-8000-00805f9b34fb`: Notificaciones periódicas de porcentaje de batería.
- **Servicio de Frecuencia Cardíaca (`0x180D`):**
  - Característica `0x2A37`: Notificación estándar Heart Rate Measurement.

#### Secuencia del Handshake Criptográfico
1. **Solicitud de Reto:** Homeo envía `[0x02, 0x00]` al punto de control de autenticación.
2. **Respuesta del Reto:** El smartwatch devuelve una semilla aleatoria de 16 bytes: `[0x10, 0x02, 0x01, ...16 bytes aleatorios...]`.
3. **Derivación de Clave y Cifrado:**
   - Se genera un par de claves efímeras ECDH sobre la curva NIST B-163.
   - El reto aleatorio de 16 bytes se cifra utilizando AES-128 (modo ECB, relleno cero) con la clave secreta `WATCH_AUTH_KEY`.
4. **Verificación de Autorización:** Homeo envía `[0x03, 0x00, ...16 bytes cifrados...]`.
5. **Sesión Establecida:** El reloj valida la firma y emite `[0x10, 0x03, 0x01]`, cambiando el estado de conexión a `CONNECTED`.

---

### 2. Renpho Elis 1 y Android Health Connect

#### Arquitectura de Integración
- Utiliza el SDK oficial de Android Jetpack Health Connect (`androidx.health.connect:connect-client:1.1.0-alpha11`).
- Lee registros de tipo `WeightRecord` y `BodyFatRecord` generados por la aplicación compañera del fabricante.
- El filtro de ingesta biológica descarta lecturas anómalas (< 35 kg o > 250 kg) para impedir que pisadas involuntarias o taras de calibración corrompan las curvas de tendencia.

#### Ventana de Deduplicación
- Los registros de Health Connect separados por menos de 3 minutos de una medición local existente en Room se comparan: si el peso coincide dentro de $\pm 0.05$ kg, la inserción duplicada se cancela.

---

### 3. Motor GPS con OpenStreetMap para Entrenamientos

- **Ciclo de Vida del Servicio:** Administrado por `WalkTrackingService` como Foreground Service de Android con tipo `location`.
- **Renderizado de Mapas:** osmdroid 6.1.18 renderiza mosaicos vectoriales sin depender de claves de API propietarias de Google Play Services.
- **Filtrado de Ruido:** Las lecturas GPS con dilución de precisión horizontal $> 20$ metros o saltos anómalos de velocidad $> 45$ km/h (en actividades a pie) se rechazan para eliminar artefactos y derivas del sensor.
