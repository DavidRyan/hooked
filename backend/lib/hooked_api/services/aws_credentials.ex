defmodule HookedApi.Services.AwsCredentials do
  @moduledoc """
  Runtime module for ensuring AWS credentials are properly configured for S3 operations.
  This module ensures credentials are available from environment variables at runtime.
  """

  require Logger

  @doc """
  Ensures AWS credentials are properly configured for ExAws.
  Call this before making any S3 requests to ensure credentials are set.
  """
  def ensure_credentials_configured do
    # Get credentials from environment
    access_key_id = System.get_env("AWS_ACCESS_KEY_ID")
    secret_access_key = System.get_env("AWS_SECRET_ACCESS_KEY")
    region = System.get_env("S3_REGION") || Application.get_env(:hooked_api, :s3_region)

    # Log the credential status (masking sensitive data)
    Logger.debug("AWS Credentials Check",
      access_key_id: mask_key(access_key_id),
      has_secret_key: secret_access_key != nil && secret_access_key != "",
      region: region
    )

    # Sanitize the secret key to handle nil or empty values
    sanitized_secret_key =
      if is_nil(secret_access_key) || secret_access_key == "" do
        Logger.warning(
          "AWS Secret Key is nil or empty - using non-empty placeholder to avoid crypto errors"
        )

        # Use a non-empty placeholder that won't cause crypto errors
        "INVALID-SECRET-KEY-PLACEHOLDER"
      else
        # Remove any whitespace that could cause crypto errors
        sanitized = String.trim(secret_access_key)

        # Handle special characters like URL encoding if present
        sanitized =
          if String.contains?(sanitized, "%") do
            Logger.warning(
              "AWS Secret Key contains URL encoded characters - attempting to decode"
            )

            try do
              URI.decode(sanitized)
            rescue
              _ ->
                Logger.warning("URL decoding failed - using the key as is")
                sanitized
            end
          else
            sanitized
          end

        # Log sanitization info
        original_length = String.length(secret_access_key)
        sanitized_length = String.length(sanitized)

        if original_length != sanitized_length do
          Logger.warning(
            "AWS Secret Key contained #{original_length - sanitized_length} whitespace characters that were removed"
          )
        end

        sanitized
      end

    # Log credential info
    Logger.warning("AWS Credentials: Using sanitized environment variables")

    # Pass environment variables to ExAws config with sanitization
    Application.put_env(:ex_aws, :access_key_id, access_key_id)
    # CRITICAL: Ensure secret_access_key is never nil to avoid crypto.mac error
    sanitized_secret_key =
      if is_nil(sanitized_secret_key),
        do: "INVALID-SECRET-KEY-PLACEHOLDER",
        else: sanitized_secret_key

    Application.put_env(:ex_aws, :secret_access_key, sanitized_secret_key)
    Application.put_env(:ex_aws, :region, region)

    # Double check the config is properly set
    current_secret = Application.get_env(:ex_aws, :secret_access_key)

    if is_nil(current_secret) do
      Logger.error("AWS secret key is still nil after setting! This will cause crypto errors.")
      # Force a value to prevent crypto errors
      Application.put_env(:ex_aws, :secret_access_key, "INVALID-SECRET-KEY-PLACEHOLDER")
    end


    # Validate configuration
    missing_config = []

    missing_config =
      if is_nil(access_key_id) or access_key_id == "",
        do: missing_config ++ ["AWS_ACCESS_KEY_ID"],
        else: missing_config

    missing_config =
      if is_nil(secret_access_key) or secret_access_key == "",
        do: missing_config ++ ["AWS_SECRET_ACCESS_KEY"],
        else: missing_config

    missing_config =
      if is_nil(region) or region == "", do: missing_config ++ ["S3_REGION"], else: missing_config

    if Enum.empty?(missing_config) do
      {:ok, %{region: region}}
    else
      Logger.error("AWS Credentials Error: Missing required AWS configuration",
        missing_env_vars: missing_config
      )

      {:error, :missing_aws_configuration}
    end
  end

  @doc """
  Tests the current AWS credentials by making a simple S3 request.
  Use this to diagnose connectivity or credential issues.
  """
  def test_credentials do
    # Ensure credentials are configured
    ensure_credentials_configured()

    # Get the S3 bucket directly from environment
    bucket = System.get_env("S3_BUCKET")

    if is_nil(bucket) || bucket == "" do
      {:error, "S3 bucket not configured"}
    else
      try do
        # Try a simple list operation
        result =
          ExAws.S3.list_objects(bucket, max_keys: 1)
          |> ExAws.request(debug_requests: true)

        case result do
          {:ok, _} ->
            {:ok, "Successfully connected to S3 bucket: #{bucket}"}

          {:error, error} ->
            {:error, "S3 connection failed: #{inspect(error)}"}
        end
      rescue
        e ->
          {:error, "AWS credentials test failed: #{Exception.message(e)}"}
      end
    end
  end

  @doc """
  Gets the current AWS configuration.
  """
  def get_aws_config do
    %{
      access_key_id:
        Application.get_env(:ex_aws, :access_key_id) || System.get_env("AWS_ACCESS_KEY_ID"),
      secret_access_key:
        mask_secret(
          Application.get_env(:ex_aws, :secret_access_key) ||
            System.get_env("AWS_SECRET_ACCESS_KEY")
        ),
      region: Application.get_env(:ex_aws, :region) || System.get_env("S3_REGION")
    }
  end

  # Masks the secret key for logging purposes
  defp mask_secret(nil), do: nil
  defp mask_secret(""), do: ""

  defp mask_secret(secret) when is_binary(secret) do
    case String.length(secret) do
      len when len > 8 ->
        first = String.slice(secret, 0..3)
        last = String.slice(secret, -4..-1)
        "#{first}...#{last}"

      _ ->
        "***"
    end
  end

  defp mask_secret(_), do: "***"

  # Masks the access key for logging purposes
  defp mask_key(nil), do: nil
  defp mask_key(""), do: ""

  defp mask_key(key) when is_binary(key) do
    case String.length(key) do
      len when len > 5 ->
        first = String.slice(key, 0..2)
        last = String.slice(key, -2..-1)
        "#{first}...#{last}"

      _ ->
        "***"
    end
  end

  defp mask_key(_), do: "***"
end
