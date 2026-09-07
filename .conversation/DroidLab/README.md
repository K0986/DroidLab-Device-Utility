# DroidLab

DroidLab is an advanced Android device information, diagnostics, monitoring, and utility toolkit. It is built as a production-quality application for power users, developers, and enthusiasts who want full visibility into their hardware and software.

## Features

- **Device Information:** Comprehensive details on hardware, model, APIs, and supported ABIs.
- **CPU & RAM Monitoring:** Real-time metrics on processor scaling, architecture, and memory pressure.
- **Storage Analyzer:** Insight into internal storage capacity and consumption.
- **Battery Monitor & History:** Real-time battery status, health, and locally persisted charging history.
- **Diagnostics:** Hardware sensor checks, display calibration tests, and touch path visualization.
- **Network Tools:** Active network topology and IP address details.
- **Root Checker:** Heuristic detection for common root packages, SU binaries, and Magisk indicators.
- **App Inspector:** View system and installed third-party applications.
- **Developer Tools:** A suite of completely offline utilities including UUID generators and timestamp converters.

## Privacy First

DroidLab processes all data locally. There are no tracking scripts, analytics SDKs, or network telemetry modules included in Version 1. The application requests only the minimum necessary permissions required for hardware introspection.

## Build Requirements

- Android Studio Koala / Gradle 9+
- JDK 17+
- Kotlin 2.2+

See `DEVELOPMENT.md` for architecture and contributing guidelines.
