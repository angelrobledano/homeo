# Frequently Asked Questions (FAQ)

[English](FAQ.md) | [Español](../es/FAQ.md)

This document answers common technical, physiological, and operational questions about Homeo.

---

## General Questions

### What is Homeo and what problem does it solve?
Homeo is a private, offline-first personal biometric and sports physiology tracking app for Android. Inspired by products like WHOOP and MacroFactor, it unifies data from smartwatches (Amazfit GTR 3) and bioimpedance scales (Renpho) into a dark OLED cockpit. It replaces noisy commercial apps with peer-reviewed athletic load monitoring (Gabbett ACWR, Banister TRIMP, EMA trend weight).

### Is Homeo free and open source?
Yes. Homeo is open source under the Apache 2.0 license. There are no subscriptions, paywalls, premium tiers, or hidden in-app purchases.

### Does Homeo require an internet connection?
No. Homeo is engineered to be 100% offline-first. Bluetooth LE communication, Health Connect synchronization, GPS workout tracking, and Room database persistence execute entirely on your Android hardware without relying on external servers.

---

## Physiological Metrics & Algorithms

### What is ACWR (Acute:Chronic Workload Ratio)?
ACWR, developed by sports scientist Dr. Tim Gabbett, evaluates the relationship between your acute training fatigue (the exertion you accumulated over the last 7 days) and your chronic fitness capacity (your average weekly load over the last 28 days).
- **Ratio < 0.8 (Under-training / Deload):** Training load is significantly below historical baseline. Suitable for active recovery or tapering.
- **Ratio 0.8 – 1.3 (Optimal Sweet Spot):** The physiological ideal where fitness improves while injury risk remains lowest.
- **Ratio 1.3 – 1.5 (High Risk / Caution):** Rapid workload ramp-up. Intensifies residual systemic fatigue.
- **Ratio > 1.5 (Danger Zone):** An acute spike exceeding chronic capacity by over 50%, historically correlated with a sharp increase in soft-tissue injury risk.

### What is TRIMP and how does it work?
TRIMP (Training Impulse), developed by Dr. Eric Banister, quantifies cardiovascular workout load. Unlike simple duration or calorie estimates, TRIMP factors in:
1. Session duration in minutes.
2. Fractional Heart Rate Reserve ($\Delta HR = \frac{HR_{avg} - HR_{rest}}{HR_{max} - HR_{rest}}$).
3. An exponential weighting coefficient ($y = e^{1.92 \cdot \Delta HR}$ for men, $estat = e^{1.67 \cdot \Delta HR}$ for women) that reflects the non-linear physiological stress of high-intensity efforts.

### How does Trend Weight (EMA) differ from my scale's reading?
Daily spot weigh-ins fluctuate by $\pm 1$–$2$ kg due to hydration, sodium intake, and muscle glycogen storage (1 gram of glycogen binds 3–4 grams of water). Homeo implements an Exponential Moving Average (EMA) with smoothing factor $\alpha = 0.10$. This mathematical filter absorbs transient water noise, revealing your true biological rate of adipose and lean mass change.

### What is the "Biological Weight Filter"?
Commercial scales occasionally register accidental taras (such as a child, pet, or backpack resting on the scale, or calibration re-zeros under 35 kg). Homeo automatically filters out measurements outside the physiological human range of 35 kg to 250 kg so that anomalous weigh-ins do not hijack your primary metrics or delta calculations.

### What does "In Baseline" mean on the Vitals card?
Rather than comparing your vitals against generic population averages, Homeo evaluates whether your Resting Heart Rate, Sleep Duration, and Weight fall within your individual physiological baseline (rolling 30-day mean $\pm$ standard deviation tolerance).

---

## Hardware & Troubleshooting

### How do I extract the 16-byte Auth Key for my Amazfit GTR 3?
The Auth Key can be extracted using:
1. **Gadgetbridge:** Follow the [official Gadgetbridge Huami pairing instructions](https://gadgetbridge.org/basics/pairing/huami/) to pair once and retrieve the 32-character hexadecimal key.
2. **Zepp Cloud Export / Modified Zepp:** Extract the key from local app database logs (`/data/data/com.huami.watch.hmwatchmanager/databases/`).
3. Place your credentials in `secrets.properties` in the project root:
   ```properties
   WATCH_MAC=XX:XX:XX:XX:XX:XX
   WATCH_AUTH_KEY=0123456789abcdef0123456789abcdef
   ```

### Why aren't my Renpho scale measurements appearing?
1. Ensure the **Health Connect** app is installed (built into Android 14+ or available via Google Play on Android 9–13).
2. Open **Homeo > Settings > Health Connect** and grant permissions for `Weight` and `Body Fat`.
3. Ensure the Renpho Health app has permission to write to Health Connect and that you have completed at least one weigh-in.

### Can I use Homeo without a smartwatch or scale?
Yes. Homeo supports manual workout entry (with RPE load estimation), subjective morning recovery check-ins, manual GPS walk tracking, and local data viewing without requiring any connected BLE accessories.
