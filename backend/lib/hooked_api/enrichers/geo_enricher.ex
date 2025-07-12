defmodule HookedApi.Enrichers.GeoEnricher do
  @behaviour HookedApi.Enrichers.Enricher

  def enrich(user_catch, exif_data) do
    case get_coordinates(user_catch, exif_data) do
      {lat, lng} when is_number(lat) and is_number(lng) ->
        enrich_with_location_data(lat, lng)
      
      _ ->
        %{}
    end
  end

  defp get_coordinates(user_catch, exif_data) do
    case get_exif_coordinates(exif_data) do
      {lat, lng} when is_number(lat) and is_number(lng) ->
        {lat, lng}
      
      _ ->
        case Map.get(user_catch, :location) do
          location when is_map(location) ->
            {Map.get(location, :latitude), Map.get(location, :longitude)}
          
          _ ->
            case Map.get(user_catch, :latitude) do
              lat when is_number(lat) ->
                {lat, Map.get(user_catch, :longitude)}
              
              _ ->
                {nil, nil}
            end
        end
    end
  end

  defp get_exif_coordinates(exif_data) do
    case {Map.get(exif_data, "GPS_Latitude"), Map.get(exif_data, "GPS_Longitude")} do
      {lat, lng} when is_number(lat) and is_number(lng) ->
        {lat, lng}
      
      _ ->
        {nil, nil}
    end
  end

  defp enrich_with_location_data(_lat, _lng) do
    %{}
  end
end