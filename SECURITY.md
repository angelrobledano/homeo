# Security Policy

[English](SECURITY.md) | [Español](SECURITY.es.md)

Homeo is an application designed to process sensitive biometric, physical activity, and health data. Security and confidentiality are built into the system architecture from the ground up.

---

## Supported Versions

Security updates are actively applied to the latest development branch on the main repository.

| Version / Branch | Supported |
| :--- | :--- |
| `master` (Latest Release) | :white_check_mark: Yes |
| Older releases | :x: No |

---

## Security Architecture & Data Handling

1. **Zero Remote Exfiltration:** Homeo is completely local. It has no backend API, no analytics SDKs, and no telemetry tracking. Health telemetry never leaves the Android device.
2. **Local Credential Protection:** Wearable device keys (such as the 16-byte Huami BLE Auth Key) are isolated in `secrets.properties` and local Room database storage. They are excluded from version control via `.gitignore`.
3. **Android Sandbox & Permissions:** Homeo requests only the minimal set of Android runtime permissions required for Bluetooth LE connectivity, foreground workout tracking, and local Health Connect access. Permissions can be revoked at any time from Android Settings.
4. **Data Portability:** User backups are exported to human-readable JSON files saved locally through Android's secure `FileProvider`, giving the user full inspection and custody of their data.

---

## Reporting a Vulnerability

We take the security of personal data seriously. If you discover a security vulnerability, we appreciate your help in responsibly disclosing it to us.

### How to Report

- **Do NOT file a public GitHub issue** for undisclosed security vulnerabilities.
- Please email the maintainers directly or use GitHub's private vulnerability reporting feature.
- Include in your report:
  - Type and description of the vulnerability.
  - Clear step-by-step instructions or proof-of-concept to reproduce the issue.
  - Affected device configurations or Android versions.
  - Potential impact on local data integrity or authorization boundaries.

### Our Response Process

1. We will acknowledge receipt of your vulnerability report within 48 hours.
2. We will validate the issue and coordinate a patch on a private development branch.
3. Once the fix is verified and released, we will publicly credit your contribution (unless you request anonymity).
