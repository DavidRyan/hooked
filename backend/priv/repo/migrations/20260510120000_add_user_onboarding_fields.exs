defmodule HookedApi.Repo.Migrations.AddUserOnboardingFields do
  use Ecto.Migration

  def up do
    alter table(:users) do
      add(:home_lat, :float)
      add(:home_lng, :float)
      add(:target_species, {:array, :string}, default: [])
      add(:onboarding_completed, :boolean, default: false, null: false)
    end
  end

  def down do
    alter table(:users) do
      remove(:home_lat)
      remove(:home_lng)
      remove(:target_species)
      remove(:onboarding_completed)
    end
  end
end
