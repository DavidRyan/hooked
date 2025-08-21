defmodule HookedApi.Enrichers.Species.SpeciesResult do
  @moduledoc """
  Standardized result format for species identification across all providers.

  This common data structure allows different providers (iNaturalist, Google Vision, etc.)
  to be swapped out without changing the enricher logic.
  """

  @type t :: %__MODULE__{
          species_name: String.t() | nil,
          scientific_name: String.t() | nil,
          common_name: String.t() | nil,
          confidence: float() | nil,
          provider: String.t(),
          provider_id: String.t() | nil,
          taxonomy: map() | nil,
          raw_response: map() | nil
        }

  defstruct [
    # Primary name to use (common name preferred, falls back to scientific)
    :species_name,
    # Scientific/Latin name (e.g., "Micropterus salmoides")
    :scientific_name,
    # Common name (e.g., "Largemouth Bass")
    :common_name,
    # Confidence score 0.0-1.0
    :confidence,
    # Provider name (e.g., "inaturalist", "google_vision")
    :provider,
    # Provider's internal ID for this species
    :provider_id,
    # Taxonomic hierarchy (kingdom, phylum, class, etc.)
    :taxonomy,
    # Full provider response for debugging
    :raw_response
  ]

  @doc """
  Creates a SpeciesResult with species_name automatically chosen.
  Prefers common_name over scientific_name.
  """
  def new(attrs) do
    result = struct(__MODULE__, attrs)

    species_name =
      cond do
        result.common_name && String.trim(result.common_name) != "" ->
          result.common_name

        result.scientific_name && String.trim(result.scientific_name) != "" ->
          result.scientific_name

        true ->
          nil
      end

    %{result | species_name: species_name}
  end

  @doc """
  Creates an empty result for when no species is identified.
  """
  def no_species_found(provider, raw_response \\ nil) do
    %__MODULE__{
      species_name: nil,
      scientific_name: nil,
      common_name: nil,
      confidence: nil,
      provider: provider,
      provider_id: nil,
      taxonomy: nil,
      raw_response: raw_response
    }
  end
end
