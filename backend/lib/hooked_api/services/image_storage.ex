defmodule HookedApi.Services.ImageStorage do
  require Logger
  alias HookedApi.Services.AwsCredentials

  @allowed_extensions ~w(.jpg .jpeg .png .webp .heic)
  @max_file_size 10_000_000
  @upload_dir "priv/static/uploads/catches"

  def upload_image(%Plug.Upload{} = upload) do
    log_memory("upload:start", filename: upload.filename, file_path: upload.path)

    with :ok <- validate_file(upload),
         {:ok, storage_path} <- store_file(upload),
         {:ok, metadata} <- build_metadata(upload, storage_path) do
      log_memory("upload:finished", filename: upload.filename, storage_path: storage_path)
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

  defp store_s3_file(filename, temp_path) do
    key = build_s3_key(filename)
    bucket = get_s3_bucket()
    region = get_s3_region()
    content_type = get_content_type(filename)

    # Ensure AWS credentials are configured at runtime
    credentials_result = AwsCredentials.ensure_credentials_configured()

    # Log AWS configuration status
    aws_config = AwsCredentials.get_aws_config()

    Logger.debug("S3 UPLOAD: AWS Configuration",
      bucket: bucket,
      region: region,
      access_key_id: aws_config.access_key_id,
      has_secret:
        if(aws_config.secret_access_key && aws_config.secret_access_key != "",
          do: true,
          else: false
        )
    )

    case credentials_result do
      {:ok, _} ->
        # First verify we have all necessary configuration
        missing_config = []

        missing_config =
          if is_nil(bucket) or bucket == "",
            do: missing_config ++ ["S3_BUCKET"],
            else: missing_config

        missing_config =
          if is_nil(region) or region == "",
            do: missing_config ++ ["S3_REGION"],
            else: missing_config

        if length(missing_config) > 0 do
          Logger.error("S3 UPLOAD ERROR: Missing required S3 configuration",
            missing_env_vars: missing_config,
            original_filename: filename
          )

          {:error, :missing_s3_configuration}
        else
          file_size =
            case File.stat(temp_path) do
              {:ok, %{size: size}} -> size
              _ -> "unknown"
            end

          Logger.info("S3 UPLOAD: Preparing to stream file to S3",
            bucket: bucket,
            region: region,
            key: key,
            original_filename: filename,
            content_type: content_type,
            temp_path: temp_path,
            file_exists: File.exists?(temp_path),
            file_size: file_size
          )

          log_memory("upload:pre_stream",
            filename: filename,
            temp_path: temp_path,
            file_size: file_size
          )

          try do
            # Use streaming upload to avoid loading entire file into memory
            result = stream_upload_to_s3(temp_path, bucket, key, content_type)

            log_memory("upload:post_stream",
              filename: filename,
              temp_path: temp_path,
              file_size: file_size,
              result: inspect(result)
            )

            case result do
              {:ok, _response} ->
                s3_url = "https://#{bucket}.s3.#{region}.amazonaws.com/#{key}"

                Logger.info("S3 UPLOAD: Successfully streamed file to S3",
                  bucket: bucket,
                  key: key,
                  file_size: file_size
                )

                {:ok, s3_url}

              {:error, reason} = error ->
                Logger.error("S3 UPLOAD ERROR: Failed to stream to S3",
                  bucket: bucket,
                  region: region,
                  key: key,
                  original_filename: filename,
                  reason: reason
                )

                error
            end
          rescue
            e ->
              Logger.error("S3 UPLOAD EXCEPTION: Exception while streaming file",
                original_filename: filename,
                temp_path: temp_path,
                exception: Exception.message(e),
                stacktrace: Exception.format_stacktrace(__STACKTRACE__)
              )

              {:error, :file_processing_failed}
          end
        end

      {:error, reason} ->
        Logger.error("S3 UPLOAD ERROR: AWS credentials configuration failed",
          reason: inspect(reason),
          original_filename: filename
        )

        {:error, :missing_s3_configuration}
    end
  end

  defp stream_upload_to_s3(file_path, bucket, key, content_type) do
    # Ensure AWS credentials are properly configured before uploading
    HookedApi.Services.AwsCredentials.ensure_credentials_configured()

    secret_key = Application.get_env(:ex_aws, :secret_access_key)
    access_key = Application.get_env(:ex_aws, :access_key_id)
    region_value = Application.get_env(:ex_aws, :region)

    if is_nil(secret_key) do
      Logger.error("S3 UPLOAD: secret_access_key is nil! Setting placeholder")
      Application.put_env(:ex_aws, :secret_access_key, "INVALID-SECRET-KEY-PLACEHOLDER")
    end

    secret_key = Application.get_env(:ex_aws, :secret_access_key)
    has_secret = secret_key != nil && secret_key != ""

    Logger.debug("S3 STREAM UPLOAD: AWS config check",
      access_key_id: access_key,
      secret_key_set: has_secret,
      region: region_value,
      bucket: bucket
    )

    try do
      # Stream the file in chunks to avoid loading entire file into memory
      # Using 5MB chunks (S3 multipart minimum is 5MB except for last part)
      chunk_size = 5 * 1024 * 1024

      file_path
      |> ExAws.S3.Upload.stream_file(chunk_size: chunk_size)
      |> ExAws.S3.upload(bucket, key, content_type: content_type)
      |> ExAws.request(
        secret_access_key: secret_key,
        access_key_id: access_key,
        region: region_value
      )
      |> case do
        {:ok, response} = result ->
          Logger.debug("S3 STREAM UPLOAD: Success",
            bucket: bucket,
            key: key
          )

          result

        {:error, error} = result ->
          Logger.error("S3 STREAM UPLOAD ERROR: Failed",
            error: error,
            bucket: bucket,
            key: key
          )

          result
      end
    rescue
      e ->
        Logger.error("S3 STREAM UPLOAD EXCEPTION: #{Exception.message(e)}",
          bucket: bucket,
          key: key,
          stacktrace: Exception.format_stacktrace(__STACKTRACE__)
        )

        {:error, {:s3_upload_exception, Exception.message(e)}}
    catch
      kind, reason ->
        Logger.error("S3 STREAM UPLOAD CAUGHT: #{kind} - #{inspect(reason)}",
          bucket: bucket,
          key: key
        )

        {:error, {:s3_upload_caught, {kind, reason}}}
    end
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

  defp delete_s3_file(image_url) do
    key = extract_s3_key_from_url(image_url)

    if is_nil(key) do
      Logger.error("S3 DELETE ERROR: Invalid S3 URL format", url: image_url)
      {:error, :invalid_url}
    else
      bucket = get_s3_bucket()

      Logger.info("S3 DELETE: Preparing to delete object from S3",
        bucket: bucket,
        key: key,
        url: image_url
      )

      operation = ExAws.S3.delete_object(bucket, key)

      try do
        operation
        |> ExAws.request()
        |> case do
          {:ok, response} ->
            Logger.info("S3 DELETE: Successfully deleted object from S3",
              bucket: bucket,
              key: key,
              response: inspect(response)
            )

            :ok

          {:error, reason} ->
            Logger.error("S3 DELETE ERROR: Failed to delete from S3",
              bucket: bucket,
              key: key,
              reason: reason
            )

            {:error, :delete_failed}
        end
      rescue
        e ->
          Logger.error("S3 DELETE EXCEPTION: Unhandled exception during S3 delete",
            exception: e,
            bucket: bucket,
            key: key
          )

          {:error, :delete_failed}
      end
    end
  end

  defp generate_s3_presigned_url(filename, content_type) do
    key = build_s3_key(filename)
    bucket = get_s3_bucket()
    region = get_s3_region()

    Logger.info("S3 PRESIGNED URL: Generating presigned URL",
      bucket: bucket,
      key: key,
      content_type: content_type,
      region: region,
      original_filename: filename
    )

    try do
      config = ExAws.Config.new(:s3)
      # Create a presigned URL that expires in 60 minutes (3600 seconds)
      case ExAws.S3.presigned_url(config, :put, bucket, key,
             expires_in: 3600,
             content_type: content_type
           ) do
        {:ok, url} ->
          Logger.info("S3 PRESIGNED URL: Successfully generated presigned URL",
            bucket: bucket,
            key: key,
            expires_in: 3600,
            url_length: String.length(url)
          )

          {:ok, url}

        {:error, reason} ->
          Logger.error("S3 PRESIGNED URL ERROR: Failed to generate presigned URL",
            bucket: bucket,
            key: key,
            reason: inspect(reason)
          )

          {:error, :presigned_url_generation_failed}
      end
    rescue
      e ->
        stack = Exception.format_stacktrace(__STACKTRACE__)

        Logger.error("S3 PRESIGNED URL EXCEPTION: Unhandled exception",
          exception: inspect(e),
          bucket: bucket,
          key: key,
          stacktrace: stack
        )

        {:error, :presigned_url_generation_failed}
    end
  end

  defp generate_unique_filename(original_filename) do
    extension = Path.extname(original_filename)
    base_name = Path.basename(original_filename, extension)
    uuid = Ecto.UUID.generate()

    "#{uuid}-#{base_name}#{extension}"
  end

  defp build_s3_key(filename) do
    unique_filename = generate_unique_filename(filename)
    "catches/#{unique_filename}"
  end

  defp extract_s3_key_from_url(image_url) do
    case URI.parse(image_url) do
      %{path: path} ->
        path
        |> String.trim_leading("/")
        |> String.split("?")
        |> List.first()

      _ ->
        nil
    end
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
      :s3 -> get_s3_file_path(image_url)
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

  def get_exaws_config do
    [
      access_key_id:
        Application.get_env(:ex_aws, :access_key_id) || System.get_env("AWS_ACCESS_KEY_ID"),
      secret_access_key:
        Application.get_env(:ex_aws, :secret_access_key) ||
          System.get_env("AWS_SECRET_ACCESS_KEY"),
      region: Application.get_env(:ex_aws, :region) || System.get_env("S3_REGION") || "us-east-2"
    ]
  end

  defp get_s3_file_path(image_url) do
    # For S3, we can't provide a local file path
    # This would typically be used for temporary downloads or to process a file
    # Let's download it to a temp file and return that path
    temp_dir = System.tmp_dir!()
    temp_filename = "#{Path.basename(image_url)}"
    temp_path = Path.join(temp_dir, temp_filename)

    Logger.info("S3 DOWNLOAD: Processing S3 URL to retrieve file",
      url: image_url,
      temp_path: temp_path
    )

    # Parse the URL to get the key
    key = extract_s3_key_from_url(image_url)

    if key do
      bucket = get_s3_bucket()

      Logger.info("S3 DOWNLOAD: Retrieving object from S3",
        bucket: bucket,
        key: key,
        destination: temp_path
      )

      # Download the file from S3 to temp location
      secret_key = Application.get_env(:ex_aws, :secret_access_key)

      try do
        result =
          ExAws.S3.get_object(bucket, key, secret_access_key: secret_key)
          |> ExAws.request(get_exaws_config())
          |> case do
            {:ok, %{body: body, headers: headers}} ->
              Logger.info("S3 DOWNLOAD: Successfully downloaded object from S3",
                bucket: bucket,
                key: key,
                content_length: byte_size(body),
                content_type: get_header_value(headers, "Content-Type")
              )

              File.write(temp_path, body)

            {:error, error} = err ->
              Logger.error("S3 DOWNLOAD ERROR: Failed to download object from S3",
                bucket: bucket,
                key: key,
                error: error
              )

              err
          end

        case result do
          :ok ->
            Logger.info("S3 DOWNLOAD: Successfully saved S3 object to temp file",
              temp_path: temp_path
            )

            {:ok, temp_path}

          {:error, reason} ->
            Logger.error("S3 DOWNLOAD ERROR: Failed to write downloaded file",
              reason: inspect(reason),
              temp_path: temp_path
            )

            {:error, reason}
        end
      rescue
        e ->
          stack = Exception.format_stacktrace(__STACKTRACE__)

          Logger.error("S3 DOWNLOAD EXCEPTION: Unhandled exception during S3 download",
            exception: inspect(e),
            bucket: bucket,
            key: key,
            stacktrace: stack
          )

          {:error, :download_failed}
      end
    else
      {:error, :invalid_url}
    end
  end

  # Helper function to get a header value from a list of headers
  defp get_header_value(headers, key) do
    Enum.find_value(headers, fn
      {^key, value} -> value
      _ -> nil
    end)
  end

  defp get_storage_backend do
    Application.get_env(:hooked_api, :image_storage_backend, :local)
  end

  defp log_memory(stage, metadata) do
    memory = :erlang.memory()

    data =
      Keyword.merge(
        [
          stage: stage,
          total_mb: bytes_to_mb(memory[:total]),
          processes_mb: bytes_to_mb(memory[:processes_used]),
          binary_mb: bytes_to_mb(memory[:binary]),
          ets_mb: bytes_to_mb(memory[:ets])
        ],
        Enum.to_list(metadata)
      )

    Logger.info("MEMORY #{format_memory(data)}")
  end

  defp bytes_to_mb(nil), do: nil
  defp bytes_to_mb(bytes), do: Float.round(bytes / 1_048_576, 2)

  defp format_memory(data) do
    data
    |> Enum.map(fn {key, value} -> "#{key}=#{inspect(value)}" end)
    |> Enum.join(" ")
  end

  defp get_s3_bucket do
    # Get directly from environment variable
    System.get_env("S3_BUCKET")
  end

  defp get_s3_region do
    # Get directly from environment variable
    System.get_env("S3_REGION")
  end

  # Helper function to test AWS credentials
  def try_aws_credentials do
    try do
      # First ensure credentials are properly configured
      HookedApi.Services.AwsCredentials.ensure_credentials_configured()

      bucket = get_s3_bucket()
      Logger.warning("TRY AWS CREDENTIALS RAW: Testing S3 bucket: #{bucket}")

      # Get the secret key directly
      secret_key = Application.get_env(:ex_aws, :secret_access_key)

      # Log current AWS config explicitly
      Logger.warning(
        "TRY AWS CREDENTIALS RAW: Current AWS config - Access Key: #{Application.get_env(:ex_aws, :access_key_id) || "NOT SET"}, Has Secret: #{secret_key != nil && secret_key != ""}, Secret Key Length: #{if(is_binary(secret_key), do: String.length(secret_key), else: 0)}, Region: #{Application.get_env(:ex_aws, :region) || "NOT SET"}"
      )

      # Try a simple operation that doesn't actually do anything
      # Just list a single object to test credentials
      ExAws.S3.list_objects(bucket, max_keys: 1)
      |> ExAws.request(debug_requests: true)
    rescue
      e ->
        error_message = Exception.message(e)
        stack = Exception.format_stacktrace(__STACKTRACE__)
        error_type = inspect(e.__struct__)

        # Log with direct string interpolation
        Logger.error(
          "CREDENTIAL TEST RAW: AWS credentials test failed - #{error_type} - #{error_message}"
        )

        Logger.error("CREDENTIAL TEST STACK RAW: #{stack}")

        # Structured logging
        Logger.error("CREDENTIAL TEST: AWS credentials test failed",
          exception: error_message,
          stacktrace: stack,
          error_type: error_type
        )

        {:error, error_message}
    end
  end
end
