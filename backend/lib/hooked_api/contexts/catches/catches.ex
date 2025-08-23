defmodule HookedApi.Catches do
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Services.{ImageStorage, EnrichmentService}

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

  def create_user_catch(user_id, attrs, %Plug.Upload{} = image_upload) do
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
