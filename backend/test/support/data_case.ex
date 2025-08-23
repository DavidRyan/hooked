defmodule HookedApi.DataCase do
  @moduledoc """
  This module defines the test case to be used by
  tests that require setting up a connection to the database.

  Finally, if the test case interacts with the database,
  we enable the SQL sandbox, so changes done to the database
  during a test are reverted at the end of the test.

  If you are using PostgreSQL, you can even run database
  tests asynchronously by setting `use HookedApi.DataCase,
  async: true`, although this option is not recommended for
  other databases.
  """

  use ExUnit.CaseTemplate

  using do
    quote do
      alias HookedApi.Repo

      import Ecto
      import Ecto.Changeset
      import Ecto.Query
      import HookedApi.DataCase
      import HookedApi.Factory
    end
  end

  setup tags do
    HookedApi.DataCase.setup_sandbox(tags)
    :ok
  end

  @doc """
  Sets up the sandbox based on the test tags.
  """
  def setup_sandbox(tags) do
    pid = Ecto.Adapters.SQL.Sandbox.start_owner!(HookedApi.Repo, shared: not tags[:async])
    on_exit(fn -> Ecto.Adapters.SQL.Sandbox.stop_owner(pid) end)
  end

  @doc """
  A helper that transforms changeset errors into a map of messages.

      assert {:error, changeset} = Accounts.create_user(%{password: "short"})
      assert "password is too short" in errors_on(changeset).password
      assert %{password: ["password is too short"]} = errors_on(changeset)

  """
  def errors_on(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {message, opts} ->
      Regex.replace(~r"%{(\w+)}", message, fn _, key ->
        opts |> Keyword.get(String.to_existing_atom(key), key) |> to_string()
      end)
    end)
  end
end
