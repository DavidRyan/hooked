defmodule HookedApi.CatchesTest do
  use HookedApi.DataCase, async: true
  import HookedApi.Factory

  alias HookedApi.Catches
  alias HookedApi.Catches.UserCatch

  describe "create_user_catch/3" do
    setup do
      user = insert(:user)

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

      %{user: user, image_upload: image_upload, valid_attrs: valid_attrs}
    end

    test "creates user catch with valid data and image", %{
      user: user,
      image_upload: image_upload,
      valid_attrs: valid_attrs
    } do
      File.mkdir_p!("priv/static/uploads/catches")
      File.write!("/tmp/test_image.jpg", "fake image data")

      assert {:ok, %UserCatch{} = user_catch} =
               Catches.create_user_catch(user.id, valid_attrs, image_upload)

      assert user_catch.user_id == user.id
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

      # Cleanup
      File.rm("/tmp/test_image.jpg")
    end

    test "returns changeset error with invalid data", %{user: user, image_upload: image_upload} do
      invalid_attrs = %{
        "species" => "",
        "location" => "",
        "latitude" => 200,
        "longitude" => -200
      }

      File.mkdir_p!("priv/static/uploads/catches")
      File.write!("/tmp/test_image.jpg", "fake image data")

      assert {:error, %Ecto.Changeset{} = changeset} =
               Catches.create_user_catch(user.id, invalid_attrs, image_upload)

      assert %{
               latitude: ["must be less than or equal to 90"],
               longitude: ["must be greater than or equal to -180"]
             } = errors_on(changeset)

      # Cleanup
      File.rm("/tmp/test_image.jpg")
    end
  end

  describe "get_user_catch/2" do
    test "returns user catch when it exists for the user" do
      user = insert(:user)
      user_catch = insert(:user_catch, user_id: user.id)

      assert found_catch = Catches.get_user_catch(user.id, user_catch.id)
      assert found_catch.id == user_catch.id
      assert found_catch.user_id == user.id
    end

    test "returns nil when user catch doesn't exist" do
      user = insert(:user)
      assert Catches.get_user_catch(user.id, Ecto.UUID.generate()) == nil
    end

    test "returns nil when catch exists but belongs to different user" do
      user1 = insert(:user)
      user2 = insert(:user)
      user_catch = insert(:user_catch, user_id: user2.id)

      assert Catches.get_user_catch(user1.id, user_catch.id) == nil
    end
  end

  describe "list_user_catches/1" do
    test "returns all catches for a specific user" do
      user1 = insert(:user)
      user2 = insert(:user)

      catch1 = insert(:user_catch, user_id: user1.id, species: "Bass")
      catch2 = insert(:user_catch, user_id: user1.id, species: "Trout")
      _catch3 = insert(:user_catch, user_id: user2.id, species: "Pike")

      catches = Catches.list_user_catches(user1.id)

      assert length(catches) == 2
      catch_ids = Enum.map(catches, & &1.id)
      assert catch1.id in catch_ids
      assert catch2.id in catch_ids
    end

    test "returns empty list when user has no catches" do
      user = insert(:user)
      assert Catches.list_user_catches(user.id) == []
    end
  end

  describe "update_user_catch/2" do
    test "updates user catch with valid data" do
      user = insert(:user)
      user_catch = insert(:user_catch, user_id: user.id)
      update_attrs = %{"species" => "Smallmouth Bass", "notes" => "Updated notes"}

      assert {:ok, %UserCatch{} = updated_catch} =
               Catches.update_user_catch(user_catch, update_attrs)

      assert updated_catch.species == "Smallmouth Bass"
      assert updated_catch.notes == "Updated notes"
    end

    test "returns changeset error with invalid data" do
      user = insert(:user)
      user_catch = insert(:user_catch, user_id: user.id)
      invalid_attrs = %{"latitude" => 200}

      assert {:error, %Ecto.Changeset{} = changeset} =
               Catches.update_user_catch(user_catch, invalid_attrs)

      assert %{
               latitude: ["must be less than or equal to 90"]
             } = errors_on(changeset)
    end
  end
end
