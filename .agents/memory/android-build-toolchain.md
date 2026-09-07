---
name: Android build toolchain
description: Native Android validation requirements and the local JDK image caveat.
---

The project requires the checked-in Gradle 9.3.1 wrapper plus an Android SDK platform matching compileSdk. Kotlin compilation can succeed in a GraalVM-based environment while Java/test tasks fail during AGP's `core-for-system-modules.jar` jlink transform.

**Why:** The Replit workspace did not initially include Java, Gradle, or an Android SDK. After provisioning them, source compilation succeeded, but unit-test and Java compilation tasks exposed a GraalVM `jlink` incompatibility that is environmental rather than a project source failure.

**How to apply:** Prefer a standard JDK 17+ distribution for complete Android validation. Keep SDK paths machine-local and out of the repository; use the Gradle wrapper for reproducible project builds.