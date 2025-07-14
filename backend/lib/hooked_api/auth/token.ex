defmodule HookedApi.Auth.Token do
  use Joken.Config

  @impl Joken.Config
  def token_config do
    default_claims(default_exp: 60 * 60 * 24 * 7) # 7 days
    |> add_claim("iss", fn -> "hooked_api" end)
    |> add_claim("aud", fn -> "hooked_app" end)
  end

  def generate_and_sign_for_user(user_id) when is_binary(user_id) do
    extra_claims = %{
      "sub" => user_id,
      "user_id" => user_id
    }
    
    generate_and_sign(extra_claims)
  end

  def verify_token(token) do
    case verify_and_validate(token, get_signer()) do
      {:ok, claims} -> {:ok, claims}
      {:error, reason} -> {:error, reason}
    end
  end

  def get_user_id_from_token(token) do
    case verify_token(token) do
      {:ok, %{"user_id" => user_id}} -> {:ok, user_id}
      {:ok, %{"sub" => user_id}} -> {:ok, user_id}
      {:error, reason} -> {:error, reason}
    end
  end

  defp get_signer do
    secret = Application.get_env(:hooked_api, :jwt_secret) || 
             raise "JWT secret not configured. Set :jwt_secret in config."
    Joken.Signer.create("HS256", secret)
  end
end