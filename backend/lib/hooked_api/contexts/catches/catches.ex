defmodule HookedApi.Catches do 
  @moduledoc """
  The Catches context.
  
  Handles all operations related to user catches including
  creating, updating, deleting, and retrieving catch records.
  """

  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Catches.UserCatch
  alias HookedApi.Enrichment
  alias HookedApi.Services.ImageStorage

  @doc """
  Returns the list of user catches.

  ## Examples

      iex> list_user_catches()
      [%UserCatch{}, ...]

  """
  def list_user_catches() do
    Repo.all(UserCatch)
  end

  @doc """
  Gets a single user catch.

  Returns `nil` if the User catch does not exist.

  ## Examples

      iex> get_user_catch(123)
      %UserCatch{}

      iex> get_user_catch(456)
      nil

  """
  def get_user_catch(id) do
    Repo.get(UserCatch, id)
  end

  @doc """
  Gets a single user catch, raising an exception if not found.

  ## Examples

      iex> get_user_catch!(123)
      %UserCatch{}

      iex> get_user_catch!(456)
      ** (Ecto.NoResultsError)

  """
  def get_user_catch!(id) do
    Repo.get!(UserCatch, id)
  end
  
  @doc """
  Creates a user catch with enriched data.

  ## Examples

      iex> create_user_catch(%{species: "Bass"})
      {:ok, %UserCatch{}}

      iex> create_user_catch(%{bad_field: "value"})
      {:error, %Ecto.Changeset{}}

  """
  def create_user_catch(attrs \\ %{}) do
    %UserCatch{}
    |> UserCatch.changeset(attrs)
    |> case do
      %Ecto.Changeset{valid?: true} = _changeset ->
        enriched_attrs = Enrichment.enrich_catch_data(attrs)
        enriched_changeset = UserCatch.changeset(%UserCatch{}, enriched_attrs)
        Repo.insert(enriched_changeset)
      changeset ->
        {:error, changeset}
    end
  end

  @doc """
  Updates a user catch.

  ## Examples

      iex> update_user_catch(user_catch, %{species: "New Species"})
      {:ok, %UserCatch{}}

      iex> update_user_catch(user_catch, %{bad_field: "value"})
      {:error, %Ecto.Changeset{}}

  """
  def update_user_catch(%UserCatch{} = user_catch, attrs) do
    user_catch
    |> UserCatch.changeset(attrs)
    |> Repo.update()
  end 

  @doc """
  Deletes a user catch and its associated image.

  ## Examples

      iex> delete_user_catch(user_catch)
      {:ok, %UserCatch{}}

      iex> delete_user_catch(user_catch)
      {:error, %Ecto.Changeset{}}

  """
  def delete_user_catch(%UserCatch{} = user_catch) do
    # Delete the image file if it exists
    if user_catch.image_url do
      ImageStorage.delete_image(user_catch.image_url)
    end
    
    Repo.delete(user_catch)
  end 

  @doc """
  Returns an `%Ecto.Changeset{}` for tracking user catch changes.

  ## Examples

      iex> change_user_catch(user_catch)
      %Ecto.Changeset{data: %UserCatch{}}

  """
  def change_user_catch(%UserCatch{} = user_catch, attrs \\ %{}) do
    UserCatch.changeset(user_catch, attrs)
  end

  @doc """
  Creates a user catch with an uploaded image.

  ## Examples

      iex> create_user_catch_with_image(attrs, %Plug.Upload{})
      {:ok, %UserCatch{}}

      iex> create_user_catch_with_image(bad_attrs, upload)
      {:error, %Ecto.Changeset{}}

  """
  def create_user_catch_with_image(attrs, %Plug.Upload{} = image_upload) do
    with {:ok, image_metadata} <- ImageStorage.upload_image(image_upload),
         attrs_with_image <- Map.merge(attrs, image_metadata),
         {:ok, user_catch} <- create_user_catch(attrs_with_image) do
      {:ok, user_catch}
    else
      {:error, %Ecto.Changeset{} = changeset} -> {:error, changeset}
      {:error, reason} -> {:error, reason}
    end
  end




end
