defmodule HookedApi.Enrichers.Species.SpeciesProvider do
  @moduledoc """
  Common interface for species identification providers.

  All species identification providers must implement this behaviour
  and return results in the standardized SpeciesResult format.
  """

  @type image_data :: binary()
  @type filename :: String.t()

  @callback identify_species(image_data, filename) ::
              {:ok, HookedApi.Enrichers.Species.SpeciesResult.t()} | {:error, term()}
  @callback validate_configuration() :: :ok | {:error, term()}
end
