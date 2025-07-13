# iNaturalist API Setup Guide

## Overview
The species enricher now uses iNaturalist's Computer Vision API to identify fish species from photos. This requires authentication.

## Changes Made

### 1. Updated Species Detection Logic
- Now uses `preferred_common_name` (e.g., "Largemouth Bass") instead of scientific name
- Falls back to scientific name if common name is unavailable
- Location: `lib/hooked_api/enrichers/species/species_enricher.ex:18-19`

### 2. Added Authentication
- Added Bearer token authentication to API requests
- Configuration placeholder added to `config/config.exs`

## Setup Instructions

### Step 1: Register iNaturalist Application
1. Go to https://www.inaturalist.org/oauth/applications/new
2. Fill out the form:
   - **Name**: "Hooked Fish App"
   - **Redirect URI**: `http://localhost:4000/auth/callback` (or your domain)
   - **Scopes**: Leave default (read access is sufficient)
3. Save the `Client ID` and `Client Secret`

### Step 2: Get Access Token
For development, you can use your personal iNaturalist account:

```bash
curl -X POST "https://www.inaturalist.org/oauth/token" \
  -d "grant_type=password" \
  -d "username=YOUR_INATURALIST_USERNAME" \
  -d "password=YOUR_INATURALIST_PASSWORD" \
  -d "client_id=YOUR_CLIENT_ID" \
  -d "client_secret=YOUR_CLIENT_SECRET"
```

This returns:
```json
{
  "access_token": "your-access-token-here",
  "token_type": "Bearer",
  "expires_in": 7200
}
```

### Step 3: Update Configuration
Replace the placeholder in `config/config.exs`:

```elixir
config :hooked_api,
  inaturalist_access_token: "your-actual-access-token-here"
```

### Step 4: Test Species Detection
```bash
# Test with a fish image
curl -X POST "https://api.inaturalist.org/v1/computervision/score_image" \
  -H "Authorization: Bearer your-access-token-here" \
  -F "image=@priv/static/uploads/catches/test-fish.png"
```

Expected response for largemouth bass:
```json
{
  "results": [
    {
      "taxon": {
        "id": 47178,
        "name": "Micropterus salmoides",
        "preferred_common_name": "Largemouth Bass"
      },
      "score": 0.85
    }
  ]
}
```

## Production Considerations

### Environment Variables
For production, use environment variables instead of hardcoded tokens:

```elixir
# config/runtime.exs
config :hooked_api,
  inaturalist_access_token: System.get_env("INATURALIST_ACCESS_TOKEN")
```

### Token Refresh
Access tokens expire. Consider implementing automatic token refresh using the refresh token flow.

### Rate Limiting
iNaturalist has rate limits. Consider implementing:
- Request queuing
- Exponential backoff
- Caching of results

## Testing
Once configured, the species enricher should return user-friendly names like:
- "Largemouth Bass" instead of "Micropterus salmoides"
- "Rainbow Trout" instead of "Oncorhynchus mykiss"
- "Northern Pike" instead of "Esox lucius"