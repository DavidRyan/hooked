defmodule HookedApiWeb.UserCatchController do
  use HookedApiWeb, :controller

  alias HookedApi.Catches

  def index(conn, _params) do
    user_catches = Catches.list_user_catches(conn.assigns[:current_user].id)
    json(conn, %{user_catches: user_catches})
  end

  def show(conn, %{"id" => id}) do
    case Catches.get_user_catch(conn.assigns[:current_user].id, id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{error: "User catch not found"})

      user_catch ->
        json(conn, %{user_catch: user_catch})
    end
  end

  def create(conn, %{"user_catch" => user_catch_params, "image" => %Plug.Upload{} = image_upload}) do
    case Catches.create_user_catch(
           conn.assigns[:current_user].id,
           user_catch_params,
           image_upload
         ) do
      {:ok, user_catch} ->
        conn
        |> put_status(:created)
        |> json(%{user_catch: user_catch})

      {:error, reason} when is_atom(reason) ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: format_error_reason(reason)})

      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{errors: changeset_errors(changeset)})
    end
  end

  def create(conn, _params) do
    conn
    |> put_status(:bad_request)
    |> json(%{error: "Image is required"})
  end

  def update(conn, %{"id" => id, "user_catch" => user_catch_params}) do
    case Catches.get_user_catch(conn.assigns[:current_user].id, id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{error: "User catch not found"})

      user_catch ->
        case Catches.update_user_catch(user_catch, user_catch_params) do
          {:ok, updated_catch} ->
            json(conn, %{user_catch: updated_catch})

          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(%{errors: changeset_errors(changeset)})
        end
    end
  end

  def delete(conn, %{"id" => id}) do
    case Catches.get_user_catch(conn.assigns[:current_user].id, id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{error: "User catch not found"})

      user_catch ->
        case Catches.delete_user_catch(user_catch) do
          {:ok, _deleted_catch} ->
            json(conn, %{message: "User catch deleted successfully"})

          {:error, _changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(%{error: "Failed to delete user catch"})
        end
    end
  end

  defp format_error_reason(:invalid_file_type),
    do: "Invalid file type. Only JPEG, PNG, WebP, and HEIC images are allowed."

  defp format_error_reason(:file_too_large), do: "File is too large. Maximum size is 10MB."
  defp format_error_reason(:storage_failed), do: "Failed to store image. Please try again."
  defp format_error_reason(reason), do: "Upload failed: #{reason}"

  defp changeset_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Regex.replace(~r"%{(\w+)}", msg, fn _, key ->
        opts |> Keyword.get(String.to_existing_atom(key), key) |> to_string()
      end)
    end)
  end
end
