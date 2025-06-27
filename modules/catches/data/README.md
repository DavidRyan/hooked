# Catches Data Layer

This module contains the data layer implementation for the catches feature, including both API and local database storage.

## Database Implementation

The module uses SQLDelight for cross-platform database support with the following components:

### Database Schema
- **CatchEntity**: Stores catch information including species, weight, length, location, and photos
- Located in: `src/commonMain/sqldelight/com/hooked/catches/data/database/Catch.sq`

### Key Components

1. **DatabaseDriverFactory**: Platform-specific database driver creation
   - Android: Uses AndroidSqliteDriver
   - iOS: Uses NativeSqliteDriver

2. **CatchLocalDataSource**: Handles all database operations
   - Insert/update catches
   - Query catches by ID or all
   - Delete operations
   - Automatic logging

3. **CatchRepositoryImpl**: Updated to use both API and database
   - Implements cache-first strategy
   - Falls back to API when local data unavailable
   - Automatically caches API responses

### Usage Example

```kotlin
// Initialize database
val driverFactory = DatabaseDriverFactory(context) // Android
val databaseModule = DatabaseModule(driverFactory)
val localDataSource = databaseModule.provideCatchLocalDataSource()

// Initialize repository with both API and database
val repository = CatchRepositoryImpl(
    catchApiService = catchApiService,
    localDataSource = localDataSource
)

// Repository automatically handles caching
val catches = repository.getCatches() // Returns cached data if available, otherwise fetches from API
```

### Caching Strategy

1. **getCatches()**: Returns local data if available, otherwise fetches from API and caches
2. **getCatchDetails()**: Returns local data if available, otherwise fetches from API and caches
3. **refreshCatches()**: Forces refresh from API and updates local cache

The repository provides automatic offline support and improved performance through local caching.