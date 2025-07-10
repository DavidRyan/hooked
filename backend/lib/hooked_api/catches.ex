defmodule HookedApi.Catches do 
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Enrichment.EnrichmentOrchestrator

  def list_user_catches() do
    Repo.all(UserCatch)
  end

  def get_user_catch(id) do
    Repo.get(UserCatch, id)
  end
  
  def create_user_catch(attrs \\ %{}) do
    %UserCatch{}
    |> UserCatch.changeset(attrs)
    |> case do
      %Ecto.Changeset{valid?: true} = changeset ->
        enriched_attrs = EnrichmentOrchestrator.enrich(attrs)
        enriched_changeset = UserCatch.changeset(%UserCatch{}, enriched_attrs)
        Repo.insert(enriched_changeset)
      changeset ->
        {:error, changeset}
    end
  end

  def update_user_catch(%UserCatch{} = user_catch, attrs) do
    user_catch
    |> UserCatch.changeset(attrs)
    |> Repo.update()
  end 

  def delete_user_catch(%UserCatch{} = user_catch) do
    Repo.delete(user_catch)
  end 

  def change_user_catch(%UserCatch{} = user_catch, attrs \\ %{}) do
    UserCatch.changeset(user_catch, attrs)
  end 
end
