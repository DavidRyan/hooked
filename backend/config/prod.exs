import Config

# Production-specific configuration
config :hooked_api, HookedApi.Endpoint,
  cache_static_manifest: "priv/static/cache_manifest.json",
  server: true

# Do not print debug messages in production
config :logger, level: :info

# Runtime production configuration (DATABASE_URL, SECRET_KEY_BASE, etc.)
# is handled in config/runtime.exs
