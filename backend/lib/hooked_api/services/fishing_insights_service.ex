defmodule HookedApi.Services.FishingInsightsService do
  @ai_provider Application.compile_env(:hooked_api, :ai_provider,
                HookedApi.Services.AiProviders.OpenaiProvider)
  require Logger

  def get_insights([]), do: {:error, :no_catches}
  def get_insights(catches) when is_list(catches) do
    with :ok <- @ai_provider.validate_configuration() do
      prompt = build_catch_analysis_prompt(catches)
      Logger.info("Sending fishing insights prompt to : #{prompt}")
      @ai_provider.send_message(prompt)
    end
  end
  def get_insights(_), do: {:error, :invalid_input}

  defp build_catch_analysis_prompt(catches) do
    total_catches = length(catches)
    species_breakdown = catches
      |> Enum.group_by(& &1.species)
      |> Enum.map(fn {species, group} -> "#{species || "Unknown"}: #{length(group)}" end)
      |> Enum.join(", ")

    locations = catches
      |> Enum.map(& &1.location)
      |> Enum.uniq()
      |> length()



    """
    Analyze these #{total_catches} fishing catches and provide insights:

    Species breakdown: #{species_breakdown}
    Unique locations: #{locations}

    Recent catches:
    #{format_recent_catches(catches)}

    Please provide Fishing tips based on this data

    Focus on time and weather like pressure, wind, and sunrise/sunset
    Be specific with suggested weather conditions
    Make sure to keep the response under 1000 characters
    No bullets or numbers, make it conversational. You are a fishing instructor
    The person you are talking to does not know what the weather was when these catches were made, so please provide the best weather conditions with specifics
    Do not add any additional fluff at the beginning or end.  Be very to the point
    Do not preface your response with "Based on the data you provided, here are some fishing tips"
    """
  end

defp format_recent_catches(catches) do
  catches
  |> Enum.take(20)
  |> Enum.map(&format_single_catch/1)  # ✅ Convert each struct to string first
  |> Enum.join("\n")
end

defp format_single_catch(user_catch) do
  weather_info = case user_catch.weather_data do
    nil -> "Weather: No data available"
    weather -> """
    Weather Data:
    - Temperature: #{weather["temperature"]}°F (feels like #{weather["feels_like"]}°F)
    - Condition: #{weather["weather_description"]} (#{weather["weather_condition"]})
    - Humidity: #{weather["humidity"]}%
    - Pressure: #{weather["pressure"]} hPa
    - Wind: #{weather["wind_speed"]} mph from #{weather["wind_direction"]}°
    - Clouds: #{weather["clouds"]}%
    - Sunrise: #{weather["sunrise"]}
    - Sunset: #{weather["sunset"]}
    - Data Source: #{weather["data_source"]} (#{weather["data_type"]})
    """
  end
  """
  #{user_catch.species || "Unknown species"} at #{user_catch.location}
  Date: #{user_catch.caught_at}
  #{weather_info}
  """
end
end
