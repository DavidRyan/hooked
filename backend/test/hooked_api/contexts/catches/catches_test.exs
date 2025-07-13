defmodule HookedApi.CatchesTest do
  use HookedApi.DataCase, async: true

  alias HookedApi.Catches
  alias HookedApi.Catches.UserCatch


  describe "create_user_catch/2" do
    setup do
      image_upload = %Plug.Upload{
        path: "/tmp/test_image.jpg",
        filename: "test_image.jpg",
        content_type: "image/jpeg"
      }

      valid_attrs = %{
        "species" => "Largemouth Bass",
        "location" => "Lake Michigan",
        "latitude" => 42.3601,
        "longitude" => -87.6298,
        "caught_at" => ~N[2024-01-15 10:30:00],
        "notes" => "Great catch!"
      }

      %{image_upload: image_upload, valid_attrs: valid_attrs}
    end

    test "creates user catch with valid data and image", %{image_upload: image_upload, valid_attrs: valid_attrs} do
      File.mkdir_p!("priv/static/uploads/catches")
      File.write!("/tmp/test_image.jpg", "fake image data")

      # Create a simple test without mocking for now
      assert {:ok, %UserCatch{} = user_catch} = Catches.create_user_catch(valid_attrs, image_upload)

      assert user_catch.species == "Largemouth Bass"
      assert user_catch.location == "Lake Michigan"
      assert user_catch.latitude == 42.3601
      assert user_catch.longitude == -87.6298
      assert user_catch.caught_at == ~N[2024-01-15 10:30:00]
      assert user_catch.notes == "Great catch!"
      assert user_catch.image_url != nil
      assert user_catch.image_filename == "test_image.jpg"
      assert user_catch.image_content_type == "image/jpeg"
      assert user_catch.image_file_size > 0
    end

    test "returns changeset error with invalid data", %{image_upload: image_upload} do
      invalid_attrs = %{
        "species" => "",
        "location" => "",
        "latitude" => 200,
        "longitude" => -200
      }

      File.mkdir_p!("priv/static/uploads/catches")
      File.write!("/tmp/test_image.jpg", "fake image data")

      assert {:error, %Ecto.Changeset{} = changeset} = Catches.create_user_catch(invalid_attrs, image_upload)

      assert %{
        species: ["can't be blank"],
        location: ["can't be blank"],
        caught_at: ["can't be blank"],
        latitude: ["must be less than or equal to 90"],
        longitude: ["must be greater than or equal to -180"]
      } = errors_on(changeset)
    end
  end

  describe "get_user_catch/1" do
    test "returns user catch when it exists" do
      user_catch = insert(:user_catch)
      assert Catches.get_user_catch(user_catch.id) == user_catch
    end

    test "returns nil when user catch doesn't exist" do
      assert Catches.get_user_catch(Ecto.UUID.generate()) == nil
    end
  end

  describe "update_user_catch/2" do
    test "updates user catch with valid data" do
      user_catch = insert(:user_catch)
      update_attrs = %{"species" => "Smallmouth Bass", "notes" => "Updated notes"}

      assert {:ok, %UserCatch{} = updated_catch} = Catches.update_user_catch(user_catch, update_attrs)
      assert updated_catch.species == "Smallmouth Bass"
      assert updated_catch.notes == "Updated notes"
    end

    test "returns changeset error with invalid data" do
      user_catch = insert(:user_catch)
      invalid_attrs = %{"species" => "", "latitude" => 200}

      assert {:error, %Ecto.Changeset{} = changeset} = Catches.update_user_catch(user_catch, invalid_attrs)

      assert %{
        species: ["can't be blank"],
        latitude: ["must be less than or equal to 90"]
      } = errors_on(changeset)
    end
  end
end