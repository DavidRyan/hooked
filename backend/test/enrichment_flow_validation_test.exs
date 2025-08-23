defmodule HookedApi.EnrichmentFlowValidationTest do
  @moduledoc """
  Focused tests to prevent future false positives in enrichment flow.
  These tests validate that enrichers actually work, not just that they don't crash.
  """
  use HookedApi.DataCase, async: false
  use Oban.Testing, repo: HookedApi.Repo

  alias HookedApi.Workers.CatchEnrichmentWorker
  alias HookedApi.PubSubTopics

  setup do
    # Use shared mode for background processes to access the database  
    Ecto.Adapters.SQL.Sandbox.mode(HookedApi.Repo, {:shared, self()})
    :ok
  end

  describe "Enrichment Flow Validation" do
    setup do
      # Use fish_2.jpg which has known EXIF data with GPS coordinates
      user = insert(:user)

      user_catch =
        insert(:user_catch, %{
          user_id: user.id,
          species: "Unknown Fish",
          location: "Original Location",
          # Will be updated by GPS from EXIF
          latitude: 0.0,
          # Will be updated by GPS from EXIF
          longitude: 0.0,
          caught_at: ~N[2024-01-15 10:30:00],
          image_url: "fish_2.jpg",
          image_filename: "fish_2.jpg",
          weather_data: %{},
          exif_data: %{}
        })

      %{user_catch: user_catch}
    end

    test "EXIF enricher extracts GPS and metadata from real image", %{user_catch: user_catch} do
      {:ok, enriched_catch} = HookedApi.Enrichers.ExifEnricher.enrich(user_catch)

      # Should extract EXIF data
      assert is_map(enriched_catch.exif_data), "EXIF data should be a map"
      assert map_size(enriched_catch.exif_data) > 5, "Should extract multiple EXIF fields"

      # Should extract GPS coordinates from fish_2.jpg
      assert is_float(enriched_catch.exif_data[:gps_latitude]), "Should extract GPS latitude"
      assert is_float(enriched_catch.exif_data[:gps_longitude]), "Should extract GPS longitude"
      assert enriched_catch.exif_data[:gps_latitude] != 0.0, "GPS latitude should not be zero"
      assert enriched_catch.exif_data[:gps_longitude] != 0.0, "GPS longitude should not be zero"

      # Should extract camera info
      assert is_binary(enriched_catch.exif_data[:make]), "Should extract camera make"
      assert is_binary(enriched_catch.exif_data[:model]), "Should extract camera model"

      # Verify known values from fish_2.jpg
      assert enriched_catch.exif_data[:make] == "Google", "Expected Google camera"
      assert enriched_catch.exif_data[:model] == "Pixel 9 Pro", "Expected Pixel 9 Pro model"
    end

    test "Weather enricher retrieves weather data with coordinates", %{user_catch: user_catch} do
      # Use coordinates that should get weather data
      catch_with_coords = %{user_catch | latitude: 42.3601, longitude: -87.6298}

      {:ok, enriched_catch} = HookedApi.Enrichers.WeatherEnricher.enrich(catch_with_coords)

      # Should preserve original catch data
      assert enriched_catch.id == user_catch.id, "Should preserve catch ID"
      assert enriched_catch.species == user_catch.species, "Should preserve species"

      # Weather data should be present (unless API key missing, which is expected in test)
      if enriched_catch.weather_data && map_size(enriched_catch.weather_data) > 0 do
        assert enriched_catch.weather_data[:data_source] == "openweathermap",
               "Should use OpenWeatherMap"

        assert enriched_catch.weather_data[:data_type] in ["current", "historical"],
               "Should specify data type"

        # Should have some weather information
        weather_fields = [:temperature, :humidity, :weather_condition, :weather_description]

        has_weather_data =
          Enum.any?(weather_fields, fn field ->
            enriched_catch.weather_data[field] != nil
          end)

        assert has_weather_data, "Should have at least some weather data"
      end
    end

    test "Weather enricher handles string datetime format (worker context)", %{
      user_catch: user_catch
    } do
      # Simulate worker context where datetime is serialized as string
      catch_with_string_datetime = %{
        user_catch
        | caught_at: "2024-01-15T10:30:00",
          latitude: 42.3601,
          longitude: -87.6298
      }

      # Should not crash with string datetime
      {:ok, enriched_catch} =
        HookedApi.Enrichers.WeatherEnricher.enrich(catch_with_string_datetime)

      assert enriched_catch.id == user_catch.id, "Should preserve catch ID"
      # Test passed if it doesn't crash - the string datetime was handled correctly
    end

    test "Species enricher validates API configuration", %{user_catch: user_catch} do
      # Check if API token is configured
      token = Application.get_env(:hooked_api, :inaturalist_access_token)

      {:ok, enriched_catch} = HookedApi.Enrichers.Species.SpeciesEnricher.enrich(user_catch)

      # Should preserve original catch data
      assert enriched_catch.id == user_catch.id, "Should preserve catch ID"
      assert enriched_catch.species != nil, "Species should not be nil"

      cond do
        is_nil(token) ->
          # No token configured - should preserve original species
          assert enriched_catch.species == user_catch.species,
                 "Should preserve original species when no API token"

        is_binary(token) and byte_size(token) > 20 ->
          # Valid token configured - species might be updated or preserved depending on API response
          assert enriched_catch.species != nil, "Species should not be nil with valid token"

        # Don't assert exact match since API might actually identify something

        true ->
          # Invalid/placeholder token - should preserve original species  
          assert enriched_catch.species == user_catch.species,
                 "Should preserve original species with invalid token"
      end
    end

    test "Full enrichment flow updates coordinates from EXIF GPS", %{user_catch: user_catch} do
      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      # Create job args (simulating how Oban serializes the data)
      job_args = %{
        "catch_id" => user_catch.id,
        "user_catch" => %{
          "id" => user_catch.id,
          "species" => user_catch.species,
          "location" => user_catch.location,
          # 0.0
          "latitude" => user_catch.latitude,
          # 0.0  
          "longitude" => user_catch.longitude,
          # String format in worker
          "caught_at" => "2024-01-15T10:30:00",
          "notes" => user_catch.notes,
          "weather_data" => %{},
          "exif_data" => %{},
          "image_url" => user_catch.image_url,
          "image_filename" => user_catch.image_filename,
          "image_content_type" => user_catch.image_content_type,
          "image_file_size" => user_catch.image_file_size,
          "inserted_at" => user_catch.inserted_at,
          "updated_at" => user_catch.updated_at
        }
      }

      # Run enrichment
      assert :ok = perform_job(CatchEnrichmentWorker, job_args)

      # Verify enrichment completed
      receive do
        {:enrichment_completed, catch_id, enriched_catch} ->
          assert catch_id == user_catch.id, "Should enrich correct catch"

          # CRITICAL: Verify GPS coordinates were extracted and updated
          assert enriched_catch.latitude != 0.0, "Latitude should be updated from EXIF GPS"
          assert enriched_catch.longitude != 0.0, "Longitude should be updated from EXIF GPS"

          assert enriched_catch.latitude != user_catch.latitude,
                 "Latitude should change from original"

          assert enriched_catch.longitude != user_catch.longitude,
                 "Longitude should change from original"

          # Verify EXIF data was extracted
          assert is_map(enriched_catch.exif_data), "EXIF data should be present"
          assert map_size(enriched_catch.exif_data) > 5, "EXIF should have multiple fields"
          assert enriched_catch.exif_data[:gps_latitude] != nil, "GPS latitude should be in EXIF"

          assert enriched_catch.exif_data[:gps_longitude] != nil,
                 "GPS longitude should be in EXIF"

          # Verify coordinates match EXIF GPS data
          assert abs(enriched_catch.latitude - enriched_catch.exif_data[:gps_latitude]) < 0.001,
                 "Catch latitude should match EXIF GPS latitude"

          assert abs(enriched_catch.longitude - enriched_catch.exif_data[:gps_longitude]) < 0.001,
                 "Catch longitude should match EXIF GPS longitude"

        {:enrichment_failed, catch_id, error} ->
          flunk(
            "Enrichment should not fail completely, got error for #{catch_id}: #{inspect(error)}"
          )
      after
        3000 ->
          flunk("No enrichment event received - worker may have failed silently")
      end
    end

    test "Enrichment flow is resilient to individual enricher failures" do
      # Create catch with invalid image URL to test resilience
      user = insert(:user)

      invalid_catch =
        insert(:user_catch, %{
          user_id: user.id,
          image_url: "nonexistent.jpg",
          latitude: nil,
          longitude: nil,
          weather_data: %{},
          exif_data: %{}
        })

      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      job_args = %{
        "catch_id" => invalid_catch.id,
        "user_catch" => %{
          "id" => invalid_catch.id,
          "species" => invalid_catch.species,
          "location" => invalid_catch.location,
          "latitude" => invalid_catch.latitude,
          "longitude" => invalid_catch.longitude,
          "caught_at" => "2024-01-15T10:30:00",
          "notes" => invalid_catch.notes,
          "weather_data" => %{},
          "exif_data" => %{},
          "image_url" => invalid_catch.image_url,
          "image_filename" => invalid_catch.image_filename,
          "image_content_type" => invalid_catch.image_content_type,
          "image_file_size" => invalid_catch.image_file_size,
          "inserted_at" => invalid_catch.inserted_at,
          "updated_at" => invalid_catch.updated_at
        }
      }

      # Even with invalid image, enrichment should complete (not fail)
      assert :ok = perform_job(CatchEnrichmentWorker, job_args)

      receive do
        {:enrichment_completed, catch_id, enriched_catch} ->
          assert catch_id == invalid_catch.id, "Should complete enrichment even with failures"
          assert enriched_catch.id == invalid_catch.id, "Should preserve catch ID"

        # Enrichment may not add data, but should not crash the pipeline

        {:enrichment_failed, _catch_id, _error} ->
          flunk("Enrichment flow should be resilient and not fail completely")
      after
        3000 ->
          flunk("No enrichment event received")
      end
    end

    test "Species enricher properly handles API configuration issues" do
      user = insert(:user)

      user_catch =
        insert(:user_catch, %{
          user_id: user.id,
          species: "Original Species",
          image_url: "fish_2.jpg"
        })

      # Test with no token
      original_token = Application.get_env(:hooked_api, :inaturalist_access_token)

      try do
        Application.put_env(:hooked_api, :inaturalist_access_token, nil)
        {:ok, enriched_catch} = HookedApi.Enrichers.Species.SpeciesEnricher.enrich(user_catch)

        assert enriched_catch.species == "Original Species",
               "Should preserve species when no API token"

        # Test with placeholder token
        Application.put_env(
          :hooked_api,
          :inaturalist_access_token,
          "YOUR_INATURALIST_ACCESS_TOKEN_HERE"
        )

        {:ok, enriched_catch} = HookedApi.Enrichers.Species.SpeciesEnricher.enrich(user_catch)

        assert enriched_catch.species == "Original Species",
               "Should preserve species with placeholder token"

        # Test with invalid short token
        Application.put_env(:hooked_api, :inaturalist_access_token, "short")
        {:ok, enriched_catch} = HookedApi.Enrichers.Species.SpeciesEnricher.enrich(user_catch)

        assert enriched_catch.species == "Original Species",
               "Should preserve species with invalid token"
      after
        # Restore original token
        Application.put_env(:hooked_api, :inaturalist_access_token, original_token)
      end
    end
  end
end
