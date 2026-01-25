defmodule HookedApi.Enrichers.Species.Providers.InaturalistProvider do
  @moduledoc """
  iNaturalist Computer Vision API provider for species identification.

  Uses iNaturalist's machine learning API to identify species from images.
  API Documentation: https://www.inaturalist.org/pages/api+reference
  """

  @behaviour HookedApi.Enrichers.Species.SpeciesProvider

  use Tesla
  require Logger

  alias HookedApi.Enrichers.Species.SpeciesResult

  plug(Tesla.Middleware.JSON)
  plug(Tesla.Middleware.Logger)
  plug(Tesla.Middleware.BaseUrl, "https://api.inaturalist.org/v1")

  adapter(Tesla.Adapter.Hackney,
    timeout: 60_000,
    recv_timeout: 120_000,
    ssl_options: [verify: :verify_peer, cacerts: :certifi.cacerts()]
  )

  @impl true
  def validate_configuration do
    case System.get_env("INATURALIST_ACCESS_TOKEN") do
      token when is_binary(token) and byte_size(token) > 10 ->
        :ok

      _ ->
        Logger.error("InaturalistProvider: Missing INATURALIST_ACCESS_TOKEN")
        {:error, :missing_api_token}
    end
  end

  @impl true
  def identify_species(image_data, filename) do
    Logger.info("InaturalistProvider: Starting species identification")
    Logger.debug("InaturalistProvider: Preparing image payload")

    try do
      token = System.get_env("INATURALIST_ACCESS_TOKEN")
      headers = build_headers(token)

      with multipart <- build_multipart_request(image_data, filename),
           _ <- Logger.info("InaturalistProvider: Sending image to iNaturalist API"),
           {:ok, %Tesla.Env{status: 200, body: body} = response} <-
             post("/computervision/score_image", multipart, headers: headers) do
        Logger.info("InaturalistProvider: Received identification results from iNaturalist API")

        parse_response(body, response)
      else
        {:ok, %Tesla.Env{status: status, body: body}} when status != 200 ->
          Logger.error("InaturalistProvider: iNaturalist API returned error status: #{status}")
          Logger.error("InaturalistProvider: Error response body: #{inspect(body)}")
          {:error, {:api_error, status, body}}

        {:error, %Tesla.Env{status: status, body: body}} ->
          Logger.error("InaturalistProvider: Tesla HTTP error - status: #{status}")
          Logger.error("InaturalistProvider: Error response body: #{inspect(body)}")
          {:error, {:http_error, status, body}}

        {:error, reason} ->
          Logger.error(
            "InaturalistProvider: Failed to communicate with iNaturalist API: #{inspect(reason)}"
          )

          {:error, {:network_error, reason}}
      end
    rescue
      error ->
        Logger.error(
          "InaturalistProvider: CRASH during species identification: #{inspect(error)}"
        )

        Logger.error(
          "InaturalistProvider: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:crash, error}}
    end
  end

  defp build_multipart_request(image_source, filename) when is_binary(image_source) do
    if File.exists?(image_source) do
      Tesla.Multipart.new()
      |> Tesla.Multipart.add_file(image_source, name: "image", filename: filename)
    else
      Tesla.Multipart.new()
      |> Tesla.Multipart.add_file_content(image_source, filename, name: "image")
    end
  end

  defp build_headers(token) when is_binary(token) do
    [{"Authorization", "Bearer #{token}"}]
  end

  defp build_headers(_token), do: []

  defp parse_response(%{"results" => results}, response) when is_list(results) do
    Logger.debug("InaturalistProvider: Number of results: #{length(results)}")

    case results do
      [best_result | _] ->
        Logger.debug("InaturalistProvider: Processing best identification result")
        build_species_result(best_result, response.body)

      [] ->
        Logger.info(
          "InaturalistProvider: iNaturalist API returned no species identification results"
        )

        {:ok, SpeciesResult.no_species_found("inaturalist", response.body)}
    end
  end

  defp parse_response(body, response) do
    Logger.error("InaturalistProvider: Unexpected response format - missing results array")
    Logger.debug("InaturalistProvider: Response body: #{inspect(body)}")
    {:error, {:invalid_response, "Missing results array", response.body}}
  end

  defp build_species_result(%{"taxon" => taxon} = result, raw_response) do
    Logger.debug("InaturalistProvider: Extracting species data from taxon")
    Logger.debug("InaturalistProvider: Taxon keys: #{inspect(Map.keys(taxon))}")

    common_name = get_string_value(taxon, "preferred_common_name")
    scientific_name = get_string_value(taxon, "name")
    confidence = get_confidence(result)
    provider_id = get_string_value(taxon, "id")
    taxonomy = extract_taxonomy(taxon)

    if common_name || scientific_name do
      species_result =
        SpeciesResult.new(%{
          common_name: common_name,
          scientific_name: scientific_name,
          confidence: confidence,
          provider: "inaturalist",
          provider_id: provider_id,
          taxonomy: taxonomy,
          raw_response: raw_response
        })

      Logger.info(
        "InaturalistProvider: Successfully extracted species: #{species_result.species_name}"
      )

      {:ok, species_result}
    else
      Logger.warning("InaturalistProvider: Could not extract valid species name from result")
      Logger.debug("InaturalistProvider: Full result: #{inspect(result)}")
      {:error, {:invalid_response, "No valid species name found", raw_response}}
    end
  end

  defp build_species_result(result, raw_response) do
    Logger.error("InaturalistProvider: Invalid result format - missing taxon data")
    Logger.debug("InaturalistProvider: Result: #{inspect(result)}")
    {:error, {:invalid_response, "Missing taxon data", raw_response}}
  end

  defp get_string_value(map, key) when is_map(map) do
    case Map.get(map, key) do
      value when is_binary(value) and value != "" -> value
      _ -> nil
    end
  end

  defp get_confidence(%{"score" => score}) when is_number(score), do: score
  defp get_confidence(_), do: nil

  defp extract_taxonomy(%{"rank" => rank, "ancestry" => ancestry} = taxon)
       when is_binary(ancestry) do
    %{
      rank: rank,
      ancestry: ancestry,
      kingdom: get_string_value(taxon, "kingdom"),
      phylum: get_string_value(taxon, "phylum"),
      class: get_string_value(taxon, "class"),
      order: get_string_value(taxon, "order"),
      family: get_string_value(taxon, "family"),
      genus: get_string_value(taxon, "genus")
    }
  end

  defp extract_taxonomy(taxon) do
    %{
      rank: get_string_value(taxon, "rank"),
      kingdom: get_string_value(taxon, "kingdom"),
      phylum: get_string_value(taxon, "phylum"),
      class: get_string_value(taxon, "class"),
      order: get_string_value(taxon, "order"),
      family: get_string_value(taxon, "family"),
      genus: get_string_value(taxon, "genus")
    }
  end
end
