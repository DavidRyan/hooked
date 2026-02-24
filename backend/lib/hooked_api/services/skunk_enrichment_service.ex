defmodule HookedApi.Services.SkunkEnrichmentService do
  @moduledoc """
  Service for enqueueing skunk enrichment jobs. Runs geo + weather enrichment
  on user skunks that have GPS coordinates.
  """
  require Logger

  alias HookedApi.Workers.SkunkEnrichmentWorker

  def enqueue_enrichment(%{id: skunk_id} = user_skunk) do
    Logger.info("SkunkEnrichmentService: Enqueueing enrichment for skunk #{skunk_id}")

    Logger.debug(
      "SkunkEnrichmentService: Incoming skunk state " <>
        "lat=#{inspect(user_skunk.latitude)}, " <>
        "lng=#{inspect(user_skunk.longitude)}, weather_present=#{not is_nil(user_skunk.weather_data)}"
    )

    skunk_data =
      user_skunk
      |> Map.from_struct()
      |> Map.drop([:__meta__, :user])
      |> convert_values()

    job_args = %{
      skunk_id: skunk_id,
      user_skunk: skunk_data
    }

    Logger.debug("SkunkEnrichmentService: Prepared job args keys=#{inspect(Map.keys(job_args))}")

    result =
      job_args
      |> SkunkEnrichmentWorker.new()
      |> Oban.insert()

    case result do
      {:ok, job} ->
        Logger.info(
          "SkunkEnrichmentService: Enqueued skunk enrichment job #{job.id} for skunk #{skunk_id}"
        )

      {:error, reason} ->
        Logger.error(
          "SkunkEnrichmentService: Failed to enqueue skunk enrichment for #{skunk_id}: #{inspect(reason)}"
        )
    end

    result
  end

  def get_configured_enrichers do
    Logger.debug("SkunkEnrichmentService: Loading configured skunk enrichers")

    enrichers =
      Application.get_env(:hooked_api, :skunk_enrichers, [
        HookedApi.Enrichers.SkunkGeoEnricher,
        HookedApi.Enrichers.SkunkWeatherEnricher
      ])

    Logger.debug("SkunkEnrichmentService: Skunk enricher modules: #{inspect(enrichers)}")

    enrichers
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
