defmodule HookedApi.Services.SkunkEnrichmentService do
  @moduledoc """
  Service for enqueueing skunk enrichment jobs. Runs geo + weather enrichment
  on user skunks that have GPS coordinates.
  """
  require Logger

  alias HookedApi.Workers.SkunkEnrichmentWorker

  def enqueue_enrichment(%{id: skunk_id} = user_skunk) do
    Logger.info("SkunkEnrichmentService: Enqueueing enrichment for skunk #{skunk_id}")

    skunk_data =
      user_skunk
      |> Map.from_struct()
      |> Map.drop([:__meta__, :user])
      |> convert_values()

    job_args = %{
      skunk_id: skunk_id,
      user_skunk: skunk_data
    }

    job_args
    |> SkunkEnrichmentWorker.new()
    |> Oban.insert()
  end

  def get_configured_enrichers do
    Application.get_env(:hooked_api, :skunk_enrichers, [
      HookedApi.Enrichers.SkunkGeoEnricher,
      HookedApi.Enrichers.SkunkWeatherEnricher
    ])
  end

  defp convert_values(map) do
    map
    |> Enum.map(fn
      {key, %NaiveDateTime{} = dt} -> {key, NaiveDateTime.to_iso8601(dt)}
      {key, %DateTime{} = dt} -> {key, DateTime.to_iso8601(dt)}
      {key, value} -> {key, value}
    end)
    |> Map.new()
  end
end
