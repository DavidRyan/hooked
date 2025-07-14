defmodule HookedApi.Enrichers.GeoEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  def enrich(user_catch) do
    case get_gps_from_exif(user_catch.exif_data) do
      {:ok, lat, lng} ->
        {:ok, %{user_catch | latitude: lat, longitude: lng}}
      _ ->
        {:ok, user_catch}
    end
  end

  defp get_gps_from_exif(exif_data) when is_map(exif_data) do
    with lat when not is_nil(lat) <- Map.get(exif_data, :gps_latitude),
         lng when not is_nil(lng) <- Map.get(exif_data, :gps_longitude) do
      {:ok, lat, lng}
    else
      _ -> {:error, :no_gps}
    end
  end
  defp get_gps_from_exif(_), do: {:error, :no_gps}
end