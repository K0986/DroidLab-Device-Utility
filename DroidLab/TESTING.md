# Testing Guide

## Testing Strategy

Given the deep hardware and OS integration nature of DroidLab, testing requires a mix of standard unit tests and mocked hardware environments.

### Local Unit Tests

Run local tests via:
```bash
./gradlew :app:testDebugUnitTest
```

### Roborazzi Screenshot Tests

We use Roborazzi to verify the layout and Material 3 theming of our core Jetpack Compose screens without needing an emulator.

**To record new screenshots:**
```bash
./gradlew :app:recordRoborazziDebug
```

**To verify existing screenshots:**
```bash
./gradlew :app:verifyRoborazziDebug
```

### Future Work

- Implement `FakeSensorManager` and `FakeBatteryManager` to unit test the helper classes.
- Add UI tests using `ComposeTestRule` for navigation verification.
