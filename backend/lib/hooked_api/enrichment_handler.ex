defmodule HookedApi.EnrichmentHandler do
  use GenServer
  require Logger

  alias HookedApi.Catches
  alias HookedApi.Skunks
  alias HookedApi.PubSubTopics

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, [], name: __MODULE__)
  end

  @impl true
  def init(_) do
    Logger.info("EnrichmentHandler starting and subscribing to enrichment events")
    Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())
    Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.skunk_enrichment())
    {:ok, %{}}
  end

  # Catch enrichment events
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

  # Skunk enrichment events
  def handle_info({:skunk_enrichment_completed, skunk_id, enriched_user_skunk}, state) do
    Logger.info("Received enrichment completion event for skunk #{skunk_id}")

    case Skunks.replace_user_skunk(enriched_user_skunk) do
      {:ok, updated_skunk} ->
        Logger.info("Successfully saved enriched skunk #{updated_skunk.id} to database")

      {:error, changeset} ->
        Logger.error(
          "Failed to save enriched skunk #{skunk_id} to database: #{inspect(changeset.errors)}"
        )
    end

    {:noreply, state}
  end

  def handle_info({:skunk_enrichment_failed, skunk_id, error}, state) do
    Logger.error("Received enrichment failure event for skunk #{skunk_id}: #{inspect(error)}")
    {:noreply, state}
  end

  def handle_info(msg, state) do
    Logger.debug("EnrichmentHandler received unexpected message: #{inspect(msg)}")
    {:noreply, state}
  end
end
