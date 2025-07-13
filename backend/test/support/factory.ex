defmodule HookedApi.Factory do
  use ExMachina.Ecto, repo: HookedApi.Repo

  alias HookedApi.Catches.UserCatch

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
      image_url: "https://storage.example.com/image.jpg",
      image_filename: "test_image.jpg",
      image_content_type: "image/jpeg",
      image_file_size: 1024
    }
  end
end