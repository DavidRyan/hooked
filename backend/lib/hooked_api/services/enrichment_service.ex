defmodule HookedApi.Services.EnrichmentService do
  alias HookedApi.Workers.CatchEnrichmentWorker

  def enqueue_enrichment(user_catch) do
    %{catch_id: user_catch.id, user_catch: user_catch}
    |> CatchEnrichmentWorker.new()
    |> Oban.insert()
  end

  def get_configured_enrichers do
    Application.get_env(:hooked_api, :enrichers, [
      HookedApi.Enrichers.GeoEnricher,
      HookedApi.Enrichers.WeatherEnricher,
      HookedApi.Enrichers.SpeciesEnricher
    ])
  end
end