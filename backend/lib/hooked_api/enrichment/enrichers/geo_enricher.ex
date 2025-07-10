defmodule HookedApi.Enrichment.Enrichers.GeoEnricher do 
  @behaviour HookedApi.Enrichment.Enricher
  def enrich(user_catch) do
    IO.inspect(user_catch)
  end
end
