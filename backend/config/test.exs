import Config

# Configure your database
#
# The MIX_TEST_PARTITION environment variable can be used
# to provide built-in test partitioning in CI environment.
# Run `mix help test` for more information.
config :hooked_api, HookedApi.Repo,
  username: "postgres",
  password: "postgres",
  hostname: "localhost",
  database: "hooked_api_test#{System.get_env("MIX_TEST_PARTITION")}",
  pool: Ecto.Adapters.SQL.Sandbox,
  pool_size: System.schedulers_online() * 2

# We don't run a server during test. If one is required,
# you can enable the server option below.
config :hooked_api, HookedApiWeb.Endpoint,
  http: [ip: {127, 0, 0, 1}, port: 4002],
  secret_key_base: "test_secret_key_base_that_is_at_least_64_characters_long_for_testing",
  server: false

# Print only warnings and errors during test
config :logger, level: :warning

# Initialize plugs at runtime for faster test compilation
config :phoenix, :plug_init_mode, :runtime

# JWT Configuration for testing
System.put_env(
  "JWT_SECRET",
  "test_jwt_secret_that_is_at_least_32_characters_long_for_testing_purposes"
)

# Disable Oban in tests
config :hooked_api, Oban, testing: :manual

# Disable rate limiting in tests
config :hooked_api, HookedApiWeb.Plugs.RateLimitPlug,
  enabled: false,
  queues: false,
  plugins: false


# Disable rate limiting in tests
config :hooked_api, HookedApiWeb.Plugs.RateLimitPlug, enabled: false
