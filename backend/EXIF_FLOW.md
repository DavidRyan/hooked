# EXIF Data Flow Setup

## Overview
The enrichment system now uses `exexif` library to extract EXIF data from images and pass it to all enrichers.

## Data Flow

### 1. **Catch Enrichment Worker**
```elixir
# lib/hooked_api/workers/catch_enrichment_worker.ex

def enrich_catch(user_catch) do
  # Extract EXIF data once using exexif
  exif_data = extract_exif_data(user_catch)
  
  # Pass same EXIF data to all enrichers
  Enum.reduce(enrichers, user_catch, fn enricher, updated_catch ->
    enricher.enrich(updated_catch, exif_data)
  end)
end

defp extract_exif_data(user_catch) do
  case Exexif.exif_from_jpeg_file(file_path) do
    {:ok, exif_data} -> exif_data  # Full EXIF map
    _ -> %{}                       # Empty map if no EXIF
  end
end
```

### 2. **GeoEnricher**
```elixir
# lib/hooked_api/enrichers/geo_enricher.ex

def enrich(user_catch, exif_data) do
  case get_gps_from_exif(exif_data) do
    {:ok, lat, lng} ->
      %{user_catch | latitude: lat, longitude: lng}
    _ ->
      user_catch
  end
end

# Extracts GPS coordinates from EXIF data
defp get_gps_from_exif(exif_data) do
  with lat <- Map.get(exif_data, :gps_latitude),
       lng <- Map.get(exif_data, :gps_longitude) do
    {:ok, lat, lng}
  end
end
```

### 3. **Other Enrichers**
```elixir
# All enrichers receive the same EXIF data
SpeciesEnricher.enrich(user_catch, exif_data)
WeatherEnricher.enrich(user_catch, exif_data)
GeoEnricher.enrich(user_catch, exif_data)
```

## Key Benefits

### ✅ **Single EXIF Extraction**
- EXIF data extracted once per image
- Shared across all enrichers
- Efficient file I/O

### ✅ **Consistent Data**
- All enrichers work with same EXIF data
- No duplicate parsing
- Standardized format from `exexif`

### ✅ **GPS Coordinate Extraction**
- Automatic GPS extraction from JPEG EXIF
- Updates user_catch coordinates if available
- Graceful fallback if no GPS data

## EXIF Data Structure

The `exexif` library provides GPS data in these keys:
```elixir
%{
  gps_latitude: 37.7749,      # Decimal degrees
  gps_longitude: -122.4194,   # Decimal degrees  
  gps_latitude_ref: "N",      # North/South
  gps_longitude_ref: "W",     # East/West
  # ... other EXIF fields
}
```

## Dependencies

```elixir
# mix.exs
{:exexif, "~> 0.0.5"}
```

## Usage

The system automatically:
1. Extracts EXIF when processing catches
2. Passes EXIF data to all enrichers
3. Updates coordinates if GPS found in EXIF
4. Continues with other enrichments