# JWT Authentication Setup

The Hooked API now includes JWT-based authentication with user registration and login.

## API Endpoints

### Public Endpoints (No Authentication Required)

#### Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "user": {
    "email": "user@example.com",
    "password": "SecurePass123!",
    "first_name": "John",
    "last_name": "Doe"
  }
}
```

**Response (201 Created):**
```json
{
  "data": {
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "first_name": "John",
      "last_name": "Doe",
      "is_active": true,
      "inserted_at": "2024-01-15T10:30:00Z",
      "updated_at": "2024-01-15T10:30:00Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### Login User
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "first_name": "John",
      "last_name": "Doe",
      "is_active": true,
      "inserted_at": "2024-01-15T10:30:00Z",
      "updated_at": "2024-01-15T10:30:00Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### Protected Endpoints (Authentication Required)

All protected endpoints require the `Authorization` header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Get Current User
```
GET /api/auth/me
Authorization: Bearer <token>
```

#### Refresh Token
```
POST /api/auth/refresh
Authorization: Bearer <token>
```

#### User Catches (All CRUD operations)
```
GET /api/user_catches
POST /api/user_catches
GET /api/user_catches/:id
PUT /api/user_catches/:id
DELETE /api/user_catches/:id
Authorization: Bearer <token>
```

## Password Requirements

- Minimum 8 characters
- Maximum 72 characters
- At least one lowercase letter
- At least one uppercase letter
- At least one digit or punctuation character

## JWT Configuration

Set the JWT secret in your environment:
```bash
export JWT_SECRET="your-super-secret-jwt-key-change-this-in-production"
```

Or add it to your `.env` file:
```
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
```

## Token Details

- **Algorithm**: HS256
- **Expiration**: 7 days
- **Issuer**: hooked_api
- **Audience**: hooked_app
- **Claims**: `sub` (user_id), `user_id`, `iss`, `aud`, `exp`, `iat`

## Error Responses

### 401 Unauthorized
```json
{
  "error": "Invalid or expired token"
}
```

### 422 Unprocessable Entity (Validation Errors)
```json
{
  "errors": {
    "email": ["can't be blank"],
    "password": ["at least one upper case character"]
  }
}
```

### 403 Forbidden
```json
{
  "error": "Account is disabled"
}
```

## Testing with curl

### Register a new user:
```bash
curl -X POST http://localhost:4000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "email": "test@example.com",
      "password": "TestPass123!",
      "first_name": "Test",
      "last_name": "User"
    }
  }'
```

### Login:
```bash
curl -X POST http://localhost:4000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'
```

### Access protected endpoint:
```bash
curl -X GET http://localhost:4000/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```