defmodule HookedApiWeb.AuthController do
  use HookedApiWeb, :controller

  alias HookedApi.Accounts
  alias HookedApi.Accounts.User

  action_fallback HookedApiWeb.FallbackController

  def register(conn, %{"user" => user_params}) do
    case Accounts.register_user(user_params) do
      {:ok, %User{} = user} ->
        case Accounts.authenticate_user(user.email, user_params["password"]) do
          {:ok, user, token} ->
            conn
            |> put_status(:created)
            |> render(:user_with_token, user: user, token: token)
          
          {:error, _reason} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(%{error: "Authentication failed"})
        end

      {:error, %Ecto.Changeset{} = changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(:errors, changeset: changeset)
    end
  end

  def login(conn, %{"email" => email, "password" => password}) do
    case Accounts.authenticate_user(email, password) do
      {:ok, user, token} ->
        render(conn, :user_with_token, user: user, token: token)
      
      {:error, _reason} ->
        conn
        |> put_status(:unauthorized)
        |> json(%{error: "Invalid email or password"})
    end
  end

  def me(conn, _params) do
    user = conn.assigns[:current_user]
    render(conn, :user, user: user)
  end

  def refresh(conn, _params) do
    user = conn.assigns[:current_user]
    
    case HookedApi.Auth.Token.generate_and_sign_for_user(user.id) do
      {:ok, token, _claims} ->
        render(conn, :user_with_token, user: user, token: token)
      
      {:error, _reason} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Token generation failed"})
    end
  end
end