defmodule HookedApi.Services.FishingInsightsService do
  @ai_provider Application.compile_env(
                 :hooked_api,
                 :ai_provider,
                 HookedApi.Services.AiProviders.OpenaiProvider
               )
  require Logger

  def get_insights(catches, skunks \\ [])
  def get_insights([], []), do: {:error, :no_data}

  def get_insights(catches, skunks) when is_list(catches) and is_list(skunks) do
    if Enum.empty?(catches) and Enum.empty?(skunks) do
      {:error, :no_data}
    else
      with :ok <- @ai_provider.validate_configuration() do
        prompt = build_analysis_prompt(catches, skunks)
        Logger.info("Sending fishing insights prompt to AI provider")
        @ai_provider.send_message(prompt)
      end
    end
  end

  defp build_analysis_prompt(catches, skunks) do
    total_catches = length(catches)
    total_skunks = length(skunks)

    species_breakdown =
      catches
      |> Enum.group_by(& &1.species)
      |> Enum.map(fn {species, group} -> "#{species || "Unknown"}: #{length(group)}" end)
      |> Enum.join(", ")

    locations =
      catches
      |> Enum.map(& &1.location)
      |> Enum.uniq()
      |> length()

    skunk_section = build_skunk_section(skunks)

    """
    Analyze these #{total_catches} fishing catches#{if total_skunks > 0, do: " and #{total_skunks} unsuccessful fishing trips (skunks)", else: ""} and provide insights:

    Species breakdown: #{species_breakdown}
    Unique locations: #{locations}

    Recent catches:
    #{format_recent_catches(catches)}
    #{skunk_section}

    Please provide Fishing tips based on this data

    Focus on time and weather like pressure, wind, and sunrise/sunset
    Be specific with suggested weather conditions
    Make sure to keep the response under 1000 characters
    No bullets or numbers, make it conversational. You are a fishing instructor
    The person you are talking to does not know what the weather was when these catches were made, so please provide the best weather conditions with specifics
    Do not add any additional fluff at the beginning or end.  Be very to the point
    Do not preface your response with "Based on the data you provided, here are some fishing tips"

    #{if total_skunks > 0, do: "Pay special attention to the unsuccessful trips (skunks) and identify what conditions or patterns led to no catches. Compare the skunk conditions with successful catch conditions to provide actionable advice on when NOT to fish or what to change.", else: ""}

    Use the recent catch data provided to specifically make insights into patterns and trends
    """
  end

  defp build_skunk_section([]), do: ""

  defp build_skunk_section(skunks) do
    skunk_entries =
      skunks
      |> Enum.take(10)
      |> Enum.map(&format_single_skunk/1)
      |> Enum.join("\n")

    """

    Unsuccessful fishing trips (skunks - no fish caught):
    #{skunk_entries}
    """
  end

  defp format_single_skunk(skunk) do
    """
    NO CATCH at #{skunk.location || "Unknown location"}
    Date: #{skunk.fished_at}
    #{if skunk.notes, do: "Notes: #{skunk.notes}", else: ""}
    #{format_weather(skunk.weather_data)}
    """
  end

  defp format_recent_catches(catches) do
    catches
    |> Enum.take(20)
    |> Enum.map(&format_single_catch/1)
    |> Enum.join("\n")
  end

  defp format_single_catch(user_catch) do
    """
    #{user_catch.species || "Unknown species"} at #{user_catch.location}
    Date: #{user_catch.caught_at}
    #{format_weather(user_catch.weather_data)}
    """
  end

  # Normalize weather data for prompt insertion. Supports both the OpenWeather
  # shape (`main.temp`, `weather[0].description`, `wind.speed`, `clouds.all`) and
  # the older flat shape (`temperature`, `weather_description`, etc.). Returns a
  # multiline string; gracefully omits fields that aren't present.
  defp format_weather(nil), do: "Weather: No data available"

  defp format_weather(weather) when is_map(weather) do
    w = extract_weather(weather)
    parts =
      [
        if(w.temp, do: "Temperature: #{format_num(w.temp)}°F" <> (if w.feels_like, do: " (feels like #{format_num(w.feels_like)}°F)", else: "")),
        if(w.description || w.condition, do: "Condition: #{w.description || w.condition}#{if w.description && w.condition && w.condition != w.description, do: " (#{w.condition})", else: ""}"),
        if(w.humidity, do: "Humidity: #{format_num(w.humidity)}%"),
        if(w.pressure, do: "Pressure: #{format_num(w.pressure)} hPa"),
        if(w.wind_speed, do: "Wind: #{format_num(w.wind_speed)} mph#{if w.wind_dir, do: " from #{format_num(w.wind_dir)}°", else: ""}"),
        if(w.clouds_pct, do: "Clouds: #{format_num(w.clouds_pct)}%"),
        if(w.sunrise, do: "Sunrise: #{w.sunrise}"),
        if(w.sunset, do: "Sunset: #{w.sunset}")
      ]
      |> Enum.reject(&is_nil/1)

    if parts == [] do
      "Weather: No data available"
    else
      "Weather Data:\n" <> Enum.map_join(parts, "\n", &("- " <> &1))
    end
  end

  defp format_weather(_), do: "Weather: No data available"

  defp extract_weather(w) do
    main = pick_map(w["main"])
    wind = pick_map(w["wind"])
    clouds = pick_map(w["clouds"])
    weather_arr = if is_list(w["weather"]), do: List.first(w["weather"]), else: nil
    weather0 = pick_map(weather_arr)

    %{
      temp: numeric(main["temp"]) || numeric(w["temp"]) || numeric(w["temperature"]),
      feels_like: numeric(main["feels_like"]) || numeric(w["feels_like"]),
      humidity: numeric(main["humidity"]) || numeric(w["humidity"]),
      pressure: numeric(main["pressure"]) || numeric(w["pressure"]),
      wind_speed: numeric(wind["speed"]) || numeric(w["wind_speed"]),
      wind_dir: numeric(wind["deg"]) || numeric(w["wind_direction"]),
      clouds_pct: numeric(clouds["all"]) || numeric(w["cloud_cover"]),
      description: string(weather0["description"]) || string(w["weather_description"]) || string(w["description"]),
      condition: string(weather0["main"]) || string(w["weather_condition"]),
      sunrise: string(w["sunrise"]),
      sunset: string(w["sunset"])
    }
  end

  defp pick_map(m) when is_map(m), do: m
  defp pick_map(_), do: %{}

  defp numeric(n) when is_number(n), do: n

  defp numeric(s) when is_binary(s) do
    case Float.parse(s) do
      {val, _} -> val
      :error -> nil
    end
  end

  defp numeric(_), do: nil

  defp string(s) when is_binary(s) and s != "", do: s
  defp string(_), do: nil

  defp format_num(n) when is_integer(n), do: Integer.to_string(n)
  defp format_num(n) when is_float(n), do: :erlang.float_to_binary(n, decimals: 1)
  defp format_num(n), do: to_string(n)
end
