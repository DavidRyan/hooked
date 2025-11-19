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

  def create(conn, %{"user_catch" => user_catch_params}) do
    case Map.pop(user_catch_params, "image_base64") do
      {nil, _} ->
        conn
        |> put_status(:bad_request)
        |> json(%{error: "image_base64 is required"})

      {image_base64, updated_params} when is_binary(image_base64) and image_base64 != "" ->
        decoded_image =
          case Base.decode64(image_base64) do
            {:ok, data} ->
              data

            :error ->
              conn
              |> put_status(:bad_request)
              |> json(%{error: "Invalid base64 data"})
              |> Kernel.then(fn conn -> throw({:halt, conn}) end)
          end

        temp_path =
          Path.join(System.tmp_dir!(), "image_upload_#{:erlang.unique_integer([:positive])}.jpg")

        File.write!(temp_path, decoded_image)

        upload = %Plug.Upload{
          path: temp_path,
          filename: "upload_from_base64.jpg",
          content_type: "image/jpeg"
        }

        try do
          case Catches.create_user_catch(
                 conn.assigns[:current_user].id,
                 updated_params,
                 upload
               ) do
            {:ok, user_catch} ->
              conn
              |> put_status(:created)
              |> json(%{user_catch: user_catch})

            {:error, reason} when is_atom(reason) ->
              conn
              |> put_status(:unprocessable_entity)
              |> json(%{error: format_error_reason(reason)})

            {:error, {:s3_upload_exception, message}} ->
              conn
              |> put_status(:internal_server_error)
              |> json(%{error: "Failed to upload image to storage: #{message}"})

            {:error, {:s3_upload_caught, {kind, reason}}} ->
              conn
              |> put_status(:internal_server_error)
              |> json(%{error: "Failed to upload image: #{kind} - #{reason}"})

            {:error, :missing_s3_configuration} ->
              conn
              |> put_status(:internal_server_error)
              |> json(%{error: "Server storage configuration error. Please contact support."})

            {:error, %Ecto.Changeset{} = changeset} ->
              conn
              |> put_status(:unprocessable_entity)
              |> json(%{errors: changeset_errors(changeset)})

            {:error, other} ->
              conn
              |> put_status(:internal_server_error)
              |> json(%{error: "Upload failed: #{inspect(other)}"})
          end
        after
          File.rm(temp_path)
        end

      {_, _} ->
        conn
        |> put_status(:bad_request)
        |> json(%{error: "Valid image_base64 data is required"})
    end
  catch
    {:halt, conn} -> conn
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

  def stats(conn, %{"user_id" => user_id}) do
    stats = Catches.get_user_catch_stats(user_id)
    json(conn, %{stats: stats})
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
