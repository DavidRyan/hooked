defmodule HookedApiWeb.AuthController do
  use HookedApiWeb, :controller

  alias HookedApi.Accounts
  alias HookedApi.Accounts.User

  action_fallback(HookedApiWeb.FallbackController)

  def register(conn, %{"user" => user_params}) do
    case Accounts.register_user(user_params) do
      {:ok, %User{} = user} ->
        case Accounts.authenticate_user(user.email, user_params["password"]) do
          {:ok, user, token} ->
            conn
            |> put_status(:created)
            |> json(%{
              user: %{
                id: user.id,
                email: user.email,
                first_name: user.first_name,
                last_name: user.last_name
              },
              token: token
            })

          {:error, reason} ->
            IO.inspect(reason, label: "Authentication error")

            conn
            |> put_status(:unprocessable_entity)
            |> json(%{error: "Authentication failed", debug: inspect(reason)})
        end

      {:error, %Ecto.Changeset{} = changeset} ->
        errors =
          Enum.map(changeset.errors, fn {field, {message, _}} ->
            %{field: field, message: message}
          end)

        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Validation failed", errors: errors})
    end
  end

  def login(conn, %{"email" => email, "password" => password}) do
    case Accounts.authenticate_user(email, password) do
      {:ok, user, token} ->
        conn
        |> json(%{
          user: %{
            id: user.id,
            email: user.email,
            first_name: user.first_name,
            last_name: user.last_name
          },
          token: token
        })

      {:error, _reason} ->
        conn
        |> put_status(:unauthorized)
        |> json(%{error: "Invalid email or password"})
    end
  end

  def me(conn, _params) do
    user = conn.assigns[:current_user]

    conn
    |> json(%{
      user: %{
        id: user.id,
        email: user.email,
        first_name: user.first_name,
        last_name: user.last_name
      }
    })
  end

  def refresh(conn, _params) do
    user = conn.assigns[:current_user]

    case HookedApi.Auth.Token.generate_and_sign_for_user(user.id) do
      {:ok, token, _claims} ->
        conn
        |> json(%{
          user: %{
            id: user.id,
            email: user.email,
            first_name: user.first_name,
            last_name: user.last_name
          },
          token: token
        })

      {:error, _reason} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Token generation failed"})
    end
  end
end
