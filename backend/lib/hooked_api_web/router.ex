defmodule HookedApiWeb.Router do
  use HookedApiWeb, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  pipeline :authenticated do
    plug :accepts, ["json"]
    plug HookedApiWeb.Plugs.AuthPlug
  end

  # Public routes (no authentication required)
  scope "/api/auth", HookedApiWeb do
    pipe_through :api

    post "/register", AuthController, :register
    post "/login", AuthController, :login
  end

  # Protected routes (authentication required)
  scope "/api", HookedApiWeb do
    pipe_through :authenticated

    # Auth routes that require authentication
    get "/auth/me", AuthController, :me
    post "/auth/refresh", AuthController, :refresh

    # User catches routes
    resources "/user_catches", UserCatchController, except: [:new, :edit]
  end
end
