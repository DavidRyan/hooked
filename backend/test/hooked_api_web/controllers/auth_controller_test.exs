defmodule HookedApiWeb.AuthControllerTest do
  use HookedApiWeb.ConnCase, async: true
  import HookedApi.Factory

  describe "POST /api/auth/register" do
    test "creates user and returns JWT token with valid data", %{conn: conn} do
      user_params = %{
        "email" => "test@example.com",
        "password" => "Password123!",
        "first_name" => "John",
        "last_name" => "Doe"
      }

      conn = post(conn, ~p"/api/auth/register", user: user_params)

      assert %{
        "data" => %{
          "user" => %{
            "id" => _,
            "email" => "test@example.com",
            "first_name" => "John",
            "last_name" => "Doe"
          },
          "token" => token
        }
      } = json_response(conn, 201)

      assert is_binary(token)
      assert String.length(token) > 50
    end

    test "returns validation errors with invalid data", %{conn: conn} do
      user_params = %{
        "email" => "invalid-email",
        "password" => "short"
      }

      conn = post(conn, ~p"/api/auth/register", user: user_params)

      assert %{
        "error" => "validation_failed",
        "details" => %{
          "email" => _,
          "password" => _
        }
      } = json_response(conn, 422)
    end

    test "returns error when email already exists", %{conn: conn} do
      user = insert(:user, email: "existing@example.com")

      user_params = %{
        "email" => user.email,
        "password" => "Password123!"
      }

      conn = post(conn, ~p"/api/auth/register", user: user_params)

      assert %{
        "error" => "validation_failed",
        "details" => %{
          "email" => ["has already been taken"]
        }
      } = json_response(conn, 422)
    end
  end

  describe "POST /api/auth/login" do
    test "returns JWT token with valid credentials", %{conn: conn} do
      user = insert(:user, email: "test@example.com")

      conn = post(conn, ~p"/api/auth/login", %{
        "email" => "test@example.com",
        "password" => "password123"
      })

      assert %{
        "data" => %{
          "user" => %{
            "id" => user_id,
            "email" => "test@example.com"
          },
          "token" => token
        }
      } = json_response(conn, 200)

      assert user_id == user.id
      assert is_binary(token)
    end

    test "returns error with invalid credentials", %{conn: conn} do
      insert(:user, email: "test@example.com")

      conn = post(conn, ~p"/api/auth/login", %{
        "email" => "test@example.com",
        "password" => "wrongpassword"
      })

      assert %{
        "error" => "authentication_failed",
        "message" => "Invalid email or password"
      } = json_response(conn, 401)
    end

    test "returns error with non-existent user", %{conn: conn} do
      conn = post(conn, ~p"/api/auth/login", %{
        "email" => "nonexistent@example.com",
        "password" => "password123"
      })

      assert %{
        "error" => "authentication_failed",
        "message" => "Invalid email or password"
      } = json_response(conn, 401)
    end
  end

  describe "GET /api/auth/me" do
    test "returns current user data with valid token", %{conn: conn} do
      user = insert(:user)
      {:ok, token, _claims} = HookedApi.Auth.Token.generate_and_sign_for_user(user.id)

      conn = 
        conn
        |> put_req_header("authorization", "Bearer #{token}")
        |> get(~p"/api/auth/me")

      assert %{
        "data" => %{
          "id" => user_id,
          "email" => email
        }
      } = json_response(conn, 200)

      assert user_id == user.id
      assert email == user.email
    end

    test "returns error without token", %{conn: conn} do
      conn = get(conn, ~p"/api/auth/me")

      assert %{"error" => "Authorization header required"} = json_response(conn, 401)
    end

    test "returns error with invalid token", %{conn: conn} do
      conn = 
        conn
        |> put_req_header("authorization", "Bearer invalid-token")
        |> get(~p"/api/auth/me")

      assert %{"error" => "Invalid or expired token"} = json_response(conn, 401)
    end
  end

  describe "POST /api/auth/refresh" do
    test "returns new token with valid token", %{conn: conn} do
      user = insert(:user)
      {:ok, token, _claims} = HookedApi.Auth.Token.generate_and_sign_for_user(user.id)

      conn = 
        conn
        |> put_req_header("authorization", "Bearer #{token}")
        |> post(~p"/api/auth/refresh")

      assert %{
        "data" => %{
          "user" => %{
            "id" => user_id
          },
          "token" => new_token
        }
      } = json_response(conn, 200)

      assert user_id == user.id
      assert is_binary(new_token)
      assert new_token != token
    end

    test "returns error without token", %{conn: conn} do
      conn = post(conn, ~p"/api/auth/refresh")

      assert %{"error" => "Authorization header required"} = json_response(conn, 401)
    end
  end
end
