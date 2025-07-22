defmodule HookedApi.EnrichmentHandler do
  use GenServer
  require Logger

  alias HookedApi.Catches
  alias HookedApi.PubSubTopics

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, [], name: __MODULE__)
  end

  @impl true
  def init(_) do
    Logger.info("EnrichmentHandler starting and subscribing to catch enrichment events")
    Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())
    {:ok, %{}}
  end

  @impl true
  def handle_info({:enrichment_completed, catch_id, enriched_user_catch}, state) do
    Logger.info("Received enrichment completion event for catch #{catch_id}")

    case Catches.replace_user_catch(enriched_user_catch) do
      {:ok, updated_catch} ->
        Logger.info("Successfully saved enriched catch #{updated_catch.id} to database")

      {:error, changeset} ->
        Logger.error(
          "Failed to save enriched catch #{catch_id} to database: #{inspect(changeset.errors)}"
        )
    end

    {:noreply, state}
  end

  def handle_info({:enrichment_failed, catch_id, error}, state) do
    Logger.error("Received enrichment failure event for catch #{catch_id}: #{inspect(error)}")
    {:noreply, state}
  end

  def handle_info(msg, state) do
    Logger.debug("EnrichmentHandler received unexpected message: #{inspect(msg)}")
    {:noreply, state}
  end
end
