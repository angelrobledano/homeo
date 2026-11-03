# Privacy Policy & Data Protection Architecture

[English](PRIVACY.md) | [Español](../es/PRIVACY.md)

At Homeo, privacy is an architectural condition, not a secondary compliance task. This document details what health data is processed, how it is secured, and why none of your personal information ever leaves your device.

---

## 1. Fundamental Privacy Tenets

- **100% Offline-First:** Homeo has no backend servers, no cloud database, and no telemetry services. Your data stays on your hardware.
- **Zero Third-Party SDKs:** No Google Firebase, no Meta Pixel, no Mixpanel, no crash reporters, and no advertising trackers.
- **Data Minimization:** We request only the precise permissions required to communicate with hardware you explicitly configure.
- **Complete Data Custody:** You can export your full database to sanitized JSON or erase it completely with a single tap in the application settings.

---

## 2. Categories of Health & Physical Data Processed

Homeo processes the following biometric and physical performance telemetry:

1. **Body Composition:** Body weight, bioelectrical impedance, and body fat percentage (ingested from scales via Health Connect).
2. **Cardiovascular Metrics:** Continuous heart rate, resting heart rate (RHR), and heart rate reserve zone distribution.
3. **Daily Physical Activity:** Step counts, active calorie burn, distance traveled, and cadence.
4. **Workout & GPS Tracking:** Exercise type, start/end timestamps, Training Impulse (TRIMP), Rate of Perceived Exertion (RPE), and GPS route coordinates during outdoor sessions.
5. **Sleep & Circadian Rhythms:** Sleep duration, sleep deficit calculations (sleep debt), and subjective rest ratings.
6. **Habits & Journal Tags:** User-logged factors (e.g., caffeine, alcohol, sauna, late meals, supplements) used exclusively for statistical correlation analysis.

---

## 3. Data Storage & Retention

- **Storage Location:** All data is persisted inside an encrypted/private SQLite database managed by Android Room, located at:
  `/data/data/com.salud.personal/databases/salud_personal_database`
- **Device Key Isolation:** Bluetooth MAC addresses and Huami cryptographic authentication keys are stored locally in application-private SharedPreferences / Room tables and local build properties. They are never transmitted over the internet.
- **Retention Period:** Data remains on the device indefinitely until the user uninstalls the app or triggers the local database purge function in the Settings screen.

---

## 4. Android Permissions Audit & Rationale

| Android Permission | Purpose & Scope | Can be Revoked? |
| :--- | :--- | :--- |
| `android.permission.BLUETOOTH_CONNECT` | Connects directly via BLE GATT to Amazfit GTR 3 smartwatches for sensor streaming. | Yes |
| `android.permission.BLUETOOTH_SCAN` | Discovers configured BLE devices during pairing and automatic reconnection routines. | Yes |
| `android.permission.ACCESS_FINE_LOCATION` | Required by Android for BLE scanning (API $\le$ 30) and high-accuracy GPS workout route mapping. | Yes |
| `android.permission.FOREGROUND_SERVICE` | Keeps the GPS tracking engine active when the screen is locked during outdoor runs. | Yes |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | Explicit foreground service type for continuous location updates (Android 14+ requirement). | Yes |
| `android.permission.health.READ_WEIGHT` | Reads scale measurements imported by companion apps (e.g., Renpho Health) in Health Connect. | Yes |
| `android.permission.health.READ_BODY_FAT` | Reads body fat percentage estimates from Health Connect. | Yes |

---

## 5. User Rights: Export, Backup & Deletion

- **Local JSON Export:** You can export your entire biometric history as a portable, human-readable JSON file via Android's secure `FileProvider`.
- **Database Purge:** A single action in **Settings > Data Management** permanently drops and recreates all Room SQLite tables, erasing all history from the device.
- **Zero Account Lock-in:** Homeo requires no user accounts, no passwords, and no cloud logins.

---

## 6. Non-Medical Product Disclaimer

Homeo is designed solely as a personal sports performance and fitness tracking tool inspired by sports science (Banister TRIMP, Gabbett ACWR). 

**Homeo is not a medical device.** It does not provide medical advice, diagnosis, treatment, or clinical monitoring. It should never substitute for the judgment of a qualified healthcare professional. Fluctuations in biometrics (resting heart rate, recovery scores, weight trends) are physiological estimates and should not be construed as clinical diagnoses.
