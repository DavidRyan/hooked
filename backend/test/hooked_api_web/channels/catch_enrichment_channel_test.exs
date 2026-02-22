defmodule HookedApiWeb.CatchEnrichmentChannelTest do
  use HookedApiWeb.ChannelCase, async: true

  alias HookedApi.Auth.Token

  setup do
    user = insert(:user)
    {:ok, token, _claims} = Token.generate_and_sign_for_user(user.id)

    {:ok, %{user: user, token: token}}
  end

  test "joins channel with valid token", %{user: user, token: token} do
    {:ok, socket} = connect(HookedApiWeb.UserSocket, %{"token" => token})

    {:ok, _, socket} =
      subscribe_and_join(
        socket,
        HookedApiWeb.CatchEnrichmentChannel,
        "catch_enrichment:#{user.id}"
      )

    assert socket.assigns.current_user_id == user.id
  end

  test "rejects join when topic user does not match token", %{token: token} do
    random_user_id = Ecto.UUID.generate()

    {:ok, socket} = connect(HookedApiWeb.UserSocket, %{"token" => token})

    assert {:error, %{reason: "unauthorized"}} =
             subscribe_and_join(
               socket,
               HookedApiWeb.CatchEnrichmentChannel,
               "catch_enrichment:#{random_user_id}"
             )
  end

  test "forwards broadcasted events to subscriber", %{user: user, token: token} do
    {:ok, socket} = connect(HookedApiWeb.UserSocket, %{"token" => token})

    {:ok, _, _socket} =
      subscribe_and_join(
        socket,
        HookedApiWeb.CatchEnrichmentChannel,
        "catch_enrichment:#{user.id}"
      )

    catch_id = Ecto.UUID.generate()

    HookedApi.Endpoint.broadcast(
      "catch_enrichment:#{user.id}",
      "enrichment_completed",
      %{catch_id: catch_id}
    )

    assert_push("enrichment_completed", %{catch_id: ^catch_id})
  end
end
