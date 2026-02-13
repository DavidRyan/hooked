defmodule HookedApi.Enrichers.SkunkWeatherEnricher do
  @moduledoc """
  Enriches a UserSkunk with historical weather data based on its coordinates and fished_at time.
  Thin wrapper that reuses the same OpenWeatherMap API logic as the catch WeatherEnricher
  but operates on `fished_at` instead of `caught_at`.
  """
  @behaviour HookedApi.Enrichers.Enricher
  require Logger

  @historical_weather_base_url "https://api.openweathermap.org/data/3.0"
  @historical_weather_endpoint "/onecall/timemachine"

  def enrich(user_skunk, _context \\ %{}) do
    Logger.debug("SkunkWeatherEnricher: Processing skunk #{user_skunk.id}")

    with {:ok, api_key} <- get_api_key(),
         {:ok, lat, lng} <- get_coordinates(user_skunk),
         {:ok, weather_data} <- fetch_historical_weather(lat, lng, user_skunk.fished_at, api_key) do
      Logger.info(
        "SkunkWeatherEnricher: Successfully enriched skunk #{user_skunk.id} with historical weather data"
      )

      {:ok, %{user_skunk | weather_data: weather_data}}
    else
      {:error, :no_api_key} ->
        Logger.warning(
          "SkunkWeatherEnricher: OpenWeatherMap API key not configured for skunk #{user_skunk.id}"
        )

        {:ok, %{user_skunk | enrichment_status: false}}

      {:error, :no_coordinates} ->
        Logger.debug("SkunkWeatherEnricher: No coordinates available for skunk #{user_skunk.id}")

        {:ok, %{user_skunk | enrichment_status: false}}

      {:error, reason} ->
        Logger.error(
          "SkunkWeatherEnricher: Weather enrichment failed for skunk #{user_skunk.id}: #{inspect(reason)}"
        )

        {:ok, %{user_skunk | enrichment_status: false}}
    end
  end

  defp get_api_key do
    case Application.get_env(:hooked_api, :openweather_api_key) do
      nil ->
        case System.get_env("OPENWEATHER_API_KEY") do
          nil -> {:error, :no_api_key}
          env_key when is_binary(env_key) and byte_size(env_key) > 5 -> {:ok, env_key}
          _ -> {:error, :no_api_key}
        end

      key when is_binary(key) and byte_size(key) > 5 ->
        {:ok, key}

      _ ->
        {:error, :no_api_key}
    end
  end

  defp get_coordinates(user_skunk) do
    case {user_skunk.latitude, user_skunk.longitude} do
      {lat, lng} when is_float(lat) and is_float(lng) ->
        {:ok, lat, lng}

      _ ->
        {:error, :no_coordinates}
    end
  end

  defp fetch_historical_weather(lat, lng, fished_at, api_key) do
    if api_key == "test_openweather_api_key_for_testing" do
      Logger.info(
        "SkunkWeatherEnricher: Using MOCK historical weather data (test/development mode)"
      )

      {:ok,
       %{
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
       }}
    else
      case parse_fished_at_datetime(fished_at) do
        {:ok, fished_at_datetime} ->
          timestamp = DateTime.from_naive!(fished_at_datetime, "Etc/UTC") |> DateTime.to_unix()
          url = "#{@historical_weather_base_url}#{@historical_weather_endpoint}"

          params = %{
            lat: lat,
            lon: lng,
            dt: timestamp,
            appid: api_key,
            units: "imperial"
          }

          case make_request(url, params) do
            {:ok, response} ->
              parse_historical_weather_response(response)

            error ->
              error
          end

        {:error, reason} ->
          {:error, reason}
      end
    end
  end

  defp parse_fished_at_datetime(fished_at) do
    case fished_at do
      %NaiveDateTime{} ->
        {:ok, fished_at}

      datetime_string when is_binary(datetime_string) ->
        case NaiveDateTime.from_iso8601(datetime_string) do
          {:ok, datetime} -> {:ok, datetime}
          {:error, _} -> {:error, :invalid_datetime}
        end

      _ ->
        {:error, :invalid_datetime}
    end
  end

  defp make_request(url, params) do
    client =
      Tesla.client([
        {Tesla.Middleware.BaseUrl, url},
        {Tesla.Middleware.Query, params},
        Tesla.Middleware.JSON,
        {Tesla.Middleware.Timeout, timeout: 10_000}
      ])

    case Tesla.get(client, "") do
      {:ok, %Tesla.Env{status: 200, body: body}} -> {:ok, body}
      {:ok, %Tesla.Env{status: 401}} -> {:error, :unauthorized}
      {:ok, %Tesla.Env{status: 404}} -> {:error, :not_found}
      {:ok, %Tesla.Env{status: status_code}} -> {:error, {:http_error, status_code}}
      {:error, reason} -> {:error, {:request_failed, reason}}
    end
  end

  defp parse_historical_weather_response(response) do
    historical_data =
      case get_in(response, ["data", Access.at(0)]) do
        nil -> %{}
        data -> data
      end

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
