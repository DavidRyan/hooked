defmodule HookedApiWeb.AiController do
  use HookedApiWeb, :controller
  alias HookedApi.Services.FishingInsightsService

  def get_insights(conn, _params) do
    user = conn.assigns[:current_user]
    catches = HookedApi.Catches.list_user_catches(user.id)

    case FishingInsightsService.get_insights(catches) do
      {:ok, insights} ->
        json(conn, %{insights: insights})

      {:error, :no_catches} ->
        json(conn, %{
          insights:
            "You haven't logged any catches yet. Start fishing and add your catches to get personalized insights!"
        })

      {:error, reason} ->
        conn
        |> put_status(:internal_server_error)
        |> json(%{error: "Failed to generate insights: #{inspect(reason)}"})
    end
  end
end
