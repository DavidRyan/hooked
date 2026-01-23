defmodule HookedApi.Enrichers.Species.SpeciesEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  require Logger

  alias HookedApi.Enrichers.Species.Providers.GoogleVisionProvider
  alias HookedApi.Services.ImageStorage

  # Configure which provider to use - you can swap this out easily
  @species_provider GoogleVisionProvider

  def enrich(user_catch) do
    Logger.info("SpeciesEnricher: Starting species identification for catch #{user_catch.id}")
    Logger.debug("SpeciesEnricher: Image URL: #{inspect(user_catch.image_url)}")
    Logger.debug("SpeciesEnricher: Current species: #{inspect(user_catch.species)}")

    try do
      case @species_provider.validate_configuration() do
        :ok ->
          case identify_species_from_image(user_catch.image_url) do
            {:ok, species_result} when not is_nil(species_result.species_name) ->
              Logger.info(
                "SpeciesEnricher: Successfully identified species '#{species_result.species_name}' for catch #{user_catch.id} using #{species_result.provider}"
              )

              Logger.debug("SpeciesEnricher: Updating catch with identified species")
              Logger.debug("SpeciesEnricher: Species data: #{inspect(species_result)}")

              normalized_species = best_match(species_result.species_name)
              enriched_catch = %{user_catch | species: normalized_species}

              Logger.info(
                "SpeciesEnricher: Species enrichment completed successfully for catch #{user_catch.id}"
              )

              {:ok, enriched_catch}

            {:ok, species_result} ->
              Logger.info(
                "SpeciesEnricher: No species could be identified for catch #{user_catch.id} using #{species_result.provider}"
              )

              Logger.debug(
                "SpeciesEnricher: Returning catch unchanged - no species identification"
              )

              {:ok, %{user_catch | enrichment_status: false}}

            {:error, {:api_error, status, _body}} ->
              Logger.error("SpeciesEnricher: API error #{status} for catch #{user_catch.id}")

              Logger.debug("SpeciesEnricher: Returning catch unchanged due to API error")
              {:ok, %{user_catch | enrichment_status: false}}

            {:error, reason} ->
              Logger.error(
                "SpeciesEnricher: Species identification failed for catch #{user_catch.id}: #{inspect(reason)}"
              )

              Logger.debug(
                "SpeciesEnricher: Returning catch unchanged due to identification failure"
              )

              {:ok, %{user_catch | enrichment_status: false}}
          end

        {:error, :no_api_key} ->
          Logger.warning("SpeciesEnricher: API token not configured for catch #{user_catch.id}")
          {:ok, %{user_catch | enrichment_status: false}}

        {:error, :invalid_api_key} ->
          Logger.error("SpeciesEnricher: Invalid API token configured for catch #{user_catch.id}")
          {:ok, %{user_catch | enrichment_status: false}}
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
        {:ok, %{user_catch | enrichment_status: false}}
    end
  end

  defp identify_species_from_image(image_url) when is_binary(image_url) do
    Logger.info("SpeciesEnricher: Starting species identification process")
    Logger.debug("SpeciesEnricher: Reading image from #{image_url}")

    try do
      with {:ok, file_path} <- ImageStorage.get_image_file_path(image_url),
           _ <- Logger.debug("SpeciesEnricher: Found image file at #{file_path}"),
           {:ok, image_data} <- File.read(file_path),
           _ <-
             Logger.debug(
               "SpeciesEnricher: Successfully read image file (#{byte_size(image_data)} bytes)"
             ),
           filename <- Path.basename(file_path) do
        Logger.info(
          "SpeciesEnricher: Delegating to #{@species_provider} for species identification"
        )

        @species_provider.identify_species(image_data, filename)
      else
        {:error, :file_not_found} ->
          Logger.warning("SpeciesEnricher: Image file not found: #{image_url}")
          {:error, :file_not_found}

        {:error, reason} when reason in [:enoent, :eacces, :eisdir] ->
          Logger.warning("SpeciesEnricher: File system error reading image: #{inspect(reason)}")
          {:error, :file_read_error}

        {:error, reason} ->
          Logger.error("SpeciesEnricher: Failed to read image file: #{inspect(reason)}")
          {:error, reason}
      end
    rescue
      error ->
        Logger.error("SpeciesEnricher: CRASH during image reading: #{inspect(error)}")

        Logger.error(
          "SpeciesEnricher: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:crash, error}}
    end
  end

  defp identify_species_from_image(image_url) do
    Logger.error("SpeciesEnricher: Invalid image URL provided: #{inspect(image_url)}")
    {:error, :invalid_image_url}
  end

    defp best_match(species_input) do
    # species list is a text file species.txt in the root of the project
    species_file_path = Path.join([Application.app_dir(:hooked_api), "..", "..", "species.txt"])
    
    species_list = 
      case File.read(species_file_path) do
        {:ok, content} ->
          content
          |> String.split("\n", trim: true)
          |> Enum.reject(&(&1 == ""))
        
        {:error, _reason} ->
          []
      end
    
    if Enum.empty?(species_list) do
      species_input
    else
      species_input_lower = String.downcase(species_input)
      
      species_list
      |> Enum.map(fn species ->
        species_lower = String.downcase(species)
        distance = levenshtein_distance(species_input_lower, species_lower)
        {species, distance}
      end)
      |> Enum.min_by(fn {_species, distance} -> distance end)
      |> elem(0)
    end
  end

defp read_species_list do
  species_file = Path.join([File.cwd!(), "species.txt"])
  
  case File.read(species_file) do
    {:ok, content} ->
      content
      |> String.split("\n", trim: true)
      |> Enum.map(&String.trim/1)
      |> Enum.reject(&(&1 == ""))
    
    {:error, _reason} ->
      Logger.warning("SpeciesEnricher: Could not read species.txt file")
      []
  end
end

  defp levenshtein_distance(string1, string2) do
    s1 = String.graphemes(string1)
    s2 = String.graphemes(string2)
    
    costs = 
      Enum.reduce(0..length(s1), %{}, fn i, acc ->
        Map.put(acc, {i, 0}, i)
      end)
    
    costs = 
      Enum.reduce(0..length(s2), costs, fn j, acc ->
        Map.put(acc, {0, j}, j)
      end)
    
    costs =
      Enum.reduce(1..length(s1), costs, fn i, acc_i ->
        Enum.reduce(1..length(s2), acc_i, fn j, acc_j ->
          cost = if Enum.at(s1, i - 1) == Enum.at(s2, j - 1), do: 0, else: 1
          
          distance = min(
            Map.get(acc_j, {i - 1, j}) + 1,
            min(
              Map.get(acc_j, {i, j - 1}) + 1,
              Map.get(acc_j, {i - 1, j - 1}) + cost
            )
          )
          
          Map.put(acc_j, {i, j}, distance)
        end)
      end)
    
    Map.get(costs, {length(s1), length(s2)})
  end
end
