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

# === Production configuration ===
if config_env() == :prod do
  database_url =
    System.get_env("DATABASE_URL") ||
      raise """
      environment variable DATABASE_URL is missing.
      For example: ecto://USER:PASS@HOST/DATABASE
      """

  maybe_ipv6 = if System.get_env("ECTO_IPV6") in ~w(true 1), do: [:inet6], else: []

  config :hooked_api, HookedApi.Repo,
    url: database_url,
    pool_size: String.to_integer(System.get_env("POOL_SIZE") || "10"),
    socket_options: maybe_ipv6,
    ssl: true,
    ssl_opts: [verify: :verify_none]

  secret_key_base =
    System.get_env("SECRET_KEY_BASE") ||
      raise """
      environment variable SECRET_KEY_BASE is missing.
      You can generate one by calling: mix phx.gen.secret
      """

  host = System.get_env("PHX_HOST") || "hooked-backend.fly.dev"
  port = String.to_integer(System.get_env("PORT") || "4000")

  config :hooked_api, HookedApi.Endpoint,
    url: [host: host, port: 443, scheme: "https"],
    http: [
      ip: {0, 0, 0, 0, 0, 0, 0, 0},
      port: port
    ],
    secret_key_base: secret_key_base
end
