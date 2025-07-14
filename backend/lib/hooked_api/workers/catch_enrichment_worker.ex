defmodule HookedApi.Workers.CatchEnrichmentWorker do
  use Oban.Worker, queue: :catch_enrichment, max_attempts: 3

  require Logger

  alias HookedApi.Services.{ImageStorage, EnrichmentService}
  alias HookedApi.Utils.ExifExtractor
  alias HookedApi.PubSubTopics

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"catch_id" => catch_id, "user_catch" => user_catch}}) do
    user_catch
    |> enrich_catch()
    |> case do
      {:ok, enriched_user_catch} ->
        broadcast_success(catch_id, enriched_user_catch)
        :ok

      {:error, error} ->
        broadcast_failure(catch_id, error)
        {:error, error}
    end
  end

  defp enrich_catch(user_catch) do
    with {:ok, exif_data} <- extract_exif_data(user_catch),
         {:ok, enriched_catch} <- apply_enrichers(user_catch) do
      {:ok, %{enriched_catch | exif_data: exif_data}}
    end
  end

  defp extract_exif_data(user_catch) do
    user_catch.image_url
    |> ImageStorage.get_image_file_path()
    |> case do
      {:ok, file_path} -> 
        {:ok, ExifExtractor.extract_from_file(file_path)}
      {:error, _reason} -> 
        {:ok, %{}}
    end
  end

  defp apply_enrichers(user_catch) do
    enrichers = EnrichmentService.get_configured_enrichers()
    
    enrichers
    |> Enum.reduce_while({:ok, user_catch}, &apply_enricher/2)
  end

  defp apply_enricher(enricher, {:ok, user_catch}) do
    case safe_enrich(enricher, user_catch) do
      {:ok, enriched_catch} -> 
        {:cont, {:ok, enriched_catch}}
      {:error, error} -> 
        Logger.warning("Enricher #{inspect(enricher)} failed: #{inspect(error)}")
        {:cont, {:ok, user_catch}}  # Continue with original catch on enricher failure
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
    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.catch_enrichment(),
      {:enrichment_completed, catch_id, enriched_user_catch}
    )
  end

  defp broadcast_failure(catch_id, error) do
    Logger.error("Failed to enrich catch #{catch_id}: #{inspect(error)}")
    
    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.catch_enrichment(),
      {:enrichment_failed, catch_id, error}
    )
  end
end
