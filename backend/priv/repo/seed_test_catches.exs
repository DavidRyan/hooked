# Seeds fake catches for an existing user, for local UI testing.
#
# Usage:
#   mix run priv/repo/seed_test_catches.exs <email> [count]
#
# Defaults to dryan31@gmail.com and 40 catches if args omitted.

alias HookedApi.{Repo, Accounts.User, Catches.UserCatch}
require Logger

{email, count} =
  case System.argv() do
    [e, c] -> {e, String.to_integer(c)}
    [e] -> {e, 40}
    [] -> {"dryan31@gmail.com", 40}
  end

user =
  case Repo.get_by(User, email: email) do
    nil ->
      IO.puts(:stderr, "No user with email #{email}")
      System.halt(1)

    user ->
      user
  end

IO.puts("Seeding #{count} catches for #{user.email} (#{user.id})")

# Real-ish Midwest fishing spots. Coordinates are approximate.
spots = [
  {"Lake Mendota", 43.1056, -89.4156},
  {"Lake Wingra", 43.0533, -89.4317},
  {"Lake Monona", 43.0594, -89.3608},
  {"Lake Geneva", 42.5919, -88.4334},
  {"Lake Winnebago", 44.0044, -88.4502},
  {"Devil's Lake", 43.4225, -89.7298},
  {"Castle Rock Lake", 44.0411, -89.9559},
  {"Petenwell Lake", 44.1428, -89.9647},
  {"Lake Pepin", 44.4486, -92.2102},
  {"Mississippi River — La Crosse", 43.8014, -91.2396},
  {"Wisconsin River — Wisconsin Dells", 43.6275, -89.7708},
  {"Big Green Lake", 43.8417, -88.9572},
  {"Lake Koshkonong", 42.8722, -88.9683},
  {"Lake Puckaway", 43.7700, -89.1300}
]

# Species with realistic relative frequency weights.
species_pool =
  [
    {"Largemouth Bass", 8},
    {"Smallmouth Bass", 5},
    {"Bluegill", 6},
    {"Crappie", 4},
    {"Yellow Perch", 3},
    {"Walleye", 5},
    {"Northern Pike", 3},
    {"Muskellunge", 1},
    {"Rainbow Trout", 2},
    {"Brown Trout", 2},
    {"Channel Catfish", 2}
  ]
  |> Enum.flat_map(fn {name, w} -> List.duplicate(name, w) end)

notes_pool = [
  nil,
  nil,
  nil,
  "Caught on a chatterbait near weed edge.",
  "Topwater bite right at dawn.",
  "Slow afternoon, finally got one drop-shotting.",
  "Big school under the dock.",
  "Fish were stacked on the breakline.",
  "Slip bobber with a leech.",
  "Crankbait in stained water."
]

descriptions_by_month = %{
  # Rough: snowy/icy early year, transitioning warm and humid in summer, cooling in fall.
  1 => ["overcast", "snow", "light snow", "freezing fog"],
  2 => ["overcast", "snow showers", "clear"],
  3 => ["overcast", "light rain", "partly cloudy", "windy"],
  4 => ["partly cloudy", "light rain", "clear"],
  5 => ["clear", "partly cloudy", "scattered thunderstorms"],
  6 => ["clear", "humid", "thunderstorms", "partly cloudy"],
  7 => ["clear", "hot and humid", "thunderstorms"],
  8 => ["clear", "humid", "thunderstorms", "partly cloudy"],
  9 => ["partly cloudy", "clear", "cool morning"],
  10 => ["overcast", "windy", "light rain", "partly cloudy"],
  11 => ["overcast", "light rain", "windy", "snow showers"],
  12 => ["overcast", "snow", "clear and cold"]
}

# Approximate seasonal temps (°F) and water temps.
air_temp_range = fn
  m when m in [12, 1, 2] -> {15, 35}
  m when m in [3, 11] -> {30, 55}
  m when m in [4, 10] -> {45, 70}
  m when m in [5, 9] -> {55, 80}
  m when m in [6, 7, 8] -> {65, 92}
end

water_temp_range = fn
  m when m in [12, 1, 2] -> {33, 38}
  m when m in [3, 11] -> {38, 50}
  m when m in [4, 10] -> {48, 62}
  m when m in [5, 9] -> {58, 72}
  m when m in [6, 7, 8] -> {68, 82}
end

rand_in = fn {lo, hi} -> lo + :rand.uniform() * (hi - lo) end

pick = fn list -> Enum.at(list, :rand.uniform(length(list)) - 1) end

now = NaiveDateTime.utc_now() |> NaiveDateTime.truncate(:second)
# Bias caught_at over the past year, weighted toward May-Oct.
random_naive_in_past_year = fn ->
  # Weighted month picker for plausibility
  month_weights = [
    {1, 1}, {2, 1}, {3, 2}, {4, 4}, {5, 8}, {6, 10},
    {7, 12}, {8, 11}, {9, 9}, {10, 6}, {11, 2}, {12, 1}
  ]

  bag = Enum.flat_map(month_weights, fn {m, w} -> List.duplicate(m, w) end)
  target_month = pick.(bag)

  today = Date.utc_today()
  target_year = if target_month > today.month, do: today.year - 1, else: today.year
  day = :rand.uniform(28)
  hour = 5 + :rand.uniform(15)
  minute = :rand.uniform(59)

  NaiveDateTime.new!(target_year, target_month, day, hour, minute, 0)
end

placeholder_image = "https://images.unsplash.com/photo-1499242611767-cf8b9be02854?w=600"

inserted =
  for i <- 1..count do
    {spot, lat, lng} = pick.(spots)
    species = pick.(species_pool)
    caught_at = random_naive_in_past_year.()
    month = caught_at.month

    description = pick.(descriptions_by_month[month])
    air_temp = rand_in.(air_temp_range.(month)) |> Float.round(1)
    water_temp = rand_in.(water_temp_range.(month)) |> Float.round(1)
    wind_speed = rand_in.({2, 18}) |> Float.round(1)
    wind_direction = :rand.uniform(360)
    humidity = rand_in.({40, 90}) |> Float.round(0)
    pressure = rand_in.({1000, 1028}) |> Float.round(0)

    weather = %{
      "description" => description,
      "temp" => Float.to_string(air_temp),
      "water_temp" => Float.to_string(water_temp),
      "wind_speed" => Float.to_string(wind_speed),
      "wind_direction" => Integer.to_string(wind_direction),
      "humidity" => :erlang.float_to_binary(humidity, decimals: 0),
      "pressure" => :erlang.float_to_binary(pressure, decimals: 0)
    }

    # Small offset so identical-spot catches aren't all at the same pixel.
    jitter_lat = lat + (:rand.uniform() - 0.5) * 0.01
    jitter_lng = lng + (:rand.uniform() - 0.5) * 0.01

    attrs = %{
      species: species,
      location: spot,
      latitude: jitter_lat,
      longitude: jitter_lng,
      caught_at: caught_at,
      notes: pick.(notes_pool),
      weather_data: weather,
      image_url: placeholder_image,
      image_content_type: "image/jpeg",
      enrichment_status: true,
      user_id: user.id
    }

    %UserCatch{}
    |> UserCatch.changeset(attrs)
    |> Repo.insert()
    |> case do
      {:ok, _} ->
        if rem(i, 10) == 0, do: IO.puts("  inserted #{i}/#{count}")
        :ok

      {:error, cs} ->
        IO.puts(:stderr, "  failed at #{i}: #{inspect(cs.errors)}")
        :error
    end
  end

ok = Enum.count(inserted, &(&1 == :ok))
IO.puts("Done. #{ok}/#{count} catches inserted.")
