defmodule HookedApi.Catches do
  use GenServer
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Services.ImageStorage
  alias HookedApi.Workers.CatchEnrichmentWorker

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, [], name: __MODULE__)
  end

  @impl true
  def init(_) do
    Phoenix.PubSub.subscribe(HookedApi.PubSub, "catch_enrichment")
    {:ok, %{}}
  end

  @impl true
  def handle_info({:enrichment_completed, catch_id, enriched_data}, state) do
    case get_user_catch(catch_id) do
      nil ->
        require Logger
        Logger.error("Catch not found for enrichment: #{catch_id}")

      user_catch ->
        case update_user_catch(user_catch, enriched_data) do
          {:ok, updated_catch} ->
            require Logger
            Logger.info("Successfully enriched catch #{updated_catch.id}")

          {:error, changeset} ->
            require Logger
            Logger.error("Failed to update catch #{catch_id}: #{inspect(changeset.errors)}")
        end
    end

    {:noreply, state}
  end

  def handle_info({:enrichment_failed, catch_id, error}, state) do
    require Logger
    Logger.error("Enrichment failed for catch #{catch_id}: #{inspect(error)}")
    {:noreply, state}
  end

  def handle_info(_msg, state), do: {:noreply, state}

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
  def create_user_catch(attrs, %Plug.Upload{} = image_upload) do
    with {:ok, image_data} <- ImageStorage.upload_image(image_upload),
         attrs_with_image <- Map.merge(attrs, image_data),
         {:ok, user_catch} <- insert_user_catch(attrs_with_image) do
      
      user_catch_data = Map.from_struct(user_catch)
      
      %{catch_id: user_catch.id, user_catch: user_catch_data}
      |> CatchEnrichmentWorker.new()
      |> Oban.insert()
      
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
