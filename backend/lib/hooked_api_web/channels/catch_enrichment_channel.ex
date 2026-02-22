defmodule HookedApiWeb.CatchEnrichmentChannel do
  use Phoenix.Channel

  @impl true
  def join(
        "catch_enrichment:" <> user_id,
        _params,
        %{assigns: %{current_user_id: current_user_id}} = socket
      ) do
    if user_id == current_user_id do
      {:ok, socket}
    else
      {:error, %{reason: "unauthorized"}}
    end
  end

  def join(_topic, _params, _socket), do: {:error, %{reason: "unauthorized"}}

  @impl true
  def handle_in(_event, _payload, socket) do
    {:noreply, socket}
  end
end
