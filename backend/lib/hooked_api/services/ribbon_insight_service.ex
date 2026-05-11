defmodule HookedApi.Services.RibbonInsightService do
  @moduledoc """
  Generates a short, glance-worthy `{headline, body}` insight for the timeline
  ribbon, using the configured AI provider over a compact stats summary of the
  user's catches.

  Cached per-user per-day via `:persistent_term` so we don't re-hit the AI on
  every screen open. Falls back to a deterministic insight if the AI call fails.
  """

  require Logger
  alias HookedApi.Services.FishingInsightsService

  @ai_provider Application.compile_env(
                 :hooked_api,
                 :ai_provider,
                 HookedApi.Services.AiProviders.OpenaiProvider
               )

  def get(user_id, catches) do
    today = Date.utc_today()

    case cached(user_id) do
      {^today, payload} ->
        payload

      _ ->
        payload = generate(catches)
        put_cache(user_id, today, payload)
        payload
    end
  end

  @doc "Force-refresh the cache for a user. Call after a catch is created."
  def invalidate(user_id) do
    :persistent_term.erase(key(user_id))
    :ok
  end

  # --- internals -------------------------------------------------------------

  defp generate([]), do: deterministic_fallback([])

  defp generate(catches) do
    with :ok <- @ai_provider.validate_configuration(),
         prompt <- build_prompt(catches),
         {:ok, raw} <- @ai_provider.send_message(prompt),
         {:ok, parsed} <- parse_json(raw),
         %{"headline" => h, "body" => b} when is_binary(h) and is_binary(b) <- parsed do
      %{
        headline: trim_to(h, 60),
        body: trim_to(b, 120)
      }
    else
      err ->
        Logger.warning("Ribbon AI generation failed, falling back: #{inspect(err)}")
        deterministic_fallback(catches)
    end
  end

  defp build_prompt(catches) do
    summary = summarize(catches)

    """
    You are a fishing insights writer. Given a user's catch history summary, output ONE short, useful, glance-worthy insight as strict JSON.

    Format: {"headline":"<6 words max>","body":"<14 words max>"}

    Rules:
    - Be specific, actionable, and personal. Reference the user's actual data (species, spots, patterns).
    - Don't just echo numbers — interpret them. Surface patterns, suggestions, or surprising facts.
    - No greetings, no preamble, no trailing notes. Just the JSON object.
    - No code fences. No commentary.

    Examples of good output:
    {"headline":"You're a Bass angler","body":"35% of your log. Try walleye for variety next month."}
    {"headline":"Petenwell is your money spot","body":"6 of 10 recent catches there. Stay on the weed edges."}
    {"headline":"Time to chase walleye","body":"You haven't logged one in 6 weeks. Low-light hours work best."}

    User data:
    #{summary}

    Output the JSON object only.
    """
  end

  defp summarize(catches) do
    total = length(catches)

    species_counts =
      catches
      |> Enum.map(& &1.species)
      |> Enum.reject(&is_nil/1)
      |> Enum.frequencies()
      |> Enum.sort_by(fn {_, c} -> -c end)
      |> Enum.take(5)
      |> Enum.map(fn {s, c} -> "#{s} (#{c})" end)
      |> Enum.join(", ")

    location_counts =
      catches
      |> Enum.map(& &1.location)
      |> Enum.reject(&is_nil/1)
      |> Enum.frequencies()
      |> Enum.sort_by(fn {_, c} -> -c end)
      |> Enum.take(5)
      |> Enum.map(fn {l, c} -> "#{l} (#{c})" end)
      |> Enum.join(", ")

    today = Date.utc_today()
    week_ago = Date.add(today, -7)
    month_ago = Date.add(today, -30)

    last_week =
      catches
      |> Enum.count(fn c ->
        c.caught_at && Date.compare(NaiveDateTime.to_date(c.caught_at), week_ago) != :lt
      end)

    last_month =
      catches
      |> Enum.count(fn c ->
        c.caught_at && Date.compare(NaiveDateTime.to_date(c.caught_at), month_ago) != :lt
      end)

    most_recent =
      catches
      |> Enum.filter(& &1.caught_at)
      |> Enum.max_by(& &1.caught_at, NaiveDateTime, fn -> nil end)

    recent_line =
      case most_recent do
        nil ->
          "Most recent: none"

        c ->
          days = Date.diff(today, NaiveDateTime.to_date(c.caught_at))
          "Most recent: #{c.species || "Unknown"} at #{c.location || "Unknown"}, #{days} days ago"
      end

    best_day =
      catches
      |> Enum.filter(& &1.caught_at)
      |> Enum.group_by(&NaiveDateTime.to_date(&1.caught_at))
      |> Enum.max_by(fn {_d, list} -> length(list) end, fn -> {nil, []} end)

    best_day_line =
      case best_day do
        {nil, _} -> "Best day: none"
        {date, list} -> "Best day: #{length(list)} catches on #{date}"
      end

    """
    Total catches: #{total}
    Top species: #{species_counts}
    Top spots: #{location_counts}
    Catches last 7 days: #{last_week}
    Catches last 30 days: #{last_month}
    #{recent_line}
    #{best_day_line}
    Today's date: #{today} (month: #{Calendar.strftime(today, "%B")})
    """
  end

  defp parse_json(raw) do
    cleaned =
      raw
      |> String.trim()
      |> strip_code_fences()

    case Jason.decode(cleaned) do
      {:ok, map} when is_map(map) -> {:ok, map}
      _ -> extract_json_object(cleaned)
    end
  end

  # If the model wrapped output in code fences despite instructions, strip them.
  defp strip_code_fences(s) do
    s
    |> String.replace(~r/\A```(?:json)?\s*/, "")
    |> String.replace(~r/\s*```\z/, "")
    |> String.trim()
  end

  # Best-effort: find the first {...} block in the string and parse it.
  defp extract_json_object(s) do
    case Regex.run(~r/\{[^{}]*\}/, s) do
      [match] -> Jason.decode(match)
      _ -> {:error, :no_json}
    end
  end

  defp trim_to(s, max) when is_binary(s) do
    s = String.trim(s)
    if String.length(s) <= max, do: s, else: String.slice(s, 0, max - 1) <> "…"
  end

  defp trim_to(_, _), do: ""

  # --- cache -----------------------------------------------------------------

  defp key(user_id), do: {:ribbon_insight, user_id}

  defp cached(user_id) do
    try do
      :persistent_term.get(key(user_id))
    rescue
      ArgumentError -> nil
    end
  end

  defp put_cache(user_id, date, payload) do
    :persistent_term.put(key(user_id), {date, payload})
  end

  # --- fallback --------------------------------------------------------------
  # Kept here so the controller can stay thin and the AI path remains optional.

  defp deterministic_fallback([]),
    do: %{
      headline: "Welcome to Hooked",
      body: "Log your first catch to start seeing patterns."
    }

  defp deterministic_fallback(catches) do
    latest =
      catches
      |> Enum.filter(& &1.caught_at)
      |> Enum.max_by(& &1.caught_at, NaiveDateTime, fn -> nil end)

    case latest do
      nil ->
        %{headline: "Tight lines", body: "Log a catch to start seeing patterns."}

      c ->
        %{
          headline: c.species || "Recent catch",
          body: "Last caught at #{c.location || "an unknown spot"}."
        }
    end
  end
end
