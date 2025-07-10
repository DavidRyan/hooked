defmodule HookedApi.Repo do
  use Ecto.Repo,
    otp_app: :hooked_api,
    adapter: Ecto.Adapters.Postgres
end