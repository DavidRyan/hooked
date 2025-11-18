defmodule HookedApi.Workers.CatchEnrichmentWorker do
  use Oban.Worker, queue: :catch_enrichment, max_attempts: 3

  require Logger

  alias HookedApi.Services.EnrichmentService
  alias HookedApi.PubSubTopics
  alias HookedApi.Catches.UserCatch

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"catch_id" => catch_id, "user_catch" => user_catch_map}} = job) do
    Logger.info("Starting enrichment job #{job.id} for catch #{catch_id}")

    try do
      # Convert map back to struct since Oban serializes structs as maps
      user_catch = struct(UserCatch, atomize_keys(user_catch_map))

      start_time = System.monotonic_time(:millisecond)

      result =
        user_catch
        |> enrich_catch()
        |> case do
          {:ok, enriched_user_catch} ->
            duration = System.monotonic_time(:millisecond) - start_time
            Logger.info("Enrichment completed for catch #{catch_id} in #{duration}ms")
            broadcast_success(catch_id, enriched_user_catch)
            :ok

          {:error, error} ->
            duration = System.monotonic_time(:millisecond) - start_time

            Logger.error(
              "Enrichment failed for catch #{catch_id} after #{duration}ms: #{inspect(error)}"
            )

            broadcast_failure(catch_id, error)
            {:error, error}
        end

      Logger.info(
        "Enrichment job #{job.id} for catch #{catch_id} completed with result: #{inspect(result)}"
      )

      result
    rescue
      error ->
        Logger.error("ENRICHMENT JOB CRASHED for catch #{catch_id}: #{inspect(error)}")
        Logger.error("Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}")
        broadcast_failure(catch_id, error)
        {:error, error}
    end
  end

  defp enrich_catch(user_catch) do
    Logger.debug("Starting enrichment process for catch #{user_catch.id}")

    case apply_enrichers(user_catch) do
      {:ok, enriched_catch} ->
        Logger.debug("Enrichment process completed for catch #{user_catch.id}")
        {:ok, enriched_catch}

      {:error, reason} = error ->
        Logger.error("Enrichment process failed for catch #{user_catch.id}: #{inspect(reason)}")
        error
    end
  end

  defp apply_enrichers(user_catch) do
    Logger.info(
      "CatchEnrichmentWorker: Starting enricher application process for catch #{user_catch.id}"
    )

    # Initialize enrichment_status to true - enrichers will flip it to false on failure
    user_catch = %{user_catch | enrichment_status: true}

    enrichers = EnrichmentService.get_configured_enrichers()

    Logger.info(
      "CatchEnrichmentWorker: Applying #{length(enrichers)} enrichers to catch #{user_catch.id}"
    )

    Logger.debug("CatchEnrichmentWorker: Enricher sequence: #{inspect(enrichers)}")

    start_time = System.monotonic_time(:millisecond)

    try do
      result =
        enrichers
        |> Enum.reduce_while({:ok, user_catch}, &apply_enricher/2)

      duration = System.monotonic_time(:millisecond) - start_time

      case result do
        {:ok, enriched_catch} ->
          Logger.info(
            "CatchEnrichmentWorker: Successfully applied all enrichers to catch #{enriched_catch.id} in #{duration}ms"
          )

          Logger.debug(
            "CatchEnrichmentWorker: Final enriched catch - species: #{inspect(enriched_catch.species)}, lat: #{inspect(enriched_catch.latitude)}, lng: #{inspect(enriched_catch.longitude)}"
          )

          Logger.debug(
            "CatchEnrichmentWorker: Weather data present: #{not is_nil(enriched_catch.weather_data)}"
          )

          Logger.debug(
            "CatchEnrichmentWorker: EXIF data present: #{not is_nil(enriched_catch.exif_data)}"
          )

          result

        {:error, reason} ->
          Logger.error(
            "CatchEnrichmentWorker: Failed to apply enrichers to catch #{user_catch.id} after #{duration}ms: #{inspect(reason)}"
          )

          result
      end
    rescue
      error ->
        duration = System.monotonic_time(:millisecond) - start_time

        Logger.error(
          "CatchEnrichmentWorker: CRASH during enricher application for catch #{user_catch.id} after #{duration}ms: #{inspect(error)}"
        )

        Logger.error(
          "CatchEnrichmentWorker: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:crash, error}}
    end
  end

  defp apply_enricher(enricher, {:ok, user_catch}) do
    Logger.info(
      "CatchEnrichmentWorker: Applying enricher #{inspect(enricher)} to catch #{user_catch.id}"
    )

    Logger.debug(
      "CatchEnrichmentWorker: Pre-enrichment state - species: #{inspect(user_catch.species)}, lat: #{inspect(user_catch.latitude)}, lng: #{inspect(user_catch.longitude)}"
    )

    start_time = System.monotonic_time(:millisecond)

    try do
      case safe_enrich(enricher, user_catch) do
        {:ok, enriched_catch} ->
          duration = System.monotonic_time(:millisecond) - start_time

          Logger.info(
            "CatchEnrichmentWorker: Enricher #{inspect(enricher)} succeeded for catch #{user_catch.id} in #{duration}ms"
          )

          # Log what changed
          changes = []

          changes =
            if user_catch.species != enriched_catch.species,
              do: [
                "species: #{inspect(user_catch.species)} -> #{inspect(enriched_catch.species)}"
                | changes
              ],
              else: changes

          changes =
            if user_catch.latitude != enriched_catch.latitude,
              do: [
                "latitude: #{inspect(user_catch.latitude)} -> #{inspect(enriched_catch.latitude)}"
                | changes
              ],
              else: changes

          changes =
            if user_catch.longitude != enriched_catch.longitude,
              do: [
                "longitude: #{inspect(user_catch.longitude)} -> #{inspect(enriched_catch.longitude)}"
                | changes
              ],
              else: changes

          changes =
            if user_catch.weather_data != enriched_catch.weather_data,
              do: [
                "weather_data: #{if is_nil(user_catch.weather_data), do: "nil", else: "present"} -> #{if is_nil(enriched_catch.weather_data), do: "nil", else: "present"}"
                | changes
              ],
              else: changes

          changes =
            if user_catch.exif_data != enriched_catch.exif_data,
              do: [
                "exif_data: #{if is_nil(user_catch.exif_data), do: "nil", else: "present"} -> #{if is_nil(enriched_catch.exif_data), do: "nil", else: "present"}"
                | changes
              ],
              else: changes

          if length(changes) > 0 do
            Logger.debug(
              "CatchEnrichmentWorker: Changes made by #{inspect(enricher)}: #{Enum.join(changes, ", ")}"
            )
          else
            Logger.debug("CatchEnrichmentWorker: No changes made by #{inspect(enricher)}")
          end

          {:cont, {:ok, enriched_catch}}

        {:error, error} ->
          duration = System.monotonic_time(:millisecond) - start_time

          Logger.warning(
            "CatchEnrichmentWorker: Enricher #{inspect(enricher)} failed for catch #{user_catch.id} after #{duration}ms: #{inspect(error)}"
          )

          Logger.debug(
            "CatchEnrichmentWorker: Continuing with original catch due to enricher failure"
          )

          # Continue with original catch on enricher failure
          {:cont, {:ok, user_catch}}
      end
    rescue
      error ->
        duration = System.monotonic_time(:millisecond) - start_time

        Logger.error(
          "CatchEnrichmentWorker: CRASH in enricher #{inspect(enricher)} for catch #{user_catch.id} after #{duration}ms: #{inspect(error)}"
        )

        Logger.error(
          "CatchEnrichmentWorker: Enricher crash stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        Logger.debug(
          "CatchEnrichmentWorker: Continuing with original catch due to enricher crash"
        )

        # Continue with original catch on enricher crash
        {:cont, {:ok, user_catch}}
    end
  end

  defp safe_enrich(enricher, user_catch) do
    Logger.debug(
      "CatchEnrichmentWorker: Calling #{inspect(enricher)}.enrich/1 for catch #{user_catch.id}"
    )

    try do
      result = enricher.enrich(user_catch)

      Logger.debug(
        "CatchEnrichmentWorker: #{inspect(enricher)}.enrich/1 returned: #{inspect(elem(result, 0))}"
      )

      result
    rescue
      error ->
        Logger.error(
          "CatchEnrichmentWorker: Enricher #{inspect(enricher)} crashed during enrich/1 call: #{inspect(error)}"
        )

        Logger.error(
          "CatchEnrichmentWorker: Crash details - error type: #{inspect(error.__struct__)}"
        )

        Logger.error(
          "CatchEnrichmentWorker: Crash stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:enricher_crash, error}}
    end
  end

  defp broadcast_success(catch_id, enriched_user_catch) do
    Logger.info("CatchEnrichmentWorker: Broadcasting enrichment success for catch #{catch_id}")

    Logger.debug(
      "CatchEnrichmentWorker: Final enriched data - species: #{inspect(enriched_user_catch.species)}"
    )

    Logger.debug(
      "CatchEnrichmentWorker: GPS coordinates - lat: #{inspect(enriched_user_catch.latitude)}, lng: #{inspect(enriched_user_catch.longitude)}"
    )

    Logger.debug(
      "CatchEnrichmentWorker: Weather data present: #{not is_nil(enriched_user_catch.weather_data)}"
    )

    Logger.debug(
      "CatchEnrichmentWorker: EXIF data present: #{not is_nil(enriched_user_catch.exif_data)}"
    )

    try do
      result =
        Phoenix.PubSub.broadcast(
          HookedApi.PubSub,
          PubSubTopics.catch_enrichment(),
          {:enrichment_completed, catch_id, enriched_user_catch}
        )

      case result do
        :ok ->
          Logger.info(
            "CatchEnrichmentWorker: Successfully broadcasted enrichment success for catch #{catch_id}"
          )

        {:error, reason} ->
          Logger.error(
            "CatchEnrichmentWorker: Failed to broadcast enrichment success for catch #{catch_id}: #{inspect(reason)}"
          )
      end

      result
    rescue
      error ->
        Logger.error(
          "CatchEnrichmentWorker: CRASH during success broadcast for catch #{catch_id}: #{inspect(error)}"
        )

        Logger.error(
          "CatchEnrichmentWorker: Broadcast crash stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:broadcast_crash, error}}
    end
  end

  defp broadcast_failure(catch_id, error) do
    Logger.error(
      "CatchEnrichmentWorker: Broadcasting enrichment failure for catch #{catch_id}: #{inspect(error)}"
    )

    Logger.error(
      "CatchEnrichmentWorker: Failure error type: #{inspect(error.__struct__ || :unknown)}"
    )

    try do
      result =
        Phoenix.PubSub.broadcast(
          HookedApi.PubSub,
          PubSubTopics.catch_enrichment(),
          {:enrichment_failed, catch_id, error}
        )

      case result do
        :ok ->
          Logger.info(
            "CatchEnrichmentWorker: Successfully broadcasted enrichment failure for catch #{catch_id}"
          )

        {:error, reason} ->
          Logger.error(
            "CatchEnrichmentWorker: Failed to broadcast enrichment failure for catch #{catch_id}: #{inspect(reason)}"
          )
      end

      result
    rescue
      broadcast_error ->
        Logger.error(
          "CatchEnrichmentWorker: CRASH during failure broadcast for catch #{catch_id}: #{inspect(broadcast_error)}"
        )

        Logger.error(
          "CatchEnrichmentWorker: Broadcast crash stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:broadcast_crash, broadcast_error}}
    end
  end

  defp atomize_keys(map) when is_map(map) do
    Map.new(map, fn
      {key, value} when is_binary(key) -> {String.to_existing_atom(key), atomize_keys(value)}
      {key, value} -> {key, atomize_keys(value)}
    end)
  end

  defp atomize_keys(value), do: value
end
