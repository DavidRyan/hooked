defmodule HookedApi.Services.ImageStorage do
  @moduledoc """
  Service for handling image uploads and storage.
  
  Supports multiple storage backends:
  - Local filesystem (development)
  - AWS S3 (production)
  - Other cloud providers (future)
  """

  require Logger

  @allowed_extensions ~w(.jpg .jpeg .png .webp .heic)
  @max_file_size 10_000_000 # 10MB
  @upload_dir "priv/static/uploads/catches"

  @doc """
  Uploads an image file and returns the storage metadata.
  
  ## Examples
  
      iex> upload_image(%Plug.Upload{filename: "fish.jpg", path: "/tmp/..."})
      {:ok, %{
        url: "/uploads/catches/uuid-fish.jpg",
        filename: "fish.jpg", 
        content_type: "image/jpeg",
        file_size: 1024
      }}
      
      iex> upload_image(%Plug.Upload{filename: "bad.txt"})
      {:error, :invalid_file_type}
  """
  def upload_image(%Plug.Upload{} = upload) do
    with :ok <- validate_file(upload),
         {:ok, storage_path} <- store_file(upload),
         {:ok, metadata} <- build_metadata(upload, storage_path) do
      {:ok, metadata}
    end
  end

  @doc """
  Deletes an image file from storage.
  """
  def delete_image(image_url) when is_binary(image_url) do
    case get_storage_backend() do
      :local -> delete_local_file(image_url)
      :s3 -> delete_s3_file(image_url)
    end
  end

  @doc """
  Generates a presigned URL for direct upload (useful for mobile apps).
  """
  def generate_presigned_url(filename, content_type) do
    case get_storage_backend() do
      :local -> {:error, :not_supported}
      :s3 -> generate_s3_presigned_url(filename, content_type)
    end
  end

  # Private functions

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

  defp validate_content_type(path) do
    # You could use a library like `file_info` or `mime` here
    # For now, we'll trust the file extension validation
    :ok
  end

  defp store_file(%Plug.Upload{filename: filename, path: temp_path}) do
    case get_storage_backend() do
      :local -> store_local_file(filename, temp_path)
      :s3 -> store_s3_file(filename, temp_path)
    end
  end

  defp store_local_file(filename, temp_path) do
    # Generate unique filename to avoid conflicts
    unique_filename = generate_unique_filename(filename)
    destination_dir = @upload_dir
    destination_path = Path.join(destination_dir, unique_filename)
    
    # Ensure upload directory exists
    File.mkdir_p!(destination_dir)
    
    case File.cp(temp_path, destination_path) do
      :ok -> 
        # Return the public URL path
        public_path = "/uploads/catches/#{unique_filename}"
        {:ok, public_path}
      {:error, reason} -> 
        Logger.error("Failed to copy file: #{inspect(reason)}")
        {:error, :storage_failed}
    end
  end

  defp store_s3_file(filename, temp_path) do
    # Implementation for S3 upload
    # You'd use ExAws.S3 or similar library here
    unique_filename = generate_unique_filename(filename)
    s3_key = "catches/#{unique_filename}"
    
    # Placeholder for S3 upload logic
    # ExAws.S3.put_object(bucket, s3_key, File.read!(temp_path))
    # |> ExAws.request()
    
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
    # Extract filename from URL like "/uploads/catches/uuid-fish.jpg"
    filename = Path.basename(image_url)
    file_path = Path.join(@upload_dir, filename)
    
    case File.rm(file_path) do
      :ok -> :ok
      {:error, :enoent} -> :ok # File already doesn't exist
      {:error, reason} -> 
        Logger.error("Failed to delete file: #{inspect(reason)}")
        {:error, :delete_failed}
    end
  end

  defp delete_s3_file(image_url) do
    # Extract S3 key from URL
    # Implementation for S3 delete
    :ok
  end

  defp generate_s3_presigned_url(filename, content_type) do
    # Implementation for S3 presigned URL
    # ExAws.S3.presigned_url(:put, bucket, key, expires_in: 3600)
    {:ok, "https://presigned-url..."}
  end

  defp generate_unique_filename(original_filename) do
    extension = Path.extname(original_filename)
    base_name = Path.basename(original_filename, extension)
    uuid = Ecto.UUID.generate()
    
    # Create filename like: "uuid-original-name.jpg"
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

  defp get_storage_backend do
    Application.get_env(:hooked_api, :image_storage_backend, :local)
  end
end