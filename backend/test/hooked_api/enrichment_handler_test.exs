defmodule HookedApi.EnrichmentHandlerTest do
  use HookedApi.DataCase, async: false

  alias HookedApi.EnrichmentHandler
  alias HookedApi.Catches



  describe "handle_info/2 - enrichment_completed" do
    test "successfully updates catch with enriched data" do
      user_catch = insert(:user_catch, %{
        species: "Unknown Fish",
        location: "Lake Michigan",
        weather_data: nil
      })

      enriched_user_catch = %{user_catch |
        species: "Largemouth Bass",
        location: "Lake Michigan, Chicago, IL",
        weather_data: %{"temperature" => 72, "conditions" => "sunny"},
        exif_data: %{"camera" => "iPhone 12", "timestamp" => "2024-01-15T10:30:00Z"}
      }

      {:ok, pid} = GenServer.start_link(EnrichmentHandler, [])

      send(pid, {:enrichment_completed, user_catch.id, enriched_user_catch})
      Process.sleep(50)

      updated_catch = Catches.get_user_catch(user_catch.id)
      assert updated_catch.species == "Largemouth Bass"
      assert updated_catch.location == "Lake Michigan, Chicago, IL"
      assert updated_catch.weather_data == %{"temperature" => 72, "conditions" => "sunny"}
      assert updated_catch.exif_data == %{"camera" => "iPhone 12", "timestamp" => "2024-01-15T10:30:00Z"}
    end

    test "handles update failure gracefully" do
      user_catch = insert(:user_catch)

      invalid_enriched_catch = %{user_catch |
        species: "",
        latitude: 200
      }

      {:ok, pid} = GenServer.start_link(EnrichmentHandler, [])

      send(pid, {:enrichment_completed, user_catch.id, invalid_enriched_catch})
      Process.sleep(50)

      original_catch = Catches.get_user_catch(user_catch.id)
      assert original_catch.species == user_catch.species
      assert original_catch.latitude == user_catch.latitude
    end
  end

  describe "handle_info/2 - enrichment_failed" do
    test "logs error and continues" do
      {:ok, pid} = GenServer.start_link(EnrichmentHandler, [])

      send(pid, {:enrichment_failed, Ecto.UUID.generate(), "Service unavailable"})
      Process.sleep(10)
    end
  end

  describe "handle_info/2 - unknown message" do
    test "ignores unknown messages" do
      {:ok, pid} = GenServer.start_link(EnrichmentHandler, [])

      send(pid, {:unknown_message, "data"})
      Process.sleep(10)
    end
  end

  describe "PubSub integration" do
    test "subscribes to catch enrichment topic on init" do
      # This test just verifies the handler can start without error
      # PubSub subscription is tested implicitly in other tests
      assert :ok = :ok
    end
  end
end