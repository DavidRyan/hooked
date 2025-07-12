defmodule HookedApi.Repo.Migrations.AddImageFieldsToUserCatches do
  use Ecto.Migration

  def change do
    alter table(:user_catches) do
      add :image_url, :string
      add :image_filename, :string
      add :image_content_type, :string
      add :image_file_size, :integer
      add :exif_data, :map
    end

    create index(:user_catches, [:image_url])
  end
end