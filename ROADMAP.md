# Product Roadmap

[English](ROADMAP.md) | [Español](ROADMAP.es.md)

This roadmap outlines the completed foundational phases and future milestones for **Homeo**, our offline-first personal sports physiology and biometric tracking system.

---

## Completed Milestones

### Phase 1: Core Foundation & Modern Android Stack :white_check_mark:
- Clean Architecture with MVI pattern, Kotlin Coroutines, and `StateFlow`.
- Reactive local persistence powered by Room SQLite 2.6 with indexed schemas.
- Dark OLED visual design system (`#0A0B0D`) adhering to WCAG AA contrast standards.
- Modular navigation graph with bottom navigation bar (Today, Workouts, Progress, Coach).

### Phase 2: Hardware Telemetry & Health Connect :white_check_mark:
- Direct Bluetooth Low Energy (BLE) connection to **Amazfit GTR 3** (Zepp OS 2021 protocol).
- Cryptographic handshake implementation using ECDH Curve B-163 and AES-128 challenge-response.
- Real-time GATT telemetry extraction: live heart rate, daily step counts, battery percentage.
- Android Health Connect integration (`androidx.health.connect:connect-client:1.1.0-alpha11`) importing weight and body fat records from **Renpho Health**.

### Phase 3: Deterministic Sports Physiology Engines :white_check_mark:
- **Acute:Chronic Workload Ratio (ACWR):** Tim Gabbett 4-zone fatigue and injury-prevention gauge.
- **Training Impulse (TRIMP):** Eric Banister session impulse weighting by heart rate reserve.
- **Trend Weight (EMA Filter):** Exponential Moving Average ($\alpha = 0.10$) isolating water/sodium shifts.
- **30-Day Resting Heart Rate Baseline:** Trimmed mean and standard deviation deviation tracker.
- **Habit Correlation Matrix:** Multi-variable Pearson/Spearman impact scoring on next-day recovery.
- **Outdoor GPS Tracking:** Foreground tracking service with offline-cached OpenStreetMap rendering.

### Phase 4: Granularity & Data Integrity :white_check_mark:
- **Interactive Temporal Selectors:** Reactive switching between 7D (Weekly), 30D (Monthly), and 90D (Quarterly) analysis windows with instant comparison recalculations.
- **Biological Weight Scale Filter:** Filtering non-human scale calibrations (< 35 kg or > 250 kg) to maintain data purity.
- **Closed Period Summaries:** Structured retrospectives across Daily, Weekly, Monthly, and Annual periods.
- **Data Portability & Widgets:** Local JSON backup/restore and Android Glance home screen widget.
- **Brand Identity:** Naming and iconography transition to **Homeo** (Homeostasis).

---

## Active & Upcoming Milestones

### Phase 5: Advanced Biometrics & Direct Scale Drivers (In Planning)
- [ ] **Direct Renpho Elis 1 BLE Broadcast Driver:** Standalone reverse-engineered QN-Scale broadcast parsing to read weight and impedance without requiring the companion app or Health Connect.
- [ ] **HRV rMSSD Analysis:** Processing nocturnal raw inter-beat intervals (IBI / RR) to compute Root Mean Square of Successive Differences for autonomic nervous system readiness.
- [ ] **FIT / GPX File Export:** Standardized workout session exports compatible with GoldenCheetah, Strava, and TrainingPeaks.
- [ ] **Multi-Day Offline Map Pre-caching:** Allowing users to download regional map tiles for remote trail runs without cellular reception.

### Phase 6: Wear OS & Wearable Extensibility (Backlog)
- [ ] **Wear OS Companion Tile:** Lightweight Wear OS companion app displaying current Recovery and remaining Strain capacity on wrist tiles.
- [ ] **Standard Bluetooth Smart BLE HRM Heart Rate Profile:** Generic connection to standard BLE chest straps (Polar H10, Garmin HRM-Pro).
- [ ] **Custom Bioimpedance Equations:** User-calibrated lean mass calculations based on Jackson-Pollock skinfold cross-validation.
