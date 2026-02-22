defmodule HookedApiWeb.ChannelCase do
  @moduledoc """
  Test case for channel tests with database sandbox support.
  """

  use ExUnit.CaseTemplate

  using do
    quote do
      import Phoenix.ChannelTest

      @endpoint HookedApi.Endpoint

      import HookedApi.Factory
    end
  end

  setup tags do
    HookedApi.DataCase.setup_sandbox(tags)
    :ok
  end
end
