# Logging Abstraction Usage

This module provides a centralized logging abstraction that works across all platforms (Android, iOS), with automatic logging for Result sealed classes.

## Basic Usage

### Using Logger directly with tags
```kotlin
import com.hooked.core.logging.Logger

Logger.debug("MyTag", "Debug message")
Logger.info("MyTag", "Info message")
Logger.warning("MyTag", "Warning message")
Logger.error("MyTag", "Error message")

// With exceptions
Logger.error("MyTag", "Something went wrong", exception)
```

### Using extension functions (recommended)
```kotlin
import com.hooked.core.logging.logDebug
import com.hooked.core.logging.logError

class MyViewModel {
    fun doSomething() {
        logDebug("Starting operation")
        
        try {
            // some work
            logInfo("Operation completed successfully")
        } catch (e: Exception) {
            logError("Operation failed", e)
        }
    }
}
```

## Automatic Result Logging

### NetworkResult
All `NetworkResult.Error` instances automatically log when created:

```kotlin
// This automatically logs: "ERROR [NetworkResult]: Network error [CatchApiService.getCatches]: Connection failed"
return NetworkResult.Error(connectionException, "CatchApiService.getCatches")

// Logs: "ERROR [NetworkResult]: Network error: Timeout"
return NetworkResult.Error(timeoutException)
```

### Use Case Results
All use cases now inherit from `UseCaseResult<T>` which automatically logs errors:

```kotlin
// All use cases return UseCaseResult<T> - errors log automatically:
// "ERROR [UseCaseResult]: Use case error [GetCatchesUseCase]: Failed to load data"
class GetCatchesUseCase {
    suspend operator fun invoke(): UseCaseResult<List<CatchEntity>> {
        return try {
            // ... success logic
            UseCaseResult.Success(data)
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetCatchesUseCase")
        }
    }
}
```

### PhotoCaptureResult
Photo capture errors automatically log:

```kotlin
// This logs: "ERROR [PhotoCapture]: Photo capture error [PhotoCapture.capturePhoto]: Camera permission denied"
return PhotoCaptureResult.Error("Camera permission denied", "PhotoCapture.capturePhoto")
```

## Context-Aware Logging

All Result classes support context information to identify where errors occurred:

```kotlin
// Network errors with context
NetworkResult.Error(exception, "UserApiService.login")

// Use case errors with context  
UseCaseResult.Error("Validation failed", null, "SubmitCatchUseCase")

// Photo errors with context
PhotoCaptureResult.Error("Gallery access denied", "PhotoCapture.pickFromGallery")
```

## Implementation Details

- **Android**: Uses `android.util.Log`
- **iOS**: Uses `NSLog`
- **Automatic logging**: All error Result types log immediately when created
- **Context tracking**: Optional context parameter shows exactly where errors occurred

## Log Levels

- `debug`: Development information
- `info`: General information  
- `warning`: Potential issues
- `error`: Errors and exceptions (including automatic Result logging)

## Benefits

1. **Zero-effort error logging**: Errors are logged automatically when Result.Error is created
2. **Context awareness**: Know exactly which API call, use case, or operation failed
3. **Consistent format**: All errors follow the same logging pattern
4. **No missed errors**: Every error Result instance is guaranteed to be logged