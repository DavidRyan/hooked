defmodule HookedApiWeb.UserCatchController do
  use HookedApiWeb, :controller

  alias HookedApi.Catches

  def index(conn, _params) do
    user_catches = Catches.list_user_catches()
    json(conn, %{user_catches: user_catches})
  end

  def show(conn, %{"id" => id}) do
    case Catches.get_user_catch(id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{error: "User catch not found"})
      user_catch ->
        json(conn, %{user_catch: user_catch})
    end
  end

  def create(conn, %{"user_catch" => user_catch_params}) do
    # todo: add validation
    # call catches.create_user_catch(user_catch_params)
    # it starts a background process to enrich the catch
    # return 200
    # broadcast created with enriched data to websockt
  end

  def update(conn, %{"id" => id, "user_catch" => user_catch_params}) do
    case Catches.get_user_catch(id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{error: "User catch not found"})
      user_catch ->
        case Catches.update_user_catch(user_catch, user_catch_params) do
          {:ok, user_catch} ->
            json(conn, %{user_catch: user_catch})
          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(%{errors: changeset_errors(changeset)})
        end
    end
  end

  def delete(conn, %{"id" => id}) do
    case Catches.get_user_catch(id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{error: "User catch not found"})
      user_catch ->
        case Catches.delete_user_catch(user_catch) do
          {:ok, _user_catch} -> 
            send_resp(conn, :no_content, "")
          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(%{errors: changeset_errors(changeset)})
        end
    end
  end

  defp changeset_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Regex.replace(~r"%{(\w+)}", msg, fn _, key ->
        opts |> Keyword.get(String.to_existing_atom(key), key) |> to_string()
      end)
    end)
  end
end
