defmodule HookedApi.Enrichers.Species.Providers.FishialProvider do
  @moduledoc """
  Fishial.AI species identification provider.

  Flow (mirrors official dev API examples):
  1) Exchange client_id/client_secret for a bearer token
  2) Request a direct-upload URL
  3) PUT the image to the direct-upload URL with md5 checksum
  4) Call recognition endpoint with the signed-id
  """

  @behaviour HookedApi.Enrichers.Species.SpeciesProvider

  require Logger
  use Tesla

  alias HookedApi.Enrichers.Species.SpeciesResult

  @auth_base "https://api-users.fishial.ai"
  @api_base "https://api.fishial.ai"

  plug(Tesla.Middleware.JSON)

  adapter(Tesla.Adapter.Hackney,
    timeout: 60_000,
    recv_timeout: 120_000,
    ssl_options: [verify: :verify_peer, cacerts: :certifi.cacerts()]
  )

  @impl true
  def validate_configuration do
    case {System.get_env("FISHIAL_CLIENT_ID"), System.get_env("FISHIAL_CLIENT_SECRET")} do
      {id, secret}
      when is_binary(id) and byte_size(id) > 3 and is_binary(secret) and byte_size(secret) > 8 ->
        :ok

      _ ->
        Logger.error("FishialProvider: Missing FISHIAL_CLIENT_ID or FISHIAL_CLIENT_SECRET")
        {:error, :missing_credentials}
    end
  end

  @impl true
  def identify_species(image_path, filename, _options \\ []) when is_binary(image_path) do
    Logger.info("FishialProvider: Starting species identification")

    with {:ok, client_id, client_secret} <- fetch_credentials(),
         {:ok, file_info} <- build_file_info(image_path, filename),
         {:ok, token} <- fetch_token(client_id, client_secret),
         {:ok, upload_info} <- request_upload(file_info, token),
         :ok <- upload_file(image_path, file_info.checksum, upload_info),
         {:ok, response} <- run_recognition(upload_info.signed_id, token),
         {:ok, species_result} <- parse_recognition(response) do
      {:ok, species_result}
    else
      {:error, reason} ->
        Logger.error("FishialProvider: Identification failed: #{inspect(reason)}")
        {:error, reason}
    end
  end

  def identify_species(_data, _filename, _options), do: {:error, :invalid_image_input}

  def provider_name, do: "fishial"

  defp fetch_credentials do
    case {System.get_env("FISHIAL_CLIENT_ID"), System.get_env("FISHIAL_CLIENT_SECRET")} do
      {id, secret} when is_binary(id) and is_binary(secret) -> {:ok, id, secret}
      _ -> {:error, :missing_credentials}
    end
  end

  defp build_file_info(path, filename) do
    case File.stat(path) do
      {:ok, stat} ->
        checksum =
          path
          |> File.read!()
          |> then(&:crypto.hash(:md5, &1))
          |> Base.encode64()

        content_type = MIME.from_path(path) || "application/octet-stream"

        {:ok,
         %{
           filename: filename,
           content_type: content_type,
           byte_size: stat.size,
           checksum: checksum
         }}

      {:error, reason} ->
        {:error, {:file_error, reason}}
    end
  end

  defp fetch_token(client_id, client_secret) do
    Logger.debug("FishialProvider: Requesting auth token")

    case post(@auth_base <> "/v1/auth/token", %{
           client_id: client_id,
           client_secret: client_secret
         }) do
      {:ok, %Tesla.Env{status: 200, body: %{"access_token" => token}}} when is_binary(token) ->
        {:ok, token}

      {:ok, %Tesla.Env{status: status, body: body}} ->
        {:error, {:auth_error, status, body}}

      {:error, reason} ->
        {:error, {:network_error, reason}}
    end
  end

  defp request_upload(file_info, token) do
    Logger.debug("FishialProvider: Requesting upload URL")

    headers = auth_headers(token)

    payload = %{
      "blob" => %{
        "filename" => file_info.filename,
        "content_type" => file_info.content_type,
        "byte_size" => file_info.byte_size,
        "checksum" => file_info.checksum
      }
    }

    case post(@api_base <> "/v1/recognition/upload", payload, headers: headers) do
      {:ok,
       %Tesla.Env{
         status: 200,
         body: %{"signed-id" => signed_id, "direct-upload" => direct_upload}
       }}
      when is_binary(signed_id) and is_map(direct_upload) ->
        {:ok,
         %{
           signed_id: signed_id,
           upload_url: Map.get(direct_upload, "url"),
           upload_headers: Map.get(direct_upload, "headers", %{})
         }}

      {:ok, %Tesla.Env{status: status, body: body}} ->
        {:error, {:upload_request_error, status, body}}

      {:error, reason} ->
        {:error, {:network_error, reason}}
    end
  end

  defp upload_file(path, checksum, %{upload_url: url, upload_headers: hdrs})
       when is_binary(url) and is_map(hdrs) do
    Logger.debug("FishialProvider: Uploading image to direct-upload URL")

    provided_headers = Enum.map(hdrs, fn {k, v} -> {k, v} end)

    headers =
      provided_headers
      |> put_header_default("Content-Md5", checksum)
      |> put_header_default("Content-Disposition", "attachment")
      |> maybe_add_content_type(Map.get(hdrs, "Content-Type"))

    case Tesla.put(url, {:file, path}, headers: headers) do
      {:ok, %Tesla.Env{status: status}} when status in 200..299 ->
        :ok

      {:ok, %Tesla.Env{status: status, body: body}} ->
        {:error, {:upload_failed, status, body}}

      {:error, reason} ->
        {:error, {:network_error, reason}}
    end
  end

  defp upload_file(_path, _checksum, _info), do: {:error, :invalid_upload_info}

  defp run_recognition(signed_id, token) do
    Logger.debug("FishialProvider: Requesting recognition for signed id #{signed_id}")

    case get(@api_base <> "/v1/recognition/image",
           query: [q: signed_id],
           headers: auth_headers(token)
         ) do
      {:ok, %Tesla.Env{status: 200, body: body}} ->
        {:ok, body}

      {:ok, %Tesla.Env{status: status, body: body}} ->
        {:error, {:recognition_error, status, body}}

      {:error, reason} ->
        {:error, {:network_error, reason}}
    end
  end

  defp parse_recognition(%{"results" => results} = body) when is_list(results) do
    best_species =
      results
      |> Enum.flat_map(&Map.get(&1, "species", []))
      |> Enum.map(fn species ->
        %{
          name: Map.get(species, "name"),
          accuracy: Map.get(species, "accuracy")
        }
      end)
      |> Enum.reject(fn %{name: name} -> is_nil(name) or name == "" end)
      |> Enum.sort_by(fn %{accuracy: acc} -> acc || 0 end, :desc)
      |> List.first()

    case best_species do
      nil ->
        {:ok, SpeciesResult.no_species_found("fishial", body)}

      %{name: name, accuracy: acc} ->
        {:ok,
         SpeciesResult.new(%{
           common_name: name,
           scientific_name: nil,
           confidence: acc,
           provider: "fishial",
           provider_id: nil,
           taxonomy: nil,
           raw_response: body
         })}
    end
  end

  defp parse_recognition(body), do: {:error, {:invalid_response, body}}

  defp auth_headers(token),
    do: [{"Authorization", "Bearer #{token}"}, {"Accept", "application/json"}]

  defp maybe_add_content_type(headers, nil), do: headers

  defp maybe_add_content_type(headers, content_type),
    do: [{"Content-Type", content_type} | headers]

  defp put_header_default(headers, _key, nil), do: headers

  defp put_header_default(headers, key, value) do
    if Enum.any?(headers, fn {k, _v} -> String.downcase(k) == String.downcase(key) end) do
      headers
    else
      [{key, value} | headers]
    end
  end
end
