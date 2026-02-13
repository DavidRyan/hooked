defmodule HookedApiWeb.UserSkunkController do
  use HookedApiWeb, :controller

  alias HookedApi.Skunks

  def index(conn, _params) do
    user_skunks = Skunks.list_user_skunks(conn.assigns[:current_user].id)
    json(conn, %{user_skunks: user_skunks})
  end

  def create(conn, %{"user_skunk" => skunk_params}) do
    case Skunks.create_user_skunk(conn.assigns[:current_user].id, skunk_params) do
      {:ok, user_skunk} ->
        conn |> put_status(:created) |> json(%{user_skunk: user_skunk})

      {:error, %Ecto.Changeset{} = changeset} ->
        conn |> put_status(:unprocessable_entity) |> json(%{errors: changeset_errors(changeset)})

      {:error, reason} ->
        conn |> put_status(:unprocessable_entity) |> json(%{error: format_error(reason)})
    end
  end

  def create(conn, _params) do
    conn |> put_status(:bad_request) |> json(%{error: "Missing user_skunk parameters"})
  end

  defp format_error(reason) when is_atom(reason), do: "Error: #{reason}"
  defp format_error(reason), do: "Error: #{inspect(reason)}"

  defp changeset_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Regex.replace(~r"%{(\w+)}", msg, fn _, key ->
        opts |> Keyword.get(String.to_existing_atom(key), key) |> to_string()
      end)
    end)
  end
end
