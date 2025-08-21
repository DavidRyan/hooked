defmodule HookedApi.Repo.Migrations.CreateInitialSchema do
  use Ecto.Migration

  def up do
    # Oban jobs table
    Oban.Migration.up(version: 12)

    # Users table
    create table(:users, primary_key: false) do
      add(:id, :binary_id, primary_key: true)
      add(:email, :string, null: false)
      add(:password_hash, :string, null: false)
      add(:first_name, :string)
      add(:last_name, :string)
      add(:is_active, :boolean, default: true, null: false)
      add(:failed_login_attempts, :integer, default: 0, null: false)
      add(:locked_until, :utc_datetime)
      add(:last_failed_login, :utc_datetime)

      timestamps(type: :utc_datetime)
    end

    create(unique_index(:users, [:email]))

    # User catches table
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

  def down do
    drop(table(:user_catches))
    drop(table(:users))
    Oban.Migration.down(version: 1)
  end
end
