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
    weather_info =
      case skunk.weather_data do
        nil ->
          "Weather: No data available"

        weather ->
          """
          Weather Data:
          - Temperature: #{weather["temperature"]}°F (feels like #{weather["feels_like"]}°F)
          - Condition: #{weather["weather_description"]} (#{weather["weather_condition"]})
          - Humidity: #{weather["humidity"]}%
          - Pressure: #{weather["pressure"]} hPa
          - Wind: #{weather["wind_speed"]} mph from #{weather["wind_direction"]}°
          - Clouds: #{weather["clouds"]}%
          """
      end

    """
    NO CATCH at #{skunk.location || "Unknown location"}
    Date: #{skunk.fished_at}
    #{if skunk.notes, do: "Notes: #{skunk.notes}", else: ""}
    #{weather_info}
    """
  end

  defp format_recent_catches(catches) do
    catches
    |> Enum.take(20)
    |> Enum.map(&format_single_catch/1)
    |> Enum.join("\n")
  end

  defp format_single_catch(user_catch) do
    weather_info =
      case user_catch.weather_data do
        nil ->
          "Weather: No data available"

        weather ->
          """
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
