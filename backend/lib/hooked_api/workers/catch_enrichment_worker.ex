defmodule HookedApi.Workers.CatchEnrichmentWorker do
  use Oban.Worker, queue: :catch_enrichment, max_attempts: 3

  alias HookedApi.Catches

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"catch_id" => catch_id}}) do
    case Catches.get_user_catch!(catch_id) do
      nil ->
        {:error, :catch_not_found}

      user_catch ->
        enrich_catch(user_catch)
    end
  rescue
    error ->
      require Logger
      Logger.error("Failed to enrich catch #{catch_id}: #{inspect(error)}")
      {:error, error}
  end

  defp enrich_catch(user_catch) do
    enrichers = [
      HookedApi.Enrichers.GeoEnricher,
      HookedApi.Enrichers.WeatherEnricher,
      HookedApi.Enrichers.SpeciesEnricher
    ]

    enriched_data = Enum.reduce(enrichers, %{}, fn enricher, acc ->
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
