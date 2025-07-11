defmodule HookedApi.Enrichers.SpeciesEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  def enrich(_user_catch, _exif_data) do
    %{}
  end
end