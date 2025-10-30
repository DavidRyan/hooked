defmodule HookedApi.Services.AiProviders.OpenaiProvider do
  @behaviour HookedApi.Services.AiProvider

  use Tesla

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
      nil -> {:error, :no_api_key}
      key when is_binary(key) and byte_size(key) > 10 -> :ok
      _ -> {:error, :invalid_api_key}
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
      {:ok, %Tesla.Env{status: 200, body: %{"choices" => [%{"message" => %{"content" => content}} | _]}}} ->
        {:ok, String.trim(content)}
      _ ->
        {:error, :ai_request_failed}
    end
  end

  def send_message(_), do: {:error, :invalid_message}
end
