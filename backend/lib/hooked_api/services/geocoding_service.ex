defmodule HookedApi.Services.GeocodingService do
  use Tesla
  require Logger

  plug Tesla.Middleware.BaseUrl, "https://api.mapbox.com/geocoding/v5/mapbox.places"

  plug Tesla.Middleware.JSON,
    decode_content_types: ["application/json", "application/vnd.geo+json"]

  adapter Tesla.Adapter.Hackney

  def get_location(lat, long) do
    types = "place,locality,neighborhood"
    access_token = Application.get_env(:hooked_api, :mapbox_api_key)
    # Mapbox expects longitude,latitude order (not lat,long)
    path = "/#{long},#{lat}.json"

    case get(path, query: [types: types, access_token: access_token]) do
      {:ok, %Tesla.Env{status: 200, body: %{"features" => [first | _]}}} ->
        {:ok, first["place_name"]}

      {:ok, %Tesla.Env{status: 200, body: _}} ->
        {:error, :no_results}

      {:ok, %Tesla.Env{status: status, body: body}} ->
        {:error, {:http_error, status, body}}

      {:error, reason} ->
        {:error, reason}
    end
  end
end
