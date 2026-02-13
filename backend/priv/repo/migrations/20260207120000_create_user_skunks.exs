defmodule HookedApi.Repo.Migrations.CreateUserSkunks do
  use Ecto.Migration

  def up do
    create table(:user_skunks, primary_key: false) do
      add(:id, :binary_id, primary_key: true)
      add(:user_id, references(:users, on_delete: :delete_all, type: :binary_id), null: false)
      add(:location, :string)
      add(:latitude, :float)
      add(:longitude, :float)
      add(:fished_at, :naive_datetime)
      add(:notes, :text)
      add(:weather_data, :map)
      add(:enrichment_status, :boolean, default: false)

      timestamps(type: :utc_datetime)
    end

    create(index(:user_skunks, [:user_id]))
  end

  def down do
    drop(table(:user_skunks))
  end
end
