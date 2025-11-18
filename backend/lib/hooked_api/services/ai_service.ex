defmodule HookedApi.Services.AiService do
  @ai_provider Application.compile_env(
                 :hooked_api,
                 :ai_provider,
                 HookedApi.Services.AiProviders.OpenaiProvider
               )

  require Logger

  def get_insights([]) do
    {:error, :no_catches}
  end

  def get_insights(catches) when is_list(catches) do
    with :ok <- @ai_provider.validate_configuration() do
      prompt = build_catch_analysis_prompt(catches)
      Logger.debug("AI Service: Generated analysis prompt (length: #{String.length(prompt)})")

      case @ai_provider.send_message(prompt) do
        {:ok, response} ->
          Logger.info("AI Service: Successfully received insights from AI provider")
          {:ok, response}

        {:error, reason} = error ->
          Logger.error("AI Service: Failed to generate insights: #{inspect(reason)}")
          error
      end
    else
      {:error, reason} = error ->
        Logger.error("AI Service: Configuration validation failed: #{inspect(reason)}")
        error
    end
  end

  def get_insights(invalid_input) do
    Logger.error("AI Service: Invalid input for insights generation: #{inspect(invalid_input)}")
    {:error, :invalid_input}
  end

  defp build_catch_analysis_prompt(catches) do
    total_catches = length(catches)

    prompt = """
    You are a fishing expert and have been tasked with analyzing a user's fishing history.
    Analyze these #{total_catches} fishing catches and provide insights:

    Recent catches:
    #{inspect(catches)}

    Please provide:
    1. Overall fishing patterns
    2. Most successful species/locations
    3. Any trends you notice
    4. Fishing tips based on this data
    """

    Logger.debug("AI Service: Generated prompt with #{String.length(prompt)}")
    prompt
  end
end
