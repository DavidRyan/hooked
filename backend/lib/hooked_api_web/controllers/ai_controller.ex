defmodule HookedApiWeb.AiController do
  use HookedApiWeb, :controller
  alias HookedApi.Services.FishingInsightsService

  def get_insights(conn, %{"user_id" => user_id}) do
    catches = HookedApi.Catches.list_user_catches(user_id)
    insights = FishingInsightsService.get_insights(catches)
    json(conn, %{insights: insights})
  end

end
