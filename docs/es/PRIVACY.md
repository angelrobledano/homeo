# Política de Privacidad y Arquitectura de Protección de Datos

[English](../en/PRIVACY.md) | [Español](PRIVACY.md)

En Homeo, la privacidad constituye una condición de diseño arquitectónico y no una mera formalidad legal. Este documento describe qué datos de salud se procesan, cómo se protegen y por qué ninguna parte de tu información personal abandona jamás tu dispositivo.

---

## 1. Principios Fundamentales de Privacidad

- **100% Local (Offline-First):** Homeo carece de servidores en la nube, bases de datos remotas y servicios de telemetría. Tu información permanece exclusivamente en tu hardware.
- **Cero SDKs de Terceros:** Sin Google Firebase, sin píxeles de Meta, sin Mixpanel, sin plataformas de reporte de errores ni rastreadores publicitarios.
- **Minimización de Datos:** Solicitamos exclusivamente los permisos indispensables para la comunicación con los dispositivos físicos que tú autorices.
- **Control Total del Usuario:** Puedes exportar tu historial completo en formato JSON legible o eliminarlo por completo con un único toque desde la pantalla de ajustes.

---

## 2. Categorías de Datos de Salud Procesados

Homeo procesa las siguientes métricas de rendimiento y biometría deportiva:

1. **Composición Corporal:** Peso, impedancia bioeléctrica y porcentaje de grasa corporal (leídos desde básculas a través de Health Connect).
2. **Métricas Cardiovasculares:** Frecuencia cardíaca continua, frecuencia cardíaca en reposo (FC reposo) y distribución de esfuerzo por zonas cardíacas.
3. **Actividad Física Diaria:** Conteo de pasos, calorías activas consumidas, distancia recorrida y cadencia.
4. **Entrenamientos y Seguimiento GPS:** Tipo de ejercicio, marcas temporales de inicio/fin, Impulso de Entrenamiento (TRIMP), Escala de Esfuerzo Percibido (RPE) y coordenadas de ruta GPS en sesiones al aire libre.
5. **Sueño y Ritmos Circadianos:** Duración del descanso, cálculo de deuda acumulada de sueño y valoración subjetiva matutina.
6. **Hábitos y Etiquetas del Diario:** Factores registrados por el deportista (cafeína, alcohol, sauna, cenas tardías, suplementación) utilizados con fines exclusivos de análisis de correlación estadística.

---

## 3. Almacenamiento y Conservación de Datos

- **Ubicación del Almacenamiento:** Todos los datos se guardan en la base de datos privada SQLite gestionada por Room en el dispositivo:
  `/data/data/com.salud.personal/databases/salud_personal_database`
- **Aislamiento de Claves de Dispositivo:** Las direcciones MAC Bluetooth y las claves criptográficas de autenticación de Huami se almacenan localmente en SharedPreferences privadas / tablas de Room y propiedades locales de compilación. Jamás se transmiten por red.
- **Periodo de Conservación:** Los datos se conservan en el teléfono indefinidamente hasta que el usuario desinstale la aplicación o ejecute el borrado manual de la base de datos en los Ajustes.

---

## 4. Auditoría y Justificación de Permisos Android

| Permiso de Android | Finalidad y Alcance | ¿Puede Revocarse? |
| :--- | :--- | :--- |
| `android.permission.BLUETOOTH_CONNECT` | Conecta directamente por BLE GATT con el smartwatch Amazfit GTR 3 para recibir telemetría. | Sí |
| `android.permission.BLUETOOTH_SCAN` | Descubre dispositivos BLE durante el emparejamiento y reconexión automática. | Sí |
| `android.permission.ACCESS_FINE_LOCATION` | Requerido por Android para escaneo BLE (API $\le$ 30) y para trazar rutas GPS precisas de entrenamientos. | Sí |
| `android.permission.FOREGROUND_SERVICE` | Mantiene el servicio de tracking GPS activo con la pantalla bloqueada durante carreras exteriores. | Sí |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | Declaración explícita del tipo de Foreground Service para geolocalización (requisito Android 14+). | Sí |
| `android.permission.health.READ_WEIGHT` | Lee los pesajes importados por aplicaciones compañeras (como Renpho Health) en Health Connect. | Sí |
| `android.permission.health.READ_BODY_FAT` | Lee estimaciones de grasa corporal desde Health Connect. | Sí |

---

## 5. Derechos del Usuario: Exportación, Copia y Borrado

- **Exportación Local en JSON:** Puedes exportar todo tu historial biométrico en un archivo JSON estructurado e independiente mediante el `FileProvider` seguro de Android.
- **Borrado Completo:** La opción en **Ajustes > Gestión de datos** elimina y regenera todas las tablas de Room SQLite, suprimiendo de forma irreversible todo el historial.
- **Sin Cuentas Obligatorias:** Homeo no requiere crear cuentas de usuario, contraseñas ni inicios de sesión en la nube.

---

## 6. Aviso Legal de Carácter No Sanitario

Homeo ha sido diseñada exclusivamente como una herramienta de seguimiento de rendimiento físico y salud personal fundamentada en modelos de ciencia deportiva (TRIMP de Banister, ACWR de Gabbett).

**Homeo no es un producto sanitario ni un dispositivo médico certificado.** No proporciona diagnósticos clínicos, tratamientos médicos ni monitorización terapéutica. Nunca debe sustituir el juicio de un profesional de la salud colegiado. Las fluctuaciones en las métricas (frecuencia cardíaca en reposo, puntuaciones de recuperación, tendencias de peso) son estimaciones fisiológicas y no deben interpretarse como diagnósticos médicos.
