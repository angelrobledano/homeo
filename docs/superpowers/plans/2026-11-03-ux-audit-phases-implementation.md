# Implementación de Mejoras UX/UI: Fases 1, 2 (1 y 3) y 3

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar de forma integral las mejoras de UX/UI y producto derivadas de la auditoría: Fase 1 (unificación de peso/composición, chips inteligentes en Coach, accesibilidad táctil 48dp, conmutador en Progreso), Fase 2 (Hero Card de estado diario y scrubbing interactivo en gráficos) y Fase 3 (correlaciones causa-efecto hábitos-biomarcadores y dashboard temporal adaptativo mañana/tarde/noche).

**Architecture:** Módulos Compose desacoplados con contratos de estado inmutables, extensiones de cálculo fisiológico determinista en el paquete `domain` y reactividad local Room/StateFlow sin llamadas de red externas.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room DB, JUnit4.

**Spec:** Auditoría integral UX/UI y Fases 1, 2 (1 y 3) y 3 aprobadas por el usuario.

## Global Constraints
- Idioma estricto: Español de España (`es_ES`).
- Cero dependencias de red o analíticas externas; funcionamiento local-first.
- Respetar WCAG 2.1 AA (mínimo 48dp en zonas táctiles).
- Mantener la paleta OLED WhoopColors (`#0A0B0D`, `#141519`, `#00E5FF`, `#00E676`, etc.).

---

### Task 1: Fase 1 — Fusión de Peso y Composición Corporal
- [ ] Crear componente unificado `WeightSummaryCard` en `DashboardScreen.kt`.
- [ ] Eliminar la separación redundante de `WeightCard` y `BodyCompositionCard`.
- [ ] Probar compilación y verificar renderizado sin regresiones.

### Task 2: Fase 1 — Chips de Consultas Inteligentes en Coach
- [ ] Reemplazar `FreeformConsultationCard` en `CoachScreen.kt` por selector de consultas contextuales en 1 toque.
- [ ] Enlazar respuestas directas con `CoachHeuristicEngine.answerQuestion`.
- [ ] Verificar que no hay cajas de texto libre que prometan un LLM irreal.

### Task 3: Fase 1 — Accesibilidad Táctil (48dp) y Conmutador en Progreso
- [ ] Revisar botones de navegación temporal en `PeriodSummaryScreen.kt` y `CalendarScreen.kt` garantizando 48dp.
- [ ] Reemplazar el banner estático de "Resúmenes y Balances Cerrados" en `ProgressScreen.kt` por una pestaña superior compacta o botón integrado.
- [ ] Limpiar la cabecera superior del Dashboard.

### Task 4: Fase 2 — Hero Card "Estado del Día" en Dashboard
- [ ] Diseñar e implementar `DayStateHeroCard` en `DashboardScreen.kt`.
- [ ] Sintetizar Recuperación (%), Semáforo, Esfuerzo (Strain vs Target), Sueño y FC en reposo en los primeros 220dp.
- [ ] Reorganizar el scroll vertical del Dashboard para eliminar tarjetas redundantes.

### Task 5: Fase 2 — Interactividad en Gráficos (Scrubbing / Puntero Táctil)
- [ ] Añadir scrubbing interactivo en el histograma de pasos horarios de `ActivityDetailScreen.kt`.
- [ ] Añadir scrubbing o inspección táctil en el gráfico de peso/RHR de `ProgressScreen.kt`.
- [ ] Verificar fluidez táctil sin interferir con el scroll vertical.

### Task 6: Fase 3 — Motor de Correlaciones Causa-Efecto (Hábitos vs Biomarcadores)
- [ ] Extender `CoachHeuristicEngine.kt` para evaluar cómo los hábitos del check-in (alcohol, cafeína, cena tardía, estrés) impactan en la FC en reposo y el sueño.
- [ ] Añadir tarjeta de correlaciones causa-efecto en `CoachScreen.kt`.
- [ ] Escribir tests unitarios para las correlaciones deterministas.

### Task 7: Fase 3 — Dashboard Adaptativo (Modo Mañana / Tarde / Noche)
- [ ] Crear enum `DashboardTimeMode` (MORNING, DAYTIME, EVENING).
- [ ] Adaptar la jerarquía visual del Dashboard según la hora actual del sistema.
- [ ] Probar modos y verificar coherencia en todos los horarios.

### Task 8: Verificación Completa y Compilación
- [ ] Ejecutar `./gradlew testDebugUnitTest` y `./gradlew assembleDebug`.
- [ ] Documentar los cambios en el resumen de entrega.
