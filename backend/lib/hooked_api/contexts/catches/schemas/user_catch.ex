defmodule HookedApi.Catches.UserCatch do
  @moduledoc """
  Schema for user catch records.
  
  Represents a fishing catch made by a user, including location,
  species, timing, and enriched data like weather information.
  """
  
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id
  
  @type t :: %__MODULE__{
    id: binary(),
    species: String.t(),
    location: String.t(),
    latitude: float(),
    longitude: float(),
    caught_at: NaiveDateTime.t(),
    notes: String.t() | nil,
    weather_data: map() | nil,
    inserted_at: DateTime.t(),
    updated_at: DateTime.t()
  }

  schema "user_catches" do
    field :species, :string
    field :location, :string
    field :latitude, :float
    field :longitude, :float
    field :caught_at, :naive_datetime
    field :notes, :string
    field :weather_data, :map

    # Future: Add user association
    # belongs_to :user, HookedApi.Accounts.User

    timestamps(type: :utc_datetime)
  end

  @doc """
  Changeset for user catch creation and updates.
  
  ## Required fields
  - species: The type of fish caught
  - location: Where the catch was made
  - caught_at: When the catch was made
  
  ## Optional fields
  - latitude, longitude: GPS coordinates
  - notes: Additional notes about the catch
  - weather_data: Enriched weather information (usually set automatically)
  """
  def changeset(user_catch, attrs) do
    user_catch
    |> cast(attrs, [:species, :location, :latitude, :longitude, :caught_at, :notes, :weather_data])
    |> validate_required([:species, :location, :caught_at])
    |> validate_length(:species, min: 1, max: 100)
    |> validate_length(:location, min: 1, max: 200)
    |> validate_length(:notes, max: 1000)
    |> validate_coordinates()
  end

  defp validate_coordinates(changeset) do
    changeset
    |> validate_number(:latitude, greater_than_or_equal_to: -90, less_than_or_equal_to: 90)
    |> validate_number(:longitude, greater_than_or_equal_to: -180, less_than_or_equal_to: 180)
  end
end
