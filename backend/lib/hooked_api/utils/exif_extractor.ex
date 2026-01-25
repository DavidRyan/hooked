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

            # Sanitize raw EXIF data to handle problematic values
            sanitized_exif = sanitize_raw_exif(exif_data)

            normalized = normalize_exif_data(sanitized_exif)
            Logger.info("Normalized EXIF data: #{inspect(normalized, limit: :infinity)}")
            normalized

          {:error, reason} ->
            Logger.error("EXIF extraction failed for #{file_path}: #{inspect(reason)}")
            %{}
        end
    end
  end

  def extract_from_file(invalid_path) do
    Logger.error("Invalid file path provided to EXIF extractor: #{inspect(invalid_path)}")
    %{}
  end

  @doc """
  Extract EXIF data directly from binary image data.
  This allows processing images in memory without saving to disk.
  """
  @spec extract_from_binary(binary()) :: map()
  def extract_from_binary(image_data) when is_binary(image_data) do
    Logger.info("Extracting EXIF data from binary image data (#{byte_size(image_data)} bytes)")

    # Write binary data to a temporary file
    tmp_dir = System.tmp_dir!()
    tmp_filename = "exif_#{:erlang.system_time(:millisecond)}_#{:rand.uniform(1_000_000)}.jpg"
    tmp_path = Path.join(tmp_dir, tmp_filename)

    try do
      case File.write(tmp_path, image_data) do
        :ok ->
          Logger.debug("Temporarily wrote #{byte_size(image_data)} bytes to #{tmp_path}")

          # Extract EXIF data from the temp file
          result = extract_from_file(tmp_path)

          result

        {:error, reason} ->
          Logger.error("Failed to write temporary file: #{inspect(reason)}")
          %{}
      end
    after
      # Always clean up the temporary file
      File.rm(tmp_path)
      Logger.debug("Cleaned up temporary file: #{tmp_path}")
    end
  end

  def extract_from_binary(invalid_data) do
    Logger.error("Invalid binary data provided to EXIF extractor")
    %{}
  end

  @doc """
  Extract EXIF data directly from binary image data.
  This allows processing images in memory without saving to disk.
  """
  @spec extract_from_binary(binary()) :: map()
  def extract_from_binary(image_data) when is_binary(image_data) do
    Logger.info("Extracting EXIF data from binary image data (#{byte_size(image_data)} bytes)")

    # Write binary data to a temporary file that will be automatically deleted
    with {:ok, tmp_path} <- Briefly.create(),
         :ok <- File.write(tmp_path, image_data) do
      Logger.debug("Temporarily wrote #{byte_size(image_data)} bytes to #{tmp_path}")

      # Extract EXIF data from the temp file
      result = extract_from_file(tmp_path)

      # Delete the temporary file
      File.rm(tmp_path)

      result
    else
      {:error, reason} ->
        Logger.error("Failed to process binary image data: #{inspect(reason)}")
        %{}
    end
  end

  def extract_from_binary(invalid_data) do
    Logger.error(
      "Invalid binary data provided to EXIF extractor: #{inspect(invalid_data, limit: 50)}"
    )

    %{}
  end

  @spec normalize_exif_data(map()) :: map()
  defp normalize_exif_data(exif_data) when is_map(exif_data) do
    Logger.debug("Normalizing EXIF data with keys: #{inspect(Map.keys(exif_data))}")

    # Extract GPS data from the GPS struct if present
    gps_data = extract_gps_data(Map.get(exif_data, :gps))

    # Extract datetime from nested :exif map (where cameras typically store it)
    exif_submap = Map.get(exif_data, :exif, %{})

    datetime = Map.get(exif_submap, :datetime_digitized)

    # Parse the datetime string if present
    parsed_datetime = parse_exif_datetime(datetime)

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

      # Image settings - use parsed datetime
      datetime: parsed_datetime,
      orientation: Map.get(exif_data, :orientation),

      # Technical details - check both top level and :exif submap
      iso_speed:
        Map.get(exif_submap, :iso_speed_ratings) || Map.get(exif_data, :iso_speed_ratings),
      focal_length: Map.get(exif_submap, :focal_length) || Map.get(exif_data, :focal_length),
      aperture: Map.get(exif_submap, :f_number) || Map.get(exif_data, :f_number),
      exposure_time: Map.get(exif_submap, :exposure_time) || Map.get(exif_data, :exposure_time),

      # Keep raw data for future use but ensure GPS struct is safely converted
      _raw: sanitize_exif_data(exif_data)
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

  # Parse EXIF datetime string like "2025:06:10 19:09:58" into NaiveDateTime
  defp parse_exif_datetime(nil), do: nil

  defp parse_exif_datetime(datetime_str) when is_binary(datetime_str) do
    # Remove surrounding quotes if present
    cleaned = datetime_str |> String.trim("\"") |> String.trim()

    # EXIF format is "YYYY:MM:DD HH:MM:SS"
    case Regex.run(~r/(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})/, cleaned) do
      [_, year, month, day, hour, minute, second] ->
        case NaiveDateTime.new(
               String.to_integer(year),
               String.to_integer(month),
               String.to_integer(day),
               String.to_integer(hour),
               String.to_integer(minute),
               String.to_integer(second)
             ) do
          {:ok, datetime} ->
            Logger.info("ExifExtractor: Parsed datetime: #{datetime}")
            datetime

          {:error, reason} ->
            Logger.warning("ExifExtractor: Failed to create datetime: #{inspect(reason)}")
            nil
        end

      _ ->
        Logger.warning("ExifExtractor: Could not parse datetime string: #{inspect(datetime_str)}")
        nil
    end
  end

  defp parse_exif_datetime(_), do: nil

  # Extract GPS coordinates from Exexif.Data.Gps struct
  def extract_gps_data(%Exexif.Data.Gps{} = gps) do
    Logger.debug("ExifExtractor: Extracting GPS data from #{inspect(gps)}")

    # Check if GPS coordinates are present
    if is_nil(gps.gps_latitude) or is_nil(gps.gps_longitude) do
      Logger.debug("ExifExtractor: GPS coordinates not present in EXIF data")
      %{latitude: nil, longitude: nil, latitude_ref: nil, longitude_ref: nil, altitude: nil}
    else
      # Sanitize GPS values before conversion
      sanitized_lat = sanitize_gps_coordinate(gps.gps_latitude)
      sanitized_lng = sanitize_gps_coordinate(gps.gps_longitude)
      sanitized_lat_ref = sanitize_ref(gps.gps_latitude_ref)
      sanitized_lng_ref = sanitize_ref(gps.gps_longitude_ref)
      sanitized_altitude = sanitize_altitude(gps.gps_altitude)

      # Extract raw latitude and longitude
      latitude = convert_gps_coordinate(sanitized_lat)
      longitude = convert_gps_coordinate(sanitized_lng)

      # Apply hemisphere sign (N/S, E/W) - default to N/E if ref is invalid
      latitude = if sanitized_lat_ref == "S" and latitude, do: -latitude, else: latitude
      longitude = if sanitized_lng_ref == "W" and longitude, do: -longitude, else: longitude

      if latitude && longitude do
        Logger.info(
          "ExifExtractor: Converted GPS coordinates - Lat: #{latitude}, Lng: #{longitude}"
        )
      else
        Logger.debug("ExifExtractor: GPS coordinates could not be converted")
      end

      %{
        latitude: latitude,
        longitude: longitude,
        latitude_ref: sanitized_lat_ref,
        longitude_ref: sanitized_lng_ref,
        altitude: sanitized_altitude
      }
    end
  end

  # Sanitize GPS coordinates to handle infinity values
  defp sanitize_gps_coordinate(nil), do: nil

  defp sanitize_gps_coordinate(coords) when is_list(coords) do
    sanitized =
      Enum.map(coords, fn
        # Replace infinity with 0.0
        :infinity -> 0.0
        value when is_number(value) -> value
        # Replace any other non-numeric value with 0.0
        _ -> 0.0
      end)

    # Return nil if any value was infinity or all values are 0.0
    if Enum.any?(coords, fn x -> x == :infinity end) || Enum.all?(coords, fn x -> x == 0.0 end),
      do: nil,
      else: sanitized
  end

  defp sanitize_gps_coordinate(_), do: nil

  # Sanitize GPS reference (N/S/E/W) to avoid NULL bytes
  # Default to North for nil values
  defp sanitize_ref(nil), do: "N"
  # Default to North for NULL bytes
  defp sanitize_ref(<<0>>), do: "N"
  defp sanitize_ref("N"), do: "N"
  defp sanitize_ref("S"), do: "S"
  defp sanitize_ref("E"), do: "E"
  defp sanitize_ref("W"), do: "W"
  # Default to North for any other invalid value
  defp sanitize_ref(_), do: "N"

  # Sanitize altitude to handle infinity
  defp sanitize_altitude(:infinity), do: nil
  defp sanitize_altitude(altitude) when is_number(altitude), do: altitude
  defp sanitize_altitude(_), do: nil

  def extract_gps_data(_),
    do: %{latitude: nil, longitude: nil, latitude_ref: nil, longitude_ref: nil, altitude: nil}

  # Convert GPS coordinate from [degrees, minutes, seconds] to decimal degrees
  defp convert_gps_coordinate([degrees, minutes, seconds])
       when is_number(degrees) and is_number(minutes) and is_number(seconds) do
    # Check if all values are zero or very close to zero - indicates likely invalid GPS data
    if (degrees == 0 || degrees == 0.0) &&
         (minutes == 0 || minutes == 0.0) &&
         (seconds == 0 || seconds == 0.0 || abs(seconds) < 0.001) do
      Logger.warning("ExifExtractor: Detected all-zero GPS coordinates, treating as invalid")
      nil
    else
      decimal = degrees + minutes / 60 + seconds / 3600

      Logger.debug(
        "ExifExtractor: Converting GPS coordinate [#{degrees}, #{minutes}, #{seconds}] to decimal: #{decimal}"
      )

      decimal
    end
  end

  # Handle when GPS coordinates are infinity values (common in malformed EXIF data)
  defp convert_gps_coordinate(coords) when is_list(coords) do
    if Enum.any?(coords, fn x -> x == :infinity end) do
      Logger.warning("ExifExtractor: Found infinity in GPS coordinates: #{inspect(coords)}")
      nil
    else
      Logger.warning("ExifExtractor: Invalid GPS coordinate format: #{inspect(coords)}")
      nil
    end
  end

  defp convert_gps_coordinate(nil), do: nil

  defp convert_gps_coordinate(invalid) do
    Logger.warning("ExifExtractor: Cannot convert GPS coordinate: #{inspect(invalid)}")
    nil
  end

  # Sanitize the exif data to make it safely encodable to JSON
  defp sanitize_exif_data(exif_data) when is_map(exif_data) do
    # Convert the GPS struct to a plain map if present
    gps_data =
      case Map.get(exif_data, :gps) do
        %Exexif.Data.Gps{} = gps ->
          # Convert the struct to a map and handle infinity values
          gps
          |> Map.from_struct()
          |> Enum.map(fn {k, v} -> {k, sanitize_gps_value(v)} end)
          |> Map.new()

        other ->
          other
      end

    # Replace the GPS struct with a plain map
    Map.put(exif_data, :gps, gps_data)
  end

  defp sanitize_exif_data(other), do: other

  # Handle infinity values in GPS data
  defp sanitize_gps_value(:infinity), do: "infinity"

  defp sanitize_gps_value(list) when is_list(list) do
    Enum.map(list, &sanitize_gps_value/1)
  end

  defp sanitize_gps_value(other), do: other

  # Sanitize the raw EXIF data to handle problematic values
  defp sanitize_raw_exif(exif_data) when is_map(exif_data) do
    # Handle GPS data specially
    gps_data =
      case exif_data[:gps] do
        %Exexif.Data.Gps{} = gps ->
          # Sanitize the GPS struct fields
          sanitized_gps = %{
            gps
            | gps_latitude_ref: sanitize_string(gps.gps_latitude_ref),
              gps_longitude_ref: sanitize_string(gps.gps_longitude_ref),
              gps_date_stamp: sanitize_string(gps.gps_date_stamp),
              gps_img_direction_ref: sanitize_string(gps.gps_img_direction_ref),
              # Handle potential infinity values
              gps_latitude: sanitize_coordinate(gps.gps_latitude),
              gps_longitude: sanitize_coordinate(gps.gps_longitude),
              gps_altitude: sanitize_number(gps.gps_altitude),
              gps_img_direction: sanitize_number(gps.gps_img_direction)
          }

          sanitized_gps

        other ->
          other
      end

    # Put the sanitized GPS data back and return the updated map
    Map.put(exif_data, :gps, gps_data)
  end

  # Ensure strings don't contain NULL bytes or other problematic characters
  defp sanitize_string(nil), do: nil

  defp sanitize_string(string) when is_binary(string) do
    string
    # Remove NULL bytes
    |> String.replace(<<0>>, "")
    # Remove control chars
    |> String.replace(~r/[\x01-\x08\x0B\x0C\x0E-\x1F\x7F]/, "")
  end

  defp sanitize_string(other), do: other

  # Handle coordinates that might contain infinity values
  defp sanitize_coordinate(coords) when is_list(coords) do
    Enum.map(coords, fn
      # Replace infinity with 0.0
      :infinity -> 0.0
      value -> value
    end)
  end

  defp sanitize_coordinate(other), do: other

  # Handle numeric values that might be infinity
  defp sanitize_number(:infinity), do: 0.0
  defp sanitize_number(value), do: value
end
