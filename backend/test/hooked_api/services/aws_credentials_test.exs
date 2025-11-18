defmodule HookedApi.Services.AwsCredentialsTest do
  use HookedApi.DataCase, async: false
  alias HookedApi.Services.AwsCredentials

  describe "AWS credentials configuration" do
    test "handles empty AWS secret key" do
      # Save original env vars
      original_key = System.get_env("AWS_ACCESS_KEY_ID")
      original_secret = System.get_env("AWS_SECRET_ACCESS_KEY")
      original_region = System.get_env("S3_REGION")

      # Set access key but leave secret key empty
      System.put_env("AWS_ACCESS_KEY_ID", "AKIAYCENXFQJROJ6JSPA")
      System.put_env("AWS_SECRET_ACCESS_KEY", "")
      System.put_env("S3_REGION", "us-east-2")

      # Ensure credentials are configured
      result = AwsCredentials.ensure_credentials_configured()

      # Verify secret key was sanitized and doesn't cause a crypto error
      assert {:error, :missing_aws_configuration} = result

      # Config should still be set with a placeholder
      assert Application.get_env(:ex_aws, :access_key_id) == "AKIAYCENXFQJROJ6JSPA"
      assert Application.get_env(:ex_aws, :secret_access_key) == "INVALID-SECRET-KEY-PLACEHOLDER"
      assert Application.get_env(:ex_aws, :region) == "us-east-2"

      # Restore original env vars
      if original_key,
        do: System.put_env("AWS_ACCESS_KEY_ID", original_key),
        else: System.delete_env("AWS_ACCESS_KEY_ID")

      if original_secret,
        do: System.put_env("AWS_SECRET_ACCESS_KEY", original_secret),
        else: System.delete_env("AWS_SECRET_ACCESS_KEY")

      if original_region,
        do: System.put_env("S3_REGION", original_region),
        else: System.delete_env("S3_REGION")
    end

    test "handles AWS secret key with whitespace" do
      # Save original env vars
      original_key = System.get_env("AWS_ACCESS_KEY_ID")
      original_secret = System.get_env("AWS_SECRET_ACCESS_KEY")
      original_region = System.get_env("S3_REGION")

      # Set access key but make secret key have whitespace
      System.put_env("AWS_ACCESS_KEY_ID", "AKIAYCENXFQJROJ6JSPA")
      System.put_env("AWS_SECRET_ACCESS_KEY", " SecretWithWhitespace  ")
      System.put_env("S3_REGION", "us-east-2")

      # Ensure credentials are configured
      result = AwsCredentials.ensure_credentials_configured()

      # Config should be sanitized with whitespace removed
      assert Application.get_env(:ex_aws, :access_key_id) == "AKIAYCENXFQJROJ6JSPA"
      assert Application.get_env(:ex_aws, :secret_access_key) == "SecretWithWhitespace"
      assert Application.get_env(:ex_aws, :region) == "us-east-2"

      # Restore original env vars
      if original_key,
        do: System.put_env("AWS_ACCESS_KEY_ID", original_key),
        else: System.delete_env("AWS_ACCESS_KEY_ID")

      if original_secret,
        do: System.put_env("AWS_SECRET_ACCESS_KEY", original_secret),
        else: System.delete_env("AWS_SECRET_ACCESS_KEY")

      if original_region,
        do: System.put_env("S3_REGION", original_region),
        else: System.delete_env("S3_REGION")
    end

    test "successfully configures valid AWS credentials" do
      # Save original env vars
      original_key = System.get_env("AWS_ACCESS_KEY_ID")
      original_secret = System.get_env("AWS_SECRET_ACCESS_KEY")
      original_region = System.get_env("S3_REGION")

      # Set all required env vars
      System.put_env("AWS_ACCESS_KEY_ID", "AKIAYCENXFQJROJ6JSPA")
      System.put_env("AWS_SECRET_ACCESS_KEY", "ValidSecretKey")
      System.put_env("S3_REGION", "us-east-2")

      # Ensure credentials are configured
      result = AwsCredentials.ensure_credentials_configured()

      # Should succeed
      assert {:ok, %{region: "us-east-2"}} = result

      # Verify config is set correctly
      assert Application.get_env(:ex_aws, :access_key_id) == "AKIAYCENXFQJROJ6JSPA"
      assert Application.get_env(:ex_aws, :secret_access_key) == "ValidSecretKey"
      assert Application.get_env(:ex_aws, :region) == "us-east-2"

      # Restore original env vars
      if original_key,
        do: System.put_env("AWS_ACCESS_KEY_ID", original_key),
        else: System.delete_env("AWS_ACCESS_KEY_ID")

      if original_secret,
        do: System.put_env("AWS_SECRET_ACCESS_KEY", original_secret),
        else: System.delete_env("AWS_SECRET_ACCESS_KEY")

      if original_region,
        do: System.put_env("S3_REGION", original_region),
        else: System.delete_env("S3_REGION")
    end
  end
end
