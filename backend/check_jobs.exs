Mix.install([
  {:ecto_sql, "~> 3.0"},
  {:postgrex, "~> 0.17"},
  {:jason, "~> 1.4"}
])

# Configure the repo
defmodule TempRepo do
  use Ecto.Repo,
    otp_app: :temp,
    adapter: Ecto.Adapters.Postgres
end

# Start the repo
{:ok, _} =
  TempRepo.start_link(
    database: "hooked_api_dev",
    username: System.get_env("USER"),
    hostname: "localhost"
  )

# Query jobs with errors
jobs =
  TempRepo.query!(
    "SELECT id, worker, queue, state, attempt, max_attempts, errors FROM oban_jobs ORDER BY inserted_at DESC LIMIT 5;"
  )

IO.puts("Recent Oban jobs:")
IO.inspect(jobs.rows, pretty: true)
