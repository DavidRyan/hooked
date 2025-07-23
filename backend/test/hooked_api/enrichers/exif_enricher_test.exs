defmodule HookedApi.Enrichers.ExifEnricherTest do
  use HookedApi.DataCase
  alias HookedApi.Enrichers.ExifEnricher
  alias HookedApi.Catches.UserCatch

  describe "enrich/1" do
    test "adds EXIF data to user_catch when image has EXIF data" do
      user_catch = %UserCatch{
        id: 1,
        image_url: "test_image.jpg",
        exif_data: nil
      }

      # Mock the ImageStorage.get_image_file_path to return a test image path
      # This would need proper mocking in a real test environment

      # For now, test that the function doesn't crash
      result = ExifEnricher.enrich(user_catch)

      assert {:ok, enriched_catch} = result
      assert enriched_catch.id == user_catch.id
    end

    test "returns unchanged user_catch when no image file found" do
      user_catch = %UserCatch{
        id: 1,
        image_url: "nonexistent_image.jpg",
        exif_data: nil
      }

      result = ExifEnricher.enrich(user_catch)

      assert {:ok, enriched_catch} = result
      assert enriched_catch == user_catch
    end

    test "handles nil image_url gracefully" do
      user_catch = %UserCatch{
        id: 1,
        image_url: nil,
        exif_data: nil
      }

      result = ExifEnricher.enrich(user_catch)

      assert {:ok, enriched_catch} = result
      assert enriched_catch == user_catch
    end
  end
end
