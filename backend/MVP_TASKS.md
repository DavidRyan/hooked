# Hooked API - MVP Tasks

## MVP Requirements
The following flows need to work for the MVP:
- Login/Auth/Register
- Adding a catch / uploading to image store (local file saving)
- Return catches for a user
- Return catch details for a user

## Current Status

### ✅ Completed Tasks
- **Auth Flow**: Login/Register/JWT tokens working perfectly
- **Database Schema**: Initial migration with proper user_id foreign key relationships
- **User Association Logic**: Code properly filters catches by user ID
- **API Structure**: All endpoints properly scoped to authenticated users
- **Error Handling**: Comprehensive validation and error responses
- **User Catch Schema**: Proper `belongs_to :user` relationship with changeset validation

### 🔧 Remaining Tasks for MVP

#### High Priority (Critical for MVP)

1. **Fix Image Data Map Merging** (catches.ex:46)
   - **Current**: `Map.merge(attrs, Map.put(image_data, Map.of(user_id: user_id)))`
   - **Fix**: `Map.merge(attrs, Map.put(image_data, "user_id", user_id))`
   - **Impact**: User association won't be saved correctly

2. **Configure Static File Serving for Images**
   - **Issue**: Uploaded images stored in `priv/static/uploads/` but not served
   - **Fix**: Update endpoint.ex to serve static files from uploads directory
   - **Impact**: Image URLs return 404s

#### Medium Priority

3. **Test Complete User Flows**
   - Test registration → catch creation → retrieval flow
   - Verify user isolation (users only see their own catches)
   - Test image upload and retrieval

4. **Add Authorization Error Handling**
   - Ensure proper error responses when users try to access other users' catches
   - Test unauthorized access scenarios

#### Low Priority (Nice to Have)

5. **API Documentation**
   - Document user-scoped behavior
   - Update any existing API docs/tests

## Technical Implementation Notes

### Database Schema
- ✅ Users table with authentication fields
- ✅ User catches table with `user_id` foreign key
- ✅ Proper indexes for performance
- ✅ Foreign key constraints with CASCADE delete

### Code Structure
- ✅ UserCatch belongs_to User relationship
- ✅ User has_many catches relationship  
- ✅ User-scoped query helpers (`for_user/2`, `for_user_and_id/3`)
- ✅ Authentication middleware working
- ✅ JWT token generation and validation

### API Endpoints
- ✅ `POST /api/auth/register` - User registration
- ✅ `POST /api/auth/login` - User login
- ✅ `GET /api/auth/me` - Get current user
- ✅ `POST /api/auth/refresh` - Refresh JWT token
- ✅ `GET /api/user_catches` - List user's catches
- ✅ `GET /api/user_catches/:id` - Get specific catch
- ✅ `POST /api/user_catches` - Create new catch with image

## File Locations

### Key Files Modified
- `lib/hooked_api/contexts/catches/schemas/user_catch.ex` - User relationship
- `lib/hooked_api/contexts/catches/catches.ex` - User-scoped queries
- `lib/hooked_api_web/controllers/user_catch_controller.ex` - User context
- `lib/hooked_api/contexts/accounts/schemas/user.ex` - Catch relationship
- `priv/repo/migrations/20250822191011_create_initial_schema.exs` - Database schema

### Files Needing Updates
- `lib/hooked_api/contexts/catches/catches.ex:46` - Fix map merging syntax
- `lib/hooked_api/endpoint.ex` - Add static file serving for uploads

## MVP Success Criteria

When these tasks are complete, the following should work:

1. **User Registration**: New users can register with email/password
2. **User Login**: Users can authenticate and receive JWT tokens
3. **Catch Creation**: Authenticated users can upload fish catches with images
4. **User Isolation**: Users only see their own catches
5. **Catch Retrieval**: Users can list and view details of their catches
6. **Image Access**: Uploaded images are accessible via returned URLs

## Next Steps After MVP

- Add S3 bucket integration for image storage
- Add catch enrichment features (species identification, weather, etc.)
- Add catch statistics and analytics
- Add social features (sharing, following, etc.)