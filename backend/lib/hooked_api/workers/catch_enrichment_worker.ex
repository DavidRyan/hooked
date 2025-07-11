defmodule HookedApi.Workers.CatchEnrichmentWorker do
  use Oban.Worker, queue: :catch_enrichment, max_attempts: 3

  alias HookedApi.Utils.ExifParser
  alias HookedApi.Services.{ImageStorage, EnrichmentService}
  alias HookedApi.PubSubTopics

  @impl Oban.Worker
  def perform(%Oban.Job{args: %{"catch_id" => catch_id, "user_catch" => user_catch_data}}) do
    user_catch = struct(HookedApi.Catches.UserCatch, user_catch_data)
    
    enriched_data = enrich_catch(user_catch)
    
    Phoenix.PubSub.broadcast(
      HookedApi.PubSub,
      PubSubTopics.catch_enrichment(),
      {:enrichment_completed, catch_id, enriched_data}
    )
    
    :ok
  rescue
    error ->
      require Logger
      Logger.error("Failed to enrich catch #{catch_id}: #{inspect(error)}")
      
      Phoenix.PubSub.broadcast(
        HookedApi.PubSub,
        PubSubTopics.catch_enrichment(),
        {:enrichment_failed, catch_id, error}
      )
      
      {:error, error}
  end

  defp enrich_catch(user_catch) do
    exif_data = extract_exif_data(user_catch)
    enrichers = EnrichmentService.get_configured_enrichers()

    enriched_data = Enum.reduce(enrichers, %{}, fn enricher, acc ->
      try do
        enricher.enrich(user_catch, exif_data)
        |> Map.merge(acc)
      rescue
        error ->
          require Logger
          Logger.error("Enricher #{enricher} failed: #{inspect(error)}")
          acc
      end
    end)

    Map.merge(enriched_data, %{"exif_data" => exif_data})
  end

  defp extract_exif_data(user_catch) do
    case ImageStorage.get_image_file_path(user_catch.image_url) do
      {:ok, file_path} ->
        case ExifParser.parse(file_path) do
          {:ok, exif_data} -> exif_data
          {:error, _reason} -> %{}
        end
      
      {:error, _reason} ->
        %{}
    end
  end
end
