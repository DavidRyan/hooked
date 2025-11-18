# Hooked App - MVP Todo List

## 🚨 CRITICAL FIXES (Must Fix Before Launch)

### Backend Security & Core Issues

- [ ] **SECURITY VULNERABILITY: Update Default Secrets** 
  - Location: `backend/config/dev.exs:41`, `backend/lib/hooked_api/endpoint.ex:7`
  - Issue: Hardcoded placeholder secrets allow anyone to forge user sessions
  - Must generate and set secure random values for production

- [ ] **Fix Image Data Map Merging Bug**
  - Location: `backend/lib/hooked_api/contexts/catches/catches.ex:51`
  - Current: `Map.merge(attrs, Map.put(image_data, Map.of(user_id: user_id)))`
  - Should be: `Map.merge(attrs, Map.put(image_data, "user_id", user_id))`
  - Impact: User association won't save correctly

- [ ] **Backend Field Mismatch**
  - Database schema missing `weight` and `length` fields for catches
  - Mobile app collects these (SubmitCatchEntity) but backend schema doesn't store them
  - Need migration to add these fields to `user_catches` table

### Test Coverage Gaps

- [ ] **9 Tests Failing**
  - Function signature mismatches after user_id implementation
  - Missing MVP test coverage for auth/catch controllers
  - Database isolation issues with background processes

- [ ] **Missing Critical Tests**
  - No tests for auth endpoints (register/login)
  - No tests for user catch controllers with isolation
  - No image upload validation tests

## 🔧 HIGH PRIORITY FEATURES (Core MVP)

### Missing CRUD Operations

- [ ] **No Edit/Update Catch UI**
  - Backend has update endpoint but mobile app has no edit screen
  - Users can't modify catches after creation

- [ ] **No Delete Catch UI**
  - Backend has delete endpoint but no mobile UI to delete catches
  - Users stuck with unwanted catches

### Data Validation & UX

- [ ] **Required Fields Not Enforced**
  - Backend schema has `validate_required([])` - no required fields!
  - Location: `backend/lib/hooked_api/contexts/catches/schemas/user_catch.ex:126`
  - Should require at minimum: species, location, caught_at

- [ ] **Missing Date/Time Picker**
  - No way to set when fish was caught (caught_at field)
  - Currently defaults to submission time

- [ ] **Weight/Length Units Not Specified**
  - No indication of lbs/kg or inches/cm
  - Needs unit selector or clear labeling

### iOS Implementation Gaps

- [ ] **iOS Photo Selection Not Implemented**
  - TODOs in `modules/catches/presentation/src/iosMain/.../SubmitCatchScreen.ios.kt`
  - Camera and gallery buttons are placeholders

- [ ] **iOS Location Permission Not Implemented**
  - Location capture won't work on iOS

## 🎯 MEDIUM PRIORITY FEATURES

### UX Improvements

- [ ] **No Empty State UI**
  - When user has no catches, shows empty grid
  - Should show onboarding/welcome message

- [ ] **No Image Preview Before Submit**
  - Can't verify photo selection before submitting

- [ ] **Loading States Need Polish**
  - No skeleton loaders or better loading feedback

- [ ] **Error Messages Too Technical**
  - Show user-friendly error messages (e.g., "Failed to submit catch: ${e.message}")

### Functionality Gaps

- [ ] **No Catch Filtering/Sorting**
  - Can't sort by date, species, or location
  - No search functionality

- [ ] **No Species Autocomplete**
  - Manual typing prone to typos and inconsistency
  - Should have common species dropdown

- [ ] **Location Name Not Auto-Filled**
  - User must manually type location even with GPS coordinates
  - Should reverse geocode to city/state

- [ ] **Enrichment Status Not Shown**
  - Backend tracks enrichment_status but UI doesn't show it
  - Users don't know if weather/EXIF data processed

## 📱 POLISH & NICE-TO-HAVES

- [ ] **No Pull-to-Refresh Indication**
  - Pull-to-refresh exists but no visual feedback during refresh

- [ ] **No Image Compression Settings**
  - Images uploaded at full size may cause slow uploads

- [ ] **No Photo Metadata Privacy**
  - EXIF extraction includes all metadata (could expose sensitive info)
  - Should let users strip location data

- [ ] **Debug Logging in Production**
  - Excessive Logger.debug statements throughout backend
  - Should be configurable by environment

- [ ] **No Logout Button**
  - Once logged in, no way to log out or switch accounts

- [ ] **No Password Reset Flow**
  - Users locked out if they forget password

- [ ] **No Email Verification**
  - Anyone can register with any email

- [ ] **No Avatar/Profile Picture**
  - User profile has no photo

- [ ] **No Catch Statistics**
  - No dashboard showing total catches, species breakdown, etc.

- [ ] **No Export Functionality**
  - Can't export catch history to CSV/PDF

## 🏗️ ARCHITECTURE IMPROVEMENTS

- [ ] **No Offline Support**
  - Requires internet connection for all operations
  - Should cache catches locally

- [ ] **No Background Sync**
  - Can't submit catches when connection restored

- [ ] **Duplicate Submit Module Structure**
  - Both `modules/submit/` and `modules/catches/` have submit functionality
  - Should consolidate

- [ ] **Rate Limiting Only on Auth**
  - No rate limiting on catch creation (abuse potential)

## 📊 MVP RECOMMENDATION

**For a functional MVP, prioritize:**

### ✅ MUST FIX (Critical)
- [ ] Security vulnerability: Update default secrets
- [ ] Fix image data map merging bug
- [ ] Add weight/length fields to database schema
- [ ] Fix failing tests
- [ ] Add missing critical tests

### ✅ MUST HAVE (Core Features)
- [ ] Edit/update catch UI
- [ ] Delete catch UI
- [ ] Enforce required fields validation
- [ ] Add date/time picker for caught_at
- [ ] Species dropdown/autocomplete
- [ ] Logout functionality

### ✅ SHOULD HAVE
- [ ] Weight/length unit selector
- [ ] iOS photo selection implementation
- [ ] iOS location permission implementation
- [ ] Empty state UI
- [ ] Image preview before submit
- [ ] Better loading states
- [ ] Reverse geocoding for location names

### ❌ CAN DEFER
- Everything in "POLISH & NICE-TO-HAVES" and "ARCHITECTURE IMPROVEMENTS"

## 📅 Estimated MVP Timeline

- **Critical fixes:** 2-3 days
- **Core features:** 1-2 weeks  
- **Should-haves:** 1 week
- **Total:** 2-3 weeks for solid MVP

---

*Generated: 2025-10-27*
