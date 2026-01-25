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
        conn |> put_status(:not_found) |> json(%{error: "Catch not found"})

      user_catch ->
        json(conn, %{user_catch: user_catch})
    end
  end

  def create(conn, %{"image" => %Plug.Upload{} = upload} = params) do
    attrs = Map.get(params, "user_catch", %{})

    case Catches.create_user_catch(conn.assigns[:current_user].id, attrs, upload) do
      {:ok, user_catch} ->
        conn |> put_status(:created) |> json(%{user_catch: user_catch})

      {:error, %Ecto.Changeset{} = changeset} ->
        conn |> put_status(:unprocessable_entity) |> json(%{errors: changeset_errors(changeset)})

      {:error, reason} ->
        conn |> put_status(:unprocessable_entity) |> json(%{error: format_error(reason)})
    end
  end

  def create(conn, _params) do
    conn |> put_status(:bad_request) |> json(%{error: "Image file is required"})
  end

  def delete(conn, %{"id" => id}) do
    case Catches.get_user_catch(conn.assigns[:current_user].id, id) do
      nil ->
        conn |> put_status(:not_found) |> json(%{error: "Catch not found"})

      user_catch ->
        case Catches.delete_user_catch(user_catch) do
          {:ok, _} -> json(conn, %{message: "Catch deleted"})
          {:error, _} -> conn |> put_status(:unprocessable_entity) |> json(%{error: "Delete failed"})
        end
    end
  end

  def stats(conn, %{"user_id" => user_id}) do
    json(conn, %{stats: Catches.get_user_catch_stats(user_id)})
  end

  defp format_error(:invalid_file_type), do: "Invalid file type. Allowed: JPEG, PNG, WebP, HEIC"
  defp format_error(:file_too_large), do: "File too large. Max 10MB"
  defp format_error(:missing_s3_configuration), do: "Storage configuration error"
  defp format_error({:s3_upload_exception, msg}), do: "Upload failed: #{msg}"
  defp format_error({:s3_upload_caught, {kind, reason}}), do: "Upload failed: #{kind} - #{reason}"
  defp format_error(reason) when is_atom(reason), do: "Error: #{reason}"
  defp format_error(reason), do: "Error: #{inspect(reason)}"

  defp changeset_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Regex.replace(~r"%{(\w+)}", msg, fn _, key ->
        opts |> Keyword.get(String.to_existing_atom(key), key) |> to_string()
      end)
    end)
  end
end
