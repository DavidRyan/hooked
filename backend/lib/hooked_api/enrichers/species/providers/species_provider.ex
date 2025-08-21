defmodule HookedApi.Enrichers.Species.Providers.SpeciesProvider do
  @moduledoc """
  Behaviour for species identification providers.

  This module defines the contract that all species identification providers must implement.
  """

  alias HookedApi.Enrichers.Species.Providers.SpeciesResult

  @doc """
  Identifies species from image data.

  ## Parameters
  - `image_data`: Binary image data
  - `filename`: Original filename for context
  - `options`: Provider-specific options (optional)

  ## Returns
  - `{:ok, [SpeciesResult.t()]}` - List of species identification results, ordered by confidence
  - `{:error, reason}` - Error tuple
  """
  @callback identify_species(binary(), String.t(), keyword()) ::
              {:ok, [SpeciesResult.t()]} | {:error, term()}

  @doc """
  Validates provider configuration.

  ## Returns
  - `:ok` - Configuration is valid
  - `{:error, reason}` - Configuration error
  """
  @callback validate_config() :: :ok | {:error, term()}

  @doc """
  Returns the provider name for logging/debugging.
  """
  @callback provider_name() :: String.t()
end
