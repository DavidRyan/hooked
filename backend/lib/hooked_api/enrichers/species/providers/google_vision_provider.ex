defmodule HookedApi.Enrichers.Species.Providers.GoogleVisionProvider do
  @moduledoc """
  Google Cloud Vision API provider for species identification.

  Uses Google Cloud Vision's label detection to identify animals and objects.
  Note: This is a basic implementation - Google Vision is not specialized for species
  identification like iNaturalist, but serves as an example of the abstraction layer.

  API Documentation: https://cloud.google.com/vision/docs/labels
  """

  @behaviour HookedApi.Enrichers.Species.SpeciesProvider

  use Tesla
  require Logger

  alias HookedApi.Enrichers.Species.SpeciesResult

  plug(Tesla.Middleware.JSON)
  plug(Tesla.Middleware.Logger)
  plug(Tesla.Middleware.BaseUrl, "https://vision.googleapis.com/v1")

  plug(Tesla.Middleware.Headers, [
    {"Authorization", "Bearer #{Application.get_env(:hooked_api, :google_vision_access_token)}"}
  ])

  adapter(Tesla.Adapter.Hackney)

  @impl true
  def validate_configuration do
    token = Application.get_env(:hooked_api, :google_vision_access_token)

    case token do
      nil ->
        Logger.warning("GoogleVisionProvider: No Google Vision API token configured")
        {:error, :no_api_key}

      token when is_binary(token) and byte_size(token) > 10 ->
        Logger.debug(
          "GoogleVisionProvider: API token configured (#{byte_size(token)} characters)"
        )

        :ok

      invalid ->
        Logger.error("GoogleVisionProvider: Invalid API token configuration: #{inspect(invalid)}")
        {:error, :invalid_api_key}
    end
  end

  @impl true
  def identify_species(image_data, _filename) do
    Logger.info("GoogleVisionProvider: Starting species identification")
    Logger.debug("GoogleVisionProvider: Processing image file (#{byte_size(image_data)} bytes)")

    try do
      with request_body <- build_vision_request(image_data),
           _ <- Logger.info("GoogleVisionProvider: Sending image to Google Vision API"),
           {:ok, %Tesla.Env{status: 200, body: body} = response} <-
             post("/images:annotate", request_body) do
        Logger.info(
          "GoogleVisionProvider: Received identification results from Google Vision API"
        )

        Logger.debug(
          "GoogleVisionProvider: Raw API response: #{inspect(body, pretty: true, limit: :infinity)}"
        )

        parse_response(body, response)
      else
        {:ok, %Tesla.Env{status: status, body: body}} when status != 200 ->
          Logger.error("GoogleVisionProvider: Google Vision API returned error status: #{status}")
          Logger.error("GoogleVisionProvider: Error response body: #{inspect(body)}")
          {:error, {:api_error, status, body}}

        {:error, %Tesla.Env{status: status, body: body}} ->
          Logger.error("GoogleVisionProvider: Tesla HTTP error - status: #{status}")
          Logger.error("GoogleVisionProvider: Error response body: #{inspect(body)}")
          {:error, {:http_error, status, body}}

        {:error, reason} ->
          Logger.error(
            "GoogleVisionProvider: Failed to communicate with Google Vision API: #{inspect(reason)}"
          )

          {:error, {:network_error, reason}}
      end
    rescue
      error ->
        Logger.error(
          "GoogleVisionProvider: CRASH during species identification: #{inspect(error)}"
        )

        Logger.error(
          "GoogleVisionProvider: Stacktrace: #{Exception.format_stacktrace(__STACKTRACE__)}"
        )

        {:error, {:crash, error}}
    end
  end

  defp build_vision_request(image_data) do
    encoded_image = Base.encode64(image_data)

    %{
      "requests" => [
        %{
          "image" => %{
            "content" => encoded_image
          },
          "features" => [
            %{
              "type" => "LABEL_DETECTION",
              "maxResults" => 10
            }
          ]
        }
      ]
    }
  end

  defp parse_response(%{"responses" => [response | _]}, full_response) do
    case response do
      %{"labelAnnotations" => labels} when is_list(labels) ->
        Logger.debug("GoogleVisionProvider: Number of labels: #{length(labels)}")
        find_best_species_match(labels, full_response.body)

      %{"error" => error} ->
        Logger.error("GoogleVisionProvider: API error in response: #{inspect(error)}")
        {:error, {:api_error, "response_error", error}}

      _ ->
        Logger.info("GoogleVisionProvider: No labels detected in image")
        {:ok, SpeciesResult.no_species_found("google_vision", full_response.body)}
    end
  end

  defp parse_response(body, response) do
    Logger.error("GoogleVisionProvider: Unexpected response format")
    Logger.debug("GoogleVisionProvider: Response body: #{inspect(body)}")
    {:error, {:invalid_response, "Unexpected response format", response.body}}
  end

  # Simple heuristic to find animal/fish species from Google Vision labels
  # This is a basic example - in practice you'd want more sophisticated matching
  defp find_best_species_match(labels, raw_response) do
    # Look for labels that might indicate fish or animal species
    fish_keywords = ["fish", "bass", "trout", "salmon", "tuna", "cod", "shark", "ray"]
    animal_keywords = ["animal", "vertebrate", "marine", "aquatic"]

    species_labels =
      labels
      |> Enum.filter(fn label ->
        description = String.downcase(label["description"] || "")
        Enum.any?(fish_keywords ++ animal_keywords, &String.contains?(description, &1))
      end)
      |> Enum.sort_by(& &1["score"], :desc)

    case species_labels do
      [best_label | _] ->
        species_result =
          SpeciesResult.new(%{
            common_name: best_label["description"],
            scientific_name: nil,
            confidence: best_label["score"],
            provider: "google_vision",
            provider_id: best_label["mid"],
            taxonomy: nil,
            raw_response: raw_response
          })

        Logger.info(
          "GoogleVisionProvider: Found potential species match: #{species_result.species_name}"
        )

        {:ok, species_result}

      [] ->
        Logger.info("GoogleVisionProvider: No fish/animal species detected in labels")
        {:ok, SpeciesResult.no_species_found("google_vision", raw_response)}
    end
  end
end
