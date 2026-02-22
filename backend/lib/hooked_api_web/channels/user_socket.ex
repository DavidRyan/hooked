defmodule HookedApiWeb.UserSocket do
  use Phoenix.Socket

  alias HookedApi.Accounts

  channel("catch_enrichment:*", HookedApiWeb.CatchEnrichmentChannel)

  @impl true
  def connect(%{"token" => token}, socket, _connect_info) do
    case Accounts.verify_token(token) do
      {:ok, user} ->
        {:ok, assign(socket, :current_user_id, user.id)}

      {:error, _reason} ->
        :error
    end
  end

  def connect(_params, _socket, _connect_info), do: :error

  @impl true
  def id(%{assigns: %{current_user_id: user_id}}) when is_binary(user_id),
    do: "users_socket:#{user_id}"

  def id(_socket), do: nil
end
