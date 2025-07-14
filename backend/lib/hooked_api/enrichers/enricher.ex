defmodule HookedApi.Enrichers.Enricher do
  @callback enrich(struct()) :: {:ok, struct()} | {:error, term()}
end