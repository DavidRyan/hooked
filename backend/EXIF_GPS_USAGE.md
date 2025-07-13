# EXIF GPS Integration with exexif

## Overview
The GeoEnricher now uses the `exexif` library to extract GPS coordinates from JPEG image EXIF data.

## Key Features

### 1. **Automatic GPS Extraction**
- Extracts GPS coordinates directly from image files
- Handles various coordinate formats (decimal, DMS)
- Applies correct hemisphere signs (N/S for latitude, E/W for longitude)

### 2. **Smart Coordinate Updates**
- Only updates user_catch coordinates if missing or EXIF is more precise
- Uses struct update syntax: `%{user_catch | latitude: lat}`
- Preserves existing coordinates if EXIF data is unavailable

### 3. **Error Handling**
- Gracefully handles images without EXIF data
- Logs debug information for GPS extraction attempts
- Falls back to existing coordinates if EXIF parsing fails

## Usage

```elixir
# The enricher automatically runs during catch processing
user_catch = %UserCatch{image_url: "/uploads/catches/photo.jpg"}
enriched_catch = GeoEnricher.enrich(user_catch, %{})

# If GPS coordinates found in EXIF:
# %{user_catch | latitude: 37.7749, longitude: -122.4194}

# If no GPS data in EXIF:
# Returns original user_catch unchanged
```

## EXIF Data Structure

The `exexif` library returns GPS data in these fields:
- `:gps_latitude` - Latitude coordinate
- `:gps_longitude` - Longitude coordinate  
- `:gps_latitude_ref` - "N" or "S"
- `:gps_longitude_ref` - "E" or "W"

## Common Scenarios

### ✅ **Images with GPS EXIF**
- Photos taken with smartphones/cameras with GPS enabled
- Coordinates extracted and applied to user_catch

### ⚠️ **Images without GPS EXIF**
- Web-uploaded images (often stripped of EXIF)
- Screenshots or edited images
- Enricher returns original user_catch unchanged

### 🔄 **Fallback Behavior**
- If EXIF extraction fails, existing coordinates are preserved
- No errors thrown - graceful degradation
- Debug logs help with troubleshooting

## Testing

To test with GPS-enabled images:
1. Take photo with smartphone GPS enabled
2. Upload directly without processing
3. Check logs for "Found GPS coordinates in EXIF" message

## Dependencies

```elixir
# mix.exs
{:exexif, "~> 0.0.5"}
```