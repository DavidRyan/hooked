defmodule HookedApi.Enrichment.Enrichers.WeatherEnricher do 
  @behaviour HookedApi.Enrichment.Enricher
  def enrich(user_catch) do
    IO.inspect(user_catch)
    user_catch
  end
end
