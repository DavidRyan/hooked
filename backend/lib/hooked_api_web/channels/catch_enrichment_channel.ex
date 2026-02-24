defmodule HookedApiWeb.CatchEnrichmentChannel do
  use Phoenix.Channel

  require Logger

  @impl true
  def join(
        "catch_enrichment:" <> user_id,
        _params,
        %{assigns: %{current_user_id: current_user_id}} = socket
      ) do
    Logger.debug("CatchEnrichmentChannel: join request for user #{user_id}")

    if user_id == current_user_id do
      Logger.info("CatchEnrichmentChannel: join approved for user #{user_id}")
      {:ok, socket}
    else
      Logger.warning(
        "CatchEnrichmentChannel: join rejected, token user #{current_user_id} does not match topic user #{user_id}"
      )

      {:error, %{reason: "unauthorized"}}
    end
  end

  def join(_topic, _params, _socket) do
    Logger.warning("CatchEnrichmentChannel: join rejected for unmatched topic")
    {:error, %{reason: "unauthorized"}}
  end

  @impl true
  def handle_in(_event, _payload, socket) do
    Logger.debug("CatchEnrichmentChannel: ignoring inbound event on enrichment channel")
    {:noreply, socket}
  end
end
