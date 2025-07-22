defmodule HookedApi.Repo.Migrations.CreateUserCatchesTable do
  use Ecto.Migration

  def change do
    create table(:user_catches, primary_key: false) do
      add(:id, :binary_id, primary_key: true)
      add(:species, :string)
      add(:location, :string)
      add(:latitude, :float)
      add(:longitude, :float)
      add(:caught_at, :naive_datetime)
      add(:notes, :text)
      add(:weather_data, :map)
      add(:exif_data, :map)
      add(:image_url, :string)
      add(:image_filename, :string)
      add(:image_content_type, :string)
      add(:image_file_size, :integer)

      timestamps(type: :utc_datetime)
    end
  end
end
