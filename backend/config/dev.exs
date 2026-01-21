import Config

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

# Runtime configuration (JWT, API keys, S3) is loaded in config/runtime.exs
