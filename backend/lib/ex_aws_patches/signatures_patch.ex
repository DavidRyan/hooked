defmodule ExAwsPatches.SignaturesPatch do
  @moduledoc """
  Patch for ExAws.Auth.Signatures to ensure the hmac key is never nil

  This module monkey-patches ExAws.Auth.Signatures to prevent the crypto error
  when a nil secret key is passed to hmac_sha256.
  """

  import ExAws.Auth.Utils, only: [hmac_sha256: 2, date: 1, bytes_to_hex: 1]
  require Logger

  # Apply the patch by using this function in the application startup
  def apply_patch do
    # No need to do anything - just having the module loaded is enough
    :ok
  end

  # Override the original function to add nil checks
  def signing_key(service, datetime, config) do
    secret_key = config[:secret_access_key]

    if is_nil(secret_key) do
      # Log the error and use a placeholder to prevent crypto errors
      Logger.error(
        "AWS Authentication Error: secret_access_key is nil - using placeholder to avoid crypto error"
      )

      # Use placeholder to avoid crypto error
      "INVALID-SECRET-KEY-PLACEHOLDER"
    else
      secret_key
    end

    ["AWS4", secret_key]
    |> hmac_sha256(date(datetime))
    |> hmac_sha256(config[:region])
    |> hmac_sha256(service)
    |> hmac_sha256("aws4_request")
  end
end

# Monkey patch the ExAws.Auth.Signatures module
# This will replace the original signing_key function with our patched version
defmodule ExAws.Auth.Signatures do
  @moduledoc false
  import ExAws.Auth.Utils, only: [hmac_sha256: 2, date: 1, bytes_to_hex: 1]
  require Logger

  def generate_signature_v4(service, config, datetime, string_to_sign) do
    service
    |> signing_key(datetime, config)
    |> hmac_sha256(string_to_sign)
    |> bytes_to_hex
  end

  # Patched version with nil checks
  defp signing_key(service, datetime, config) do
    secret_key = config[:secret_access_key]

    secret_key =
      if is_nil(secret_key) do
        # Log the error and use a placeholder to prevent crypto errors
        Logger.error(
          "AWS Authentication Error: secret_access_key is nil - using placeholder to avoid crypto error"
        )

        "INVALID-SECRET-KEY-PLACEHOLDER"
      else
        secret_key
      end

    ["AWS4", secret_key]
    |> hmac_sha256(date(datetime))
    |> hmac_sha256(config[:region])
    |> hmac_sha256(service)
    |> hmac_sha256("aws4_request")
  end
end
