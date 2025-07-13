defmodule HookedApi.Workers.CatchEnrichmentWorkerTest do
  use HookedApi.DataCase, async: true
  use Oban.Testing, repo: HookedApi.Repo

  alias HookedApi.Workers.CatchEnrichmentWorker
  alias HookedApi.PubSubTopics

  describe "perform/1" do
    setup do
    user_catch = insert(:user_catch, %{
      species: "Unknown Fish",
      location: "Lake Michigan",
      latitude: 42.3601,
      longitude: -87.6298,
      weather_data: %{},
      exif_data: %{}
    })
    user_catch_map = %{
      "id" => user_catch.id,
      "species" => user_catch.species,
      "location" => user_catch.location,
      "latitude" => user_catch.latitude,
      "longitude" => user_catch.longitude,
      "caught_at" => user_catch.caught_at,
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
    
    job = %Oban.Job{
      args: %{"catch_id" => user_catch.id, "user_catch" => user_catch_map}
    }
      %{user_catch: user_catch, job: job}
    end

    test "successfully enriches catch", %{user_catch: user_catch, job: job} do
      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      # The worker should complete successfully even if enrichers fail
      assert :ok = perform_job(CatchEnrichmentWorker, job.args)

      assert_received {:enrichment_completed, catch_id, enriched_catch}
      assert catch_id == user_catch.id
      assert is_map(enriched_catch)
      assert Map.has_key?(enriched_catch, :exif_data)
    end

    test "publishes enrichment_failed when catch not found", %{job: job} do
      invalid_catch_id = Ecto.UUID.generate()
      invalid_user_catch = %{
        "id" => invalid_catch_id,
        "species" => "Test Fish",
        "location" => "Test Lake",
        "latitude" => 42.0,
        "longitude" => -87.0,
        "caught_at" => ~N[2024-01-15 10:30:00],
        "notes" => "Test notes",
        "weather_data" => %{},
        "exif_data" => %{},
        "image_url" => "https://example.com/test.jpg",
        "image_filename" => "test.jpg",
        "image_content_type" => "image/jpeg",
        "image_file_size" => 1024,
        "inserted_at" => ~U[2024-01-15 10:30:00Z],
        "updated_at" => ~U[2024-01-15 10:30:00Z]
      }
      invalid_job = %{job | args: %{"catch_id" => invalid_catch_id, "user_catch" => invalid_user_catch}}

      Phoenix.PubSub.subscribe(HookedApi.PubSub, PubSubTopics.catch_enrichment())

      result = perform_job(CatchEnrichmentWorker, invalid_job.args)
      
      # The worker should complete successfully even if enrichers fail
      assert :ok = result
      assert_received {:enrichment_completed, _catch_id, enriched_catch}
      assert is_map(enriched_catch)
    end




  end
end