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
    user = get_user_by_email(email)
    
    case check_account_lockout(user) do
      {:ok, user} ->
        case User.valid_password?(user, password) do
          true ->
            reset_failed_login_attempts(user)
            case Token.generate_and_sign_for_user(user.id) do
              {:ok, token, _claims} -> {:ok, user, token}
              {:error, reason} -> {:error, reason}
            end
          
          false ->
            increment_failed_login_attempts(user)
            {:error, :invalid_credentials}
        end
      
      {:error, :account_locked} ->
        # Still run password hash to prevent timing attacks
        Bcrypt.no_user_verify()
        {:error, :invalid_credentials}
      
      {:error, :account_disabled} ->
        # Run password hash to prevent timing attacks and hide account status
        Bcrypt.no_user_verify()
        {:error, :invalid_credentials}
      
      {:error, :user_not_found} ->
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

  # Account lockout constants
  @max_failed_attempts 5
  @lockout_duration_minutes 15

  defp check_account_lockout(nil), do: {:error, :user_not_found}
  
  defp check_account_lockout(%User{is_active: false}), do: {:error, :account_disabled}
  
  defp check_account_lockout(%User{locked_until: nil} = user), do: {:ok, user}
  
  defp check_account_lockout(%User{locked_until: locked_until} = user) do
    now = DateTime.utc_now()
    
    case DateTime.compare(now, locked_until) do
      :gt ->
        # Lock has expired, reset the user
        unlock_user(user)
        {:ok, user}
      
      _ ->
        {:error, :account_locked}
    end
  end

  defp increment_failed_login_attempts(%User{} = user) do
    new_attempts = user.failed_login_attempts + 1
    now = DateTime.utc_now()
    
    attrs = %{
      failed_login_attempts: new_attempts,
      last_failed_login: now
    }
    
    attrs = if new_attempts >= @max_failed_attempts do
      lockout_until = DateTime.add(now, @lockout_duration_minutes * 60, :second)
      Map.put(attrs, :locked_until, lockout_until)
    else
      attrs
    end
    
    user
    |> User.update_changeset(attrs)
    |> Repo.update()
  end

  defp reset_failed_login_attempts(%User{failed_login_attempts: 0}), do: :ok
  
  defp reset_failed_login_attempts(%User{} = user) do
    user
    |> User.update_changeset(%{
      failed_login_attempts: 0,
      locked_until: nil,
      last_failed_login: nil
    })
    |> Repo.update()
  end

  defp unlock_user(%User{} = user) do
    user
    |> User.update_changeset(%{
      failed_login_attempts: 0,
      locked_until: nil,
      last_failed_login: nil
    })
    |> Repo.update()
  end
end