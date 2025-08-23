defmodule HookedApi.EnrichmentFlowIntegrationTest do
  use HookedApi.DataCase, async: false
  use Oban.Testing, repo: HookedApi.Repo

  setup do
    # Use shared mode for background processes to access the database
    Ecto.Adapters.SQL.Sandbox.mode(HookedApi.Repo, {:shared, self()})
    :ok
  end

  alias HookedApi.Services.EnrichmentService
  alias HookedApi.Workers.CatchEnrichmentWorker
  alias HookedApi.PubSubTopics

  describe "Full Enrichment Flow Integration" do
    setup do
      # Create a user catch with real image data
      user = insert(:user)

      user_catch =
        insert(:user_catch, %{
          user_id: user.id,
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
        })

      %{user_catch: user_catch}
    end

    test "enriches a catch through the complete flow", %{user_catch: user_catch} do
      # Subscribe to enrichment events
      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      # Start the enrichment process
      assert {:ok, job} = EnrichmentService.enqueue_enrichment(user_catch)
      assert job.args[:catch_id] == user_catch.id

      # Perform the job
      assert :ok = perform_job(CatchEnrichmentWorker, job.args)

      # Verify we received the enrichment completion event
      assert_received {:enrichment_completed, catch_id, enriched_catch}
      assert catch_id == user_catch.id
      assert enriched_catch.id == user_catch.id

      # Verify enrichment results
      # EXIF data should be present if the image file exists
      IO.puts("Original catch data:")
      IO.inspect(user_catch, pretty: true)

      IO.puts("\nEnriched catch data:")
      IO.inspect(enriched_catch, pretty: true)

      # Check what got enriched
      assert enriched_catch.species != nil
      assert enriched_catch.location != nil

      # Log the enrichment results
      IO.puts("\n=== ENRICHMENT RESULTS ===")
      IO.puts("Species: #{enriched_catch.species}")
      IO.puts("Location: #{enriched_catch.location}")
      IO.puts("Latitude: #{enriched_catch.latitude}")
      IO.puts("Longitude: #{enriched_catch.longitude}")

      IO.puts(
        "Weather data present: #{not is_nil(enriched_catch.weather_data) and map_size(enriched_catch.weather_data) > 0}"
      )

      IO.puts(
        "EXIF data present: #{not is_nil(enriched_catch.exif_data) and map_size(enriched_catch.exif_data) > 0}"
      )

      if enriched_catch.exif_data && map_size(enriched_catch.exif_data) > 0 do
        IO.puts("EXIF keys: #{inspect(Map.keys(enriched_catch.exif_data))}")
      end

      if enriched_catch.weather_data && map_size(enriched_catch.weather_data) > 0 do
        IO.puts("Weather keys: #{inspect(Map.keys(enriched_catch.weather_data))}")
      end
    end

    test "handles enrichment with test image file", %{user_catch: user_catch} do
      # Update the catch to use test_image.jpg which exists in the project root
      updated_catch =
        Map.merge(user_catch, %{
          image_url: "test_image.jpg",
          image_filename: "test_image.jpg"
        })

      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      # Create job arguments manually to avoid struct serialization issues
      job_args = %{
        "catch_id" => user_catch.id,
        "user_catch" => Map.from_struct(updated_catch) |> Map.drop([:__meta__])
      }

      assert :ok = perform_job(CatchEnrichmentWorker, job_args)

      assert_received {:enrichment_completed, catch_id, enriched_catch}
      assert catch_id == user_catch.id

      IO.puts("\n=== TEST IMAGE ENRICHMENT RESULTS ===")
      IO.puts("Species: #{enriched_catch.species}")
      IO.puts("Location: #{enriched_catch.location}")

      IO.puts(
        "Weather data present: #{not is_nil(enriched_catch.weather_data) and map_size(enriched_catch.weather_data) > 0}"
      )

      IO.puts(
        "EXIF data present: #{not is_nil(enriched_catch.exif_data) and map_size(enriched_catch.exif_data) > 0}"
      )
    end

    test "verifies individual enrichers work", %{user_catch: user_catch} do
      IO.puts("\n=== TESTING INDIVIDUAL ENRICHERS ===")

      # Test EXIF Enricher
      exif_result = HookedApi.Enrichers.ExifEnricher.enrich(user_catch)
      IO.puts("EXIF Enricher result: #{inspect(elem(exif_result, 0))}")

      # Test Geo Enricher
      geo_result = HookedApi.Enrichers.GeoEnricher.enrich(user_catch)
      IO.puts("Geo Enricher result: #{inspect(elem(geo_result, 0))}")

      # Test Weather Enricher
      weather_result = HookedApi.Enrichers.WeatherEnricher.enrich(user_catch)
      IO.puts("Weather Enricher result: #{inspect(elem(weather_result, 0))}")

      # Test Species Enricher
      species_result = HookedApi.Enrichers.Species.SpeciesEnricher.enrich(user_catch)
      IO.puts("Species Enricher result: #{inspect(elem(species_result, 0))}")

      # All enrichers should return either :ok or :error tuples
      assert match?({:ok, _}, exif_result) or match?({:error, _}, exif_result)
      assert match?({:ok, _}, geo_result) or match?({:error, _}, geo_result)
      assert match?({:ok, _}, weather_result) or match?({:error, _}, weather_result)
      assert match?({:ok, _}, species_result) or match?({:error, _}, species_result)
    end

    test "tests enrichment service configuration" do
      enrichers = EnrichmentService.get_configured_enrichers()

      IO.puts("\n=== CONFIGURED ENRICHERS ===")
      IO.puts("Number of enrichers: #{length(enrichers)}")

      Enum.each(enrichers, fn enricher ->
        IO.puts("- #{inspect(enricher)}")

        # Verify each enricher module exists and has the enrich/1 function
        assert Code.ensure_loaded?(enricher)
        assert function_exported?(enricher, :enrich, 1)
      end)

      # Should have the default enrichers
      assert HookedApi.Enrichers.ExifEnricher in enrichers
      assert HookedApi.Enrichers.GeoEnricher in enrichers
      assert HookedApi.Enrichers.WeatherEnricher in enrichers
      assert HookedApi.Enrichers.Species.SpeciesEnricher in enrichers
    end
  end
end
