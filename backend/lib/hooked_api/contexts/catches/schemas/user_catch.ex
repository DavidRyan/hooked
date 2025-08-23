defmodule HookedApi.Catches.UserCatch do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query

  @derive {Jason.Encoder,
           only: [
             :id,
             :species,
             :location,
             :latitude,
             :longitude,
             :caught_at,
             :notes,
             :weather_data,
             :exif_data,
             :image_url,
             :image_filename,
             :image_content_type,
             :image_file_size,
             :inserted_at,
             :updated_at,
             :user_id
           ]}
  @derive {JSON.Encoder,
           only: [
             :id,
             :species,
             :location,
             :latitude,
             :longitude,
             :caught_at,
             :notes,
             :weather_data,
             :exif_data,
             :image_url,
             :image_filename,
             :image_content_type,
             :image_file_size,
             :inserted_at,
             :updated_at,
             :user_id
           ]}
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
          exif_data: map() | nil,
          image_url: String.t() | nil,
          image_filename: String.t() | nil,
          image_content_type: String.t() | nil,
          image_file_size: integer() | nil,
          inserted_at: DateTime.t(),
          updated_at: DateTime.t(),
          user_id: binary()
        }

  schema "user_catches" do
    field(:species, :string)
    field(:location, :string)
    field(:latitude, :float)
    field(:longitude, :float)
    field(:caught_at, :naive_datetime)
    field(:notes, :string)
    field(:weather_data, :map)
    field(:exif_data, :map)
    belongs_to :user, HookedApi.Accounts.User
    field(:image_url, :string)
    field(:image_filename, :string)
    field(:image_content_type, :string)
    field(:image_file_size, :integer)

    timestamps(type: :utc_datetime)
  end

  def changeset(user_catch, attrs) do
    user_catch
    |> cast(attrs, [
      :species,
      :location,
      :latitude,
      :longitude,
      :caught_at,
      :notes,
      :weather_data,
      :exif_data,
      :image_url,
      :image_filename,
      :image_content_type,
      :image_file_size,
      :user_id
    ])
    |> validate_required([])
    |> validate_length(:species, min: 1, max: 100)
    |> validate_length(:location, min: 1, max: 200)
    |> validate_length(:notes, max: 1000)
    |> validate_coordinates()
    |> validate_image()
  end

  defp validate_coordinates(changeset) do
    changeset
    |> validate_number(:latitude, greater_than_or_equal_to: -90, less_than_or_equal_to: 90)
    |> validate_number(:longitude, greater_than_or_equal_to: -180, less_than_or_equal_to: 180)
  end

  defp validate_image(changeset) do
    changeset
    |> validate_length(:image_filename, max: 255)
    |> validate_length(:image_url, max: 500)
    |> validate_inclusion(:image_content_type, [
      "image/jpeg",
      "image/png",
      "image/webp",
      "image/heic"
    ])
    |> validate_number(:image_file_size, greater_than: 0, less_than: 10_000_000)
    |> assoc_constraint(:user)
  end

  def for_user(query \\ __MODULE__, user_id) do
    from uc in query, where: uc.user_id == ^user_id
  end

  def for_user_and_id(query \\ __MODULE__, user_id, id) do
    from uc in query, where: uc.user_id == ^user_id and uc.id == ^id
  end
end
