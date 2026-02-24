defmodule HookedApiWeb.UserSocket do
  use Phoenix.Socket

  require Logger

  alias HookedApi.Accounts

  channel("catch_enrichment:*", HookedApiWeb.CatchEnrichmentChannel)

  @impl true
  def connect(%{"token" => token}, socket, _connect_info) do
    Logger.debug("UserSocket: connect attempt with token")

    case Accounts.verify_token(token) do
      {:ok, user} ->
        Logger.info("UserSocket: authenticated user #{user.id}")
        {:ok, assign(socket, :current_user_id, user.id)}

      {:error, _reason} ->
        Logger.warning("UserSocket: token verification failed")
        :error
    end
  end

  def connect(_params, _socket, _connect_info) do
    Logger.warning("UserSocket: missing token on connect")
    :error
  end

  @impl true
  def id(%{assigns: %{current_user_id: user_id}}) when is_binary(user_id),
    do: "users_socket:#{user_id}"

  def id(_socket), do: nil
end
