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
    Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())
    {:ok, %{}}
  end

  @impl true
  def handle_info({:enrichment_completed, catch_id, enriched_data}, state) do
    case Catches.get_user_catch(catch_id) do
      nil ->
        Logger.error("Catch not found for enrichment: #{catch_id}")

      user_catch ->
        case Catches.update_user_catch(user_catch, enriched_data) do
          {:ok, updated_catch} ->
            Logger.info("Successfully enriched catch #{updated_catch.id}")

          {:error, changeset} ->
            Logger.error("Failed to update catch #{catch_id}: #{inspect(changeset.errors)}")
        end
    end

    {:noreply, state}
  end

  def handle_info({:enrichment_failed, catch_id, error}, state) do
    Logger.error("Enrichment failed for catch #{catch_id}: #{inspect(error)}")
    {:noreply, state}
  end

  def handle_info(_msg, state), do: {:noreply, state}
end