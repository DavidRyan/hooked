defmodule HookedApi.Enrichment do
  alias HookedApi.Enrichment.EnrichmentOrchestrator
  def enrich_catch_data(attrs) do
    EnrichmentOrchestrator.enrich(attrs)
  end
end
