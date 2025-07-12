defmodule HookedApi.Services.ImageStorage do
  require Logger

  @allowed_extensions ~w(.jpg .jpeg .png .webp .heic)
  @max_file_size 10_000_000
  @upload_dir "priv/static/uploads/catches"

  def upload_image(%Plug.Upload{} = upload) do
    with :ok <- validate_file(upload),
         {:ok, storage_path} <- store_file(upload),
         {:ok, metadata} <- build_metadata(upload, storage_path) do
      {:ok, metadata}
    end
  end

  def delete_image(image_url) when is_binary(image_url) do
    case get_storage_backend() do
      :local -> delete_local_file(image_url)
      :s3 -> delete_s3_file(image_url)
    end
  end
  def generate_presigned_url(filename, content_type) do
    case get_storage_backend() do
      :local -> {:error, :not_supported}
      :s3 -> generate_s3_presigned_url(filename, content_type)
    end
  end

  defp validate_file(%Plug.Upload{filename: filename, path: path}) do
    with :ok <- validate_extension(filename),
         :ok <- validate_file_size(path),
         :ok <- validate_content_type(path) do
      :ok
    end
  end

  defp validate_extension(filename) do
    extension = Path.extname(filename) |> String.downcase()

    if extension in @allowed_extensions do
      :ok
    else
      {:error, :invalid_file_type}
    end
  end

  defp validate_file_size(path) do
    case File.stat(path) do
      {:ok, %{size: size}} when size <= @max_file_size -> :ok
      {:ok, %{size: _}} -> {:error, :file_too_large}
      {:error, _} -> {:error, :file_not_found}
    end
  end

  defp validate_content_type(_path) do
    :ok
  end

  defp store_file(%Plug.Upload{filename: filename, path: temp_path}) do
    case get_storage_backend() do
      :local -> store_local_file(filename, temp_path)
      :s3 -> store_s3_file(filename, temp_path)
    end
  end

  defp store_local_file(filename, temp_path) do
    unique_filename = generate_unique_filename(filename)
    destination_dir = @upload_dir
    destination_path = Path.join(destination_dir, unique_filename)

    File.mkdir_p!(destination_dir)

    case File.cp(temp_path, destination_path) do
      :ok ->
        public_path = "/uploads/catches/#{unique_filename}"
        {:ok, public_path}

      {:error, reason} ->
        Logger.error("Failed to copy file: #{inspect(reason)}")
        {:error, :storage_failed}
    end
  end

  defp store_s3_file(filename, _temp_path) do
    unique_filename = generate_unique_filename(filename)
    s3_key = "catches/#{unique_filename}"

    {:ok, "https://your-bucket.s3.amazonaws.com/#{s3_key}"}
  end

  defp build_metadata(%Plug.Upload{filename: filename, path: temp_path}, storage_url) do
    with {:ok, %{size: file_size}} <- File.stat(temp_path),
         content_type <- get_content_type(filename) do
      metadata = %{
        "image_url" => storage_url,
        "image_filename" => filename,
        "image_content_type" => content_type,
        "image_file_size" => file_size
      }

      {:ok, metadata}
    else
      {:error, _} -> {:error, :metadata_failed}
    end
  end

  defp delete_local_file(image_url) do
    filename = Path.basename(image_url)
    file_path = Path.join(@upload_dir, filename)

    case File.rm(file_path) do
      :ok ->
        :ok

      {:error, :enoent} ->
        :ok

      {:error, reason} ->
        Logger.error("Failed to delete file: #{inspect(reason)}")
        {:error, :delete_failed}
    end
  end

  defp delete_s3_file(_image_url) do
    :ok
  end

  defp generate_s3_presigned_url(_filename, _content_type) do
    {:ok, "https://presigned-url..."}
  end

  defp generate_unique_filename(original_filename) do
    extension = Path.extname(original_filename)
    base_name = Path.basename(original_filename, extension)
    uuid = Ecto.UUID.generate()

    "#{uuid}-#{base_name}#{extension}"
  end

  defp get_content_type(filename) do
    case Path.extname(filename) |> String.downcase() do
      ".jpg" -> "image/jpeg"
      ".jpeg" -> "image/jpeg"
      ".png" -> "image/png"
      ".webp" -> "image/webp"
      ".heic" -> "image/heic"
      _ -> "application/octet-stream"
    end
  end

  def get_image_file_path(image_url) when is_binary(image_url) do
    case get_storage_backend() do
      :local -> get_local_file_path(image_url)
      :s3 -> {:error, :s3_not_supported}
    end
  end

  def get_image_file_path(_), do: {:error, :no_image}

  defp get_local_file_path(image_url) do
    filename = Path.basename(image_url)
    upload_dir = Application.get_env(:hooked_api, :image_upload_dir, @upload_dir)
    file_path = Path.join(upload_dir, filename)
    
    if File.exists?(file_path) do
      {:ok, file_path}
    else
      {:error, :file_not_found}
    end
  end

  defp get_storage_backend do
    Application.get_env(:hooked_api, :image_storage_backend, :local)
  end
end
