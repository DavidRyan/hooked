defmodule HookedApi.Catches do
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Services.{ImageStorage, EnrichmentService}
  alias HookedApi.Utils.ExifExtractor
  require Logger

  def get_user_catch_stats(user_id) do
    catches =
      UserCatch
      |> UserCatch.for_user(user_id)
      |> Repo.all()

    total_catches = Enum.count(catches)

    species_breakdown =
      catches
      |> Enum.group_by(fn x -> x.species end)
      |> Enum.map(fn {species, group} -> {species, Enum.count(group)} end)

    unique_species =
      catches
      |> Enum.map(& &1.species)
      |> Enum.uniq()
      |> Enum.count()

    unique_locations =
      catches
      |> Enum.map(fn x -> "#{Float.round(x.latitude, 6)},#{Float.round(x.longitude, 6)}" end)
      |> Enum.uniq()

    result = %{
      total_catches: total_catches,
      species_breakdown: species_breakdown,
      unique_species: unique_species,
      unique_locations: unique_locations
    }

    {:ok, result}
  end

  def list_user_catches(user_id) do
    UserCatch
    |> UserCatch.for_user(user_id)
    |> Repo.all()
  end

  def get_user_catch(user_id, id) do
    UserCatch
    |> UserCatch.for_user_and_id(user_id, id)
    |> Repo.one()
  end

  def get_user_catch!(id) do
    Repo.get!(UserCatch, id)
  end

  def update_user_catch(%UserCatch{} = user_catch, attrs) do
    user_catch
    |> UserCatch.changeset(attrs)
    |> Repo.update()
  end

  def delete_user_catch(%UserCatch{} = user_catch) do
    Repo.delete(user_catch)
  end

  def replace_user_catch(%UserCatch{} = enriched_user_catch) do
    # Get the original record from the database to create a proper changeset
    original_catch = get_user_catch!(enriched_user_catch.id)

    # Convert enriched struct to map and remove metadata fields
    attrs =
      enriched_user_catch
      |> Map.from_struct()
      |> Map.drop([:__meta__, :id, :inserted_at, :updated_at])

    # Create changeset from original record with new attributes
    original_catch
    |> UserCatch.changeset(attrs)
    |> Repo.update()
  end

  def create_user_catch(user_id, attrs, %Plug.Upload{path: temp_path} = image_upload) do
    # Upload image and create catch
    # Pass temp_path to enrichment so it can use the local file instead of downloading from S3
    enrichment_path = copy_enrichment_upload(temp_path)

    result =
      with {:ok, image_data} <- ImageStorage.upload_image(image_upload),
           attrs_with_image <- Map.merge(attrs, Map.put(image_data, "user_id", user_id)),
           {:ok, user_catch} <- insert_user_catch(attrs_with_image),
           {:ok, _job} <- EnrichmentService.enqueue_enrichment(user_catch, enrichment_path) do
        {:ok, user_catch}
      end

    case result do
      {:ok, user_catch} ->
        {:ok, user_catch}

      {:error, %Ecto.Changeset{} = changeset} ->
        # Clean up temp file on failure
        File.rm(temp_path)
        {:error, changeset}

      {:error, reason} ->
        # Clean up temp file on failure
        File.rm(temp_path)
        {:error, reason}
    end
  end

  defp insert_user_catch(attrs) do
    %UserCatch{}
    |> UserCatch.changeset(attrs)
    |> Repo.insert()
  end

  defp copy_enrichment_upload(temp_path) when is_binary(temp_path) do
    log_memory("enrichment_copy:start", temp_path: temp_path)
    temp_dir = Path.join(System.tmp_dir!(), "hooked-enrichment")
    File.mkdir_p!(temp_dir)

    extension = Path.extname(temp_path)
    filename = "#{Ecto.UUID.generate()}#{extension}"
    destination = Path.join(temp_dir, filename)

    case File.cp(temp_path, destination) do
      :ok ->
        Logger.debug("Copied upload to enrichment temp file: #{destination}")
        log_memory("enrichment_copy:done", temp_path: destination)
        destination

      {:error, reason} ->
        Logger.warning("Failed to copy upload for enrichment: #{inspect(reason)}")

        nil
    end
  end

  defp copy_enrichment_upload(_temp_path), do: nil

  defp log_memory(stage, metadata) do
    memory = :erlang.memory()

    data =
      Keyword.merge(
        [
          stage: stage,
          total_mb: bytes_to_mb(memory[:total]),
          processes_mb: bytes_to_mb(memory[:processes_used]),
          binary_mb: bytes_to_mb(memory[:binary]),
          ets_mb: bytes_to_mb(memory[:ets])
        ],
        Enum.to_list(metadata)
      )

    Logger.info("MEMORY #{format_memory(data)}")
  end

  defp bytes_to_mb(nil), do: nil
  defp bytes_to_mb(bytes), do: Float.round(bytes / 1_048_576, 2)

  defp format_memory(data) do
    data
    |> Enum.map(fn {key, value} -> "#{key}=#{inspect(value)}" end)
    |> Enum.join(" ")
  end
end
