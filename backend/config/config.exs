import Config

config :hooked_api,
  ecto_repos: [HookedApi.Repo],
  generators: [timestamp_type: :utc_datetime, binary_id: true]

config :hooked_api, HookedApi.Endpoint,
  url: [host: "localhost"],
  adapter: Bandit.PhoenixAdapter,
  render_errors: [
    formats: [json: HookedApiWeb.ErrorJSON],
    layout: false
  ],
  pubsub_server: HookedApi.PubSub,
  live_view: [signing_salt: "your-signing-salt"]

config :hooked_api, Oban,
  engine: Oban.Engines.Basic,
  queues: [catch_enrichment: 5],
  repo: HookedApi.Repo,
  plugins: [
    {Oban.Plugins.Lifeline, rescue_after: :timer.minutes(30)},
    {Oban.Plugins.Pruner, max_age: 300}
  ]

config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id, :oban_job]

# Enable Oban logging
config :logger,
  level: :info

# Log Oban job failures
config :oban, :log, false

config :phoenix, :json_library, Jason

# Image storage configuration
config :hooked_api,
  image_storage_backend: :local,
  image_upload_dir: "priv/static/uploads/catches",
  max_image_size: 10_000_000,
  enrichers: [
    HookedApi.Enrichers.ExifEnricher,
    HookedApi.Enrichers.GeoEnricher,
    HookedApi.Enrichers.WeatherEnricher,
    HookedApi.Enrichers.Species.SpeciesEnricher
  ]

# iNaturalist API configuration
config :hooked_api,
  inaturalist_access_token: System.get_env("INATURALIST_ACCESS_TOKEN")

# OpenWeatherMap API configuration
config :hooked_api,
  openweather_api_key: System.get_env("OPENWEATHER_API_KEY")

# Tesla configuration
config :tesla, disable_deprecated_builder_warning: true

# JWT configuration - secret now loaded at runtime for security

# Hammer rate limiting configuration
config :hammer,
  backend: {Hammer.Backend.ETS, [expiry_ms: 60_000 * 60 * 4, cleanup_interval_ms: 60_000 * 10]}

# Import environment specific config
import_config "#{config_env()}.exs"
