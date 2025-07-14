defmodule HookedApi.Enrichers.WeatherEnricher do
  @behaviour HookedApi.Enrichers.Enricher
  
  require Logger

  @current_weather_base_url "https://api.openweathermap.org/data/2.5"
  @historical_weather_base_url "https://api.openweathermap.org/data/3.0"
  @current_weather_endpoint "/weather"
  @historical_weather_endpoint "/onecall/timemachine"

  def enrich(user_catch) do
    with {:ok, api_key} <- get_api_key(),
         {:ok, lat, lng} <- get_coordinates(user_catch),
         {:ok, weather_data} <- fetch_weather_data(lat, lng, user_catch.caught_at, api_key) do
      {:ok, %{user_catch | weather_data: weather_data}}
    else
      {:error, :no_api_key} ->
        Logger.warning("OpenWeatherMap API key not configured")
        {:ok, user_catch}
      
      {:error, :no_coordinates} ->
        Logger.debug("No coordinates available for weather enrichment")
        {:ok, user_catch}
      
      {:error, reason} ->
        Logger.error("Weather enrichment failed: #{inspect(reason)}")
        {:ok, user_catch}  # Continue without weather data on failure
    end
  end

  defp get_api_key do
    case Application.get_env(:hooked_api, :openweather_api_key) do
      nil -> {:error, :no_api_key}
      key when is_binary(key) -> {:ok, key}
      _ -> {:error, :invalid_api_key}
    end
  end

  defp get_coordinates(user_catch) do
    case {user_catch.latitude, user_catch.longitude} do
      {lat, lng} when is_float(lat) and is_float(lng) ->
        {:ok, lat, lng}
      _ ->
        {:error, :no_coordinates}
    end
  end

  defp fetch_weather_data(lat, lng, caught_at, api_key) do
    case is_historical_request?(caught_at) do
      true -> fetch_historical_weather(lat, lng, caught_at, api_key)
      false -> fetch_current_weather(lat, lng, api_key)
    end
  end

  defp is_historical_request?(caught_at) do
    now = NaiveDateTime.utc_now()
    NaiveDateTime.diff(now, caught_at, :hour) > 1
  end

  defp fetch_current_weather(lat, lng, api_key) do
    url = "#{@current_weather_base_url}#{@current_weather_endpoint}"
    
    params = %{
      lat: lat,
      lon: lng,
      appid: api_key,
      units: "imperial"
    }

    case make_request(url, params) do
      {:ok, response} -> parse_current_weather_response(response)
      error -> error
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

    case make_request(url, params) do
      {:ok, response} -> parse_historical_weather_response(response)
      error -> error
    end
  end

  defp make_request(url, params) do
    client = Tesla.client([
      {Tesla.Middleware.BaseUrl, url},
      {Tesla.Middleware.Query, params},
      Tesla.Middleware.JSON,
      {Tesla.Middleware.Timeout, timeout: 10_000}
    ])

    case Tesla.get(client, "") do
      {:ok, %Tesla.Env{status: 200, body: body}} ->
        {:ok, body}
      
      {:ok, %Tesla.Env{status: 401}} ->
        {:error, :unauthorized}
      
      {:ok, %Tesla.Env{status: 404}} ->
        {:error, :not_found}
      
      {:ok, %Tesla.Env{status: status_code}} ->
        {:error, {:http_error, status_code}}
      
      {:error, reason} ->
        {:error, {:request_failed, reason}}
    end
  end

  defp parse_current_weather_response(response) do
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

    {:ok, weather_data}
  end

  defp parse_historical_weather_response(response) do
    current_data = Map.get(response, "current", %{})
    
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