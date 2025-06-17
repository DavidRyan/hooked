# Automatic Result Logging Architecture

This document explains how all Result sealed classes automatically log errors when they occur.

## Architecture Overview

All Result classes in the app inherit automatic logging behavior:

```
Result Classes with Auto-Logging:
├── NetworkResult<T>
│   ├── Success<T>
│   ├── Loading  
│   └── Error (auto-logs)
├── UseCaseResult<T> 
│   ├── Success<T>
│   └── Error (auto-logs)
└── PhotoCaptureResult
    ├── Success
    ├── Cancelled
    └── Error (auto-logs)
```

## Use Case Inheritance

All use cases now return the base `UseCaseResult<T>` type instead of custom sealed classes:

### Before (Custom Result Classes)
```kotlin
// Each use case had its own result class with duplicate logging
sealed class SubmitCatchUseCaseResult {
    data class Success(val catchId: Long) : SubmitCatchUseCaseResult()
    data class Error(val message: String, ...) : SubmitCatchUseCaseResult() {
        init { /* duplicate logging code */ }
    }
}

sealed class GetCatchesUseCaseResult {
    data class Success(val catches: List<CatchEntity>) : GetCatchesUseCaseResult()
    data class Error(val message: String, ...) : GetCatchesUseCaseResult() {
        init { /* duplicate logging code */ }
    }
}
```

### After (Unified Base Class)
```kotlin
// All use cases return UseCaseResult<T> with inherited logging
class SubmitCatchUseCase {
    suspend operator fun invoke(entity: SubmitCatchEntity): UseCaseResult<Long> {
        return try {
            // success logic
            UseCaseResult.Success(catchId)
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "SubmitCatchUseCase")
        }
    }
}

class GetCatchesUseCase {
    suspend operator fun invoke(): UseCaseResult<List<CatchEntity>> {
        return try {
            // success logic  
            UseCaseResult.Success(catches)
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetCatchesUseCase")
        }
    }
}
```

## ViewModels Updated

ViewModels now handle the unified `UseCaseResult<T>` type:

```kotlin
// Before
when (val result = submitCatchUseCase(entity)) {
    is SubmitCatchUseCaseResult.Success -> {
        // handle success with result.catchId
    }
    is SubmitCatchUseCaseResult.Error -> {
        // handle error with result.message
    }
}

// After  
when (val result = submitCatchUseCase(entity)) {
    is UseCaseResult.Success -> {
        // handle success with result.data (generic)
    }
    is UseCaseResult.Error -> {
        // handle error with result.message
    }
}
```

## Benefits

1. **DRY Principle**: No duplicate logging code across use cases
2. **Consistency**: All use cases follow the same error handling pattern
3. **Type Safety**: Generic `UseCaseResult<T>` provides type-safe success data
4. **Automatic Logging**: All errors are guaranteed to be logged with context
5. **Easier Maintenance**: Single source of truth for use case result handling

## Log Output Examples

```
ERROR [UseCaseResult]: Use case error [SubmitCatchUseCase]: Network timeout
ERROR [UseCaseResult]: Use case error [GetCatchesUseCase]: Failed to fetch catches  
ERROR [NetworkResult]: Network error [CatchApiService.getCatches]: Connection refused
ERROR [PhotoCapture]: Photo capture error [PhotoCapture.capturePhoto]: Camera permission denied
```

Each error includes:
- **Tag**: The result class type (UseCaseResult, NetworkResult, PhotoCapture)
- **Context**: The specific operation that failed (e.g., SubmitCatchUseCase)
- **Message**: The human-readable error description
- **Exception**: The underlying throwable (when available)