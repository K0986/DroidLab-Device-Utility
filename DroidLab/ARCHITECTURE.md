# DroidLab Architecture

## Overview

DroidLab uses a modern Android architecture based on **MVVM** and **Clean Architecture** principles.

To accelerate initial development while maintaining strict logical boundaries, the application uses **Package-by-Feature modularity** within a single Gradle module (`:app`). This avoids the overhead of managing dozens of `build.gradle.kts` files for Version 1, while keeping the code perfectly structured for extraction into multi-module Gradle setups in the future.

## Package Structure

```
com.example
├── core
│   ├── ui        # Common UI components, Theme, Color, Typography
│   ├── common    # Utility functions, extensions, formatters
│   ├── data      # Room databases, DataStore preferences, Repositories
│   └── di        # Hilt modules
└── feature
    ├── dashboard # Main screen combining summaries
    ├── device    # Device information
    ├── cpu       # CPU monitoring
    ├── memory    # RAM monitoring
    ├── storage   # Storage analysis
    ├── battery   # Battery monitoring
    ├── sensors   # Sensor testing
    ├── display   # Display/Touch testing
    ├── network   # Network diagnostics
    ├── root      # Root checker
    ├── apps      # Installed applications
    ├── devtools  # Developer utilities
    └── system    # System information
```

## Future Gradle Modularization

The current package structure strictly isolates features. When the app grows, `com.example.feature.cpu` can be moved to a `:feature:cpu` Gradle module with minimal refactoring, because features do not depend on each other (they only depend on `:core`).

## State Management

Each feature utilizes a `ViewModel` managing a single `StateFlow` representing the UI state.
UI components consume this state using `collectAsStateWithLifecycle()`.

## Dependency Injection

Hilt is used for Dependency Injection. Repositories and hardware data sources are injected into ViewModels, keeping the UI easily testable.
