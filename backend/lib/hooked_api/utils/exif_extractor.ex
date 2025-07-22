defmodule HookedApi.Utils.ExifExtractor do
  require Logger

  @spec extract_from_file(String.t()) :: map()
  def extract_from_file(file_path) when is_binary(file_path) do
    Logger.info("Extracting EXIF data from file: #{file_path}")

    case File.exists?(file_path) do
      false ->
        Logger.error("EXIF extraction failed: File does not exist at #{file_path}")
        %{}

      true ->
        Logger.debug("File exists, attempting EXIF extraction with Exexif library")

        case Exexif.exif_from_jpeg_file(file_path) do
          {:ok, exif_data} ->
            Logger.info("Successfully extracted EXIF data from #{file_path}")
            Logger.debug("Raw EXIF data keys: #{inspect(Map.keys(exif_data))}")

            normalized = normalize_exif_data(exif_data)
            Logger.info("Normalized EXIF data: #{inspect(normalized, limit: :infinity)}")
            normalized

          {:error, reason} ->
            Logger.error("EXIF extraction failed for #{file_path}: #{inspect(reason)}")
            %{}

          other ->
            Logger.error("Unexpected EXIF extraction result for #{file_path}: #{inspect(other)}")
            %{}
        end
    end
  end

  def extract_from_file(invalid_path) do
    Logger.error("Invalid file path provided to EXIF extractor: #{inspect(invalid_path)}")
    %{}
  end

  @spec normalize_exif_data(map()) :: map()
  defp normalize_exif_data(exif_data) when is_map(exif_data) do
    Logger.debug("Normalizing EXIF data with keys: #{inspect(Map.keys(exif_data))}")

    # Extract GPS data from the GPS struct if present
    gps_data = extract_gps_data(Map.get(exif_data, :gps))

    normalized = %{
      # GPS coordinates
      gps_latitude: gps_data.latitude,
      gps_longitude: gps_data.longitude,
      gps_latitude_ref: gps_data.latitude_ref,
      gps_longitude_ref: gps_data.longitude_ref,
      gps_altitude: gps_data.altitude,
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

    # Log specific GPS data if present
    if normalized.gps_latitude && normalized.gps_longitude do
      Logger.info(
        "GPS coordinates found - Lat: #{normalized.gps_latitude} (#{normalized.gps_latitude_ref}), Lng: #{normalized.gps_longitude} (#{normalized.gps_longitude_ref})"
      )
    else
      Logger.info("No GPS coordinates found in EXIF data")
    end

    # Log camera info if present
    if normalized.make || normalized.model do
      Logger.info("Camera info - Make: #{normalized.make}, Model: #{normalized.model}")
    end

    normalized
  end

  defp normalize_exif_data(invalid_data) do
    Logger.warning("Invalid EXIF data provided for normalization: #{inspect(invalid_data)}")
    %{}
  end

  # Extract GPS coordinates from Exexif.Data.Gps struct
  defp extract_gps_data(%Exexif.Data.Gps{} = gps) do
    latitude = convert_gps_coordinate(gps.gps_latitude)
    longitude = convert_gps_coordinate(gps.gps_longitude)

    %{
      latitude: latitude,
      longitude: longitude,
      latitude_ref: gps.gps_latitude_ref,
      longitude_ref: gps.gps_longitude_ref,
      altitude: gps.gps_altitude
    }
  end

  defp extract_gps_data(_),
    do: %{latitude: nil, longitude: nil, latitude_ref: nil, longitude_ref: nil, altitude: nil}

  # Convert GPS coordinate from [degrees, minutes, seconds] to decimal degrees
  defp convert_gps_coordinate([degrees, minutes, seconds])
       when is_number(degrees) and is_number(minutes) and is_number(seconds) do
    degrees + minutes / 60 + seconds / 3600
  end

  defp convert_gps_coordinate(_), do: nil
end
