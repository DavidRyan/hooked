defmodule HookedApi.Enrichers.WeatherEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  require Logger

  @historical_weather_base_url "https://api.openweathermap.org/data/3.0"
  @historical_weather_endpoint "/onecall/timemachine"

  def enrich(user_catch, _context \\ %{}) do
    Logger.debug("WeatherEnricher: Processing catch #{user_catch.id}")

    with {:ok, api_key} <- get_api_key(),
         {:ok, lat, lng} <- get_coordinates(user_catch),
         {:ok, weather_data} <- fetch_historical_weather(lat, lng, user_catch.caught_at, api_key) do
      Logger.info(
        "WeatherEnricher: Successfully enriched catch #{user_catch.id} with historical weather data"
      )

      {:ok, %{user_catch | weather_data: weather_data}}
    else
      {:error, :no_api_key} ->
        Logger.warning(
          "WeatherEnricher: OpenWeatherMap API key not configured for catch #{user_catch.id}"
        )

        {:ok, %{user_catch | enrichment_status: false}}

      {:error, :no_coordinates} ->
        Logger.debug(
          "WeatherEnricher: No coordinates available for weather enrichment for catch #{user_catch.id}"
        )

        {:ok, %{user_catch | enrichment_status: false}}

      {:error, reason} ->
        Logger.error(
          "WeatherEnricher: Weather enrichment failed for catch #{user_catch.id}: #{inspect(reason)}"
        )

        # Continue without weather data on failure
        {:ok, %{user_catch | enrichment_status: false}}
    end
  end

  defp get_api_key do
    # First try to get from Application config (which loads from .env in dev.exs)
    case Application.get_env(:hooked_api, :openweather_api_key) do
      nil ->
        # Try direct environment variable as fallback
        case System.get_env("OPENWEATHER_API_KEY") do
          nil ->
            Logger.warning(
              "WeatherEnricher: No OpenWeatherMap API key found in config or environment"
            )

            {:error, :no_api_key}

          env_key when is_binary(env_key) and byte_size(env_key) > 5 ->
            Logger.debug(
              "WeatherEnricher: API key found in environment (#{String.length(env_key)} characters)"
            )

            {:ok, env_key}

          _ ->
            Logger.warning("WeatherEnricher: Invalid API key in environment")
            {:error, :invalid_api_key}
        end

      key when is_binary(key) and byte_size(key) > 5 ->
        Logger.debug(
          "WeatherEnricher: API key found in application config (#{String.length(key)} characters)"
        )

        {:ok, key}

      invalid ->
        Logger.error("WeatherEnricher: Invalid API key configuration: #{inspect(invalid)}")
        {:error, :invalid_api_key}
    end
  end

  defp get_coordinates(user_catch) do
    Logger.debug("WeatherEnricher: Checking coordinates for catch #{user_catch.id}")

    Logger.debug(
      "WeatherEnricher: Latitude: #{inspect(user_catch.latitude)}, Longitude: #{inspect(user_catch.longitude)}"
    )

    case {user_catch.latitude, user_catch.longitude} do
      {lat, lng} when is_float(lat) and is_float(lng) ->
        Logger.info("WeatherEnricher: Valid coordinates found - Lat: #{lat}, Lng: #{lng}")
        {:ok, lat, lng}

      {lat, lng} ->
        Logger.warning(
          "WeatherEnricher: Invalid coordinates - Lat: #{inspect(lat)}, Lng: #{inspect(lng)}"
        )

        {:error, :no_coordinates}
    end
  end

  defp fetch_historical_weather(lat, lng, caught_at, api_key) do
    # Use a test response in development and test environments if no real API key 
    if api_key == "test_openweather_api_key_for_testing" do
      Logger.info("WeatherEnricher: Using MOCK historical weather data (test/development mode)")

      mock_weather_data = %{
        temperature: 72.5,
        feels_like: 70.2,
        humidity: 65,
        pressure: 1012,
        visibility: 10000,
        wind_speed: 5.8,
        wind_direction: 180,
        weather_condition: "clear",
        weather_description: "clear sky",
        clouds: 0,
        data_source: "openweathermap-mock",
        data_type: "historical"
      }

      {:ok, mock_weather_data}
    else
      case parse_caught_at_datetime(caught_at) do
        {:ok, caught_at_datetime} ->
          timestamp = DateTime.from_naive!(caught_at_datetime, "Etc/UTC") |> DateTime.to_unix()
          url = "#{@historical_weather_base_url}#{@historical_weather_endpoint}"

          params = %{
            lat: lat,
            lon: lng,
            dt: timestamp,
            appid: api_key,
            units: "imperial"
          }

          Logger.info("WeatherEnricher: Fetching historical weather from #{url}")

          Logger.debug(
            "WeatherEnricher: Request params: #{inspect(Map.put(params, :appid, "[REDACTED]"))}"
          )

          Logger.debug("WeatherEnricher: Timestamp: #{timestamp} (#{caught_at_datetime})")

          case make_request(url, params) do
            {:ok, response} ->
              Logger.info("WeatherEnricher: Successfully received historical weather data")
              Logger.debug("WeatherEnricher: Response keys: #{inspect(Map.keys(response))}")
              parse_historical_weather_response(response)

            error ->
              Logger.error(
                "WeatherEnricher: Historical weather request failed: #{inspect(error)}"
              )

              error
          end

        {:error, reason} ->
          {:error, reason}
      end
    end
  end

  defp parse_caught_at_datetime(caught_at) do
    case caught_at do
      %NaiveDateTime{} ->
        {:ok, caught_at}

      datetime_string when is_binary(datetime_string) ->
        case NaiveDateTime.from_iso8601(datetime_string) do
          {:ok, datetime} ->
            {:ok, datetime}

          {:error, _} ->
            Logger.error(
              "WeatherEnricher: Could not parse caught_at datetime: #{inspect(datetime_string)}"
            )

            {:error, :invalid_datetime}
        end

      _ ->
        Logger.error("WeatherEnricher: Invalid caught_at format: #{inspect(caught_at)}")
        {:error, :invalid_datetime}
    end
  end

  defp make_request(url, params) do
    full_url = url <> "?" <> URI.encode_query(Map.put(params, :appid, "[REDACTED]"))
    Logger.debug("WeatherEnricher: Making request to #{full_url}")
    Logger.info("WeatherEnricher: FULL REQUEST DETAILS - URL: #{url}")

    Logger.info(
      "WeatherEnricher: FULL REQUEST PARAMS: #{inspect(Map.put(params, :appid, "[REDACTED]"), pretty: true)}"
    )

    client =
      Tesla.client([
        {Tesla.Middleware.BaseUrl, url},
        {Tesla.Middleware.Query, params},
        Tesla.Middleware.JSON,
        {Tesla.Middleware.Timeout, timeout: 10_000}
      ])

    case Tesla.get(client, "") do
      {:ok, %Tesla.Env{status: 200, body: body}} ->
        Logger.info("WeatherEnricher: HTTP 200 - Request successful")

        Logger.info(
          "WeatherEnricher: FULL RESPONSE BODY: #{inspect(body, pretty: true, limit: :infinity)}"
        )

        {:ok, body}

      {:ok, %Tesla.Env{status: 401, body: body}} ->
        Logger.error(
          "WeatherEnricher: HTTP 401 - Unauthorized. API key may be invalid or expired"
        )

        Logger.error(
          "WeatherEnricher: FULL ERROR RESPONSE BODY: #{inspect(body, pretty: true, limit: :infinity)}"
        )

        {:error, :unauthorized}

      {:ok, %Tesla.Env{status: 404, body: body}} ->
        Logger.error("WeatherEnricher: HTTP 404 - Not found")

        Logger.error(
          "WeatherEnricher: FULL ERROR RESPONSE BODY: #{inspect(body, pretty: true, limit: :infinity)}"
        )

        {:error, :not_found}

      {:ok, %Tesla.Env{status: status_code, body: body}} ->
        Logger.error("WeatherEnricher: HTTP #{status_code} - Unexpected status code")

        Logger.error(
          "WeatherEnricher: FULL ERROR RESPONSE BODY: #{inspect(body, pretty: true, limit: :infinity)}"
        )

        {:error, {:http_error, status_code}}

      {:error, reason} ->
        Logger.error(
          "WeatherEnricher: Request failed: #{inspect(reason, pretty: true, limit: :infinity)}"
        )

        {:error, {:request_failed, reason}}
    end
  end

  defp parse_historical_weather_response(response) do
    Logger.debug("WeatherEnricher: Parsing historical weather response")

    Logger.info(
      "WeatherEnricher: RAW HISTORICAL RESPONSE TO PARSE: #{inspect(response, pretty: true, limit: :infinity)}"
    )

    # Historical API returns data in a "data" array, get the first (and only) entry
    historical_data =
      case get_in(response, ["data", Access.at(0)]) do
        nil ->
          Logger.warning("WeatherEnricher: No data found in historical response")
          Logger.debug("WeatherEnricher: Available response keys: #{inspect(Map.keys(response))}")
          %{}

        data ->
          data
      end

    Logger.info(
      "WeatherEnricher: EXTRACTED HISTORICAL DATA: #{inspect(historical_data, pretty: true, limit: :infinity)}"
    )

    weather_data = %{
      temperature: get_in(historical_data, ["temp"]),
      feels_like: get_in(historical_data, ["feels_like"]),
      humidity: get_in(historical_data, ["humidity"]),
      pressure: get_in(historical_data, ["pressure"]),
      visibility: Map.get(historical_data, "visibility"),
      wind_speed: get_in(historical_data, ["wind_speed"]),
      wind_direction: get_in(historical_data, ["wind_deg"]),
      weather_condition: get_weather_condition(historical_data),
      weather_description: get_weather_description(historical_data),
      clouds: get_in(historical_data, ["clouds"]),
      sunrise: get_in(historical_data, ["sunrise"]) |> unix_to_datetime(),
      sunset: get_in(historical_data, ["sunset"]) |> unix_to_datetime(),
      data_source: "openweathermap",
      data_type: "historical"
    }

    Logger.info(
      "WeatherEnricher: Parsed historical weather data - Temp: #{weather_data.temperature}°F, Condition: #{weather_data.weather_description}"
    )

    Logger.info(
      "WeatherEnricher: FINAL PARSED HISTORICAL WEATHER DATA: #{inspect(weather_data, pretty: true, limit: :infinity)}"
    )

    {:ok, weather_data}
  end

  defp get_weather_condition(data) do
    case get_in(data, ["weather", Access.at(0), "main"]) do
      nil -> nil
      condition -> String.downcase(condition)
    end
  end

  defp get_weather_description(data) do
    get_in(data, ["weather", Access.at(0), "description"])
  end

  defp unix_to_datetime(nil), do: nil

  defp unix_to_datetime(timestamp) when is_integer(timestamp) do
    DateTime.from_unix!(timestamp) |> DateTime.to_naive()
  end
end
