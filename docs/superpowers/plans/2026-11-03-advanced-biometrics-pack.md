# Advanced Biometrics Pack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 4 core high-utility biometrics features matching leading apps (Whoop/Athlytic/Oura) without fluff: 7-day cumulative sleep debt, quick manual workout logging with sRPE-TRIMP, closed periodic performance summaries (Day/Week/Month/Quarter/Year) with time navigation, and native Android home screen glanceable widget + local data export.

**Architecture:** Offline-first native Android (Jetpack Compose, Room SQLite, Health Connect). Domain layer deterministic calculations backed by unit tests (TDD). Clean UI with OLED dark theme `#0A0B0D` and semantic Whoop colors.

**Tech Stack:** Kotlin, Jetpack Compose, Room DB, Android AppWidget / RemoteViews or Glance, JUnit 4, Kotlinx Coroutines.

---

## Global Constraints
- Target device: Google Pixel 7a (Android 14/15, API 34+).
- OLED true black `#0A0B0D` background, zero white flashes, min 48dp touch targets.
- All copy in Spanish (`es_ES`), clean, strictly metric and physiological, zero motivational filler.
- Idempotent data handling, zero fake numbers or unverified mock data.

---

### Task 1: Cumulative 7-Day Sleep Debt Engine (`SleepDebtCalculator` & UI Integration)

**Files:**
- Create: `app/src/main/java/com/salud/personal/domain/sleep/SleepDebtCalculator.kt`
- Create: `app/src/test/java/com/salud/personal/domain/sleep/SleepDebtCalculatorTest.kt`
- Modify: `app/src/main/java/com/salud/personal/domain/coach/CoachEngine.kt`
- Modify: `app/src/main/java/com/salud/personal/ui/coach/CoachScreen.kt`
- Modify: `app/src/main/java/com/salud/personal/ui/sleep/SleepDetailScreen.kt`

**Interfaces:**
- Produces: `SleepDebtResult(sleepNeedHours: Double, cumulativeDebtMinutes: Int, dailyCompMinutes: Int, debtStatus: SleepDebtStatus)`
- Function: `SleepDebtCalculator.calculateDebt(recentSleeps: List<SleepSessionEntity>, targetSleepHours: Double = 8.0): SleepDebtResult`

- [ ] **Step 1: Write unit tests in `SleepDebtCalculatorTest.kt` covering exact debt calculation, zero debt, surplus, and max daily compensation capping (+45m).**
- [ ] **Step 2: Run tests to verify failure.**
- [ ] **Step 3: Implement `SleepDebtCalculator.kt`.**
- [ ] **Step 4: Run tests to verify all pass.**
- [ ] **Step 5: Integrate sleep debt into `CoachEngine.kt` and display in `CoachScreen` (Planificador de descanso) and `SleepDetailScreen`.**

---

### Task 2: Quick Manual Workout Logging (`ManualWorkoutDialog` & sRPE-TRIMP)

**Files:**
- Create: `app/src/main/java/com/salud/personal/domain/strain/ManualWorkoutEstimator.kt`
- Create: `app/src/test/java/com/salud/personal/domain/strain/ManualWorkoutEstimatorTest.kt`
- Create: `app/src/main/java/com/salud/personal/ui/workout/ManualWorkoutDialog.kt`
- Modify: `app/src/main/java/com/salud/personal/ui/workout/WorkoutScreen.kt`
- Modify: `app/src/main/java/com/salud/personal/ui/workout/WorkoutViewModel.kt`

**Interfaces:**
- Produces: `ManualWorkoutEstimator.estimateTrimp(type: WorkoutType, durationMinutes: Int, rpe: Int): Double`
- Produces: `WorkoutViewModel.addManualWorkout(type: WorkoutType, name: String, durationMinutes: Int, rpe: Int, notes: String?)`

- [ ] **Step 1: Write unit tests for `ManualWorkoutEstimatorTest.kt` validating TRIMP estimation for CrossFit, Strength, Running based on duration and RPE 1-10.**
- [ ] **Step 2: Run tests to verify failure.**
- [ ] **Step 3: Implement `ManualWorkoutEstimator.kt`.**
- [ ] **Step 4: Run tests to verify pass.**
- [ ] **Step 5: Implement `ManualWorkoutDialog.kt` with clean OLED sliders for Duration and RPE, and wire into `WorkoutScreen.kt` and `WorkoutViewModel.kt`.**

---

### Task 3: Closed Periodic Summary Engine & Screen (`PeriodSummaryCalculator` & `PeriodSummaryScreen`)

**Files:**
- Create: `app/src/main/java/com/salud/personal/domain/summary/PeriodSummaryCalculator.kt`
- Create: `app/src/main/java/com/salud/personal/domain/summary/PeriodSummaryModels.kt`
- Create: `app/src/test/java/com/salud/personal/domain/summary/PeriodSummaryCalculatorTest.kt`
- Create: `app/src/main/java/com/salud/personal/ui/summary/PeriodSummaryScreen.kt`
- Create: `app/src/main/java/com/salud/personal/ui/summary/PeriodSummaryViewModel.kt`
- Modify: `app/src/main/java/com/salud/personal/ui/navigation/MainNavHost.kt`
- Modify: `app/src/main/java/com/salud/personal/ui/trends/ProgressScreen.kt`

**Interfaces:**
- Produces: `PeriodSummary(periodType: PeriodType, startDate: LocalDate, endDate: LocalDate, label: String, totalWorkouts: Int, totalTrimp: Double, optimalDays: Int, overstrainDays: Int, avgRecovery: Int?, avgSleepHours: Double?, totalSteps: Int, avgSteps: Int, weightDeltaKg: Double?, peakStrainDay: LocalDate?, peakStrainValue: Double?, peakRecoveryDay: LocalDate?, peakRecoveryValue: Int?)`
- Function: `PeriodSummaryCalculator.computeSummary(periodType: PeriodType, referenceDate: LocalDate, workouts: List<WorkoutSessionEntity>, activities: List<DailyActivityEntity>, sleeps: List<SleepSessionEntity>, checkIns: List<DailyCheckInEntity>, weights: List<WeightMeasurementEntity>): PeriodSummary`

- [ ] **Step 1: Write unit tests in `PeriodSummaryCalculatorTest.kt` covering Day, Week, Month, Quarter, and Year bounds and aggregations.**
- [ ] **Step 2: Run tests to verify failure.**
- [ ] **Step 3: Implement `PeriodSummaryModels.kt` and `PeriodSummaryCalculator.kt`.**
- [ ] **Step 4: Run tests to verify pass.**
- [ ] **Step 5: Implement `PeriodSummaryViewModel.kt` and `PeriodSummaryScreen.kt` with period chips and forward/backward date navigation.**
- [ ] **Step 6: Add navigation entry in `MainNavHost.kt` and direct access button in `ProgressScreen.kt`.**

---

### Task 4: Home Screen Glanceable Widget & Local Data Export

**Files:**
- Create: `app/src/main/java/com/salud/personal/ui/widget/SaludAppWidgetProvider.kt`
- Create: `app/src/main/res/layout/widget_salud_glance.xml`
- Create: `app/src/main/res/xml/salud_appwidget_info.xml`
- Create: `app/src/main/java/com/salud/personal/domain/export/DataExporter.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/salud/personal/ui/settings/SettingsScreen.kt`

**Interfaces:**
- Produces: `SaludAppWidgetProvider` showing current Recovery %, Strain vs Target, and daily steps.
- Produces: `DataExporter.exportToJson(context: Context, database: AppDatabase): File`

- [ ] **Step 1: Create widget layout XML `widget_salud_glance.xml` with dark OLED theme, progress indicators, and compact biometrics.**
- [ ] **Step 2: Implement `SaludAppWidgetProvider.kt` querying the Room database directly to populate the widget.**
- [ ] **Step 3: Register widget in `AndroidManifest.xml`.**
- [ ] **Step 4: Implement `DataExporter.kt` to serialize user Room records into an exportable JSON file.**
- [ ] **Step 5: Add export button and share intent in `SettingsScreen.kt`.**

---

### Task 5: Hardware Verification on Google Pixel 7a
- [ ] **Step 1: Run full unit test suite `./gradlew testDebugUnitTest`.**
- [ ] **Step 2: Deploy to Pixel 7a `./gradlew installDebug`.**
- [ ] **Step 3: Capture ADB screenshots of Sleep Debt, Manual Workout dialog, Periodic Summary Screen, and Settings Export.**
- [ ] **Step 4: Verify touch targets, animations, and typography.**
