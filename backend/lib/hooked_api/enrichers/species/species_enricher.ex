defmodule HookedApi.Enrichers.Species.SpeciesEnricher do
  @moduledoc """
  Enriches user catches with species identification using iNaturalist's Computer Vision API.
  
  This enricher downloads the catch image, sends it to iNaturalist for species identification,
  and updates the catch with the identified species name (preferring common names over scientific names).
  """
  
  @behaviour HookedApi.Enrichers.Enricher
  
  use Tesla
  require Logger
  
  plug Tesla.Middleware.JSON
  plug Tesla.Middleware.Logger
  plug Tesla.Middleware.Multipart
  plug Tesla.Middleware.BaseUrl, "https://api.inaturalist.org/v1"
  plug Tesla.Middleware.Headers, [
    {"Authorization", "Bearer #{Application.get_env(:hooked_api, :inaturalist_access_token)}"}
  ]

  adapter Tesla.Adapter.Hackney

  @doc """
  Enriches a user catch with species identification.
  
  ## Parameters
  - user_catch: The user catch struct to enrich
  - _exif_data: EXIF data (unused in this enricher)
  
  ## Returns
  - Updated user catch with species field set, or original catch if identification fails
  """
  @spec enrich(map(), map()) :: map()
  def enrich(user_catch, _exif_data) do
    case identify_species(user_catch.image_url) do
      {:ok, species} -> 
        %{user_catch | species: species}
      {:error, reason} ->
        Logger.warning("Species identification failed: #{inspect(reason)}")
        user_catch
    end
  end

  @doc """
  Identifies species directly from image URL using stream processing.
  """
  @spec identify_species(String.t()) :: {:ok, String.t()} | {:error, term()}
  defp identify_species(image_url) do
    with {:ok, %Tesla.Env{status: 200, body: image_data}} <- Tesla.get(image_url),
         multipart <- Tesla.Multipart.new() |> Tesla.Multipart.add_file_content(image_data, "image.jpg", name: "image"),
         {:ok, %Tesla.Env{status: 200, body: %{"results" => [best_result | _]}}} <- post("/computervision/score_image", multipart) do
      extract_species_name(best_result)
    else
      {:ok, %Tesla.Env{status: 200, body: %{"results" => []}}} ->
        {:error, :no_species_identified}
      {:ok, %Tesla.Env{status: status}} ->
        {:error, {:api_error, status}}
      {:error, reason} ->
        {:error, reason}
    end
  end

  @doc """
  Extracts the preferred species name from iNaturalist API response.
  """
  @spec extract_species_name(map()) :: {:ok, String.t()} | {:error, :invalid_response}
  defp extract_species_name(%{"taxon" => taxon}) do
    species_name = 
      case taxon do
        %{"preferred_common_name" => name} when is_binary(name) and name != "" -> name
        %{"name" => name} when is_binary(name) and name != "" -> name
        _ -> nil
      end
    
    case species_name do
      nil -> {:error, :invalid_response}
      name -> {:ok, name}
    end
  end
  defp extract_species_name(_), do: {:error, :invalid_response}


end
