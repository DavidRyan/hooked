defmodule ExAwsPatches.SignaturesPatch do
  @moduledoc """
  Helper module to validate AWS credentials before making ExAws calls.

  Instead of monkey-patching ExAws.Auth.Signatures (which causes duplicate module
  errors in releases), this module provides validation functions to check
  credentials before making AWS API calls.
  """

  require Logger

  @doc """
  Called at application startup. Validates that AWS credentials are configured.
  """
  def apply_patch do
    # Log a warning if AWS credentials are not configured
    validate_credentials()
    :ok
  end

  @doc """
  Validates that AWS credentials are present in the configuration.
  Returns {:ok, config} if valid, {:error, reason} if not.
  """
  def validate_credentials do
    config = ExAws.Config.new(:s3)

    cond do
      is_nil(config[:access_key_id]) ->
        Logger.warning("AWS credentials not configured: access_key_id is nil")
        {:error, :missing_access_key}

      is_nil(config[:secret_access_key]) ->
        Logger.warning("AWS credentials not configured: secret_access_key is nil")
        {:error, :missing_secret_key}

      true ->
        {:ok, config}
    end
  end

  @doc """
  Checks if AWS credentials are configured and valid.
  """
  def credentials_configured? do
    case validate_credentials() do
      {:ok, _} -> true
      {:error, _} -> false
    end
  end
end
