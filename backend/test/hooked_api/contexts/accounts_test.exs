defmodule HookedApi.AccountsTest do
  use HookedApi.DataCase, async: true
  import HookedApi.Factory

  alias HookedApi.Accounts
  alias HookedApi.Accounts.User

  describe "register_user/1" do
    test "creates user with valid attributes" do
      valid_attrs = %{
        "email" => "test@example.com",
        "password" => "Password123!",
        "first_name" => "John",
        "last_name" => "Doe"
      }

      assert {:ok, %User{} = user} = Accounts.register_user(valid_attrs)
      assert user.email == "test@example.com"
      assert user.first_name == "John"
      assert user.last_name == "Doe"
      assert user.is_active == true
      assert user.failed_login_attempts == 0
      assert Bcrypt.verify_pass("Password123!", user.password_hash)
    end

    test "returns changeset error with invalid attributes" do
      invalid_attrs = %{
        "email" => "invalid-email",
        "password" => "short"
      }

      assert {:error, %Ecto.Changeset{} = changeset} = Accounts.register_user(invalid_attrs)
      assert %{email: ["must be a valid email address"]} = errors_on(changeset)
      assert %{password: _} = errors_on(changeset)
    end

    test "returns error when email already exists" do
      existing_user = insert(:user, email: "existing@example.com")

      attrs = %{
        "email" => existing_user.email,
        "password" => "Password123!"
      }

      assert {:error, %Ecto.Changeset{} = changeset} = Accounts.register_user(attrs)
      assert %{email: ["has already been taken"]} = errors_on(changeset)
    end
  end

  describe "authenticate_user/2" do
    test "returns user and token with valid credentials" do
      user = insert(:user, email: "test@example.com")

      assert {:ok, authenticated_user, token} = 
        Accounts.authenticate_user("test@example.com", "password123")
      
      assert authenticated_user.id == user.id
      assert is_binary(token)
      assert String.length(token) > 50
    end

    test "returns error with invalid password" do
      insert(:user, email: "test@example.com")

      assert {:error, :invalid_credentials} = 
        Accounts.authenticate_user("test@example.com", "wrongpassword")
    end

    test "returns error with non-existent email" do
      assert {:error, :invalid_credentials} = 
        Accounts.authenticate_user("nonexistent@example.com", "password123")
    end

    test "locks account after max failed attempts" do
      user = insert(:user, email: "test@example.com")

      # Make 5 failed attempts
      for _ <- 1..5 do
        Accounts.authenticate_user("test@example.com", "wrongpassword")
      end

      # Check account is locked
      updated_user = Accounts.get_user(user.id)
      assert updated_user.failed_login_attempts == 5
      assert updated_user.locked_until != nil

      # Even correct password should fail
      assert {:error, :invalid_credentials} = 
        Accounts.authenticate_user("test@example.com", "password123")
    end

    test "resets failed attempts on successful login" do
      user = insert(:user, 
        email: "test@example.com",
        failed_login_attempts: 3
      )

      assert {:ok, _user, _token} = 
        Accounts.authenticate_user("test@example.com", "password123")

      updated_user = Accounts.get_user(user.id)
      assert updated_user.failed_login_attempts == 0
    end
  end

  describe "verify_token/1" do
    test "returns user with valid token" do
      user = insert(:user)
      {:ok, token, _claims} = HookedApi.Auth.Token.generate_and_sign_for_user(user.id)

      assert {:ok, verified_user} = Accounts.verify_token(token)
      assert verified_user.id == user.id
    end

    test "returns error with invalid token" do
      assert {:error, _reason} = Accounts.verify_token("invalid-token")
    end

    test "returns error for inactive user" do
      user = insert(:user, is_active: false)
      {:ok, token, _claims} = HookedApi.Auth.Token.generate_and_sign_for_user(user.id)

      assert {:error, :account_disabled} = Accounts.verify_token(token)
    end
  end

  describe "get_user_by_email/1" do
    test "returns user with existing email" do
      user = insert(:user, email: "test@example.com")

      assert found_user = Accounts.get_user_by_email("test@example.com")
      assert found_user.id == user.id
    end

    test "returns nil with non-existent email" do
      assert Accounts.get_user_by_email("nonexistent@example.com") == nil
    end
  end
end
