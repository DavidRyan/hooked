defmodule HookedApi.Enrichers.ExifEnricher do
  @behaviour HookedApi.Enrichers.Enricher
  require Logger

  alias HookedApi.Services.ImageStorage
  alias HookedApi.Utils.ExifExtractor

  def enrich(user_catch) do
    Logger.info("ExifEnricher: Starting EXIF enrichment for catch #{user_catch.id}")

    Logger.debug(
      "ExifEnricher: Input catch has existing EXIF data: #{not is_nil(user_catch.exif_data)}"
    )

    try do
      case extract_exif_data(user_catch) do
        {:ok, exif_data} ->
          Logger.info("ExifEnricher: Successfully extracted EXIF data for catch #{user_catch.id}")

          Logger.debug("ExifEnricher: Updating catch with EXIF data")

          enriched_catch = %{user_catch | exif_data: exif_data}

          Logger.info(
            "ExifEnricher: EXIF enrichment completed successfully for catch #{user_catch.id}"
          )

          {:ok, enriched_catch}

        {:error, reason} ->
          Logger.warning(
            "ExifEnricher: EXIF extraction failed for catch #{user_catch.id}: #{inspect(reason)}"
          )

          Logger.debug("ExifEnricher: Returning catch unchanged due to EXIF extraction failure")
          {:ok, user_catch}
      end
    rescue
      error ->
        Logger.error(
          "ExifEnricher: CRASH during EXIF enrichment for catch #{user_catch.id}: #{inspect(error)}"
        )

        Logger.error("ExifEnricher: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}")
        Logger.error("ExifEnricher: Returning catch unchanged due to crash")
        {:ok, user_catch}
    end
  end

  defp extract_exif_data(user_catch) do
    Logger.debug(
      "ExifEnricher: Extracting EXIF data for catch #{user_catch.id} from image: #{user_catch.image_url}"
    )

    user_catch.image_url
    |> ImageStorage.get_image_file_path()
    |> case do
      {:ok, file_path} ->
        Logger.debug("ExifEnricher: Found image file at #{file_path}, extracting EXIF data")
        exif_data = ExifExtractor.extract_from_file(file_path)

        Logger.debug(
          "ExifEnricher: Extracted EXIF data for catch #{user_catch.id}: #{inspect(Map.keys(exif_data))}"
        )

        {:ok, exif_data}

      {:error, reason} ->
        Logger.warning(
          "ExifEnricher: Could not get image file path for catch #{user_catch.id}: #{inspect(reason)}"
        )

        {:error, reason}
    end
  end
end
