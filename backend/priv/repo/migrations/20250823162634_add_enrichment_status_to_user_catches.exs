defmodule HookedApi.Repo.Migrations.AddEnrichmentStatusToUserCatches do
  use Ecto.Migration

  def change do
    alter table(:user_catches) do
      add(:enrichment_status, :boolean)
    end
  end
end
