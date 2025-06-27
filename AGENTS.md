# Agent Guidelines for Hooked

## Build/Test Commands
- `./gradlew build` - Build entire project
- `./gradlew test` - Run all unit tests
- `./gradlew check` - Run all checks including lint
- `./gradlew lint` - Run lint checks
- `./gradlew :modules:catches:presentation:test` - Run tests for specific module
- `./gradlew iosX64Test` - Run iOS tests for x64 simulator
- `./gradlew testDebugUnitTest` - Run Android debug unit tests

## Code Style Guidelines
- **Package Structure**: Follow `com.hooked.{module}.{layer}` pattern (e.g., `com.hooked.catches.presentation`)
- **Imports**: Group by type - compose imports first, then androidx, then project imports, then external libs
- **Naming**: Use PascalCase for classes/composables, camelCase for functions/variables, SCREAMING_SNAKE_CASE for constants
- **Data Classes**: Use `data class` for entities, DTOs, and state objects with explicit parameter names
- **Error Handling**: Use `NetworkResult<T>` sealed class for network operations with automatic logging
- **Compose**: Use `@Composable` functions with proper parameter ordering (modifier last, viewModel injected via koinViewModel())
- **Architecture**: Follow Clean Architecture - separate data/domain/presentation layers per feature module
- **State Management**: Use sealed classes for UI state, intents, and effects with descriptive names
- **Constants**: Define animation/UI constants in dedicated objects (e.g., `AnimationConstants`)
- **Logging**: Use `Logger` class for consistent logging across platforms with context information

## Module Structure
Each feature follows: `modules/{feature}/{data|domain|presentation}` with shared `core` modules for common functionality.