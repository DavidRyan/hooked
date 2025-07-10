defmodule Backend.Repo.Migrations.CreateUserCatches do
  use Ecto.Migration

  def change do
    create table(:user_catches, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :species, :string
      add :location, :string
      add :latitude, :float
      add :longitude, :float
      add :caught_at, :naive_datetime
      add :notes, :text
      add :weather_data, :map

      timestamps(type: :utc_datetime)
    end
  end
end