defmodule HookedApi.Skunks.UserSkunk do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query

  @derive {Jason.Encoder,
           only: [
             :id,
             :location,
             :latitude,
             :longitude,
             :fished_at,
             :notes,
             :weather_data,
             :inserted_at,
             :updated_at,
             :user_id,
             :enrichment_status
           ]}
  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id

  @type t :: %__MODULE__{
          id: binary(),
          location: String.t() | nil,
          latitude: float() | nil,
          longitude: float() | nil,
          fished_at: NaiveDateTime.t() | nil,
          notes: String.t() | nil,
          weather_data: map() | nil,
          inserted_at: DateTime.t(),
          updated_at: DateTime.t(),
          user_id: binary(),
          enrichment_status: boolean()
        }

  schema "user_skunks" do
    field(:location, :string)
    field(:latitude, :float)
    field(:longitude, :float)
    field(:fished_at, :naive_datetime)
    field(:notes, :string)
    field(:weather_data, :map)
    field(:enrichment_status, :boolean, default: false)
    belongs_to(:user, HookedApi.Accounts.User)

    timestamps(type: :utc_datetime)
  end

  def changeset(user_skunk, attrs) do
    user_skunk
    |> cast(attrs, [
      :location,
      :latitude,
      :longitude,
      :fished_at,
      :notes,
      :weather_data,
      :user_id,
      :enrichment_status
    ])
    |> validate_required([:user_id, :fished_at])
    |> validate_length(:location, max: 255)
    |> validate_length(:notes, max: 1000)
    |> validate_coordinates()
    |> assoc_constraint(:user)
  end

  defp validate_coordinates(changeset) do
    changeset
    |> validate_number(:latitude, greater_than_or_equal_to: -90, less_than_or_equal_to: 90)
    |> validate_number(:longitude, greater_than_or_equal_to: -180, less_than_or_equal_to: 180)
  end

  def for_user(query \\ __MODULE__, user_id) do
    from(us in query, where: us.user_id == ^user_id)
  end

  def for_user_and_id(query \\ __MODULE__, user_id, id) do
    from(us in query, where: us.user_id == ^user_id and us.id == ^id)
  end
end
