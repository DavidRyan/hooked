defmodule HookedApi.Enrichers.SkunkGeoEnricher do
  @moduledoc """
  Enriches a UserSkunk with a reverse-geocoded location name from its GPS coordinates.
  Unlike the GeoEnricher for catches, this does not extract coordinates from EXIF data —
  coordinates are provided directly from the device GPS.
  """
  @behaviour HookedApi.Enrichers.Enricher
  require Logger

  def enrich(user_skunk, _context \\ %{}) do
    Logger.info("SkunkGeoEnricher: Starting geo enrichment for skunk #{user_skunk.id}")

    try do
      case {user_skunk.latitude, user_skunk.longitude} do
        {lat, lng} when is_float(lat) and is_float(lng) ->
          Logger.info(
            "SkunkGeoEnricher: Reverse geocoding coordinates for skunk #{user_skunk.id}: #{lat}, #{lng}"
          )

          case HookedApi.Services.GeocodingService.get_location(lat, lng) do
            {:ok, location_name} ->
              Logger.info(
                "SkunkGeoEnricher: Successfully geocoded skunk #{user_skunk.id}: #{location_name}"
              )

              {:ok, %{user_skunk | location: location_name}}

            {:error, reason} ->
              Logger.warning(
                "SkunkGeoEnricher: Reverse geocoding failed for skunk #{user_skunk.id}: #{inspect(reason)}"
              )

              {:ok, user_skunk}
          end

        _ ->
          Logger.info("SkunkGeoEnricher: No coordinates available for skunk #{user_skunk.id}")
          {:ok, user_skunk}
      end
    rescue
      error ->
        Logger.error(
          "SkunkGeoEnricher: CRASH during geo enrichment for skunk #{user_skunk.id}: #{inspect(error)}"
        )

        {:ok, %{user_skunk | enrichment_status: false}}
    end
  end
end
