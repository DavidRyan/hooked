defmodule HookedApi.Enrichment.EnrichmentOrchestrator do
  def enrich(user_catch) do 
    enrichers = [
      HookedApi.Enrichment.Enrichers.GeoEnricher,
      HookedApi.Enrichment.Enrichers.WeatherEnricher
    ]
    Enum.reduce(enrichers, user_catch, fn enricher, user_catch ->
      enricher.enrich(user_catch)
    end)
  end
end
