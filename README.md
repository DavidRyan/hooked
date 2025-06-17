# Hooked 🎣

A cross-platform mobile application for tracking and sharing fishing catches, built with Kotlin Multiplatform and Rust.

## Features

- **Track Catches**: Record details about your fishing catches including species, weight, length, location, and date
- **Photo Upload**: Capture and share photos of your catches
- **Browse Catches**: View a grid of all recorded catches with detailed information
- **Cross-Platform**: Native apps for both Android and iOS from a shared codebase

## Tech Stack

### Mobile
- **Kotlin Multiplatform Mobile (KMM)** - Shared business logic between platforms
- **Jetpack Compose** - Modern declarative UI for Android
- **SwiftUI** - Native iOS UI (via KMM integration)
- **Koin** - Dependency injection
- **Ktor** - HTTP client for API calls
- **Navigation Compose** - Navigation framework

### Backend

## Project Structure

```
hooked/
├── backend/              # API server
├── composeApp/          # Main mobile application
│   ├── commonMain/      # Shared Kotlin code
│   ├── androidMain/     # Android-specific code
│   └── iosMain/         # iOS-specific code
├── modules/             # Feature modules (Clean Architecture)
│   ├── core/           # Shared utilities and UI components
│   ├── catches/        # Browse catches feature
│   └── submit/         # Submit new catch feature
├── iosApp/             # iOS Xcode project
└── infra/              # Infrastructure and deployment
```

## Getting Started

### Prerequisites

- **JDK 17** or higher
- **Android Studio** (latest stable version)
- **Xcode** (for iOS development, macOS only)

### Backend Setup

### Mobile Development

#### Android

1. Open the project in Android Studio

2. Build and run on emulator or device:
   ```bash
   ./gradlew installDebug
   ```

#### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode

2. Select your target device/simulator

3. Build and run (⌘+R)

## Development

### Running Tests

```bash
# Mobile tests
./gradlew test

### Code Quality

```bash
# Kotlin linting
./gradlew lint

### Building for Release

#### Android
```bash
./gradlew assembleRelease
```

#### iOS
Build archive in Xcode with Product → Archive

#### Backend
```bash
cd backend && cargo build --release
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Run tests and linting before committing
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
