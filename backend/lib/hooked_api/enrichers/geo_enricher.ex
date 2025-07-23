defmodule HookedApi.Enrichers.GeoEnricher do
  @behaviour HookedApi.Enrichers.Enricher
  require Logger

  def enrich(user_catch) do
    Logger.info("GeoEnricher: Starting GPS enrichment for catch #{user_catch.id}")

    Logger.debug(
      "GeoEnricher: Input catch has existing coordinates - lat: #{inspect(user_catch.latitude)}, lng: #{inspect(user_catch.longitude)}"
    )

    Logger.debug("GeoEnricher: EXIF data present: #{not is_nil(user_catch.exif_data)}")

    try do
      case get_gps_from_exif(user_catch.exif_data) do
        {:ok, lat, lng} ->
          Logger.info(
            "GeoEnricher: Successfully extracted GPS coordinates for catch #{user_catch.id}: #{lat}, #{lng}"
          )

          Logger.debug("GeoEnricher: Updating catch with GPS coordinates from EXIF")

          enriched_catch = %{user_catch | latitude: lat, longitude: lng}

          Logger.info(
            "GeoEnricher: GPS enrichment completed successfully for catch #{user_catch.id}"
          )

          {:ok, enriched_catch}

        {:error, :no_gps} ->
          Logger.info(
            "GeoEnricher: No GPS coordinates found in EXIF data for catch #{user_catch.id}"
          )

          Logger.debug("GeoEnricher: Returning catch unchanged - no GPS data available")
          {:ok, user_catch}

        {:error, reason} ->
          Logger.warning(
            "GeoEnricher: GPS extraction failed for catch #{user_catch.id}: #{inspect(reason)}"
          )

          Logger.debug("GeoEnricher: Returning catch unchanged due to GPS extraction failure")
          {:ok, user_catch}
      end
    rescue
      error ->
        Logger.error(
          "GeoEnricher: CRASH during GPS enrichment for catch #{user_catch.id}: #{inspect(error)}"
        )

        Logger.error("GeoEnricher: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}")
        Logger.error("GeoEnricher: Returning catch unchanged due to crash")
        {:ok, user_catch}
    end
  end

  defp get_gps_from_exif(exif_data) when is_map(exif_data) do
    Logger.debug(
      "GeoEnricher: Extracting GPS from EXIF data with keys: #{inspect(Map.keys(exif_data))}"
    )

    Logger.info(
      "GeoEnricher: FULL EXIF DATA: #{inspect(exif_data, pretty: true, limit: :infinity)}"
    )

    lat = Map.get(exif_data, :gps_latitude)
    lng = Map.get(exif_data, :gps_longitude)

    Logger.debug("GeoEnricher: Raw GPS values - lat: #{inspect(lat)}, lng: #{inspect(lng)}")

    with lat when not is_nil(lat) <- lat,
         lng when not is_nil(lng) <- lng do
      Logger.debug("GeoEnricher: Valid GPS coordinates found - lat: #{lat}, lng: #{lng}")
      {:ok, lat, lng}
    else
      _ ->
        Logger.debug("GeoEnricher: No valid GPS coordinates in EXIF data")
        {:error, :no_gps}
    end
  end

  defp get_gps_from_exif(exif_data) do
    Logger.debug("GeoEnricher: Invalid EXIF data format: #{inspect(exif_data)}")
    {:error, :no_gps}
  end
end
