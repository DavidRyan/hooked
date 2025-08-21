defmodule HookedApi.EnrichmentManualTest do
  use HookedApi.DataCase, async: false
  use Oban.Testing, repo: HookedApi.Repo

  alias HookedApi.Workers.CatchEnrichmentWorker
  alias HookedApi.PubSubTopics
  alias HookedApi.Catches.UserCatch

  describe "Manual Enrichment Testing" do
    test "test individual enrichers with real data and validate results" do
      # Create a realistic user catch
      user_catch = %UserCatch{
        id: Ecto.UUID.generate(),
        species: "Unknown Fish",
        location: "Test Lake",
        latitude: 42.3601,
        longitude: -87.6298,
        caught_at: ~N[2024-01-15 10:30:00],
        image_url: "fish_2.jpg",
        image_filename: "fish_2.jpg",
        image_content_type: "image/jpeg",
        weather_data: nil,
        exif_data: nil
      }

      IO.puts("\n=== TESTING INDIVIDUAL ENRICHERS WITH VALIDATION ===")
      IO.puts("Testing with catch ID: #{user_catch.id}")
      IO.puts("Image file: #{user_catch.image_url}")

      # Test EXIF Enricher
      IO.puts("\n--- EXIF Enricher ---")

      case HookedApi.Enrichers.ExifEnricher.enrich(user_catch) do
        {:ok, enriched_catch} ->
          if enriched_catch.exif_data && map_size(enriched_catch.exif_data) > 0 do
            IO.puts(
              "✓ EXIF enrichment successful - extracted #{map_size(enriched_catch.exif_data)} fields"
            )

            IO.puts("EXIF data keys: #{inspect(Map.keys(enriched_catch.exif_data))}")

            # Validate that GPS data was extracted from fish_2.jpg
            assert enriched_catch.exif_data[:gps_latitude] != nil,
                   "Expected GPS latitude in EXIF data"

            assert enriched_catch.exif_data[:gps_longitude] != nil,
                   "Expected GPS longitude in EXIF data"

            assert enriched_catch.exif_data[:make] != nil, "Expected camera make in EXIF data"
          else
            IO.puts("⚠ EXIF enrichment returned success but no data extracted")

            flunk(
              "EXIF enricher should extract data from fish_2.jpg but got: #{inspect(enriched_catch.exif_data)}"
            )
          end

        {:error, reason} ->
          IO.puts("✗ EXIF enrichment failed: #{inspect(reason)}")
          flunk("EXIF enricher should work with fish_2.jpg but failed: #{inspect(reason)}")
      end

      # Test Geo Enricher
      IO.puts("\n--- Geo Enricher ---")

      case HookedApi.Enrichers.GeoEnricher.enrich(user_catch) do
        {:ok, enriched_catch} ->
          IO.puts("✓ Geo enrichment successful")
          IO.puts("Original location: #{user_catch.location}")
          IO.puts("Enriched location: #{enriched_catch.location}")

          # Geo enricher should preserve the original location if no GPS data available
          assert enriched_catch.location != nil, "Location should not be nil after geo enrichment"

        {:error, reason} ->
          IO.puts("✗ Geo enrichment failed: #{inspect(reason)}")
          flunk("Geo enricher failed: #{inspect(reason)}")
      end

      # Test Weather Enricher - with proper datetime handling validation
      IO.puts("\n--- Weather Enricher ---")

      case HookedApi.Enrichers.WeatherEnricher.enrich(user_catch) do
        {:ok, enriched_catch} ->
          IO.puts("✓ Weather enrichment successful")

          if enriched_catch.weather_data && map_size(enriched_catch.weather_data) > 0 do
            IO.puts("Weather data keys: #{inspect(Map.keys(enriched_catch.weather_data))}")

            # Validate essential weather fields are present
            assert enriched_catch.weather_data[:data_source] != nil,
                   "Expected data_source in weather data"

            assert enriched_catch.weather_data[:data_type] != nil,
                   "Expected data_type in weather data"

            # Should have at least temperature or some weather info
            has_weather_info =
              enriched_catch.weather_data[:temperature] != nil ||
                enriched_catch.weather_data[:weather_condition] != nil ||
                enriched_catch.weather_data[:humidity] != nil

            assert has_weather_info, "Expected some weather information to be present"
          else
            IO.puts("⚠ Weather enrichment returned success but no data - likely API key missing")
          end

        {:error, reason} ->
          IO.puts("✗ Weather enrichment failed: #{inspect(reason)}")
          flunk("Weather enricher failed: #{inspect(reason)}")
      end

      # Test Species Enricher - validate API configuration and file reading
      IO.puts("\n--- Species Enricher ---")
      token = Application.get_env(:hooked_api, :inaturalist_access_token)

      IO.puts(
        "API token configured: #{if token && byte_size(token) > 20, do: "Yes (#{byte_size(token)} chars)", else: "No or invalid"}"
      )

      case HookedApi.Enrichers.Species.SpeciesEnricher.enrich(user_catch) do
        {:ok, enriched_catch} ->
          IO.puts("✓ Species enrichment completed")
          IO.puts("Original species: #{user_catch.species}")
          IO.puts("Enriched species: #{enriched_catch.species}")

          # Species enricher should at least preserve original species
          assert enriched_catch.species != nil, "Species should not be nil after enrichment"

          # Validate behavior based on API configuration
          if is_nil(token) or token == "YOUR_INATURALIST_ACCESS_TOKEN_HERE" or
               byte_size(token || "") <= 10 do
            assert enriched_catch.species == user_catch.species,
                   "Should preserve original species when API not configured"

            IO.puts("✓ Correctly preserved species due to missing/invalid API configuration")
          else
            # With valid API token, species might be updated or preserved
            assert enriched_catch.species != nil, "Species should not be nil with valid API token"
            IO.puts("✓ API token is configured - species may be updated by API")
          end

        {:error, reason} ->
          IO.puts("✗ Species enrichment failed: #{inspect(reason)}")
          flunk("Species enricher failed: #{inspect(reason)}")
      end

      assert true
    end

    test "test full enrichment worker flow with validation" do
      # Create a user catch in the database
      user_catch =
        insert(:user_catch, %{
          species: "Unknown Fish",
          location: "Test Lake",
          latitude: 42.3601,
          longitude: -87.6298,
          caught_at: ~N[2024-01-15 10:30:00],
          image_url: "fish_2.jpg",
          image_filename: "fish_2.jpg",
          image_content_type: "image/jpeg",
          weather_data: %{},
          exif_data: %{}
        })

      IO.puts("\n=== TESTING FULL WORKER FLOW WITH VALIDATION ===")
      IO.puts("Database catch ID: #{user_catch.id}")

      # Subscribe to enrichment events
      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      # Create job args manually  
      job_args = %{
        "catch_id" => user_catch.id,
        "user_catch" => %{
          "id" => user_catch.id,
          "species" => user_catch.species,
          "location" => user_catch.location,
          "latitude" => user_catch.latitude,
          "longitude" => user_catch.longitude,
          "caught_at" => user_catch.caught_at,
          "notes" => user_catch.notes,
          "weather_data" => user_catch.weather_data,
          "exif_data" => user_catch.exif_data,
          "image_url" => user_catch.image_url,
          "image_filename" => user_catch.image_filename,
          "image_content_type" => user_catch.image_content_type,
          "image_file_size" => user_catch.image_file_size,
          "inserted_at" => user_catch.inserted_at,
          "updated_at" => user_catch.updated_at
        }
      }

      # Perform the job
      IO.puts("Running enrichment job...")
      result = perform_job(CatchEnrichmentWorker, job_args)
      IO.puts("Job result: #{inspect(result)}")

      # Validate job completed successfully
      assert result == :ok, "Enrichment job should complete successfully"

      # Check for completion event
      receive do
        {:enrichment_completed, catch_id, enriched_catch} ->
          IO.puts("\n✓ Enrichment completed for catch #{catch_id}")

          # Validate that the enrichment actually worked
          assert catch_id == user_catch.id, "Catch ID should match"
          assert enriched_catch.id == user_catch.id, "Enriched catch ID should match original"

          # Validate EXIF enrichment worked
          assert enriched_catch.exif_data != nil, "EXIF data should be present"
          assert is_map(enriched_catch.exif_data), "EXIF data should be a map"
          assert map_size(enriched_catch.exif_data) > 0, "EXIF data should not be empty"

          # Validate GPS coordinates were extracted and updated
          assert enriched_catch.latitude != user_catch.latitude,
                 "Latitude should be updated from EXIF GPS data"

          assert enriched_catch.longitude != user_catch.longitude,
                 "Longitude should be updated from EXIF GPS data"

          IO.puts("Final enriched data validation:")
          IO.puts("- Species: #{enriched_catch.species}")
          IO.puts("- Location: #{enriched_catch.location}")
          IO.puts("- Coordinates: #{enriched_catch.latitude}, #{enriched_catch.longitude}")

          IO.puts(
            "- Weather data present: #{not is_nil(enriched_catch.weather_data) and map_size(enriched_catch.weather_data) > 0}"
          )

          IO.puts(
            "- EXIF data present: #{not is_nil(enriched_catch.exif_data) and map_size(enriched_catch.exif_data) > 0}"
          )

          if enriched_catch.exif_data && map_size(enriched_catch.exif_data) > 0 do
            IO.puts("- EXIF keys: #{inspect(Map.keys(enriched_catch.exif_data))}")

            assert :gps_latitude in Map.keys(enriched_catch.exif_data),
                   "GPS latitude should be in EXIF data"

            assert :gps_longitude in Map.keys(enriched_catch.exif_data),
                   "GPS longitude should be in EXIF data"
          end

          if enriched_catch.weather_data && map_size(enriched_catch.weather_data) > 0 do
            IO.puts("- Weather keys: #{inspect(Map.keys(enriched_catch.weather_data))}")

            assert :data_source in Map.keys(enriched_catch.weather_data),
                   "Weather data should have data_source"
          end

        {:enrichment_failed, catch_id, error} ->
          IO.puts("\n✗ Enrichment failed for catch #{catch_id}: #{inspect(error)}")
          flunk("Enrichment should not fail completely, got: #{inspect(error)}")
      after
        2000 ->
          flunk(
            "No enrichment event received within timeout - enrichment may have failed silently"
          )
      end

      assert true
    end

    test "test datetime handling in weather enricher specifically" do
      # Test that weather enricher handles string datetime correctly
      user_catch_with_string_datetime = %UserCatch{
        id: Ecto.UUID.generate(),
        species: "Test Fish",
        location: "Test Location",
        latitude: 42.3601,
        longitude: -87.6298,
        # String format like in worker context
        caught_at: "2024-01-15T10:30:00",
        image_url: "fish_2.jpg",
        weather_data: nil,
        exif_data: nil
      }

      IO.puts("\n=== TESTING WEATHER ENRICHER WITH STRING DATETIME ===")
      IO.puts("Testing datetime: #{user_catch_with_string_datetime.caught_at} (string)")

      case HookedApi.Enrichers.WeatherEnricher.enrich(user_catch_with_string_datetime) do
        {:ok, enriched_catch} ->
          IO.puts("✓ Weather enricher handled string datetime successfully")
          # Should not crash and should return a result
          assert enriched_catch.id == user_catch_with_string_datetime.id,
                 "Catch ID should be preserved"

        {:error, reason} ->
          IO.puts("✗ Weather enricher failed with string datetime: #{inspect(reason)}")
          flunk("Weather enricher should handle string datetime format, got: #{inspect(reason)}")
      end

      assert true
    end
  end
end
