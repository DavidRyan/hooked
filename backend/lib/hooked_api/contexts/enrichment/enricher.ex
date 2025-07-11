defmodule HookedApi.Enrichment.Enricher do
  @callback enrich(HookedApi.Catch) :: HookedApi.Catch
end
