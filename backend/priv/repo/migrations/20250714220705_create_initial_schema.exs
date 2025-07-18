defmodule HookedApi.Repo.Migrations.CreateInitialSchema do
  use Ecto.Migration

  def change do
    # Create users table with authentication and security features
    create table(:users, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :email, :string, null: false
      add :password_hash, :string, null: false
      add :first_name, :string
      add :last_name, :string
      add :is_active, :boolean, default: true, null: false
      
      # Account lockout fields
      add :failed_login_attempts, :integer, default: 0, null: false
      add :locked_until, :utc_datetime
      add :last_failed_login, :utc_datetime

      timestamps(type: :utc_datetime)
    end

    create unique_index(:users, [:email])

    # Create user_catches table with enrichment data
    create table(:user_catches, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :species, :string, null: false
      add :location, :string, null: false
      add :latitude, :float
      add :longitude, :float
      add :caught_at, :naive_datetime, null: false
      add :notes, :text
      
      # Enrichment data fields
      add :weather_data, :map
      add :exif_data, :map
      
      # Image fields
      add :image_url, :string
      add :image_filename, :string
      add :image_content_type, :string
      add :image_file_size, :integer

      timestamps(type: :utc_datetime)
    end

    create index(:user_catches, [:species])
    create index(:user_catches, [:location])
    create index(:user_catches, [:caught_at])
    create index(:user_catches, [:latitude, :longitude])
    create index(:user_catches, [:image_url])

    # Add Oban jobs table for background processing
    Oban.Migration.up(version: 12)
  end
end
