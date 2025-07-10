defmodule HookedApiWeb.Router do
  use HookedApiWeb, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/api", HookedApiWeb do
    pipe_through :api

    resources "/user_catches", UserCatchController, except: [:new, :edit]
  end
end
