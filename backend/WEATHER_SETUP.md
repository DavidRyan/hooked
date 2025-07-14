# Weather Enricher Setup

The Weather Enricher uses OpenWeatherMap API to add weather data to fishing catches.

## Setup Instructions

1. **Get an API Key**
   - Sign up at [OpenWeatherMap](https://openweathermap.org/api)
   - Subscribe to the "One Call API 3.0" (free tier includes 1000 calls/day)
   - Copy your API key

2. **Configure the API Key**
   - Set the environment variable: `export OPENWEATHER_API_KEY=your_api_key_here`
   - Or add it to your `.env` file: `OPENWEATHER_API_KEY=your_api_key_here`

3. **Weather Data Fields**
   The enricher adds the following fields to the `weather_data` map:
   - `temperature` - Temperature in Fahrenheit
   - `feels_like` - Feels like temperature in Fahrenheit
   - `humidity` - Humidity percentage
   - `pressure` - Atmospheric pressure in hPa
   - `visibility` - Visibility in meters
   - `wind_speed` - Wind speed in mph
   - `wind_direction` - Wind direction in degrees
   - `weather_condition` - Main weather condition (e.g., "clear", "rain")
   - `weather_description` - Detailed description (e.g., "light rain")
   - `clouds` - Cloudiness percentage
   - `sunrise` - Sunrise time
   - `sunset` - Sunset time
   - `data_source` - Always "openweathermap"
   - `data_type` - "current" or "historical"

## How It Works

- **Current Weather**: For catches within the last hour, fetches current weather
- **Historical Weather**: For older catches, fetches historical weather data
- **Coordinates Required**: Weather enrichment only works if the catch has latitude/longitude coordinates (usually from GPS EXIF data)
- **Graceful Degradation**: If weather data can't be fetched, the catch is processed normally without weather data

## API Limits

- Free tier: 1000 calls/day
- Historical data: Available for the past 40 years
- Rate limit: 60 calls/minute

## Testing

To test the weather enricher:

```elixir
# In IEx
user_catch = %HookedApi.Catches.UserCatch{
  latitude: 37.7749,
  longitude: -122.4194,
  caught_at: ~N[2024-01-15 14:30:00]
}

{:ok, enriched} = HookedApi.Enrichers.WeatherEnricher.enrich(user_catch)
IO.inspect(enriched.weather_data)
```