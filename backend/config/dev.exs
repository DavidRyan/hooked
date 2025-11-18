import Config

# Simple .env file loader
defmodule EnvLoader do
  def load_env do
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
end

EnvLoader.load_env()

config :hooked_api, HookedApi.Repo,
  username: "postgres",
  password: "postgres",
  hostname: "localhost",
  database: "hooked_api_dev",
  stacktrace: true,
  show_sensitive_data_on_connection_error: true,
  pool_size: 10

config :hooked_api, HookedApi.Endpoint,
  http: [ip: {127, 0, 0, 1}, port: 4000],
  check_origin: false,
  code_reloader: true,
  debug_errors: true,
  secret_key_base:
    System.get_env("SECRET_KEY_BASE") || raise("SECRET_KEY_BASE environment variable is not set"),
  watchers: []

config :hooked_api, :dev_routes, true

config :logger, :console,
  format: "[$level] $message $metadata\n",
  level: :debug,
  metadata: [
    :access_key_id,
    :secret_key_set,
    :region,
    :s3_bucket,
    :bucket,
    :key,
    :error,
    :exception,
    :stacktrace
  ]

config :phoenix, :stacktrace_depth, 20

config :phoenix, :plug_init_mode, :runtime

# JWT Configuration - now loaded from environment
config :hooked_api,
       :jwt_secret,
       System.get_env("JWT_SECRET") || raise("JWT_SECRET environment variable is not set")

# OpenWeatherMap API Configuration
config :hooked_api, :openweather_api_key, System.get_env("OPENWEATHER_API_KEY")

# S3 configuration
config :ex_aws,
  access_key_id: System.get_env("AWS_ACCESS_KEY_ID"),
  secret_access_key: System.get_env("AWS_SECRET_ACCESS_KEY"),
  region: System.get_env("S3_REGION")

config :hooked_api,
  s3_bucket: System.get_env("S3_BUCKET")
