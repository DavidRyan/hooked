# Simple test script to verify S3 setup
# Run this with: mix run test/test_s3.exs
# 
# If you encounter issues with AWS credentials, try running with:
# AWS_ACCESS_KEY_ID=your_key AWS_SECRET_ACCESS_KEY=your_secret S3_BUCKET=your_bucket S3_REGION=your_region mix run test/test_s3.exs

defmodule TestS3 do
  require Logger

  def run do
    # Ensure environment variables are loaded
    load_env()

    # Print current config
    print_config()

    # Test S3 connection
    test_s3_connection()

    # Test upload
    test_upload()

    # Done
    IO.puts("\n✅ S3 testing complete!")
  end

  defp load_env do
    IO.puts("\n📁 Loading environment variables...")

    env_file = ".env"

    if File.exists?(env_file) do
      env_file
      |> File.read!()
      |> String.split("\n")
      |> Enum.each(fn line ->
        case String.split(line, "=", parts: 2) do
          [key, value] when key != "" and value != "" ->
            System.put_env(String.trim(key), String.trim(value))

          _ ->
            :ok
        end
      end)

      IO.puts("   Environment variables loaded")
    else
      IO.puts("   ❌ .env file not found!")
    end
  end

  defp print_config do
    IO.puts("\n📊 Current S3 configuration:")
    IO.puts("   Storage backend: #{Application.get_env(:hooked_api, :image_storage_backend)}")

    # Retrieve bucket and credentials from environment directly
    bucket = System.get_env("S3_BUCKET")
    region = System.get_env("S3_REGION")
    access_key = System.get_env("AWS_ACCESS_KEY_ID") || "not set"
    secret_key = System.get_env("AWS_SECRET_ACCESS_KEY") || "not set"

    # Update application config with values from environment
    Application.put_env(:hooked_api, :s3_bucket, bucket)
    Application.put_env(:hooked_api, :s3_region, region)

    # Update ExAws config directly with sanitization for secret key
    # Handle empty or nil secret keys
    sanitized_secret_key =
      if is_nil(secret_key) || secret_key == "" do
        IO.puts("   ⚠️ AWS Secret Key is empty - using placeholder to avoid crypto errors")
        "INVALID-SECRET-KEY-PLACEHOLDER"
      else
        # Remove whitespace
        sanitized = String.trim(secret_key)

        # Handle URL encoded characters
        if String.contains?(sanitized, "%") do
          IO.puts("   ⚠️ AWS Secret Key contains URL encoded characters - attempting to decode")

          try do
            URI.decode(sanitized)
          rescue
            _ ->
              IO.puts("   ⚠️ URL decoding failed - using the key as is")
              sanitized
          end
        else
          sanitized
        end
      end

    Application.put_env(:ex_aws, :access_key_id, access_key)
    Application.put_env(:ex_aws, :secret_access_key, sanitized_secret_key)
    Application.put_env(:ex_aws, :region, region)

    IO.puts("   S3 bucket: #{bucket}")
    IO.puts("   S3 region: #{region}")

    # Mask credentials for display

    masked_access_key =
      if String.length(access_key) > 6 do
        String.slice(access_key, 0..3) <> "..." <> String.slice(access_key, -2..-1)
      else
        "❌ invalid key"
      end

    masked_secret_key =
      if String.length(secret_key) > 6 do
        String.slice(secret_key, 0..3) <> "..." <> String.slice(secret_key, -2..-1)
      else
        "❌ invalid key"
      end

    IO.puts("   AWS access key: #{masked_access_key}")
    IO.puts("   AWS secret key: #{masked_secret_key}")
  end

  defp test_s3_connection do
    IO.puts("\n🔄 Testing S3 connection...")

    bucket = Application.get_env(:hooked_api, :s3_bucket)

    case ExAws.S3.list_objects(bucket, max_keys: 1) |> ExAws.request() do
      {:ok, _response} ->
        IO.puts("   ✅ Successfully connected to S3 bucket: #{bucket}")

      {:error, error} ->
        IO.puts("   ❌ Failed to connect to S3: #{inspect(error)}")
    end
  end

  defp test_upload do
    IO.puts("\n📤 Testing image upload...")

    # Create a test file
    test_file_path = "/tmp/test_upload.txt"
    File.write!(test_file_path, "Test file for S3 upload - #{DateTime.utc_now()}")

    # Create mock upload struct (similar to Plug.Upload)
    upload = %{
      filename: "test_upload.txt",
      path: test_file_path,
      content_type: "text/plain"
    }

    # Manually call the important functions
    IO.puts("   Uploading test file...")

    bucket = Application.get_env(:hooked_api, :s3_bucket)
    test_key = "test/test_#{System.os_time()}.txt"

    result =
      test_file_path
      |> File.read!()
      |> upload_to_s3(bucket, test_key, "text/plain")

    case result do
      {:ok, _response} ->
        s3_url =
          "https://#{bucket}.s3.#{Application.get_env(:hooked_api, :s3_region)}.amazonaws.com/#{test_key}"

        IO.puts("   ✅ Upload successful!")
        IO.puts("   File URL: #{s3_url}")

        # Test download
        IO.puts("\n📥 Testing download...")

        download_result =
          ExAws.S3.get_object(bucket, test_key)
          |> ExAws.request()

        case download_result do
          {:ok, %{body: body}} ->
            IO.puts("   ✅ Download successful!")
            IO.puts("   File content: #{String.slice(body, 0..50)}...")

          {:error, reason} ->
            IO.puts("   ❌ Download failed: #{inspect(reason)}")
        end

        # Test delete
        IO.puts("\n🗑️  Testing delete...")

        delete_result =
          ExAws.S3.delete_object(bucket, test_key)
          |> ExAws.request()

        case delete_result do
          {:ok, _} ->
            IO.puts("   ✅ Delete successful!")

          {:error, reason} ->
            IO.puts("   ❌ Delete failed: #{inspect(reason)}")
        end

      {:error, reason} ->
        IO.puts("   ❌ Upload failed: #{inspect(reason)}")
    end
  end

  defp upload_to_s3(file_binary, bucket, key, content_type) do
    try do
      ExAws.S3.put_object(bucket, key, file_binary, [
        {:content_type, content_type}
        # Remove ACL setting as the bucket doesn't support ACLs
      ])
      |> ExAws.request()
    rescue
      e ->
        Logger.error("S3 upload error: #{inspect(e)}")
        {:error, :s3_error}
    end
  end
end

# Run the test
TestS3.run()
