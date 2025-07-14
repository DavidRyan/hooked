defmodule HookedApiWeb.AuthJSON do
  alias HookedApi.Accounts.User

  @doc """
  Renders a user with token.
  """
  def user_with_token(%{user: user, token: token}) do
    %{
      data: %{
        user: data(user),
        token: token
      }
    }
  end

  @doc """
  Renders a user.
  """
  def user(%{user: user}) do
    %{data: data(user)}
  end

  @doc """
  Renders changeset errors.
  """
  def errors(%{changeset: changeset}) do
    %{errors: translate_errors(changeset)}
  end

  defp data(%User{} = user) do
    %{
      id: user.id,
      email: user.email,
      first_name: user.first_name,
      last_name: user.last_name,
      is_active: user.is_active,
      inserted_at: user.inserted_at,
      updated_at: user.updated_at
    }
  end

  defp translate_errors(changeset) do
    Ecto.Changeset.traverse_errors(changeset, &translate_error/1)
  end

  defp translate_error({msg, opts}) do
    Regex.replace(~r"%{(\w+)}", msg, fn _, key ->
      opts |> Keyword.get(String.to_existing_atom(key), key) |> to_string()
    end)
  end
end