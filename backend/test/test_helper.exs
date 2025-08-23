ExUnit.start()
Ecto.Adapters.SQL.Sandbox.mode(HookedApi.Repo, :manual)

{:ok, _} = Application.ensure_all_started(:ex_machina)

# Disable Oban in tests to prevent background job interference
Application.put_env(:hooked_api, Oban, testing: :manual)

# Ensure upload directories exist for tests
File.mkdir_p!("priv/static/uploads/catches")
