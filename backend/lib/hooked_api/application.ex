defmodule HookedApi.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      HookedApi.Repo,
      {Phoenix.PubSub, name: HookedApi.PubSub},
      HookedApi.Catches,
      HookedApi.Endpoint
    ]

    opts = [strategy: :one_for_one, name: HookedApi.Supervisor]
    Supervisor.start_link(children, opts)
  end

  @impl true
  def config_change(changed, _new, removed) do
    HookedApi.Endpoint.config_change(changed, removed)
    :ok
  end
end
