# ADR 0001: Selección del Stack y Estrategia de Código

- **Fecha:** 2026-10-27
- **Estado:** Aceptado

## Contexto
El usuario busca una aplicación Android personal para centralizar datos de salud y entrenamiento físico procedentes de un smartwatch **Amazfit GTR 3** (Zepp OS) y una báscula inteligente **RENPHO Elis 1** (QN-Scale BLE), con sincronización hacia Google Fit / Health Connect y cálculo de carga de entrenamiento (TRIMP/ACWR) con avisos al reloj, eliminando el bloatware y telemetría de las aplicaciones oficiales.

Se evaluaron dos caminos técnicos principales:
1. **Opción A (Fork de Gadgetbridge):** Clonar el repositorio completo de Gadgetbridge y modificar su UI y lógica.
2. **Opción B (App nativa Android limpia con Kotlin + Jetpack Compose):** Construir una aplicación nativa moderna utilizando los drivers de protocolo de ingeniería inversa extraídos/adaptados de Gadgetbridge (para Huami/Zepp OS) y openScale (para Renpho QN-Scale).

## Decisión
Se elige la **Opción B: Aplicación nativa Android en Kotlin con Jetpack Compose y Room**.

### Evaluación UX / DX / AX:
- **UX (Experiencia de usuario):**
  - Jetpack Compose y Material 3 permiten una interfaz reactiva, sobria, sin ruido visual (Regla de diseño 1), con tipografía clara, gráficos honestos y sin elementos decorativos o textos de relleno.
  - La app resultante es ligera (<15 MB frente a >100 MB), rápida en el arranque y con consumo mínimo de batería.
- **DX (Experiencia del desarrollador):**
  - Separación limpia de capas (MVI / Clean Architecture) sin la deuda técnica de un monolito de 10 años en Java y vistas XML antiguas.
  - TDD determinista para los modelos de datos, normalización de métricas, deduplicación y algoritmos de fatiga deportiva (TRIMP, ACWR).
  - API moderna de Android Health Connect (`androidx.health.connect:connect-client`).
- **AX (Experiencia del agente):**
  - Un codebase limpio, tipado en Kotlin, con módulos bien delimitados permite a cualquier agente navegar el código, ejecutar tests unitarios con `gradlew test`, verificar invariantes y extender funcionalidad sin desorientarse en miles de clases de dispositivos no relacionados.

## Consecuencias
- **Positivas:**
  - Control absoluto sobre el modelo de dominio y la privacidad (cero trackers, 100% offline-first).
  - Facilidad para incorporar el motor de fatiga deportiva y los reportes matutinos al reloj.
  - Cumplimiento estricto de las directrices de `AGENTS.md`.
- **Mitigaciones / Retos:**
  - Debemos portar y verificar con tests unitarios la lógica de autenticación criptográfica BLE (AES challenge-response) de Zepp OS y el parser de bytes de QN-Scale, utilizando como fuente de verdad los drivers probados de Gadgetbridge y openScale.
