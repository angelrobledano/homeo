# Contributing to Homeo

[English](CONTRIBUTING.md) | [Español](CONTRIBUTING.es.md)

Thank you for your interest in contributing to **Homeo**! We welcome contributions from developers worldwide. Our goal is to maintain a high-quality, scientifically sound, offline-first personal health and athletic performance application.

---

## Language & Communication

- **Bilingual Community:** You are welcome to submit Issues, Pull Requests, and join discussions in either **English** or **Spanish**.
- **Code Identifiers:** All source code identifiers (classes, methods, variables, database tables, comments, and commit messages) must remain in **English**.
- **UI User-Facing Text:** User-facing strings in the application default to English (`values/strings.xml`), with full European Spanish localization provided in `values-es/strings.xml`.

---

## Mandatory Bilingual Documentation Policy

To ensure international accessibility while supporting the Spanish-speaking community, **all project documentation is strictly bilingual**:

1. **Dual-Language Parity:** Every documentation file exists in both English and Spanish:
   - Root level: `FILENAME.md` (English) and `FILENAME.es.md` (Spanish).
   - Technical docs: `docs/en/FILENAME.md` and `docs/es/FILENAME.md`.
2. **Synchronization Requirement:** Whenever you introduce a new feature, refactor architecture, adjust database schemas, or update setup steps:
   - Update the English documentation.
   - Update the corresponding Spanish documentation.
   - Verify both versions contain identical structure, depth, and technical specifications.
   - Verify that all internal markdown links resolve correctly.
3. **Pull Request Validation:** Pull Requests that introduce or alter functionality without updating both language versions will be requested to complete the missing translation before merging.

---

## Architectural & Design Principles

All contributions must respect the core tenets established in the project:

1. **UX / DX / AX Alignment:** Every decision must balance User Experience (clarity, honesty, speed), Developer Experience (testability, clean code), and Agent Experience (discoverable truth, modular boundaries).
2. **Rule 1 — Cut Non-Essential Text:** No motivational platitudes, empty marketing taglines, or redundant help labels. Let visual hierarchy and concise metric values speak for themselves.
3. **Deterministic Domain Logic:** Physiological calculations (ACWR, TRIMP, baseline standard deviations, EMA weight trends) must reside in the `domain/` layer with zero dependency on UI or Android platform frameworks.
4. **Offline-First & Zero Leakage:** Never send user health metrics, locations, or tokens to third-party endpoints. All Room database transactions and BLE communications remain on the physical device.

---

## Development Workflow

### 1. Environment Setup

- Install **Android Studio Hedgehog** (2023.1.1) or **Ladybug** (2024.2+).
- Use **JDK 17**.
- Configure Android SDK Platform **API 34** (Android 14) and build tools.

### 2. Branching & Commits

- Branch naming convention:
  - `feat/feature-name`
  - `fix/bug-description`
  - `docs/documentation-update`
  - `refactor/clean-up`
- Commit messages follow Conventional Commits (in English):
  ```text
  feat(trends): implement 90-day workload comparative analysis
  fix(weight): filter anomalous bioimpedance scale taras
  docs(api): update BLE GATT packet decoding documentation
  ```

### 3. Test-Driven Development (TDD)

Every domain algorithm, repository parser, and data transformation must have automated unit test coverage before submission:

```bash
# Run the complete unit test suite
./gradlew testDebugUnitTest

# Assemble debug binary
./gradlew assembleDebug
```

Verify that all tests pass (`BUILD SUCCESSFUL`) with zero warnings or regressions.

---

## Contributing App Translations (i18n)

Homeo is designed as an open-source, global health companion. Adding support for a new language is simple and requires **no Kotlin code changes**:

1. **Locate the base translation file:**
   Open `app/src/main/res/values/strings.xml` (the canonical English source).
2. **Create your language directory:**
   Create a new resource folder following Android's standard locale qualifier `values-<ISO-639-1>` under `app/src/main/res/`:
   ```bash
   # Examples:
   app/src/main/res/values-fr/   # French
   app/src/main/res/values-de/   # German
   app/src/main/res/values-it/   # Italian
   app/src/main/res/values-pt/   # Portuguese
   app/src/main/res/values-ja/   # Japanese
   ```
3. **Copy and translate:**
   Copy `strings.xml` into your new folder and translate each string between the XML tags:
   ```xml
   <!-- Example in values-de/strings.xml -->
   <string name="dash_recovery">ERHOLUNG</string>
   <string name="dash_strain">Belastung</string>
   ```
4. **Verify and submit:**
   Run `./gradlew assembleDebug` to verify that your XML has no syntax errors, then open a Pull Request titled `feat(i18n): add <Language> translation`.

---

## Submitting a Pull Request

1. Fork the repository and create your feature branch from `main`.
2. Ensure your changes compile cleanly and pass all automated tests.
3. Verify that both **English** and **Spanish** documentation files have been updated if applicable.
4. Submit your Pull Request detailing:
   - What problem is solved.
   - Evidence of testing (unit test outputs or device screenshots).
   - Any hardware or permission requirements involved.

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please report any unacceptable behavior to the project maintainers.
