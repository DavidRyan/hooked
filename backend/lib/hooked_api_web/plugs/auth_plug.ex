defmodule HookedApiWeb.Plugs.AuthPlug do
  @moduledoc """
  Plug for JWT authentication.
  """

  import Plug.Conn
  import Phoenix.Controller

  alias HookedApi.Accounts

  def init(opts), do: opts

  def call(conn, _opts) do
    case get_token_from_header(conn) do
      {:ok, token} ->
        case Accounts.verify_token(token) do
          {:ok, user} ->
            assign(conn, :current_user, user)
          
          {:error, _reason} ->
            conn
            |> put_status(:unauthorized)
            |> json(%{error: "Invalid or expired token"})
            |> halt()
        end
      
      {:error, :no_token} ->
        conn
        |> put_status(:unauthorized)
        |> json(%{error: "Authorization header required"})
        |> halt()
    end
  end

  defp get_token_from_header(conn) do
    case get_req_header(conn, "authorization") do
      ["Bearer " <> token] -> {:ok, token}
      _ -> {:error, :no_token}
    end
  end
end