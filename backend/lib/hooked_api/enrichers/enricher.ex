defmodule HookedApi.Enrichers.Enricher do
  @callback enrich(struct(), map()) :: {:ok, struct()} | {:error, term()}
end