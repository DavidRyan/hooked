defmodule HookedApiWeb.AuthController do
  use HookedApiWeb, :controller

  alias HookedApi.Accounts
  alias HookedApi.Accounts.User

  action_fallback(HookedApiWeb.FallbackController)

  # Helper function to create detailed validation error responses
  defp validation_error_response(changeset) do
    errors = extract_changeset_errors(changeset)

    %{
      error: "validation_failed",
      message: "The provided data failed validation",
      details: errors,
      code: "VALIDATION_ERROR"
    }
  end

  # Helper function to create authentication error responses
  defp authentication_error_response(reason) do
    message =
      case reason do
        :invalid_credentials -> "Invalid email or password"
        :account_locked -> "Account is temporarily locked due to too many failed login attempts"
        :account_disabled -> "Account is disabled"
        :user_not_found -> "Invalid email or password"
        _ -> "Authentication failed"
      end

    %{
      error: "authentication_failed",
      message: message,
      code: "AUTH_ERROR"
    }
  end

  # Extract errors from Ecto changeset with proper formatting
  defp extract_changeset_errors(changeset) do
    Enum.reduce(changeset.errors, %{}, fn {field, {message, opts}}, acc ->
      formatted_message = format_error_message(message, opts)

      case Map.get(acc, field) do
        nil -> Map.put(acc, field, [formatted_message])
        existing -> Map.put(acc, field, existing ++ [formatted_message])
      end
    end)
  end

  # Format error message with interpolated values
  defp format_error_message(message, opts) do
    Enum.reduce(opts, message, fn {key, value}, acc ->
      formatted_value =
        case value do
          v when is_list(v) -> inspect(v)
          v -> to_string(v)
        end

      String.replace(acc, "%{#{key}}", formatted_value)
    end)
  end

  def register(conn, %{"user" => user_params}) do
    case Accounts.register_user(user_params) do
      {:ok, %User{} = user} ->
        case Accounts.authenticate_user(user.email, user_params["password"]) do
          {:ok, user, token} ->
            conn
            |> put_status(:created)
            |> json(%{
              data: %{
                user: %{
                  id: user.id,
                  email: user.email,
                  first_name: user.first_name,
                  last_name: user.last_name
                },
                token: token
              }
            })

          {:error, reason} ->
            IO.inspect(reason, label: "Authentication error after registration")
            error_response = authentication_error_response(reason)

            conn
            |> put_status(:unprocessable_entity)
            |> json(error_response)
        end

      {:error, %Ecto.Changeset{} = changeset} ->
        error_response = validation_error_response(changeset)

        conn
        |> put_status(:unprocessable_entity)
        |> json(error_response)
    end
  end

  def login(conn, %{"email" => email, "password" => password}) do
    case Accounts.authenticate_user(email, password) do
      {:ok, user, token} ->
        conn
        |> json(%{
          data: %{
            user: %{
              id: user.id,
              email: user.email,
              first_name: user.first_name,
              last_name: user.last_name
            },
            token: token
          }
        })

      {:error, reason} ->
        error_response = authentication_error_response(reason)

        conn
        |> put_status(:unauthorized)
        |> json(error_response)
    end
  end

  def me(conn, _params) do
    user = conn.assigns[:current_user]

    conn
    |> json(%{
      data: %{
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
          data: %{
            user: %{
              id: user.id,
              email: user.email,
              first_name: user.first_name,
              last_name: user.last_name
            },
            token: token
          }
        })

      {:error, _reason} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Token generation failed"})
    end
  end
end
