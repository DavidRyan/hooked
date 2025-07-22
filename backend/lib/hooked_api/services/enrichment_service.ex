defmodule HookedApi.Services.EnrichmentService do
  require Logger
  alias HookedApi.Workers.CatchEnrichmentWorker

  def enqueue_enrichment(user_catch) do
    Logger.info("Enqueueing enrichment for catch #{user_catch.id}")

    result =
      %{catch_id: user_catch.id, user_catch: user_catch}
      |> CatchEnrichmentWorker.new()
      |> Oban.insert()

    case result do
      {:ok, job} ->
        Logger.info("Successfully enqueued enrichment job #{job.id} for catch #{user_catch.id}")
        result

      {:error, reason} ->
        Logger.error(
          "Failed to enqueue enrichment for catch #{user_catch.id}: #{inspect(reason)}"
        )

        result
    end
  end

  def get_configured_enrichers do
    enrichers =
      Application.get_env(:hooked_api, :enrichers, [
        HookedApi.Enrichers.GeoEnricher,
        HookedApi.Enrichers.WeatherEnricher,
        HookedApi.Enrichers.Species.SpeciesEnricher
      ])

    Logger.debug("Configured enrichers: #{inspect(enrichers)}")
    enrichers
  end
end
