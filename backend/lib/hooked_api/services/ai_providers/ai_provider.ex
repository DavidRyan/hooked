defmodule HookedApi.Services.AiProvider do
  @moduledoc """
  Common interface for AI service providers.

  All AI providers must implement this behaviour for text-based interactions.
  """

  @type message :: String.t()
  @type response :: String.t()

  @callback send_message(message) :: {:ok, response} | {:error, term()}
  @callback validate_configuration() :: :ok | {:error, term()}
  @callback provider_name() :: String.t()
end
