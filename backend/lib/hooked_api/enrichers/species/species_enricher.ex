defmodule HookedApi.Enrichers.Species.SpeciesEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  use Tesla
  require Logger

  plug(Tesla.Middleware.JSON)
  plug(Tesla.Middleware.Logger)
  plug(Tesla.Middleware.Multipart)
  plug(Tesla.Middleware.BaseUrl, "https://api.inaturalist.org/v1")

  plug(Tesla.Middleware.Headers, [
    {"Authorization", "Bearer #{Application.get_env(:hooked_api, :inaturalist_access_token)}"}
  ])

  adapter(Tesla.Adapter.Hackney)

  def enrich(user_catch) do
    Logger.info("SpeciesEnricher: Starting species identification for catch #{user_catch.id}")
    Logger.debug("SpeciesEnricher: Image URL: #{inspect(user_catch.image_url)}")
    Logger.debug("SpeciesEnricher: Current species: #{inspect(user_catch.species)}")

    try do
      case identify_species(user_catch.image_url) do
        {:ok, species} ->
          Logger.info(
            "SpeciesEnricher: Successfully identified species '#{species}' for catch #{user_catch.id}"
          )

          Logger.debug("SpeciesEnricher: Updating catch with identified species")

          enriched_catch = %{user_catch | species: species}

          Logger.info(
            "SpeciesEnricher: Species enrichment completed successfully for catch #{user_catch.id}"
          )

          {:ok, enriched_catch}

        {:error, :no_species_identified} ->
          Logger.info(
            "SpeciesEnricher: No species could be identified for catch #{user_catch.id}"
          )

          Logger.debug("SpeciesEnricher: Returning catch unchanged - no species identification")
          {:ok, user_catch}

        {:error, {:api_error, status}} ->
          Logger.error(
            "SpeciesEnricher: iNaturalist API error #{status} for catch #{user_catch.id}"
          )

          Logger.debug("SpeciesEnricher: Returning catch unchanged due to API error")
          {:ok, user_catch}

        {:error, reason} ->
          Logger.error(
            "SpeciesEnricher: Species identification failed for catch #{user_catch.id}: #{inspect(reason)}"
          )

          Logger.debug("SpeciesEnricher: Returning catch unchanged due to identification failure")
          {:ok, user_catch}
      end
    rescue
      error ->
        Logger.error(
          "SpeciesEnricher: CRASH during species identification for catch #{user_catch.id}: #{inspect(error)}"
        )

        Logger.error(
          "SpeciesEnricher: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        Logger.error("SpeciesEnricher: Returning catch unchanged due to crash")
        {:ok, user_catch}
    end
  end

  @spec identify_species(String.t()) :: {:ok, String.t()} | {:error, term()}
  defp identify_species(image_url) when is_binary(image_url) do
    Logger.info("SpeciesEnricher: Starting species identification process")
    Logger.debug("SpeciesEnricher: Downloading image from #{image_url}")

    try do
      with {:ok, %Tesla.Env{status: 200, body: image_data}} <- Tesla.get(image_url),
           _ <-
             Logger.debug(
               "SpeciesEnricher: Successfully downloaded image (#{byte_size(image_data)} bytes)"
             ),
           multipart <-
             Tesla.Multipart.new()
             |> Tesla.Multipart.add_file_content(image_data, "image.jpg", name: "image"),
           _ <-
             Logger.info("SpeciesEnricher: Sending image to iNaturalist API for identification"),
           {:ok, %Tesla.Env{status: 200, body: %{"results" => results}}} <-
             post("/computervision/score_image", multipart) do
        Logger.info("SpeciesEnricher: Received identification results from iNaturalist API")
        Logger.debug("SpeciesEnricher: Number of results: #{length(results)}")

        case results do
          [best_result | _] ->
            Logger.debug("SpeciesEnricher: Processing best identification result")
            extract_species_name(best_result)

          [] ->
            Logger.info(
              "SpeciesEnricher: iNaturalist API returned no species identification results"
            )

            {:error, :no_species_identified}
        end
      else
        {:ok, %Tesla.Env{status: status, body: body}} when status != 200 ->
          Logger.error("SpeciesEnricher: iNaturalist API returned error status: #{status}")
          Logger.error("SpeciesEnricher: Error response body: #{inspect(body)}")
          {:error, {:api_error, status}}

        {:error, %Tesla.Env{status: status, body: body}} ->
          Logger.error("SpeciesEnricher: Tesla HTTP error - status: #{status}")
          Logger.error("SpeciesEnricher: Error response body: #{inspect(body)}")
          {:error, {:http_error, status}}

        {:error, reason} ->
          Logger.error(
            "SpeciesEnricher: Failed to communicate with iNaturalist API: #{inspect(reason)}"
          )

          {:error, reason}
      end
    rescue
      error ->
        Logger.error("SpeciesEnricher: CRASH during species identification: #{inspect(error)}")

        Logger.error(
          "SpeciesEnricher: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:crash, error}}
    end
  end

  defp identify_species(image_url) do
    Logger.error("SpeciesEnricher: Invalid image URL provided: #{inspect(image_url)}")
    {:error, :invalid_image_url}
  end

  @spec extract_species_name(map()) :: {:ok, String.t()} | {:error, :invalid_response}
  defp extract_species_name(%{"taxon" => taxon} = result) do
    Logger.debug("SpeciesEnricher: Extracting species name from taxon data")
    Logger.debug("SpeciesEnricher: Taxon keys: #{inspect(Map.keys(taxon))}")

    species_name =
      case taxon do
        %{"preferred_common_name" => name} when is_binary(name) and name != "" ->
          Logger.debug("SpeciesEnricher: Found preferred common name: #{name}")
          name

        %{"name" => name} when is_binary(name) and name != "" ->
          Logger.debug("SpeciesEnricher: Found scientific name: #{name}")
          name

        _ ->
          Logger.debug("SpeciesEnricher: No valid name found in taxon data")
          nil
      end

    case species_name do
      nil ->
        Logger.warning("SpeciesEnricher: Could not extract species name from result")
        Logger.debug("SpeciesEnricher: Full result: #{inspect(result)}")
        {:error, :invalid_response}

      name ->
        Logger.info("SpeciesEnricher: Successfully extracted species name: #{name}")
        {:ok, name}
    end
  end

  defp extract_species_name(result) do
    Logger.error("SpeciesEnricher: Invalid result format - missing taxon data")
    Logger.debug("SpeciesEnricher: Result: #{inspect(result)}")
    {:error, :invalid_response}
  end
end
