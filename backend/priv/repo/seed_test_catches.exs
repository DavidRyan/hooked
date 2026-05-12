# Seeds richly varied fake catches for an existing user, for local UI testing.
#
# Usage:
#   mix run priv/repo/seed_test_catches.exs <email> [count] [--wipe]
#
# Defaults to dryan31@gmail.com and 50 catches if args omitted.
# `--wipe` deletes the user's existing catches before seeding.

alias HookedApi.{Repo, Accounts.User, Catches.UserCatch}
import Ecto.Query, warn: false
require Logger

args = System.argv()

{email, count, wipe?} =
  case args do
    [] -> {"dryan31@gmail.com", 50, false}
    [e] -> {e, 50, false}
    [e, c] -> {e, String.to_integer(c), false}
    [e, c, "--wipe"] -> {e, String.to_integer(c), true}
    [e, "--wipe"] -> {e, 50, true}
    [e, "--wipe", c] -> {e, String.to_integer(c), true}
  end

user =
  case Repo.get_by(User, email: email) do
    nil ->
      IO.puts(:stderr, "No user with email #{email}")
      System.halt(1)

    user ->
      user
  end

if wipe? do
  {n, _} = Repo.delete_all(from c in UserCatch, where: c.user_id == ^user.id)
  IO.puts("Wiped #{n} existing catches for #{user.email}")
end

IO.puts("Seeding #{count} catches for #{user.email} (#{user.id})")

# Real-ish Midwest fishing spots.
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

# Species profiles: relative frequency, preferred hour ranges, preferred month
# ranges, preferred pressure trend, preferred temp range. Used to make catches
# look like the user has real patterns rather than pure noise.
species_profiles = [
  %{
    name: "Largemouth Bass",
    weight: 8,
    hours: [{6, 9}, {18, 21}],
    months: [5, 6, 7, 8, 9, 10],
    pressure_bias: :falling,
    temp_bias: :warm
  },
  %{
    name: "Smallmouth Bass",
    weight: 5,
    hours: [{6, 10}, {17, 20}],
    months: [5, 6, 7, 8, 9, 10],
    pressure_bias: :stable,
    temp_bias: :warm
  },
  %{
    name: "Walleye",
    weight: 6,
    hours: [{4, 7}, {19, 22}, {0, 2}],
    months: [4, 5, 6, 9, 10, 11],
    pressure_bias: :rising,
    temp_bias: :cool
  },
  %{
    name: "Northern Pike",
    weight: 4,
    hours: [{8, 12}, {15, 18}],
    months: [4, 5, 6, 9, 10, 11],
    pressure_bias: :falling,
    temp_bias: :cool
  },
  %{
    name: "Muskellunge",
    weight: 1,
    hours: [{8, 11}, {17, 20}],
    months: [6, 7, 8, 9, 10],
    pressure_bias: :falling,
    temp_bias: :moderate
  },
  %{
    name: "Bluegill",
    weight: 7,
    hours: [{10, 16}],
    months: [5, 6, 7, 8, 9],
    pressure_bias: :stable,
    temp_bias: :warm
  },
  %{
    name: "Crappie",
    weight: 3,
    hours: [{5, 8}, {18, 21}],
    months: [4, 5, 6, 9, 10],
    pressure_bias: :stable,
    temp_bias: :moderate
  },
  %{
    name: "Yellow Perch",
    weight: 3,
    hours: [{7, 11}],
    months: [4, 5, 10, 11, 12, 1, 2],
    pressure_bias: :rising,
    temp_bias: :cool
  },
  %{
    name: "Rainbow Trout",
    weight: 2,
    hours: [{5, 9}, {18, 20}],
    months: [3, 4, 5, 9, 10, 11],
    pressure_bias: :stable,
    temp_bias: :cool
  },
  %{
    name: "Brown Trout",
    weight: 2,
    hours: [{4, 7}, {19, 22}],
    months: [3, 4, 10, 11],
    pressure_bias: :falling,
    temp_bias: :cool
  },
  %{
    name: "Channel Catfish",
    weight: 2,
    hours: [{20, 23}, {0, 3}],
    months: [6, 7, 8],
    pressure_bias: :falling,
    temp_bias: :warm
  }
]

species_bag =
  Enum.flat_map(species_profiles, fn p -> List.duplicate(p, p.weight) end)

# Conditions menu — wider than before. Includes pre/post-frontal flavors that
# correlate with pressure trend.
conditions = %{
  rising: [
    {"clear", 0.4},
    {"sunny", 0.2},
    {"high pressure, cool morning", 0.15},
    {"crisp and clear", 0.15},
    {"breezy and clear", 0.1}
  ],
  falling: [
    {"overcast with falling pressure", 0.25},
    {"thunderstorms building", 0.2},
    {"warm front approaching", 0.15},
    {"humid and still", 0.15},
    {"scattered thunderstorms", 0.15},
    {"drizzle and gusty wind", 0.1}
  ],
  stable: [
    {"partly cloudy", 0.3},
    {"calm and warm", 0.2},
    {"light winds", 0.15},
    {"clear", 0.15},
    {"hazy and humid", 0.1},
    {"overcast", 0.1}
  ]
}

# Winter overrides the above; less variety but realistic.
winter_conditions = [
  "overcast and cold",
  "light snow",
  "snow showers",
  "ice fog",
  "clear and cold",
  "windy and cold"
]

# Approximate seasonal air temps (°F).
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

# Pressure ranges per trend.
pressure_for = fn
  :rising -> {1018, 1030}
  :stable -> {1010, 1020}
  :falling -> {995, 1012}
end

# Pick adjustments based on temp_bias inside the month's seasonal range.
adjust_temp = fn {lo, hi}, bias ->
  case bias do
    :cool -> {lo, lo + (hi - lo) * 0.6}
    :moderate -> {lo + (hi - lo) * 0.2, lo + (hi - lo) * 0.8}
    :warm -> {lo + (hi - lo) * 0.4, hi}
  end
end

rand_in = fn {lo, hi} -> lo + :rand.uniform() * (hi - lo) end
pick = fn list -> Enum.at(list, :rand.uniform(length(list)) - 1) end

# Map our description strings to (rough) OpenWeather codes/main groups so the
# mcp-server tools that read weather.weather[0].main can categorize them.
weather_code_for = fn desc ->
  cond do
    String.contains?(desc, "thunder") or String.contains?(desc, "storm") -> 200
    String.contains?(desc, "drizzle") -> 300
    String.contains?(desc, "rain") or String.contains?(desc, "shower") -> 500
    String.contains?(desc, "snow") -> 600
    String.contains?(desc, "fog") or String.contains?(desc, "mist") or String.contains?(desc, "haze") -> 701
    String.contains?(desc, "clear") or String.contains?(desc, "sunny") or String.contains?(desc, "crisp") -> 800
    String.contains?(desc, "partly cloudy") -> 802
    String.contains?(desc, "overcast") -> 804
    true -> 801
  end
end

main_for = fn desc ->
  cond do
    String.contains?(desc, "thunder") or String.contains?(desc, "storm") -> "Thunderstorm"
    String.contains?(desc, "drizzle") -> "Drizzle"
    String.contains?(desc, "rain") or String.contains?(desc, "shower") -> "Rain"
    String.contains?(desc, "snow") -> "Snow"
    String.contains?(desc, "fog") or String.contains?(desc, "mist") or String.contains?(desc, "haze") -> "Atmosphere"
    String.contains?(desc, "clear") or String.contains?(desc, "sunny") or String.contains?(desc, "crisp") -> "Clear"
    true -> "Clouds"
  end
end

cloud_pct_for = fn desc ->
  cond do
    String.contains?(desc, "clear") or String.contains?(desc, "sunny") or String.contains?(desc, "crisp") -> 5
    String.contains?(desc, "partly cloudy") or String.contains?(desc, "breezy and clear") -> 35
    String.contains?(desc, "overcast") -> 95
    String.contains?(desc, "hazy") -> 60
    String.contains?(desc, "thunder") or String.contains?(desc, "storm") or String.contains?(desc, "rain") -> 85
    String.contains?(desc, "snow") -> 90
    true -> 50
  end
end

weighted_pick = fn pairs ->
  total = Enum.reduce(pairs, 0.0, fn {_, w}, acc -> acc + w end)
  r = :rand.uniform() * total

  Enum.reduce_while(pairs, 0.0, fn {value, w}, acc ->
    next = acc + w
    if r <= next, do: {:halt, value}, else: {:cont, next}
  end)
end

# Time-of-day picker: choose one of the species' preferred hour windows,
# but with 15% chance of a random outlier so it doesn't look too patterned.
pick_hour = fn windows ->
  if :rand.uniform() < 0.15 do
    :rand.uniform(24) - 1
  else
    {lo, hi} = pick.(windows)
    lo + :rand.uniform(max(hi - lo, 1)) - 1
  end
end

pick_month_for_species = fn species ->
  # 80% chance to pick one of the species' preferred months, else random.
  if :rand.uniform() < 0.8 do
    pick.(species.months)
  else
    :rand.uniform(12)
  end
end

# Build a plausible random datetime — biased by species preferences but
# spread across days, hours, and minutes.
random_naive = fn species ->
  today = Date.utc_today()
  month = pick_month_for_species.(species)
  # Pick year: if month is in the future this calendar year, fall back to last year.
  year = if month > today.month, do: today.year - 1, else: today.year
  day = :rand.uniform(28)
  hour = pick_hour.(species.hours) |> rem(24)
  minute = :rand.uniform(60) - 1
  NaiveDateTime.new!(year, month, day, hour, minute, 0)
end

notes_pool = [
  nil, nil, nil, nil,
  "Caught on a chatterbait near weed edge.",
  "Topwater bite right at dawn.",
  "Slow afternoon, finally got one drop-shotting.",
  "Big school under the dock.",
  "Fish were stacked on the breakline.",
  "Slip bobber with a leech.",
  "Crankbait in stained water.",
  "Cast and retrieve in 8 ft of water.",
  "Stormy and they were hammering.",
  "Slow troll along the weed line.",
  "Light bite — barely felt it.",
  "Pre-frontal feeding window."
]

placeholder_image = "https://images.unsplash.com/photo-1499242611767-cf8b9be02854?w=600"

inserted =
  for i <- 1..count do
    species_profile = pick.(species_bag)
    species_name = species_profile.name
    {spot, lat, lng} = pick.(spots)
    caught_at = random_naive.(species_profile)
    month = caught_at.month

    # Choose trend with species bias, but 25% chance of off-bias for noise.
    trend =
      if :rand.uniform() < 0.75 do
        species_profile.pressure_bias
      else
        pick.([:rising, :stable, :falling])
      end

    description =
      cond do
        month in [12, 1, 2] -> pick.(winter_conditions)
        true -> weighted_pick.(conditions[trend])
      end

    {p_lo, p_hi} = pressure_for.(trend)
    pressure = rand_in.({p_lo, p_hi}) |> Float.round(0)

    air_temp = rand_in.(adjust_temp.(air_temp_range.(month), species_profile.temp_bias)) |> Float.round(1)
    water_temp = rand_in.(adjust_temp.(water_temp_range.(month), species_profile.temp_bias)) |> Float.round(1)

    # Wind: gusty during falling pressure, light otherwise.
    wind_speed =
      case trend do
        :falling -> rand_in.({8, 22}) |> Float.round(1)
        :stable -> rand_in.({2, 10}) |> Float.round(1)
        :rising -> rand_in.({4, 14}) |> Float.round(1)
      end

    wind_direction = :rand.uniform(360)
    humidity = rand_in.({30, 95}) |> Float.round(0)

    # Mirror the OpenWeather "Current Weather" JSON shape so the mcp-server
    # analytical tools (which read weather.main.temp, weather.weather[0].description,
    # weather.wind.speed) can actually find data. Also keep a flat top-level mirror
    # for legacy callers / the mobile detail screen.
    weather = %{
      "weather" => [%{
        "id" => weather_code_for.(description),
        "main" => main_for.(description),
        "description" => description,
        "icon" => "01d"
      }],
      "main" => %{
        "temp" => air_temp,
        "feels_like" => air_temp - 2.0,
        "temp_min" => air_temp - 5.0,
        "temp_max" => air_temp + 5.0,
        "pressure" => trunc(pressure),
        "humidity" => trunc(humidity)
      },
      "wind" => %{
        "speed" => wind_speed,
        "deg" => wind_direction
      },
      "clouds" => %{"all" => cloud_pct_for.(description)},
      "visibility" => 10000,
      "dt" => NaiveDateTime.to_erl(caught_at) |> :calendar.datetime_to_gregorian_seconds() |> Kernel.-(62167219200),
      # Flat fields for legacy mobile readers + the stats screen.
      "description" => description,
      "temp" => Float.to_string(air_temp),
      "water_temp" => Float.to_string(water_temp),
      "wind_speed" => Float.to_string(wind_speed),
      "wind_direction" => Integer.to_string(wind_direction),
      "humidity" => :erlang.float_to_binary(humidity, decimals: 0),
      "pressure" => :erlang.float_to_binary(pressure, decimals: 0),
      "pressure_trend" => Atom.to_string(trend)
    }

    jitter_lat = lat + (:rand.uniform() - 0.5) * 0.01
    jitter_lng = lng + (:rand.uniform() - 0.5) * 0.01

    attrs = %{
      species: species_name,
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
