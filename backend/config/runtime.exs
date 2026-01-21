import Config

# Runtime configuration is executed after compilation and before the
# application starts. This is the recommended place for loading secrets
# and environment-specific configuration.

# Load .env file in dev/test environments
if config_env() in [:dev, :test] do
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
  end
end

# S3/AWS Configuration
config :hooked_api,
  s3_bucket: System.get_env("S3_BUCKET"),
  s3_region: System.get_env("S3_REGION")

config :ex_aws,
  access_key_id: System.get_env("AWS_ACCESS_KEY_ID"),
  secret_access_key: System.get_env("AWS_SECRET_ACCESS_KEY"),
  json_codec: Jason

config :ex_aws, :s3,
  region: System.get_env("S3_REGION"),
  scheme: "https://"

# Species identification provider API configurations
config :hooked_api,
  inaturalist_access_token: System.get_env("INATURALIST_ACCESS_TOKEN"),
  google_vision_access_token: System.get_env("GOOGLE_VISION_ACCESS_TOKEN")

# OpenWeatherMap API configuration
config :hooked_api,
  openweather_api_key: System.get_env("OPENWEATHER_API_KEY")

# Mapbox API configuration
config :hooked_api,
  mapbox_api_key: System.get_env("MAPBOX_ACCESS_TOKEN")

# AI Provider configuration
config :hooked_api,
  openai_api_key: System.get_env("OPENAI_API_KEY")

# JWT Configuration
if config_env() != :test do
  config :hooked_api,
         :jwt_secret,
         System.get_env("JWT_SECRET") || raise("JWT_SECRET environment variable is not set")
end

# Endpoint secret key base (for dev)
if config_env() == :dev do
  config :hooked_api, HookedApi.Endpoint,
    secret_key_base:
      System.get_env("SECRET_KEY_BASE") ||
        raise("SECRET_KEY_BASE environment variable is not set")
end
