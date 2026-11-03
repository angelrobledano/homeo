package com.salud.personal.domain.coach

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Snapshot del estado de salud del usuario para alimentar al coach.
 * Sin datos personales identificables (cumple privacidad estricta).
 */
data class UserHealthSnapshot(
    val stepsToday: Int,
    val stepGoal: Int = 10_000,
    val recoveryScore: Int, // 0..100
    val restingHeartRate: Int?,
    val baselineRestingHeartRate: Int? = null,
    val sleepHours: Double,
    val recent7DaysTrimp: Double,
    val lastWorkoutType: String?,
    val weightTrendKgDelta: Double?,
    val sleepNeedHours: Double = 7.75,
    val sleepDebtHours: Double = 0.0,
    val currentDayStrain: Double = 0.0,
    val targetStrainMin: Double = 8.0,
    val targetStrainMax: Double = 12.0,
    val acwrRatio: Double = 1.0,
    val isAcwrCalibrated: Boolean = true,
    val rpeRecommendation: Int = 7,
    val targetBedtime: String = "23:15",
    val caffeineCutoffTime: String = "16:00",
    val dinnerCutoffTime: String = "21:00",
    val habitTip: String? = null,
    val habitInsights: List<com.salud.personal.domain.habits.HabitInsight> = emptyList()
)

data class CoachBiometricFactor(
    val label: String,
    val value: String,
    val isLimiting: Boolean
)

data class CoachRecommendation(
    val actionHeadline: String,
    val reasoning: String,
    val supportingData: String,
    val quickAlternative: String,
    val keyFactors: List<CoachBiometricFactor> = emptyList()
)

data class WorkoutPrescription(
    val title: String,
    val targetStrainRange: String,
    val recommendedRpe: String,
    val suggestedActivity: String,
    val rationale: String
)

data class SleepCoachPlan(
    val sleepNeedFormatted: String,
    val sleepDebtFormatted: String,
    val targetBedtime: String,
    val caffeineCutoff: String,
    val dinnerCutoff: String,
    val tip: String
)

data class LoadGuardStatus(
    val acwrRatioText: String,
    val statusText: String,
    val isOverreaching: Boolean,
    val advice: String,
    val isCalibrated: Boolean = true
)

data class QuickFaqItem(
    val id: String,
    val question: String,
    val answer: String
)

data class HabitCorrelationAlert(
    val habitTitle: String,
    val icon: String,
    val impactHeadline: String,
    val metricDeltaText: String,
    val advice: String
)

data class CoachIntelligence(
    val prescription: WorkoutPrescription,
    val sleepPlan: SleepCoachPlan,
    val loadGuard: LoadGuardStatus,
    val quickFaqs: List<QuickFaqItem>,
    val habitCorrelationAlerts: List<HabitCorrelationAlert> = emptyList()
)

object CoachHeuristicEngine {

    /**
     * Motor de reglas determinista offline.
     * Genera la inteligencia completa del coach diario (entreno, sueño, carga y preguntas frecuentes).
     */
    fun generateIntelligence(snapshot: UserHealthSnapshot): CoachIntelligence {
        val rec = generateRecommendation(snapshot)
        val recovery = snapshot.recoveryScore

        // 1. Prescripción de entrenamiento
        val prescription = when {
            recovery >= 67 -> WorkoutPrescription(
                title = "Día de Carga y Rendimiento",
                targetStrainRange = "${String.format(Locale.US, "%.1f", snapshot.targetStrainMin)} - ${String.format(Locale.US, "%.1f", snapshot.targetStrainMax)} pts",
                recommendedRpe = "RPE 8 - 9 (Tolerancia máxima al esfuerzo)",
                suggestedActivity = if (snapshot.lastWorkoutType == "CROSSFIT_WOD" || snapshot.lastWorkoutType == "STRENGTH") {
                    "Fuerza pesada o WOD metabólico. Si hay fatiga muscular previa, opta por carrera/remo en intervalos."
                } else {
                    "WOD de alta intensidad, series de fuerza máxima o levantamientos olímpicos."
                },
                rationale = "Sistema nervioso central y cardiovascular plenamente recuperados para tolerar sobrecarga progresiva."
            )
            recovery >= 34 -> WorkoutPrescription(
                title = "Mantenimiento Aeróbico y Técnica",
                targetStrainRange = "${String.format(Locale.US, "%.1f", snapshot.targetStrainMin)} - ${String.format(Locale.US, "%.1f", snapshot.targetStrainMax)} pts",
                recommendedRpe = "RPE 6 - 7 (Evitar fallo neuromuscular)",
                suggestedActivity = "Cardio constante en Zona 2 (60-70% FC máx) de 35-45 min o fuerza técnica con descansos amplios.",
                rationale = "Capacidad media de asimilación. Un esfuerzo excesivo hoy comprometería la recuperación de mañana."
            )
            else -> WorkoutPrescription(
                title = "Descanso Activo / Regeneración",
                targetStrainRange = "${String.format(Locale.US, "%.1f", snapshot.targetStrainMin)} - ${String.format(Locale.US, "%.1f", snapshot.targetStrainMax)} pts",
                recommendedRpe = "RPE ≤ 5 (Sin fatiga)",
                suggestedActivity = "Caminata suave al aire libre de 20-30 min, estiramientos dinámicos o trabajo de movilidad articular.",
                rationale = "Biomarcadores en zona baja. El cuerpo requiere restaurar el sistema parasimpático antes de volver a cargar."
            )
        }

        // 2. Plan de sueño
        val needMin = (snapshot.sleepNeedHours * 60).roundToInt()
        val needH = needMin / 60
        val needM = needMin % 60
        val debtMin = (snapshot.sleepDebtHours * 60).roundToInt().coerceAtLeast(0)

        val sleepPlan = SleepCoachPlan(
            sleepNeedFormatted = "${needH}h ${needM}m",
            sleepDebtFormatted = if (debtMin > 0) "+${debtMin}m deuda (7d)" else "Sin deuda de sueño",
            targetBedtime = snapshot.targetBedtime,
            caffeineCutoff = snapshot.caffeineCutoffTime,
            dinnerCutoff = snapshot.dinnerCutoffTime,
            tip = if (debtMin > 45) {
                "Arrastras ${debtMin}m de deuda en los últimos 7 días. Se ha compensado parte en tu hora sugerida de hoy para acelerar tu recuperación."
            } else if (debtMin > 15) {
                "Tienes ${debtMin}m de déficit acumulado. Adelanta unos minutos tu descanso de hoy para restaurar el sueño profundo."
            } else {
                "Tu descanso está alineado con tu necesidad biológica. Mantén la regularidad de horario para afianzar el ritmo circadiano."
            }
        )

        // 3. Monitor de carga y sobreentrenamiento
        val loadGuard = if (!snapshot.isAcwrCalibrated) {
            LoadGuardStatus(
                acwrRatioText = "--",
                statusText = "Calibrando Línea Base (<14 días)",
                isOverreaching = false,
                advice = "Se requieren al menos 14 días de entrenamientos acumulados para evaluar con rigor el ratio ACWR (agudo:crónico) sin falsas alarmas.",
                isCalibrated = false
            )
        } else when {
            snapshot.acwrRatio > 1.4 -> LoadGuardStatus(
                acwrRatioText = String.format(Locale.US, "%.2f", snapshot.acwrRatio),
                statusText = "Alerta de Sobrecarga (Pico Agudo)",
                isOverreaching = true,
                advice = "El volumen de los últimos 7 días creció un 40%+ sobre tu promedio mensual. Reduce el volumen en las próximas 48h para evitar lesiones.",
                isCalibrated = true
            )
            snapshot.acwrRatio in 0.8..1.3 -> LoadGuardStatus(
                acwrRatioText = String.format(Locale.US, "%.2f", snapshot.acwrRatio),
                statusText = "Zona Óptima de Carga",
                isOverreaching = false,
                advice = "Tu progresión de entrenamiento está perfectamente calibrada con tu capacidad construida en las últimas 4 semanas.",
                isCalibrated = true
            )
            else -> LoadGuardStatus(
                acwrRatioText = String.format(Locale.US, "%.2f", snapshot.acwrRatio),
                statusText = "Descarga / Carga Baja",
                isOverreaching = false,
                advice = "Volumen bajo de esfuerzo. Buen momento para acumular sesiones de calidad sin riesgo de fatiga residual.",
                isCalibrated = true
            )
        }

        // 4. Preguntas clave con respuesta inmediata
        val quickFaqs = listOf(
            QuickFaqItem(
                id = "train_today",
                question = "¿Debo entrenar hoy o descansar?",
                answer = answerQuestion("¿Debo entrenar hoy o descansar?", snapshot)
            ),
            QuickFaqItem(
                id = "bedtime",
                question = "¿A qué hora debo acostarme hoy?",
                answer = "Tu necesidad total para esta noche es de ${needH}h ${needM}m${if (debtMin > 0) " (incluye ${debtMin}m de deuda)" else ""}. Para despertar descansado a tu hora habitual, acuéstate a las ${snapshot.targetBedtime}. Cena antes de las ${snapshot.dinnerCutoffTime} para evitar picos de FC nocturna."
            ),
            QuickFaqItem(
                id = "intensity",
                question = "¿Puedo entrenar al fallo o alta intensidad?",
                answer = answerQuestion("¿Puedo entrenar a RPE 9 o fallo?", snapshot)
            ),
            QuickFaqItem(
                id = "why_recovery",
                question = "¿Por qué mi recuperación es del ${snapshot.recoveryScore}%?",
                answer = answerQuestion("¿Por qué mi recuperación es ${snapshot.recoveryScore}%?", snapshot)
            )
        )

        val habitAlerts = snapshot.habitInsights.filter {
            it.impact == com.salud.personal.domain.habits.HabitImpact.NEGATIVE || it.rhrDelta >= 1.5 || it.recoveryDelta <= -4
        }.map { insight ->
            val deltaParts = mutableListOf<String>()
            if (insight.recoveryDelta != 0) {
                val sign = if (insight.recoveryDelta > 0) "+${insight.recoveryDelta}" else "${insight.recoveryDelta}"
                deltaParts.add("$sign% recuperación")
            }
            if (insight.rhrDelta > 0) {
                deltaParts.add("+${insight.rhrDelta} lpm FC reposo")
            }
            if (insight.restorativeSleepDeltaPct != null && insight.restorativeSleepDeltaPct != 0) {
                val sign = if (insight.restorativeSleepDeltaPct > 0) "+${insight.restorativeSleepDeltaPct}" else "${insight.restorativeSleepDeltaPct}"
                deltaParts.add("$sign% sueño profundo/REM")
            }
            val deltaStr = deltaParts.joinToString(" · ")

            val advice = when (insight.habit) {
                com.salud.personal.domain.habits.HabitType.LATE_MEAL ->
                    "Cenar al menos 2 horas antes de acostarse reduce la actividad simpática gástrica y estabiliza el pulso basal nocturno."
                com.salud.personal.domain.habits.HabitType.LATE_CAFFEINE ->
                    "La vida media de la cafeína es de 5 a 7 horas. Suspender su ingesta después de las ${snapshot.caffeineCutoffTime} protege el descanso."
                com.salud.personal.domain.habits.HabitType.ALCOHOL ->
                    "El alcohol fragmenta la arquitectura REM y acelera la frecuencia cardíaca durante la primera mitad de la noche."
                com.salud.personal.domain.habits.HabitType.HIGH_STRESS ->
                    "El estrés elevado mantiene alto el tono simpático. 10 minutos de respiración lenta o estiramientos facilitan la entrada en vagotonía."
                com.salud.personal.domain.habits.HabitType.SCREEN_BEFORE_BED ->
                    "La luz de pantallas inhibe la secreción de melatonina. Activar filtro de noche o suspenderlas 30 min antes ayuda al inicio del sueño."
                else -> insight.insightSummary
            }

            HabitCorrelationAlert(
                habitTitle = insight.habit.title,
                icon = insight.habit.icon,
                impactHeadline = "Patrón detectado en tu diario",
                metricDeltaText = deltaStr.ifEmpty { insight.insightSummary },
                advice = advice
            )
        }

        return CoachIntelligence(
            prescription = prescription,
            sleepPlan = sleepPlan,
            loadGuard = loadGuard,
            quickFaqs = quickFaqs,
            habitCorrelationAlerts = habitAlerts
        )
    }

    /**
     * Motor de reglas determinista offline (compatibilidad).
     */
    fun generateRecommendation(snapshot: UserHealthSnapshot): CoachRecommendation {
        val factors = mutableListOf<CoachBiometricFactor>()

        // Evaluación de sueño
        if (snapshot.sleepHours < 6.5) {
            factors.add(CoachBiometricFactor("Sueño", "${String.format(Locale.US, "%.1f", snapshot.sleepHours)}h (Deuda)", isLimiting = true))
        } else if (snapshot.sleepHours >= 7.5) {
            factors.add(CoachBiometricFactor("Sueño", "${String.format(Locale.US, "%.1f", snapshot.sleepHours)}h (Óptimo)", isLimiting = false))
        } else {
            factors.add(CoachBiometricFactor("Sueño", "${String.format(Locale.US, "%.1f", snapshot.sleepHours)}h (Aceptable)", isLimiting = false))
        }

        // Evaluación de FC en reposo contra línea base personal
        snapshot.restingHeartRate?.let { rhr ->
            val baseline = snapshot.baselineRestingHeartRate ?: 50
            val delta = rhr - baseline
            if (delta >= 5) {
                factors.add(CoachBiometricFactor("RHR", "$rhr bpm (+${delta} lpm sobre base)", isLimiting = true))
            } else {
                factors.add(CoachBiometricFactor("RHR", "$rhr bpm (Estable en base)", isLimiting = false))
            }
        }

        // Evaluación de carga TRIMP acumulada
        if (snapshot.recent7DaysTrimp > 320.0) {
            factors.add(CoachBiometricFactor("Carga 7d", "${snapshot.recent7DaysTrimp.toInt()} TRIMP (Alta)", isLimiting = true))
        } else {
            factors.add(CoachBiometricFactor("Carga 7d", "${snapshot.recent7DaysTrimp.toInt()} TRIMP (Asimilada)", isLimiting = false))
        }

        return when {
            // Caso 1: Recuperación roja (< 34%) o fatiga extrema
            snapshot.recoveryScore < 34 -> {
                CoachRecommendation(
                    actionHeadline = "Día de descarga o recuperación activa",
                    reasoning = "Tu nivel de recuperación está comprometido. Priorizar descanso previene sobreentrenamiento y lesiones.",
                    supportingData = "Recuperación: ${snapshot.recoveryScore}% · Sueño: ${String.format(Locale.US, "%.1f", snapshot.sleepHours)}h · Carga 7d: ${snapshot.recent7DaysTrimp.toInt()} TRIMP",
                    quickAlternative = "Caminata suave de 20-30 min a paso ligero para activar circulación sin fatiga neuromuscular.",
                    keyFactors = factors
                )
            }

            // Caso 2: Recuperación moderada (34-66%)
            snapshot.recoveryScore in 34..66 -> {
                val remainingSteps = (snapshot.stepGoal - snapshot.stepsToday).coerceAtLeast(0)
                CoachRecommendation(
                    actionHeadline = "Sesión de intensidad moderada o trabajo técnico",
                    reasoning = "Tu cuerpo puede tolerar carga moderada, pero evita ir al fallo en WODs de alta intensidad cardiovascular.",
                    supportingData = "Recuperación: ${snapshot.recoveryScore}% · Pasos pendientes: $remainingSteps",
                    quickAlternative = "Completa tu meta de pasos o realiza 30 min de trabajo de movilidad y fuerza básica RPE 6-7.",
                    keyFactors = factors
                )
            }

            // Caso 3: Recuperación verde (≥ 67%)
            else -> {
                CoachRecommendation(
                    actionHeadline = "Óptimo para WOD intenso o entrenamiento de alta demanda",
                    reasoning = "Tus biomarcadores indican plena recuperación del sistema nervioso central y descanso adecuado.",
                    supportingData = "Recuperación: ${snapshot.recoveryScore}% · FC Reposo óptima${snapshot.restingHeartRate?.let { " ($it bpm)" } ?: ""}",
                    quickAlternative = "Si dispones de poco tiempo, realiza un WOD tipo sprint de 12-15 minutos (ej. intervalos aeróbicos Z4/Z5).",
                    keyFactors = factors
                )
            }
        }
    }

    /**
     * Responde preguntas fisiológicas offline de forma contextual a los biomarcadores del usuario.
     */
    fun answerQuestion(question: String, snapshot: UserHealthSnapshot): String {
        val q = question.lowercase()
        val recovery = snapshot.recoveryScore
        val sleepStr = String.format(Locale.US, "%.1f", snapshot.sleepHours)

        return when {
            q.contains("entrenar") || q.contains("descansar") || q.contains("hacer hoy") -> {
                when {
                    recovery >= 67 -> "Tus biomarcadores avalan entrenar con intensidad hoy. Tu recuperación es del $recovery% y el objetivo de esfuerzo recomendado es de ${String.format(Locale.US, "%.1f", snapshot.targetStrainMin)} a ${String.format(Locale.US, "%.1f", snapshot.targetStrainMax)} pts. Si vas a entrenar fuerza o WOD, puedes trabajar a RPE 8-9."
                    recovery >= 34 -> "Puedes entrenar hoy, pero moderando la intensidad. Con una recuperación del $recovery%, prioriza trabajo técnico o cardio continuo en Zona 2 aeróbica a RPE 6-7. Evita series al fallo o entrenamientos dobles."
                    else -> "Hoy se aconseja priorizar descanso o recuperación activa ligera (caminata suave de 20-30 min). Con una recuperación del $recovery%, una sesión exigente aumentaría el riesgo de sobrecarga sin producir adaptaciones positivas."
                }
            }
            q.contains("fuerza") || q.contains("cardio") -> {
                when {
                    recovery >= 67 -> "Con recuperación del $recovery%, el SNC está en estado óptimo para levantamiento de fuerza pesado con descansos completos (2-3 min) o intervalos de cardio anaeróbico de alta intensidad."
                    recovery >= 34 -> "Tu recuperación es moderada ($recovery%). Se recomienda fuerza técnica en rangos de 8-12 repeticiones a RPE 6-7, o cardio en Zona 2 aeróbica de 30-40 min evitando agotamiento."
                    else -> "Con recuperación baja ($recovery%), evita cargas máximas axiales y cardio láctico. Enfócate en movilidad articular suave, estiramientos y caminata ligera."
                }
            }
            q.contains("sueño") || q.contains("dormir") || q.contains("cansad") || q.contains("acostarme") -> {
                "Registraste ${sleepStr}h de descanso. Una siesta breve de 20 min antes de las 15:00 reduce la presión de adenosina sin inhibir el sueño nocturno. Tu hora límite sugerida para ir a la cama hoy es ${snapshot.targetBedtime}. Suspende cafeína después de las ${snapshot.caffeineCutoffTime} y evita pantallas en la cama."
            }
            q.contains("rpe") || q.contains("fallo") || q.contains("intenso") || q.contains("duro") -> {
                if (recovery >= 67) {
                    "Sí, tus biomarcadores toleran una sesión demandante RPE 8-9 con entrada en calor progresiva."
                } else {
                    "No recomendable hoy. Con $recovery% de recuperación, trabajar a RPE 8-9 eleva de forma exponencial el riesgo de fatiga neural residual y sobrecarga articular."
                }
            }
            q.contains("por qué") || q.contains("por que") || q.contains("puntuación") || q.contains("recuperación") -> {
                "Tu recuperación del $recovery% integra: descanso nocturno (${sleepStr}h), FC basal${snapshot.restingHeartRate?.let { " ($it bpm)" } ?: ""} y fatiga acumulada de ${snapshot.recent7DaysTrimp.toInt()} pts TRIMP en los últimos 7 días."
            }
            else -> {
                "Basado en tu recuperación actual de $recovery% y carga acumulada de ${snapshot.recent7DaysTrimp.toInt()} pts, la recomendación principal es apuntar a un esfuerzo de ${String.format(Locale.US, "%.1f", snapshot.targetStrainMin)}-${String.format(Locale.US, "%.1f", snapshot.targetStrainMax)} pts modulando la intensidad."
            }
        }
    }
}
