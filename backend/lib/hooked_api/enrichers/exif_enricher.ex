defmodule HookedApi.Enrichers.ExifEnricher do
  @behaviour HookedApi.Enrichers.Enricher
  require Logger

  alias HookedApi.Services.ImageStorage
  alias HookedApi.Utils.ExifExtractor

  def enrich(user_catch, context \\ %{}) do
    Logger.info("ExifEnricher: Starting EXIF enrichment for catch #{user_catch.id}")

    Logger.debug(
      "ExifEnricher: Input catch has existing EXIF data: #{not is_nil(user_catch.exif_data)}"
    )

    Logger.debug("ExifEnricher: Context local_image_path: #{inspect(context[:local_image_path])}")

    try do
      case extract_exif_data(user_catch, context) do
        {:ok, exif_data} ->
          Logger.info("ExifEnricher: Successfully extracted EXIF data for catch #{user_catch.id}")

          Logger.debug("ExifEnricher: Updating catch with EXIF data")

          enriched_catch =
            user_catch
            |> maybe_apply_exif_datetime(exif_data)
            |> Map.put(:exif_data, exif_data)

          Logger.info(
            "ExifEnricher: EXIF enrichment completed successfully for catch #{user_catch.id}"
          )

          {:ok, enriched_catch}

        {:error, reason} ->
          Logger.warning(
            "ExifEnricher: EXIF extraction failed for catch #{user_catch.id}: #{inspect(reason)}"
          )

          Logger.debug("ExifEnricher: Returning catch unchanged due to EXIF extraction failure")
          {:ok, %{user_catch | enrichment_status: false}}
      end
    rescue
      error ->
        Logger.error(
          "ExifEnricher: CRASH during EXIF enrichment for catch #{user_catch.id}: #{inspect(error)}"
        )

        Logger.error("ExifEnricher: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}")
        Logger.error("ExifEnricher: Returning catch unchanged due to crash")
        {:ok, %{user_catch | enrichment_status: false}}
    end
  end

  defp extract_exif_data(user_catch, context) do
    Logger.debug(
      "ExifEnricher: Extracting EXIF data for catch #{user_catch.id} from image: #{user_catch.image_url}"
    )

    # Try local file first, fall back to S3 download
    case get_image_path(user_catch.image_url, context) do
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

  defp get_image_path(image_url, %{local_image_path: path}) when is_binary(path) do
    if File.exists?(path) do
      Logger.debug("ExifEnricher: Using local image file: #{path}")
      {:ok, path}
    else
      Logger.debug("ExifEnricher: Local file not found, falling back to S3 download")
      ImageStorage.get_image_file_path(image_url)
    end
  end

  defp get_image_path(image_url, _context) do
    Logger.debug("ExifEnricher: No local path provided, downloading from S3")
    ImageStorage.get_image_file_path(image_url)
  end

  defp maybe_apply_exif_datetime(user_catch, exif_data) do
    case Map.get(exif_data, :datetime) do
      %NaiveDateTime{} = datetime ->
        Logger.info("ExifEnricher: Setting caught_at from EXIF datetime: #{datetime}")
        %{user_catch | caught_at: datetime}

      _ ->
        user_catch
    end
  end
end
