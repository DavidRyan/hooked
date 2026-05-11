defmodule HookedApiWeb.InsightsController do
  use HookedApiWeb, :controller

  alias HookedApi.Services.RibbonInsightService

  @moduledoc """
  Lightweight insights surfaced in the mobile timeline ribbon.

  Distinct from `AiController.get_insights/2` which returns a long AI-generated
  blob for the stats screen. This endpoint returns a short structured
  `{headline, body}` pair tailored for a one-line glance — AI-generated when
  available, with a deterministic fallback inside the service.

  Responses are cached per-user-per-day inside the service.
  """

  def ribbon(conn, _params) do
    user = conn.assigns[:current_user]
    catches = HookedApi.Catches.list_user_catches(user.id)
    payload = RibbonInsightService.get(user.id, catches)
    json(conn, payload)
  end
end
