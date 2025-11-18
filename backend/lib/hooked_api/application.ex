defmodule HookedApi.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    # Apply patches to ExAws to fix nil secret key issues
    ExAwsPatches.SignaturesPatch.apply_patch()

    children = [
      HookedApi.Repo,
      {Oban, Application.fetch_env!(:hooked_api, Oban)},
      {Phoenix.PubSub, name: HookedApi.PubSub},
      HookedApi.EnrichmentHandler,
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
