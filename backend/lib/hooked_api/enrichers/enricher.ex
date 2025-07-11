defmodule HookedApi.Enrichers.Enricher do
  @callback enrich(struct(), map()) :: map()
end