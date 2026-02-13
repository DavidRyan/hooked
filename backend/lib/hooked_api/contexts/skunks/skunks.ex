defmodule HookedApi.Skunks do
  import Ecto.Query, warn: false
  alias HookedApi.Repo
  alias HookedApi.Skunks.UserSkunk
  alias HookedApi.Services.SkunkEnrichmentService
  require Logger

  def list_user_skunks(user_id) do
    UserSkunk
    |> UserSkunk.for_user(user_id)
    |> Repo.all()
  end

  def get_user_skunk(user_id, id) do
    UserSkunk
    |> UserSkunk.for_user_and_id(user_id, id)
    |> Repo.one()
  end

  def get_user_skunk!(id) do
    Repo.get!(UserSkunk, id)
  end

  def create_user_skunk(user_id, attrs) do
    attrs_with_user = Map.put(attrs, "user_id", user_id)

    case insert_user_skunk(attrs_with_user) do
      {:ok, user_skunk} ->
        # Enqueue enrichment if we have coordinates
        if user_skunk.latitude && user_skunk.longitude do
          SkunkEnrichmentService.enqueue_enrichment(user_skunk)
        end

        {:ok, user_skunk}

      {:error, changeset} ->
        {:error, changeset}
    end
  end

  def update_user_skunk(%UserSkunk{} = user_skunk, attrs) do
    user_skunk
    |> UserSkunk.changeset(attrs)
    |> Repo.update()
  end

  def replace_user_skunk(%UserSkunk{} = enriched_user_skunk) do
    original_skunk = get_user_skunk!(enriched_user_skunk.id)

    attrs =
      enriched_user_skunk
      |> Map.from_struct()
      |> Map.drop([:__meta__, :id, :inserted_at, :updated_at])

    original_skunk
    |> UserSkunk.changeset(attrs)
    |> Repo.update()
  end

  def get_user_skunk_stats(user_id) do
    skunks =
      UserSkunk
      |> UserSkunk.for_user(user_id)
      |> Repo.all()

    total_skunks = Enum.count(skunks)

    skunks_by_location =
      skunks
      |> Enum.filter(& &1.location)
      |> Enum.group_by(& &1.location)
      |> Enum.map(fn {location, group} -> {location, Enum.count(group)} end)

    unique_locations =
      skunks
      |> Enum.filter(fn s -> s.latitude && s.longitude end)
      |> Enum.map(fn s -> "#{Float.round(s.latitude, 6)},#{Float.round(s.longitude, 6)}" end)
      |> Enum.uniq()

    %{
      total_skunks: total_skunks,
      skunks_by_location: skunks_by_location,
      unique_locations: unique_locations
    }
  end

  defp insert_user_skunk(attrs) do
    %UserSkunk{}
    |> UserSkunk.changeset(attrs)
    |> Repo.insert()
  end
end
