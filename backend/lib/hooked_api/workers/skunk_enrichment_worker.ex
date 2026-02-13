defmodule HookedApi.Workers.SkunkEnrichmentWorker do
  @moduledoc """
  Oban worker that enriches skunk records with geo (reverse geocoding) and weather data.
  Simpler than CatchEnrichmentWorker — no image/EXIF processing.
  """
  use Oban.Worker, queue: :skunk_enrichment, max_attempts: 3

  require Logger

  alias HookedApi.Services.SkunkEnrichmentService
  alias HookedApi.PubSubTopics
  alias HookedApi.Skunks.UserSkunk

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"skunk_id" => skunk_id, "user_skunk" => user_skunk_map}} = job) do
    Logger.info("Starting enrichment job #{job.id} for skunk #{skunk_id}")

    try do
      user_skunk = struct(UserSkunk, atomize_keys(user_skunk_map))
      context = %{}

      start_time = System.monotonic_time(:millisecond)

      result =
        user_skunk
        |> apply_enrichers(context)
        |> case do
          {:ok, enriched_user_skunk} ->
            duration = System.monotonic_time(:millisecond) - start_time
            Logger.info("Enrichment completed for skunk #{skunk_id} in #{duration}ms")
            broadcast_success(skunk_id, enriched_user_skunk)
            :ok

          {:error, error} ->
            duration = System.monotonic_time(:millisecond) - start_time

            Logger.error(
              "Enrichment failed for skunk #{skunk_id} after #{duration}ms: #{inspect(error)}"
            )

            broadcast_failure(skunk_id, error)
            {:error, error}
        end

      result
    rescue
      error ->
        Logger.error("ENRICHMENT JOB CRASHED for skunk #{skunk_id}: #{inspect(error)}")
        Logger.error("Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}")
        broadcast_failure(skunk_id, error)
        {:error, error}
    end
  end

  defp apply_enrichers(user_skunk, context) do
    user_skunk = %{user_skunk | enrichment_status: true}
    enrichers = SkunkEnrichmentService.get_configured_enrichers()

    Logger.info(
      "SkunkEnrichmentWorker: Applying #{length(enrichers)} enrichers to skunk #{user_skunk.id}"
    )

    try do
      enrichers
      |> Enum.reduce_while({:ok, user_skunk, context}, &apply_enricher/2)
      |> case do
        {:ok, enriched_skunk, _context} -> {:ok, enriched_skunk}
        {:error, _reason} = error -> error
      end
    rescue
      error ->
        Logger.error(
          "SkunkEnrichmentWorker: CRASH during enricher application for skunk #{user_skunk.id}: #{inspect(error)}"
        )

        {:error, {:crash, error}}
    end
  end

  defp apply_enricher(enricher, {:ok, user_skunk, context}) do
    Logger.info(
      "SkunkEnrichmentWorker: Applying enricher #{inspect(enricher)} to skunk #{user_skunk.id}"
    )

    start_time = System.monotonic_time(:millisecond)

    try do
      case enricher.enrich(user_skunk, context) do
        {:ok, enriched_skunk} ->
          duration = System.monotonic_time(:millisecond) - start_time

          Logger.info(
            "SkunkEnrichmentWorker: Enricher #{inspect(enricher)} succeeded for skunk #{user_skunk.id} in #{duration}ms"
          )

          {:cont, {:ok, enriched_skunk, context}}

        {:error, error} ->
          duration = System.monotonic_time(:millisecond) - start_time

          Logger.warning(
            "SkunkEnrichmentWorker: Enricher #{inspect(enricher)} failed for skunk #{user_skunk.id} after #{duration}ms: #{inspect(error)}"
          )

          {:cont, {:ok, user_skunk, context}}
      end
    rescue
      error ->
        Logger.error(
          "SkunkEnrichmentWorker: CRASH in enricher #{inspect(enricher)} for skunk #{user_skunk.id}: #{inspect(error)}"
        )

        {:cont, {:ok, user_skunk, context}}
    end
  end

  defp broadcast_success(skunk_id, enriched_user_skunk) do
    Logger.info("SkunkEnrichmentWorker: Broadcasting enrichment success for skunk #{skunk_id}")

    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.skunk_enrichment(),
      {:skunk_enrichment_completed, skunk_id, enriched_user_skunk}
    )
  end

  defp broadcast_failure(skunk_id, error) do
    Logger.error("SkunkEnrichmentWorker: Broadcasting enrichment failure for skunk #{skunk_id}")

    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.skunk_enrichment(),
      {:skunk_enrichment_failed, skunk_id, error}
    )
  end

  defp atomize_keys(map) when is_map(map) do
    Map.new(map, fn
      {key, value} when is_binary(key) -> {String.to_existing_atom(key), atomize_keys(value)}
      {key, value} -> {key, atomize_keys(value)}
    end)
  end

  defp atomize_keys(value), do: value
end
