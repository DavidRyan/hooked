defmodule HookedApi.Catches.UserCatch do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id
  schema "user_catches" do
    field :species, :string
    field :location, :string
    field :latitude, :float
    field :longitude, :float
    field :caught_at, :naive_datetime
    field :notes, :string
    field :weather_data, :map

    timestamps(type: :utc_datetime)
  end

  @doc false
  def changeset(user_catch, attrs) do
    user_catch
    |> cast(attrs, [:species, :location, :latitude, :longitude, :caught_at, :notes, :weather_data])
    |> validate_required([:species, :location, :caught_at])
  end
end
