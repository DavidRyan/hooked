defmodule Backend.Catches do 
  import Ecto.Query, warn: false
  alias Ecto.Repo
  alias Backend.UserCatches.UserCatch
  alias Backend.Repo
  alias HookedApi.Enrichment.EnrichmentOrchastrator

  def list_user_catches() do
    Repo.all(UserCatch)
  end

  def get_user_catch(id) do
    Repo.get(UserCatch, id)
  end
  
  def create_user_catch(user_catch) do
    enriched_user_catch = EnrichmentOrchastrator.enrich(user_catch)
    Repo.insert(enriched_user_catch)
  end

  def update_user_catch(user_catch) do
    
  end 
  def delete_user_catch(id) do
    
  end 
  def change_user_catch(user_catch) do
    
  end 
end
