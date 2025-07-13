defmodule HookedApi.Utils.ExifExtractor do
  @spec extract_from_file(String.t()) :: map()
  def extract_from_file(file_path) when is_binary(file_path) do
    case Exexif.exif_from_jpeg_file(file_path) do
      {:ok, exif_data} -> normalize_exif_data(exif_data)
      _ -> %{}
    end
  end
  def extract_from_file(_), do: %{}

  @spec normalize_exif_data(map()) :: map()
  defp normalize_exif_data(exif_data) when is_map(exif_data) do
    %{
      # GPS coordinates
      gps_latitude: Map.get(exif_data, :gps_latitude),
      gps_longitude: Map.get(exif_data, :gps_longitude),
      gps_latitude_ref: Map.get(exif_data, :gps_latitude_ref),
      gps_longitude_ref: Map.get(exif_data, :gps_longitude_ref),
      
      # Camera info
      make: Map.get(exif_data, :make),
      model: Map.get(exif_data, :model),
      
      # Image settings
      datetime: Map.get(exif_data, :datetime),
      orientation: Map.get(exif_data, :orientation),
      
      # Technical details
      iso_speed: Map.get(exif_data, :iso_speed_ratings),
      focal_length: Map.get(exif_data, :focal_length),
      aperture: Map.get(exif_data, :f_number),
      exposure_time: Map.get(exif_data, :exposure_time),
      
      # Keep raw data for future use
      _raw: exif_data
    }
  end
  defp normalize_exif_data(_), do: %{}
end