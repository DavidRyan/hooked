defmodule HookedApiWeb.UserCatchControllerTest do
  use HookedApiWeb.ConnCase

  import HookedApi.Factory

  setup %{conn: conn} do
    user = insert(:user)
    {:ok, token, _claims} = HookedApi.Auth.Token.generate_and_sign_for_user(user.id)

    conn =
      conn
      |> put_req_header("accept", "application/json")
      |> put_req_header("authorization", "Bearer #{token}")

    %{conn: conn, user: user}
  end

  describe "GET /api/user_catches" do
    test "returns user's catches when authenticated", %{conn: conn, user: user} do
      insert(:user_catch, user_id: user.id, species: "Bass")
      insert(:user_catch, user_id: user.id, species: "Pike")

      conn = get(conn, ~p"/api/user_catches")

      assert %{"user_catches" => catches} = json_response(conn, 200)
      assert length(catches) == 2
      assert Enum.all?(catches, fn user_catch -> user_catch["user_id"] == user.id end)
    end

    test "returns empty list when user has no catches", %{conn: conn} do
      conn = get(conn, ~p"/api/user_catches")

      assert %{"user_catches" => []} = json_response(conn, 200)
    end

    test "only returns the authenticated user's catches", %{conn: conn, user: user} do
      other_user = insert(:user)
      insert(:user_catch, user_id: user.id, species: "Bass")
      insert(:user_catch, user_id: other_user.id, species: "Pike")

      conn = get(conn, ~p"/api/user_catches")

      assert %{"user_catches" => catches} = json_response(conn, 200)
      assert length(catches) == 1
      assert hd(catches)["user_id"] == user.id
      assert hd(catches)["species"] == "Bass"
    end

    test "returns 401 when not authenticated" do
      conn = build_conn() |> put_req_header("accept", "application/json")
      conn = get(conn, ~p"/api/user_catches")

      assert %{"error" => "Authorization header required"} = json_response(conn, 401)
    end
  end

  describe "GET /api/user_catches/:id" do
    test "returns user's catch when authenticated and authorized", %{conn: conn, user: user} do
      user_catch = insert(:user_catch, user_id: user.id, species: "Salmon", notes: "Great catch!")

      conn = get(conn, ~p"/api/user_catches/#{user_catch.id}")

      assert %{"user_catch" => returned_catch} = json_response(conn, 200)
      assert returned_catch["id"] == user_catch.id
      assert returned_catch["species"] == "Salmon"
      assert returned_catch["notes"] == "Great catch!"
      assert returned_catch["user_id"] == user.id
    end

    test "returns 404 when catch doesn't exist", %{conn: conn} do
      conn = get(conn, ~p"/api/user_catches/00000000-0000-0000-0000-000000000000")

      assert %{"error" => "User catch not found"} = json_response(conn, 404)
    end

    test "returns 404 when trying to access another user's catch", %{conn: conn} do
      other_user = insert(:user)
      other_catch = insert(:user_catch, user_id: other_user.id, species: "Pike")

      conn = get(conn, ~p"/api/user_catches/#{other_catch.id}")

      assert %{"error" => "User catch not found"} = json_response(conn, 404)
    end
  end

  describe "POST /api/user_catches" do
    test "creates catch with valid data", %{conn: conn, user: user} do
      catch_params = %{
        "species" => "Largemouth Bass",
        "location" => "Lake Michigan",
        "latitude" => 42.3601,
        "longitude" => -87.6298,
        "caught_at" => "2024-01-15T10:30:00",
        "notes" => "Great catch!"
      }

      image_upload = build(:image_upload)
      conn = post(conn, ~p"/api/user_catches", user_catch: catch_params, image: image_upload)

      assert %{
               "user_catch" => %{
                 "id" => _,
                 "species" => "Largemouth Bass",
                 "location" => "Lake Michigan",
                 "latitude" => 42.3601,
                 "longitude" => -87.6298,
                 "notes" => "Great catch!",
                 "user_id" => user_id
               }
             } = json_response(conn, 201)

      assert user_id == user.id
    end

    test "creates catch with image data", %{conn: conn} do
      catch_params = %{
        "species" => "Bass",
        "location" => "Lake",
        "latitude" => 42.0,
        "longitude" => -87.0,
        "caught_at" => "2024-01-15T10:30:00"
      }

      image_upload = build(:image_upload)
      conn = post(conn, ~p"/api/user_catches", user_catch: catch_params, image: image_upload)

      assert %{
               "user_catch" => %{
                 "species" => "Bass"
               }
             } = json_response(conn, 201)
    end

    test "returns validation errors with invalid data", %{conn: conn} do
      catch_params = %{
        "species" => "",
        "latitude" => 100.0,
        "longitude" => -200.0
      }

      image_upload = build(:image_upload)
      conn = post(conn, ~p"/api/user_catches", user_catch: catch_params, image: image_upload)

      assert %{"errors" => _errors} = json_response(conn, 422)
    end

    test "returns error when image is missing", %{conn: conn} do
      catch_params = %{
        "species" => "Bass",
        "location" => "Lake"
      }

      conn = post(conn, ~p"/api/user_catches", user_catch: catch_params)

      assert %{"error" => "Image is required"} = json_response(conn, 400)
    end
  end

  describe "PUT /api/user_catches/:id" do
    test "updates user's catch with valid data", %{conn: conn, user: user} do
      user_catch =
        insert(:user_catch, user_id: user.id, species: "Unknown", notes: "Initial notes")

      update_params = %{
        "species" => "Identified Bass",
        "notes" => "Updated notes"
      }

      conn = put(conn, ~p"/api/user_catches/#{user_catch.id}", user_catch: update_params)

      assert %{
               "user_catch" => %{
                 "species" => "Identified Bass",
                 "notes" => "Updated notes"
               }
             } = json_response(conn, 200)
    end

    test "returns 404 when trying to update another user's catch", %{conn: conn} do
      other_user = insert(:user)
      other_catch = insert(:user_catch, user_id: other_user.id, species: "Pike")

      update_params = %{"species" => "Updated Species"}

      conn = put(conn, ~p"/api/user_catches/#{other_catch.id}", user_catch: update_params)

      assert %{"error" => "User catch not found"} = json_response(conn, 404)
    end
  end

  describe "DELETE /api/user_catches/:id" do
    test "deletes user's catch", %{conn: conn, user: user} do
      user_catch = insert(:user_catch, user_id: user.id)

      conn = delete(conn, ~p"/api/user_catches/#{user_catch.id}")

      assert %{"message" => "User catch deleted successfully"} = json_response(conn, 200)
    end

    test "returns 404 when trying to delete another user's catch", %{conn: conn} do
      other_user = insert(:user)
      other_catch = insert(:user_catch, user_id: other_user.id)

      conn = delete(conn, ~p"/api/user_catches/#{other_catch.id}")

      assert %{"error" => "User catch not found"} = json_response(conn, 404)
    end
  end
end
