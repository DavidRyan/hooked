defmodule HookedApi.Enrichers.SpeciesEnricherTest do
  use HookedApi.DataCase, async: false

  alias HookedApi.Enrichers.Species.SpeciesEnricher
  alias HookedApi.Catches.UserCatch

  describe "SpeciesEnricher" do
    test "validates API configuration properly" do
      user_catch = %UserCatch{
        id: Ecto.UUID.generate(),
        species: "Unknown Fish",
        image_url: "fish_2.jpg"
      }

      # Test with no API token (nil)
      original_token = Application.get_env(:hooked_api, :inaturalist_access_token)

      try do
        # Test case 1: No token configured
        Application.put_env(:hooked_api, :inaturalist_access_token, nil)
        {:ok, result} = SpeciesEnricher.enrich(user_catch)
        assert result.species == user_catch.species, "Should preserve species when no token"

        # Test case 2: Placeholder token
        Application.put_env(
          :hooked_api,
          :inaturalist_access_token,
          "YOUR_INATURALIST_ACCESS_TOKEN_HERE"
        )

        {:ok, result} = SpeciesEnricher.enrich(user_catch)

        assert result.species == user_catch.species,
               "Should preserve species with placeholder token"

        # Test case 3: Invalid short token  
        Application.put_env(:hooked_api, :inaturalist_access_token, "short")
        {:ok, result} = SpeciesEnricher.enrich(user_catch)
        assert result.species == user_catch.species, "Should preserve species with invalid token"

        # Test case 4: Valid-looking token (would make real API call)
        Application.put_env(
          :hooked_api,
          :inaturalist_access_token,
          "valid_looking_token_that_is_long_enough_12345"
        )

        {:ok, result} = SpeciesEnricher.enrich(user_catch)
        # With a fake token, it would attempt API call and fail gracefully
        assert result.species == user_catch.species, "Should preserve species when API call fails"
      after
        # Restore original configuration
        Application.put_env(:hooked_api, :inaturalist_access_token, original_token)
      end
    end

    test "fails appropriately when API configuration is wrong" do
      user_catch = %UserCatch{
        id: Ecto.UUID.generate(),
        species: "Test Fish",
        image_url: "fish_2.jpg"
      }

      original_token = Application.get_env(:hooked_api, :inaturalist_access_token)

      try do
        # This should NOT cause a 401 error now - it should be handled gracefully
        Application.put_env(
          :hooked_api,
          :inaturalist_access_token,
          "YOUR_INATURALIST_ACCESS_TOKEN_HERE"
        )

        {:ok, result} = SpeciesEnricher.enrich(user_catch)

        # The key assertion: should not get 401 error, should handle gracefully
        assert result.species == "Test Fish",
               "Should preserve original species with invalid token"

        # The enricher should detect the placeholder and not make an API call
        # This prevents the 401 Unauthorized error
      after
        Application.put_env(:hooked_api, :inaturalist_access_token, original_token)
      end
    end

    test "handles missing image file gracefully" do
      user_catch = %UserCatch{
        id: Ecto.UUID.generate(),
        species: "Test Fish",
        image_url: "nonexistent.jpg"
      }

      original_token = Application.get_env(:hooked_api, :inaturalist_access_token)

      try do
        Application.put_env(
          :hooked_api,
          :inaturalist_access_token,
          "valid_token_for_testing_123456789"
        )

        {:ok, result} = SpeciesEnricher.enrich(user_catch)

        # Should preserve original species when image file not found
        assert result.species == "Test Fish", "Should preserve species when image not found"
      after
        Application.put_env(:hooked_api, :inaturalist_access_token, original_token)
      end
    end
  end
end
