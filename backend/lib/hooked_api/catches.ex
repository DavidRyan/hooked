defmodule Backend.Catches do 
  import Ecto.Query, warn: false
  alias Backend.UserCatches.UserCatch
  alias Backend.Repo

  def list_user_catches() do
    Repo.all(UserCatch)
  end

  def get_user_catch(id) do
    Repo.get(UserCatch, id)
  end
  
  def create_user_catch(user_catch) do
  end

  def update_user_catch(user_catch) do
    
  end 
  def delete_user_catch(id) do
    
  end 
  def change_user_catch(user_catch) do
    
  end 
end
