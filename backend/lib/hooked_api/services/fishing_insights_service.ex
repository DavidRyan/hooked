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

    Please provide:
    1. Overall fishing patterns
    2. Most successful species/locations
    3. Any trends you notice
    4. Fishing tips based on this data
    """
  end

  defp format_recent_catches(catches) do
    catches
    |> Enum.take(5)
    |> Enum.map(fn user_catch ->
      "#{user_catch.species || "Unknown"} at #{user_catch.location} on #{user_catch.caught_at}"
    end)
    |> Enum.join("\n")
  end
end
