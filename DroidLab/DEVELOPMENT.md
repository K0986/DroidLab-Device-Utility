# Development Guide

DroidLab is built using standard modern Android development patterns.

## Technical Stack

- **UI:** Jetpack Compose + Material 3
- **Language:** Kotlin
- **Asynchrony:** Kotlin Coroutines & Flow
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room Database & DataStore
- **Build System:** Gradle Kotlin DSL + Version Catalogs

## Architectural Structure

The project currently uses a **Package-by-Feature** approach within the `:app` module to maximize feature isolation while minimizing Gradle overhead in the V1 stage.

```
com.example
├── core
│   ├── common    # Utility helpers for OS/Hardware introspection
│   ├── data      # Room DB entities and DAOs
│   ├── di        # Hilt modules
│   └── ui        # Theming and shared components
└── feature
    ├── battery
    ├── cpu
    ├── ... (other features)
```

## Adding Features

1. **Helper Layer:** Implement OS-level interaction in `core.common` (e.g., `SensorInfoHelper`). Provide it as `@Singleton`.
2. **ViewModel Layer:** Inject the helper into a Hilt `ViewModel`. Map the raw data into a specific `UiState` data class and expose it as a `StateFlow`.
3. **UI Layer:** Create a Compose screen that collects the `StateFlow` and binds to Material 3 components.
4. **Navigation:** Add the new route to the `NavHost` in `MainActivity.kt`.

## Future Roadmap

Planned architecture enhancements for future versions:
- Extraction of features into strict Gradle modules (`:feature:cpu`, `:feature:battery`).
- Shizuku integration for privileged ADB shell operations.
- Wireless ADB client capabilities.
