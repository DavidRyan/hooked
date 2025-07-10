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

#config :hooked_api, Oban,
#  engine: Oban.Engines.Basic,
#  queues: [default: 10, enrichment: 5],
#  repo: HookedApi.Repo

config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

config :phoenix, :json_library, Jason

