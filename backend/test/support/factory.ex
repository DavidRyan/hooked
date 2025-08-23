defmodule HookedApi.Factory do
  use ExMachina.Ecto, repo: HookedApi.Repo

  alias HookedApi.Accounts.User
  alias HookedApi.Catches.UserCatch

  def user_factory do
    %User{
      email: sequence(:email, &"user#{&1}@example.com"),
      password_hash: Bcrypt.hash_pwd_salt("password123"),
      first_name: "Test",
      last_name: "User",
      is_active: true,
      failed_login_attempts: 0
    }
  end

  def user_catch_factory do
    %UserCatch{
      species: "Largemouth Bass",
      location: "Lake Michigan",
      latitude: 42.3601,
      longitude: -87.6298,
      caught_at: ~N[2024-01-15 10:30:00],
      notes: "Great catch!",
      weather_data: %{},
      exif_data: %{},
      image_url: "/uploads/catches/test_image.jpg",
      image_filename: "test_image.jpg",
      image_content_type: "image/jpeg",
      image_file_size: 1024
    }
  end

  def image_upload_factory do
    %Plug.Upload{
      path: Path.join([File.cwd!(), "test_image.jpg"]),
      content_type: "image/jpeg",
      filename: "test_image.jpg"
    }
  end
end
