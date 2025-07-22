defmodule HookedApi.Workers.CatchEnrichmentWorker do
  use Oban.Worker, queue: :catch_enrichment, max_attempts: 3

  require Logger

  alias HookedApi.Services.{ImageStorage, EnrichmentService}
  alias HookedApi.Utils.ExifExtractor
  alias HookedApi.PubSubTopics

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"catch_id" => catch_id, "user_catch" => user_catch_map}} = job) do
    Logger.info("Starting enrichment job #{job.id} for catch #{catch_id}")

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
  end

  defp enrich_catch(user_catch) do
    Logger.debug("Starting enrichment process for catch #{user_catch.id}")

    with {:ok, exif_data} <- extract_exif_data(user_catch),
         {:ok, enriched_catch} <- apply_enrichers(user_catch) do
      Logger.debug("Enrichment process completed for catch #{user_catch.id}")
      {:ok, %{enriched_catch | exif_data: exif_data}}
    else
      {:error, reason} = error ->
        Logger.error("Enrichment process failed for catch #{user_catch.id}: #{inspect(reason)}")
        error
    end
  end

  defp extract_exif_data(user_catch) do
    Logger.debug(
      "Extracting EXIF data for catch #{user_catch.id} from image: #{user_catch.image_url}"
    )

    result =
      user_catch.image_url
      |> ImageStorage.get_image_file_path()
      |> case do
        {:ok, file_path} ->
          Logger.debug("Found image file at #{file_path}, extracting EXIF data")
          exif_data = ExifExtractor.extract_from_file(file_path)

          Logger.debug(
            "Extracted EXIF data for catch #{user_catch.id}: #{inspect(Map.keys(exif_data))}"
          )

          {:ok, exif_data}

        {:error, reason} ->
          Logger.warning(
            "Could not get image file path for catch #{user_catch.id}: #{inspect(reason)}"
          )

          {:ok, %{}}
      end

    result
  end

  defp apply_enrichers(user_catch) do
    enrichers = EnrichmentService.get_configured_enrichers()
    Logger.info("Applying #{length(enrichers)} enrichers to catch #{user_catch.id}")

    result =
      enrichers
      |> Enum.reduce_while({:ok, user_catch}, &apply_enricher/2)

    case result do
      {:ok, enriched_catch} ->
        Logger.info("Successfully applied all enrichers to catch #{enriched_catch.id}")
        result

      {:error, reason} ->
        Logger.error("Failed to apply enrichers to catch #{user_catch.id}: #{inspect(reason)}")
        result
    end
  end

  defp apply_enricher(enricher, {:ok, user_catch}) do
    Logger.debug("Applying enricher #{inspect(enricher)} to catch #{user_catch.id}")
    start_time = System.monotonic_time(:millisecond)

    case safe_enrich(enricher, user_catch) do
      {:ok, enriched_catch} ->
        duration = System.monotonic_time(:millisecond) - start_time

        Logger.info(
          "Enricher #{inspect(enricher)} succeeded for catch #{user_catch.id} in #{duration}ms"
        )

        {:cont, {:ok, enriched_catch}}

      {:error, error} ->
        duration = System.monotonic_time(:millisecond) - start_time

        Logger.warning(
          "Enricher #{inspect(enricher)} failed for catch #{user_catch.id} after #{duration}ms: #{inspect(error)}"
        )

        # Continue with original catch on enricher failure
        {:cont, {:ok, user_catch}}
    end
  end

  defp safe_enrich(enricher, user_catch) do
    enricher.enrich(user_catch)
  rescue
    error ->
      Logger.error("Enricher #{inspect(enricher)} crashed: #{inspect(error)}")
      {:error, error}
  end

  defp broadcast_success(catch_id, enriched_user_catch) do
    Logger.info("Broadcasting enrichment success for catch #{catch_id}")

    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.catch_enrichment(),
      {:enrichment_completed, catch_id, enriched_user_catch}
    )
  end

  defp broadcast_failure(catch_id, error) do
    Logger.error("Broadcasting enrichment failure for catch #{catch_id}: #{inspect(error)}")

    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.catch_enrichment(),
      {:enrichment_failed, catch_id, error}
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
