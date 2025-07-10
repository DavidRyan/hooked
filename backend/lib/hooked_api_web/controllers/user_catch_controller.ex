defmodule HookedApiWeb.UserCatchController do
  use HookedApiWeb, :controller

  alias Backend.Catches

  def index(conn, _params) do
    user_catches = Catches.list_user_catches()
    json(conn, %{user_catches: user_catches})
  end

  def show(conn, %{"id" => id}) do
    user_catch = Catches.get_user_catch(id)
    json(conn, %{user_catch: user_catch})
  end

  def create(conn, %{"user_catch" => user_catch_params}) do
    case Catches.create_ser_catch(user_catch_params) do
      {:ok, user_catch} ->
        conn
        |> put_status(:created)
        |> json(user_catch)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{errors: changeset})
    end
  end

  def update(conn, %{"id" => id, "user_catch" => user_catch_params}) do
  end

  def delete(conn, %{"id" => id}) do
    user_catch = Catches.get_user_catch(id)
    case Catches.delete_user_catch(user_catch) do
      {:ok, user_catch} -> 
        send_resp(conn, :no_content, "")
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Error deleting user_catch"})
    end
  end

  defp changeset_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      #TODO: Add error message to response
      msg
    end)
  end
end
