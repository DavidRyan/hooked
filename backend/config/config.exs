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
  repo: HookedApi.Repo

config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

config :phoenix, :json_library, Jason

# Image storage configuration
config :hooked_api,
  image_storage_backend: :local,
  image_upload_dir: "priv/static/uploads/catches",
  max_image_size: 10_000_000,
  enrichers: [
    HookedApi.Enrichers.GeoEnricher,
    HookedApi.Enrichers.WeatherEnricher,
    HookedApi.Enrichers.Species.SpeciesEnricher
  ]

# iNaturalist API configuration
config :hooked_api,
  inaturalist_access_token: "YOUR_INATURALIST_ACCESS_TOKEN_HERE"

# OpenWeatherMap API configuration
config :hooked_api,
  openweather_api_key: System.get_env("OPENWEATHER_API_KEY")

# Tesla configuration
config :tesla, disable_deprecated_builder_warning: true

# Import environment specific config
import_config "#{config_env()}.exs"
