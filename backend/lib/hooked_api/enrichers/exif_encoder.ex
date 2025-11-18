defmodule HookedApi.Enrichers.ExifEncoder do
  @moduledoc """
  Implementation of Jason.Encoder protocol for the Exexif.Data.Gps struct.
  This allows the GPS data to be properly serialized to JSON.
  """

  require Protocol

  # Implement Jason.Encoder for Exexif.Data.Gps
  if Code.ensure_loaded?(Exexif.Data.Gps) && Code.ensure_loaded?(Jason.Encoder) do
    Protocol.derive(Jason.Encoder, Exexif.Data.Gps,
      only: [
        :gps_version_id,
        :gps_latitude_ref,
        :gps_latitude,
        :gps_longitude_ref,
        :gps_longitude,
        :gps_altitude_ref,
        :gps_altitude,
        :gps_time_stamp,
        :gps_satellites,
        :gps_status,
        :gps_measure_mode,
        :gps_dop,
        :gps_speed_ref,
        :gps_speed,
        :gps_track_ref,
        :gps_track,
        :gps_img_direction_ref,
        :gps_img_direction,
        :gps_map_datum,
        :gps_dest_latitude_ref,
        :gps_dest_latitude,
        :gps_dest_longitude_ref,
        :gps_dest_longitude,
        :gps_dest_bearing_ref,
        :gps_dest_bearing,
        :gps_dest_distance_ref,
        :gps_dest_distance,
        :gps_processing_method,
        :gps_area_information,
        :gps_date_stamp,
        :gps_differential,
        :gps_h_positioning_errorl
      ]
    )
  end
end
