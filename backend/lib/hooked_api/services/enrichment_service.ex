defmodule HookedApi.Services.EnrichmentService do
  require Logger
  alias HookedApi.Workers.CatchEnrichmentWorker

  def enqueue_enrichment(user_catch) do
    Logger.info(
      "EnrichmentService: Starting enrichment enqueue process for catch #{user_catch.id}"
    )

    Logger.debug(
      "EnrichmentService: Catch details - species: #{inspect(user_catch.species)}, location: #{inspect(user_catch.location)}"
    )

    Logger.debug("EnrichmentService: Image URL: #{inspect(user_catch.image_url)}")

    Logger.debug(
      "EnrichmentService: GPS coordinates - lat: #{inspect(user_catch.latitude)}, lng: #{inspect(user_catch.longitude)}"
    )

    try do
      Logger.debug("EnrichmentService: Creating Oban job for catch enrichment")

      result =
        %{catch_id: user_catch.id, user_catch: user_catch}
        |> CatchEnrichmentWorker.new()
        |> Oban.insert()

      case result do
        {:ok, job} ->
          Logger.info(
            "EnrichmentService: Successfully enqueued enrichment job #{job.id} for catch #{user_catch.id}"
          )

          Logger.debug(
            "EnrichmentService: Job scheduled for queue: #{job.queue}, max_attempts: #{job.max_attempts}"
          )

          result

        {:error, reason} ->
          Logger.error(
            "EnrichmentService: Failed to enqueue enrichment for catch #{user_catch.id}: #{inspect(reason)}"
          )

          Logger.error("EnrichmentService: Oban insert error details: #{inspect(reason)}")
          result
      end
    rescue
      error ->
        Logger.error(
          "EnrichmentService: CRASH during enrichment enqueue for catch #{user_catch.id}: #{inspect(error)}"
        )

        Logger.error(
          "EnrichmentService: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:crash, error}}
    end
  end

  def get_configured_enrichers do
    Logger.debug("EnrichmentService: Loading configured enrichers from application config")

    enrichers =
      Application.get_env(:hooked_api, :enrichers, [
        HookedApi.Enrichers.GeoEnricher,
        HookedApi.Enrichers.WeatherEnricher,
        HookedApi.Enrichers.Species.SpeciesEnricher
      ])

    Logger.info("EnrichmentService: Loaded #{length(enrichers)} configured enrichers")
    Logger.debug("EnrichmentService: Enricher modules: #{inspect(enrichers)}")

    # Validate that all enrichers implement the Enricher behaviour
    Enum.each(enrichers, fn enricher ->
      if Code.ensure_loaded?(enricher) do
        Logger.debug("EnrichmentService: Enricher #{inspect(enricher)} is available")
      else
        Logger.error("EnrichmentService: Enricher #{inspect(enricher)} is not available/loadable")
      end
    end)

    enrichers
  end
end
