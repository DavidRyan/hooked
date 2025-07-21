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
  secret_key_base: "your-secret-key-base-here",
  watchers: []

config :hooked_api, :dev_routes, true

config :logger, :console, format: "[$level] $message\n"

config :phoenix, :stacktrace_depth, 20

config :phoenix, :plug_init_mode, :runtime

# JWT Configuration
config :hooked_api,
       :jwt_secret,
       "lSecx5L/Nqhs1Lp4XezmvOA+xgpTBB0Hn9W4aT7EnRXfPswG/8wdvql9uwQROsze"
