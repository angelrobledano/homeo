# Homeo

[English](README.md) | [Español](README.es.md)

[![Android](https://img.shields.io/badge/Plataforma-Android%2014%2B%20(API%2034)-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Lenguaje-Kotlin%202.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Arquitectura](https://img.shields.io/badge/Arquitectura-MVI%20%2F%20Clean%20Architecture-blue.svg?style=flat)]()
[![Privacidad](https://img.shields.io/badge/Privacidad-100%25%20Local%20%2F%20Cero%20Nube-00E676.svg?style=flat)]()
[![Licencia](https://img.shields.io/badge/Licencia-Apache%202.0-lightgrey.svg?style=flat)](LICENSE)

> **Cockpit de Telemetría Biométrica y Homeostasis Fisiológica Dinámica**  
> Aplicación de salud y rendimiento atlético nativa para Android con funcionamiento prioritariamente local (offline-first). Conexión directa por BLE con wearables (Amazfit GTR 3), sincronización con básculas mediante Android Health Connect (Renpho), modelos de fisiología deportiva deterministas (ACWR de Gabbett, TRIMP de Banister, tendencia de peso EMA tipo MacroFactor) y diseño oscuro OLED de alto contraste inspirado en WHOOP.

---

## Tabla de Contenidos

- [Principios Fundamentales](#principios-fundamentales)
- [Funcionalidades Principales](#funcionalidades-principales)
- [Fundamentos Científicos y Fisiológicos](#fundamentos-científicos-y-fisiológicos)
- [Matriz de Integración de Dispositivos](#matriz-de-integración-de-dispositivos)
- [Arquitectura y Stack Tecnológico](#arquitectura-y-stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Puesta en Marcha y Desarrollo](#puesta-en-marcha-y-desarrollo)
- [Privacidad y Seguridad](#privacidad-y-seguridad)
- [Índice de Documentación](#índice-de-documentación)
- [Cómo Contribuir](#cómo-contribuir)
- [Licencia](#licencia)

---

## Principios Fundamentales

1. **Cero Humo, Cero Pseudociencia:** Sin eslóganes motivacionales, sin puntuaciones de salud arbitrarias y sin diagnósticos clínicos encubiertos a partir de métricas de consumo. Exclusivamente cálculos deterministas basados en literatura de fisiología deportiva revisada por pares.
2. **Línea Base Personal frente a Medias Poblacionales:** Tus estándares biométricos se comparan con tu propia línea base histórica (frecuencia cardíaca en reposo a 30 días, capacidad crónica de carga a 28 días y peso real con media móvil exponencial).
3. **100% Local (Offline-First) y Privada:** Todos los datos se procesan y almacenan localmente en el dispositivo mediante Room SQLite. Cero servidores externos en la nube, cero analíticas, cero rastreadores.
4. **Integraciones Robustas con Hardware:** Negociación directa BLE GATT utilizando controladores de protocolo de ingeniería inversa (handshake criptográfico Huami 2021 ECDH/AES-128) y sincronización local mediante Android Health Connect.

---

## Funcionalidades Principales

- **Cockpit Biométrico Diario (`DashboardScreen`):**
  - **Anillo de Recuperación Dinámica:** Calculado a partir de las desviaciones de tu FC en reposo frente a tu línea base de 30 días y la duración del sueño.
  - **Objetivo de Esfuerzo Diario (Strain):** Objetivo dinámico de carga adaptado al nivel de recuperación matutino.
  - **Tarjeta de Constantes Vitales en Línea Base:** Evaluación en tiempo real de si la FC en reposo, el sueño y el peso se mantienen dentro de tu rango biológico habitual.
  - **Tarjeta de Peso con Filtro Biológico:** Descarta errores de calibración y pisadas accidentales (< 35 kg o > 250 kg) calculando el delta real a 7 días.
  - **Estado de Conexión del Smartwatch:** Telemetría en tiempo real de conectividad BLE y porcentaje de batería para Amazfit GTR 3.

- **Carga Atlética y Tendencias (`ProgressScreen`):**
  - **Selectores de Granularidad Temporal (7D / 30D / 90D):** Comparación inmediata de carga (TRIMP), pasos, sueño y FC en reposo contra el periodo anterior equivalente (7d vs 7d, 30d vs 30d, 90d vs 90d).
  - **Ratio de Carga Aguda:Crónica (ACWR):** Indicador visual de 4 zonas (<0.8 Descarga, 0.8–1.3 Rango Óptimo, 1.3–1.5 Precaución, >1.5 Zona de Riesgo) basado en el modelo de Tim Gabbett.
  - **Historial de Carga de 4 Semanas:** Gráfico de barras que contrasta la capacidad crónica frente a picos agudos de esfuerzo.
  - **Tendencia de FC en Reposo a 30 Días:** Curva de evolución diaria comparada contra la línea base fisiológica personal.
  - **Tendencia de Peso (Filtro EMA):** Media Móvil Exponencial ($\alpha = 0.10$) que aísla fluctuaciones osmóticas de agua, sodio y glucógeno para reflejar cambios reales de tejido graso y muscular.
  - **Matriz de Impacto de Hábitos (Journal):** Análisis estadístico de correlación que evalúa cómo hábitos como la hidratación nocturna, sauna, alcohol o magnesio inciden sobre la recuperación y la deuda de sueño.

- **Análisis de Entrenamientos y Seguimiento GPS (`WorkoutScreen`):**
  - Registro de actividades al aire libre con GPS en tiempo real y mapas cacheados localmente mediante osmdroid (OpenStreetMap).
  - Registro manual de entrenamientos con estimación de carga según RPE (Escala Borg CR10).
  - Distribución de esfuerzo por zonas de frecuencia cardíaca (Zona 1 Recuperación a Zona 5 Anaeróbica/Máxima).

- **Coach Fisiológico Determinista (`CoachScreen`):**
  - Recomendaciones basadas en reglas para ajustar volumen de entrenamiento, horarios de sueño y descanso activo.
  - Calculadora de deuda de sueño acumulada en ventanas móviles de 7 días.

- **Balances de Periodos Cerrados (`PeriodSummaryScreen`):**
  - Retrospectivas estructuradas con balances cerrados por Día, Semana, Mes y Año.

- **Portabilidad de Datos y Widget de Escritorio:**
  - Copia de seguridad local mediante exportación e importación en formato JSON legible y sin dependencias externas.
  - Widget para la pantalla de inicio (`HomeoAppWidget`) con estado rápido de recuperación, esfuerzo y pasos.

---

## Fundamentos Científicos y Fisiológicos

| Métrica | Algoritmo Científico | Finalidad |
| :--- | :--- | :--- |
| **ACWR** | Ratio de Carga Aguda:Crónica de Tim Gabbett | Compara la fatiga de 7 días contra la capacidad aeróbica de 28 días para mitigar riesgo de lesión. |
| **TRIMP** | Training Impulse de Eric Banister | Cuantifica la carga del entrenamiento ponderando duración, frecuencia cardíaca de reserva y factor exponencial por sexo. |
| **Tendencia de Peso** | Media Móvil Exponencial ($\alpha = 0.10$) | Filtra fluctuaciones de agua (retención por glucógeno/sodio) para extraer el delta biológico real. |
| **Línea Base FC Reposo** | Media recortada y desviación típica a 30 días | Detecta sobreentrenamiento, estrés del sistema nervioso simpático o incubación de enfermedades. |
| **Deuda de Sueño** | Déficit acumulado de necesidad biológica a 7 días | Determina la presión homeostática de sueño y guía ajustes en la hora de acostarse. |

---

## Matriz de Integración de Dispositivos

| Dispositivo / Fuente | Protocolo / API | Datos Telemetría | Tipo de Sincronización | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **Amazfit GTR 3** | BLE GATT (`0xFEE0`/`0xFEE1`), Huami 2021 ECDH B-163 + AES-128 | Frecuencia cardíaca, pasos, distancia, calorías, batería. | BLE directo (Local) | **Verificado en Pixel 7a** |
| **Renpho Elis 1** | Android Health Connect (`androidx.health.connect:connect-client`) | Peso (`kg`), Grasa corporal (`%`). | Bajo demanda / Inicio de app | **Verificado en Pixel 7a** |
| **Seguimiento GPS** | Localización de Android + osmdroid | Coordenadas, ritmo, desnivel, distancia. | Primer plano en tiempo real | **Verificado en Pixel 7a** |

Para información técnica detallada sobre tramas BLE y autenticación, consulta [docs/es/DEVICE_COMPATIBILITY.md](docs/es/DEVICE_COMPATIBILITY.md).

---

## Arquitectura y Stack Tecnológico

Homeo está construida como una aplicación Android moderna, modular y reactiva:

- **Lenguaje:** Kotlin 2.0 con tipado estático estricto y seguridad frente a nulos.
- **Framework de UI:** Jetpack Compose con tokens de diseño Material 3.
- **Patrón de Estado:** MVI (Model-View-Intent) soportado por Coroutines y `StateFlow`.
- **Persistencia Local:** Room SQLite 2.6 con esquemas indexados y migraciones controladas.
- **Plataforma de Salud:** Android Health Connect Client API.
- **Diseño Visual:** Paleta oscura OLED personalizada `WhoopColors` (`#0A0B0D`) conforme a los estándares de contraste WCAG AA.

El diagrama arquitectónico completo y el flujo de datos se detallan en [docs/es/ARCHITECTURE.md](docs/es/ARCHITECTURE.md).

---

## Estructura del Proyecto

```text
app/src/main/java/com/salud/personal/
├── data/
│   ├── ble/            # Gestor BLE GATT, autenticación criptográfica Huami y decodificadores
│   ├── healthconnect/  # Cliente de sincronización y permisos de Health Connect
│   ├── local/          # Base de datos Room, TypeConverters, DAOs y Entidades
│   ├── repository/     # Repositorios que unifican BLE, Room y Health Connect
│   └── service/        # Servicio en primer plano para tracking GPS de entrenamientos
├── domain/
│   ├── coach/          # Motor heurístico para recomendaciones de carga y descanso
│   ├── habits/         # Calculadora de correlación estadística entre hábitos y recuperación
│   ├── model/          # Modelos de dominio puros independientes de hardware o base de datos
│   ├── recovery/       # Cálculo del índice de recuperación (FC reposo + Sueño + Sensaciones)
│   ├── sleep/          # Motor de deuda de sueño y ventanas circadianas
│   ├── strain/         # Estimadores de TRIMP y carga manual de ejercicios
│   ├── summary/        # Generador de balances cerrados (Día, Semana, Mes, Año)
│   ├── trends/         # ACWR (Gabbett), tendencia de peso EMA y comparativas temporales
│   └── vitals/         # Evaluador de constantes vitales frente a línea base individual
└── ui/
    ├── activity/       # Pantalla de tracking GPS y visor de mapas
    ├── coach/          # Interfaz de recomendaciones y guía del deportista
    ├── dashboard/      # Cockpit principal, anillos, estado vital y telemetría del reloj
    ├── easyui/         # Primitivas reutilizables de UI (tarjetas Spotlight, botones táctiles)
    ├── navigation/     # Grafo de navegación Jetpack y barra de navegación inferior
    ├── recovery/       # Desglose de recuperación y cuestionario matutino
    ├── settings/       # Emparejamiento de dispositivos, credenciales y gestión de datos
    ├── summary/        # Pantallas de balances de periodos cerrados
    ├── theme/          # Paleta de colores OLED, tipografías y formas
    ├── trends/         # Pantalla de progreso, medidores ACWR e histogramas de carga
    ├── widget/         # Proveedor del widget para pantalla de inicio de Android
    └── workout/        # Historial de sesiones, zonas cardíacas y diálogo manual
```

---

## Puesta en Marcha y Desarrollo

### Requisitos Previos

- **Android Studio:** Hedgehog (2023.1.1) o Ladybug (2024.2+).
- **JDK:** OpenJDK 17 o superior.
- **Android SDK:** Plataforma API 34 (Android 14) y herramientas de compilación.
- **Dispositivo de Prueba:** Dispositivo Android físico con Bluetooth 5.0+ (Pixel 7a recomendado) o emulador Android 14.

### Compilación y Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/your-username/homeo.git
   cd homeo
   ```
2. Compilar APK en modo debug:
   ```bash
   ./gradlew assembleDebug
   ```
3. Ejecutar la suite completa de pruebas unitarias:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Instalar en el dispositivo conectado:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

Para instrucciones detalladas sobre configuración de credenciales (`secrets.properties`) y depuración, revisa [docs/es/DEVELOPMENT.md](docs/es/DEVELOPMENT.md).

---

## Privacidad y Seguridad

Los datos de salud son sumamente sensibles. Homeo aborda la privacidad como requisito estructural:
- **Cero Telemetría de Terceros:** Sin Google Analytics, Firebase, Crashlytics ni logs remotos.
- **Aislamiento de Credenciales:** Las direcciones MAC y claves BLE residen exclusivamente en el archivo local `secrets.properties` y en la base de datos local cifrada/privada.
- **Control del Usuario:** Exportación local de todo el historial en formato JSON transparente y editable.

Para consultar la auditoría de permisos y retención de datos, lee [docs/es/PRIVACY.md](docs/es/PRIVACY.md).

---

## Índice de Documentación

| Documento en Inglés | Documento en Español | Descripción |
| :--- | :--- | :--- |
| [ARCHITECTURE.md](docs/en/ARCHITECTURE.md) | [ARCHITECTURE.md](docs/es/ARCHITECTURE.md) | Capas del sistema, flujos de datos, esquema Room y gestión de estado. |
| [DEVELOPMENT.md](docs/en/DEVELOPMENT.md) | [DEVELOPMENT.md](docs/es/DEVELOPMENT.md) | Configuración de entorno, directrices de testing y comandos de compilación. |
| [DEVICE_COMPATIBILITY.md](docs/en/DEVICE_COMPATIBILITY.md) | [DEVICE_COMPATIBILITY.md](docs/es/DEVICE_COMPATIBILITY.md) | Matriz de hardware, protocolos BLE e integración Health Connect. |
| [PRIVACY.md](docs/en/PRIVACY.md) | [PRIVACY.md](docs/es/PRIVACY.md) | Política de protección de datos, auditoría de permisos y almacenamiento. |
| [FAQ.md](docs/en/FAQ.md) | [FAQ.md](docs/es/FAQ.md) | Preguntas frecuentes, resolución de problemas y detalles de algoritmos. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | [CONTRIBUTING.es.md](CONTRIBUTING.es.md) | Guía de contribución y regla de mantenimiento bilingüe obligatorio. |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | [CODE_OF_CONDUCT.es.md](CODE_OF_CONDUCT.es.md) | Estándares de conducta de la comunidad. |
| [SECURITY.md](SECURITY.md) | [SECURITY.es.md](SECURITY.es.md) | Política de reporte de vulnerabilidades y seguridad local. |
| [ROADMAP.md](ROADMAP.md) | [ROADMAP.es.md](ROADMAP.es.md) | Hoja de ruta del proyecto e hitos futuros. |

---

## Cómo Contribuir

¡Aceptamos contribuciones de desarrolladores tanto internacionales como hispanohablantes! Puedes abrir Issues y Pull Requests en inglés o en español indistintamente.

Por favor, lee [CONTRIBUTING.es.md](CONTRIBUTING.es.md) (o [CONTRIBUTING.md](CONTRIBUTING.md)) antes de enviar código. **Nota obligatoria:** Cualquier cambio en la documentación debe aplicarse simultáneamente en ambas versiones (inglés y español).

---

## Licencia

Homeo se distribuye bajo la licencia [Apache License 2.0](LICENSE).
