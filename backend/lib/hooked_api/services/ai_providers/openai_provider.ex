defmodule HookedApi.Services.AiProviders.OpenaiProvider do
  @behaviour HookedApi.Services.AiProvider

  use Tesla
  require Logger

  plug(Tesla.Middleware.JSON)
  plug(Tesla.Middleware.BaseUrl, "https://api.openai.com/v1")

  plug(Tesla.Middleware.Headers, [
    {"Authorization", "Bearer #{Application.get_env(:hooked_api, :openai_api_key)}"}
  ])

  adapter(Tesla.Adapter.Hackney)

  @impl true
  def provider_name, do: "openai"

  @impl true
  def validate_configuration do

    case Application.get_env(:hooked_api, :openai_api_key) do
      nil ->
        Logger.error("OpenAI Provider: No API key configured")
        {:error, :no_api_key}

      key when is_binary(key) and byte_size(key) > 10 ->
        Logger.info("OpenAI Provider: API key properly configured (length: #{byte_size(key)})")
        :ok

      _ ->
        Logger.error("OpenAI Provider: Invalid API key configuration")
        {:error, :invalid_api_key}
    end
  end

  @impl true
  def send_message(message) when is_binary(message) do

    payload = %{
      model: "gpt-3.5-turbo",
      messages: [%{role: "user", content: message}],
      max_tokens: 1000,
      temperature: 0.7
    }

    case post("/chat/completions", payload) do
      {:ok,
       %Tesla.Env{
         status: 200,
         body: %{"choices" => [%{"message" => %{"content" => content}} | _]}
       }} ->
        trimmed_content = String.trim(content)
        content_length = String.length(trimmed_content)

        Logger.info(
          "OpenAI Provider: Received successful response (content length: #{content_length})"
        )

        Logger.debug(
          "OpenAI Provider: First 100 chars of response: #{String.slice(trimmed_content, 0..99)}"
        )

        {:ok, trimmed_content}


      error ->
        Logger.info("OpenAI Provider Failed: Request details: #{inspect(error)}")
        {:error, :ai_request_failed}
    end
  end

  def send_message(_) do
    Logger.error("OpenAI Provider: Invalid message format provided")
    {:error, :invalid_message}
  end
end
