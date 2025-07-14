defmodule HookedApi.Enrichers.Species.SpeciesEnricher do
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

  def enrich(user_catch) do
    case identify_species(user_catch.image_url) do
      {:ok, species} -> 
        {:ok, %{user_catch | species: species}}
      {:error, reason} ->
        Logger.warning("Species identification failed: #{inspect(reason)}")
        {:ok, user_catch}
    end
  end

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
