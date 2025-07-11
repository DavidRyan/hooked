defmodule HookedApi.Workers.CatchEnrichmentWorker do
  use Oban.Worker, queue: :catch_enrichment, max_attempts: 3

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"catch_id" => catch_id, "user_catch" => user_catch_data}}) do
    user_catch = struct(HookedApi.Catches.UserCatch, user_catch_data)
    
    enriched_data = enrich_catch(user_catch)
    
    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      "catch_enrichment",
      {:enrichment_completed, catch_id, enriched_data}
    )
    
    :ok
  rescue
    error ->
      require Logger
      Logger.error("Failed to enrich catch #{catch_id}: #{inspect(error)}")
      
      Phoenix.PubSub.broadcast(
        HookedApi.PubSub,
        "catch_enrichment",
        {:enrichment_failed, catch_id, error}
      )
      
      {:error, error}
  end

  defp enrich_catch(user_catch) do
    enrichers = [
      HookedApi.Enrichers.GeoEnricher,
      HookedApi.Enrichers.WeatherEnricher,
      HookedApi.Enrichers.SpeciesEnricher
    ]

    Enum.reduce(enrichers, %{}, fn enricher, acc ->
      try do
        enricher.enrich(user_catch)
        |> Map.merge(acc)
      rescue
        error ->
          require Logger
          Logger.error("Enricher #{enricher} failed: #{inspect(error)}")
          acc
      end
    end)
  end
end
