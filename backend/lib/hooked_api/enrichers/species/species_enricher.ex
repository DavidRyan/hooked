defmodule HookedApi.Enrichers.Species.SpeciesEnricher do
  @behaviour HookedApi.Enrichers.Enricher
  use Tesla
  plug Tesla.Middleware.JSON
  plug Tesla.Middleware.Logger
  plug Tesla.Middleware.Multipart
  plug Tesla.Middleware.BaseUrl, "https://api.inaturalist.org/v1"
  plug Tesla.Middleware.Headers, [
    {"Authorization", "Bearer #{Application.get_env(:hooked_api, :inaturalist_access_token)}"}
  ]

  adapter Tesla.Adapter.Hackney
  @tempfile "tmp/file.jpg"

  def enrich(user_catch, _exif_data) do

    with {:ok, _} <- download_image(user_catch.image_url),
      multipart <- Tesla.Multipart.new()  |> Tesla.Multipart.add_file(@tempfile, name: "image"),
      {:ok, %Tesla.Env{status: 200, body: body}} <- post("/computervision/score_image", multipart) do
        [best | _] = body["results"]
        # Use preferred common name if available, fallback to scientific name
        species = best["taxon"]["preferred_common_name"] || best["taxon"]["name"]
        cleanup(@tempfile)
        Map.put(user_catch, :species, species)
        else 
          {:error, _error} ->
          cleanup(@tempfile)
            user_catch
      end
  end

def download_image(image_url) do
  case Tesla.get(image_url) do
    {:ok, %Tesla.Env{status: 200, body: body}} ->
      File.mkdir_p!("tmp")
      case File.write(@tempfile, body) do
        :ok -> {:ok, @tempfile}
        error -> {:error, error}
      end

    {:ok, %Tesla.Env{status: status}} ->
      {:error, "Failed to download image: status #{status}"}

    {:error, error} ->
      {:error, error}
  end
end

def cleanup(tempfile) do
  if File.exists?(tempfile) do
    File.rm(tempfile)
  end
end
end
