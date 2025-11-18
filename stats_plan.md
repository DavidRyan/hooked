# Backend Stats Implementation Plan

## Current Schema Analysis
The `UserCatch` schema currently includes:
- `species`, `location`, `latitude`, `longitude`, `caught_at`, `notes`
- `weather_data`, `exif_data` (maps)
- Image metadata fields

**⚠️ Critical Issue**: The frontend `StatsEntity` expects `weight` and `length` fields, but these don't exist in the current backend schema. The stats calculations will need to be adjusted.

## Phase 1: Context Layer

### 1.1 Add Stats Function to Catches Context
**File**: `backend/lib/hooked_api/contexts/catches/catches.ex`

Add a `get_user_stats/1` function that calculates statistics from user catches.

## Phase 2: Controller Layer

### 2.1 Add Stats Action to UserCatchController
**File**: `backend/lib/hooked_api_web/controllers/user_catch_controller.ex`

Add a `stats/2` action that calls the context function and returns JSON response.

## Phase 3: Routing

### 3.1 Add Stats Route
**File**: `backend/lib/hooked_api_web/router.ex`

Add `get "/user_catches/stats", UserCatchController, :stats` to the authenticated scope.

## Phase 4: Testing

### 4.1 Add Stats Tests
**File**: `backend/test/hooked_api_web/controllers/user_catch_controller_test.exs`

Add comprehensive tests for:
- Stats calculation accuracy
- User isolation (only own catches)
- Empty state handling
- Authentication requirements

## Phase 5: Schema Enhancement (Future)

### 5.1 Add Weight/Length Fields
**Migration**: `backend/priv/repo/migrations/XXX_add_weight_length_to_user_catches.exs`

Add `weight` and `length` float fields to support complete stats functionality.

**Schema Update**: `backend/lib/hooked_api/contexts/catches/schemas/user_catch.ex`

Update the UserCatch schema to include weight and length fields.

## Phase 6: Performance Optimization

### 6.1 Database-Level Stats Calculation
Replace context function with efficient database queries using Ecto aggregations instead of loading all records into memory.

## Implementation Timeline
- **Day 1**: Context function + Controller action + Route (2-3 hours)
- **Day 2**: Tests + Error handling (2-3 hours)
- **Day 3**: Performance optimization (2-3 hours)
- **Day 4**: Schema migration for weight/length (if needed) (2-3 hours)

## API Response Format
The endpoint will return a JSON object matching the frontend StatsEntity structure with total_catches, species_breakdown, unique_species, unique_locations, and catch statistics.

## Testing Checklist
- [ ] Stats calculation accuracy
- [ ] User isolation (only own catches)
- [ ] Empty state handling
- [ ] Authentication required
- [ ] Performance with large datasets
- [ ] Error handling for database issues

This plan provides a solid foundation that matches the frontend expectations while noting the schema limitations that should be addressed in future iterations.