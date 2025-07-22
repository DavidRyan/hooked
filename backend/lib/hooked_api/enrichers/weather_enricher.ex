defmodule HookedApi.Enrichers.WeatherEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  require Logger

  @current_weather_base_url "https://api.openweathermap.org/data/2.5"
  @historical_weather_base_url "https://api.openweathermap.org/data/3.0"
  @current_weather_endpoint "/weather"
  @historical_weather_endpoint "/onecall/timemachine"

  def enrich(user_catch) do
    Logger.debug("WeatherEnricher: Processing catch #{user_catch.id}")

    with {:ok, api_key} <- get_api_key(),
         {:ok, lat, lng} <- get_coordinates(user_catch),
         {:ok, weather_data} <- fetch_weather_data(lat, lng, user_catch.caught_at, api_key) do
      Logger.info(
        "WeatherEnricher: Successfully enriched catch #{user_catch.id} with weather data"
      )

      {:ok, %{user_catch | weather_data: weather_data}}
    else
      {:error, :no_api_key} ->
        Logger.warning(
          "WeatherEnricher: OpenWeatherMap API key not configured for catch #{user_catch.id}"
        )

        {:ok, user_catch}

      {:error, :no_coordinates} ->
        Logger.debug(
          "WeatherEnricher: No coordinates available for weather enrichment for catch #{user_catch.id}"
        )

        {:ok, user_catch}

      {:error, reason} ->
        Logger.error(
          "WeatherEnricher: Weather enrichment failed for catch #{user_catch.id}: #{inspect(reason)}"
        )

        # Continue without weather data on failure
        {:ok, user_catch}
    end
  end

  defp get_api_key do
    case Application.get_env(:hooked_api, :openweather_api_key) do
      nil ->
        Logger.warning("WeatherEnricher: No OpenWeatherMap API key configured")
        {:error, :no_api_key}

      key when is_binary(key) ->
        Logger.debug("WeatherEnricher: API key found (#{String.length(key)} characters)")
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

  defp fetch_weather_data(lat, lng, caught_at, api_key) do
    case is_historical_request?(caught_at) do
      true ->
        Logger.debug(
          "WeatherEnricher: Fetching historical weather data for #{lat}, #{lng} at #{caught_at}"
        )

        fetch_historical_weather(lat, lng, caught_at, api_key)

      false ->
        Logger.debug("WeatherEnricher: Fetching current weather data for #{lat}, #{lng}")
        fetch_current_weather(lat, lng, api_key)
    end
  end

  defp is_historical_request?(caught_at) do
    now = NaiveDateTime.utc_now()
    hours_diff = NaiveDateTime.diff(now, caught_at, :hour)
    is_historical = hours_diff > 1

    Logger.debug(
      "WeatherEnricher: Time difference: #{hours_diff} hours, using #{if is_historical, do: "historical", else: "current"} weather API"
    )

    is_historical
  end

  defp fetch_current_weather(lat, lng, api_key) do
    url = "#{@current_weather_base_url}#{@current_weather_endpoint}"

    params = %{
      lat: lat,
      lon: lng,
      appid: api_key,
      units: "imperial"
    }

    Logger.info("WeatherEnricher: Fetching current weather from #{url}")

    Logger.debug(
      "WeatherEnricher: Request params: #{inspect(Map.put(params, :appid, "[REDACTED]"))}"
    )

    case make_request(url, params) do
      {:ok, response} ->
        Logger.info("WeatherEnricher: Successfully received current weather data")
        Logger.debug("WeatherEnricher: Response keys: #{inspect(Map.keys(response))}")
        parse_current_weather_response(response)

      error ->
        Logger.error("WeatherEnricher: Current weather request failed: #{inspect(error)}")
        error
    end
  end

  defp fetch_historical_weather(lat, lng, caught_at, api_key) do
    timestamp = DateTime.from_naive!(caught_at, "Etc/UTC") |> DateTime.to_unix()
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

    Logger.debug("WeatherEnricher: Timestamp: #{timestamp} (#{caught_at})")

    case make_request(url, params) do
      {:ok, response} ->
        Logger.info("WeatherEnricher: Successfully received historical weather data")
        Logger.debug("WeatherEnricher: Response keys: #{inspect(Map.keys(response))}")
        parse_historical_weather_response(response)

      error ->
        Logger.error("WeatherEnricher: Historical weather request failed: #{inspect(error)}")
        error
    end
  end

  defp make_request(url, params) do
    full_url = url <> "?" <> URI.encode_query(Map.put(params, :appid, "[REDACTED]"))
    Logger.debug("WeatherEnricher: Making request to #{full_url}")

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
        {:ok, body}

      {:ok, %Tesla.Env{status: 401, body: body}} ->
        Logger.error(
          "WeatherEnricher: HTTP 401 - Unauthorized. API key may be invalid or expired"
        )

        Logger.error("WeatherEnricher: Response body: #{inspect(body)}")
        {:error, :unauthorized}

      {:ok, %Tesla.Env{status: 404, body: body}} ->
        Logger.error("WeatherEnricher: HTTP 404 - Not found")
        Logger.error("WeatherEnricher: Response body: #{inspect(body)}")
        {:error, :not_found}

      {:ok, %Tesla.Env{status: status_code, body: body}} ->
        Logger.error("WeatherEnricher: HTTP #{status_code} - Unexpected status code")
        Logger.error("WeatherEnricher: Response body: #{inspect(body)}")
        {:error, {:http_error, status_code}}

      {:error, reason} ->
        Logger.error("WeatherEnricher: Request failed: #{inspect(reason)}")
        {:error, {:request_failed, reason}}
    end
  end

  defp parse_current_weather_response(response) do
    Logger.debug("WeatherEnricher: Parsing current weather response")

    weather_data = %{
      temperature: get_in(response, ["main", "temp"]),
      feels_like: get_in(response, ["main", "feels_like"]),
      humidity: get_in(response, ["main", "humidity"]),
      pressure: get_in(response, ["main", "pressure"]),
      visibility: Map.get(response, "visibility"),
      wind_speed: get_in(response, ["wind", "speed"]),
      wind_direction: get_in(response, ["wind", "deg"]),
      weather_condition: get_weather_condition(response),
      weather_description: get_weather_description(response),
      clouds: get_in(response, ["clouds", "all"]),
      sunrise: get_in(response, ["sys", "sunrise"]) |> unix_to_datetime(),
      sunset: get_in(response, ["sys", "sunset"]) |> unix_to_datetime(),
      data_source: "openweathermap",
      data_type: "current"
    }

    Logger.info(
      "WeatherEnricher: Parsed weather data - Temp: #{weather_data.temperature}°F, Condition: #{weather_data.weather_description}"
    )

    {:ok, weather_data}
  end

  defp parse_historical_weather_response(response) do
    Logger.debug("WeatherEnricher: Parsing historical weather response")
    current_data = Map.get(response, "current", %{})

    if map_size(current_data) == 0 do
      Logger.warning("WeatherEnricher: No 'current' data found in historical response")
      Logger.debug("WeatherEnricher: Available response keys: #{inspect(Map.keys(response))}")
    end

    weather_data = %{
      temperature: get_in(current_data, ["temp"]),
      feels_like: get_in(current_data, ["feels_like"]),
      humidity: get_in(current_data, ["humidity"]),
      pressure: get_in(current_data, ["pressure"]),
      visibility: Map.get(current_data, "visibility"),
      wind_speed: get_in(current_data, ["wind_speed"]),
      wind_direction: get_in(current_data, ["wind_deg"]),
      weather_condition: get_weather_condition(current_data),
      weather_description: get_weather_description(current_data),
      clouds: get_in(current_data, ["clouds"]),
      sunrise: get_in(current_data, ["sunrise"]) |> unix_to_datetime(),
      sunset: get_in(current_data, ["sunset"]) |> unix_to_datetime(),
      data_source: "openweathermap",
      data_type: "historical"
    }

    Logger.info(
      "WeatherEnricher: Parsed historical weather data - Temp: #{weather_data.temperature}°F, Condition: #{weather_data.weather_description}"
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
