defmodule HookedApi.Catches do
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Services.{ImageStorage, EnrichmentService}

  def list_user_catches() do
    Repo.all(UserCatch)
  end

  def get_user_catch(id) do
    Repo.get(UserCatch, id)
  end

  def get_user_catch!(id) do
    Repo.get!(UserCatch, id)
  end

  def update_user_catch(%UserCatch{} = user_catch, attrs) do
    user_catch
    |> UserCatch.changeset(attrs)
    |> Repo.update()
  end

  def replace_user_catch(%UserCatch{} = enriched_user_catch) do
    # Get the original record from the database to create a proper changeset
    original_catch = get_user_catch!(enriched_user_catch.id)
    
    # Convert enriched struct to map and remove metadata fields
    attrs = enriched_user_catch
    |> Map.from_struct()
    |> Map.drop([:__meta__, :id, :inserted_at, :updated_at])
    
    # Create changeset from original record with new attributes
    original_catch
    |> UserCatch.changeset(attrs)
    |> Repo.update()
  end

  def create_user_catch(attrs, %Plug.Upload{} = image_upload) do
    with {:ok, image_data} <- ImageStorage.upload_image(image_upload),
         attrs_with_image <- Map.merge(attrs, image_data),
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
