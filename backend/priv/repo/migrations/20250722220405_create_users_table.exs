defmodule HookedApi.Repo.Migrations.CreateUsersTable do
  use Ecto.Migration

  def change do
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
  end
end
