# Species Enricher - Provider Abstraction Layer

The Species Enricher uses a pluggable provider architecture that allows you to easily swap out different species identification services without changing the core enricher logic.

## Architecture

```
SpeciesEnricher
    ↓
SpeciesProvider (behaviour/interface)
    ↓
Concrete Providers:
    - InaturalistProvider 
    - GoogleVisionProvider
    - (Your custom providers)
    ↓
SpeciesResult (standardized output)
```

## Common Data Format

All providers return results in the standardized `SpeciesResult` format:

```elixir
%SpeciesResult{
  species_name: "Largemouth Bass",           # Primary name (common preferred)
  scientific_name: "Micropterus salmoides",  # Scientific/Latin name
  common_name: "Largemouth Bass",            # Common name
  confidence: 0.85,                          # Confidence score 0.0-1.0
  provider: "inaturalist",                   # Provider identifier
  provider_id: "47178",                      # Provider's internal ID
  taxonomy: %{                               # Taxonomic hierarchy
    rank: "species",
    kingdom: "Animalia", 
    phylum: "Chordata",
    class: "Actinopterygii",
    order: "Perciformes",
    family: "Centrarchidae",
    genus: "Micropterus"
  },
  raw_response: %{...}                       # Full provider response
}
```

## Available Providers

### 1. iNaturalist Provider (Default)
- **Best for**: Accurate fish/wildlife species identification
- **API**: iNaturalist Computer Vision API
- **Strengths**: Specialized for biodiversity, high accuracy, taxonomic data
- **Configuration**: `INATURALIST_ACCESS_TOKEN` environment variable

### 2. Google Vision Provider (Example)
- **Best for**: General object/animal detection
- **API**: Google Cloud Vision Label Detection API
- **Strengths**: Fast, reliable infrastructure, broad object recognition
- **Limitations**: Not specialized for species, less taxonomic detail
- **Configuration**: `GOOGLE_VISION_ACCESS_TOKEN` environment variable

## Swapping Providers

To swap providers, simply change the `@species_provider` module attribute in `species_enricher.ex`:

```elixir
# Current (iNaturalist)
@species_provider InaturalistProvider

# Switch to Google Vision
@species_provider GoogleVisionProvider

# Switch to your custom provider
@species_provider MyCustomProvider
```

## Creating Custom Providers

### 1. Implement the SpeciesProvider Behaviour

```elixir
defmodule MyApp.Enrichers.Species.Providers.MyCustomProvider do
  @behaviour HookedApi.Enrichers.Species.SpeciesProvider

  alias HookedApi.Enrichers.Species.SpeciesResult

  @impl true
  def validate_configuration do
    # Check API keys, configuration, etc.
    case Application.get_env(:hooked_api, :my_custom_api_key) do
      nil -> {:error, :no_api_key}
      key when is_binary(key) -> :ok
      _ -> {:error, :invalid_api_key}
    end
  end

  @impl true
  def identify_species(image_data, filename) do
    # Your API call logic here
    case make_api_call(image_data, filename) do
      {:ok, response} ->
        # Map response to SpeciesResult
        species_result = SpeciesResult.new(%{
          common_name: extract_common_name(response),
          scientific_name: extract_scientific_name(response),
          confidence: extract_confidence(response),
          provider: "my_custom_provider",
          provider_id: extract_id(response),
          taxonomy: extract_taxonomy(response),
          raw_response: response
        })
        
        {:ok, species_result}
        
      {:error, reason} ->
        {:error, reason}
    end
  end
  
  # Your helper functions...
end
```

### 2. Update the Enricher

```elixir
# In species_enricher.ex, change:
@species_provider MyApp.Enrichers.Species.Providers.MyCustomProvider
```

### 3. Configure Environment

```elixir
# In config/config.exs or environment variables
config :hooked_api, :my_custom_api_key, System.get_env("MY_CUSTOM_API_KEY")
```

## Provider Interface

### Required Functions

```elixir
@callback validate_configuration() :: :ok | {:error, term()}
@callback identify_species(image_data :: binary(), filename :: String.t()) :: 
  {:ok, SpeciesResult.t()} | {:error, term()}
```

### Error Handling

Providers should return standardized error types:

```elixir
# Configuration errors
{:error, :no_api_key}
{:error, :invalid_api_key}

# API errors  
{:error, {:api_error, status_code, response_body}}
{:error, {:http_error, status_code, response_body}}
{:error, {:network_error, reason}}

# Response parsing errors
{:error, {:invalid_response, message, raw_response}}

# System errors
{:error, {:crash, exception}}
```

## Example Provider Implementations

### iNaturalist Provider Features
- OAuth Bearer token authentication
- Multipart form file upload
- Taxonomic hierarchy extraction
- Confidence scoring
- Comprehensive error handling

### Google Vision Provider Features  
- Base64 image encoding
- JSON request/response
- Label filtering for animal/fish detection
- Simple confidence mapping

## Testing Your Provider

```elixir
# Test configuration validation
assert :ok = MyCustomProvider.validate_configuration()

# Test species identification
{:ok, image_data} = File.read("test_fish.jpg")
{:ok, result} = MyCustomProvider.identify_species(image_data, "test_fish.jpg")

assert result.species_name != nil
assert result.provider == "my_custom_provider"
assert is_float(result.confidence)
```

## Benefits of This Architecture

1. **Easy Provider Swapping**: Change one line to switch providers
2. **Consistent Interface**: All providers return standardized SpeciesResult
3. **Graceful Degradation**: Configuration validation prevents runtime errors
4. **Rich Data**: Standardized format captures confidence, taxonomy, raw data
5. **Testability**: Each provider can be tested independently
6. **Extensibility**: Add new providers without changing existing code

## Configuration Examples

```bash
# iNaturalist (default)
export INATURALIST_ACCESS_TOKEN="your_inaturalist_token"

# Google Vision
export GOOGLE_VISION_ACCESS_TOKEN="your_google_token"

# Multiple providers (for fallback logic)
export PRIMARY_SPECIES_PROVIDER="inaturalist"  
export FALLBACK_SPECIES_PROVIDER="google_vision"
```

This abstraction layer makes it trivial to experiment with different species identification services and choose the best one for your specific use case!