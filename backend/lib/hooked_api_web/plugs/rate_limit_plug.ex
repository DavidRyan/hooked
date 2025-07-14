defmodule HookedApiWeb.Plugs.RateLimitPlug do
  @moduledoc """
  Rate limiting plug to prevent brute force attacks.
  """

  import Plug.Conn
  import Phoenix.Controller

  def init(opts) do
    %{
      max_requests: Keyword.get(opts, :max_requests, 5),
      window_ms: Keyword.get(opts, :window_ms, 60_000), # 1 minute
      bucket_name: Keyword.get(opts, :bucket_name, "auth")
    }
  end

  def call(conn, opts) do
    identifier = get_identifier(conn)
    bucket_name = "#{opts.bucket_name}:#{identifier}"

    case Hammer.check_rate(bucket_name, opts.window_ms, opts.max_requests) do
      {:allow, _count} ->
        conn

      {:deny, _limit} ->
        conn
        |> put_status(:too_many_requests)
        |> json(%{
          error: "Too many requests. Please try again later.",
          retry_after: div(opts.window_ms, 1000)
        })
        |> halt()
    end
  end

  defp get_identifier(conn) do
    # Use IP address as identifier
    case get_peer_data(conn) do
      %{address: address} -> 
        address |> :inet.ntoa() |> to_string()
      _ -> 
        "unknown"
    end
  end
end