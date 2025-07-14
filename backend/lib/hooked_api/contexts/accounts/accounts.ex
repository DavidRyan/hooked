defmodule HookedApi.Accounts do
  @moduledoc """
  The Accounts context.
  """

  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Accounts.User
  alias HookedApi.Auth.Token

  @doc """
  Gets a single user.
  """
  def get_user(id), do: Repo.get(User, id)

  @doc """
  Gets a user by email.
  """
  def get_user_by_email(email) when is_binary(email) do
    Repo.get_by(User, email: email)
  end

  @doc """
  Gets a user by email and password.
  """
  def get_user_by_email_and_password(email, password)
      when is_binary(email) and is_binary(password) do
    user = get_user_by_email(email)
    if User.valid_password?(user, password), do: user
  end

  @doc """
  Registers a user.
  """
  def register_user(attrs \\ %{}) do
    %User{}
    |> User.registration_changeset(attrs)
    |> Repo.insert()
  end

  @doc """
  Authenticates a user and returns a JWT token.
  """
  def authenticate_user(email, password) do
    case get_user_by_email_and_password(email, password) do
      %User{is_active: true} = user ->
        case Token.generate_and_sign_for_user(user.id) do
          {:ok, token, _claims} -> {:ok, user, token}
          {:error, reason} -> {:error, reason}
        end
      
      %User{is_active: false} ->
        {:error, :account_disabled}
      
      nil ->
        # Run password hash to prevent timing attacks
        Bcrypt.no_user_verify()
        {:error, :invalid_credentials}
    end
  end

  @doc """
  Verifies a JWT token and returns the user.
  """
  def verify_token(token) do
    case Token.get_user_id_from_token(token) do
      {:ok, user_id} ->
        case get_user(user_id) do
          %User{is_active: true} = user -> {:ok, user}
          %User{is_active: false} -> {:error, :account_disabled}
          nil -> {:error, :user_not_found}
        end
      
      {:error, reason} ->
        {:error, reason}
    end
  end

  @doc """
  Updates a user.
  """
  def update_user(%User{} = user, attrs) do
    user
    |> User.update_changeset(attrs)
    |> Repo.update()
  end

  @doc """
  Deletes a user (soft delete by setting is_active to false).
  """
  def delete_user(%User{} = user) do
    update_user(user, %{is_active: false})
  end

  @doc """
  Returns an `%Ecto.Changeset{}` for tracking user changes.
  """
  def change_user(%User{} = user, attrs \\ %{}) do
    User.update_changeset(user, attrs)
  end

  @doc """
  Lists all active users.
  """
  def list_users do
    User
    |> where([u], u.is_active == true)
    |> Repo.all()
  end
end