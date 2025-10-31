defmodule HookedApi.Services.AiService do
  @ai_provider Application.compile_env(:hooked_api, :ai_provider,
                HookedApi.Services.AiProviders.OpenaiProvider)

  def get_insights([]), do: {:error, :no_catches}
  def get_insights(catches) when is_list(catches) do
    with :ok <- @ai_provider.validate_configuration() do
      prompt = build_catch_analysis_prompt(catches)
      @ai_provider.send_message(prompt)
    end
  end
  def get_insights(_), do: {:error, :invalid_input}

  defp build_catch_analysis_prompt(catches) do
    total_catches = length(catches)

    """
    You are a fishing expert and have been tasked with analyzing a user's fishing history.
    Analyze these #{total_catches} fishing catches and provide insights:

    Recent catches:
    #{catches}

    Please provide:
    1. Overall fishing patterns
    2. Most successful species/locations
    3. Any trends you notice
    4. Fishing tips based on this data
    """
  end

end
