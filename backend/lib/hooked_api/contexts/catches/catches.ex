defmodule HookedApi.Catches do
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Services.{ImageStorage, EnrichmentService}
  alias HookedApi.Utils.ExifExtractor
  require Logger

  def get_user_catch_stats(user_id) do
    catches = UserCatch
    |> UserCatch.for_user(user_id)
    |> Repo.all()

    total_catches = Enum.count(catches)

    species_breakdown = catches
    |> Enum.group_by(fn x -> x.species end)
    |> Enum.map(fn {species, group} -> {species, Enum.count(group)} end)

    unique_species = catches
    |> Enum.map(& &1.species)
    |> Enum.uniq()
    |> Enum.count()

    unique_locations = catches
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
    # Read the image file to extract EXIF data
    binary_data = File.read!(temp_path)
    exif_data = ExifExtractor.extract_from_file(temp_path)

    # Add EXIF data directly to attrs
    attrs = Map.put(attrs, "latitude", exif_data[:gps_latitude] || attrs["latitude"])
    attrs = Map.put(attrs, "longitude", exif_data[:gps_longitude] || attrs["longitude"])
    attrs = Map.put(attrs, "caught_at", exif_data[:datetime] || attrs["caught_at"])

    # Upload image and create catch
    with {:ok, image_data} <- ImageStorage.upload_image(image_upload),
         attrs_with_image <- Map.merge(attrs, Map.put(image_data, "user_id", user_id)),
         {:ok, user_catch} <- insert_user_catch(attrs_with_image),
         {:ok, _job} <- EnrichmentService.enqueue_enrichment(user_catch) do
      {:ok, user_catch}
    else
      {:error, %Ecto.Changeset{} = changeset} -> {:error, changeset}
      {:error, reason} -> {:error, reason}
    end
  end

  defp insert_user_catch(attrs) do
    %UserCatch{}
    |> UserCatch.changeset(attrs)
    |> Repo.insert()
  end
end
