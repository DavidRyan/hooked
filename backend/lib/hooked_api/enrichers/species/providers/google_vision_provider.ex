defmodule HookedApi.Enrichers.Species.Providers.GoogleVisionProvider do

  @behaviour HookedApi.Enrichers.Species.SpeciesProvider

  use Tesla
  require Logger

  alias HookedApi.Enrichers.Species.SpeciesResult

  plug(Tesla.Middleware.JSON)
  plug(Tesla.Middleware.Logger)
  plug(Tesla.Middleware.BaseUrl, "https://vision.googleapis.com/v1")

  @impl true
  def validate_configuration do
    token = System.get_env("GOOGLE_VISION_ACCESS_TOKEN")

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
  def identify_species(image_source, _filename) do
    Logger.info("GoogleVisionProvider: Starting species identification")
    token = System.get_env("GOOGLE_VISION_ACCESS_TOKEN")

    try do
      # Support both URL strings and raw binary data
      request_body = build_vision_request(image_source)

      Logger.info("GoogleVisionProvider: Sending request to Google Vision API")

      case post("/images:annotate?key=#{token}", request_body) do
        {:ok, %Tesla.Env{status: 200, body: body} = response} ->
          Logger.info(
            "GoogleVisionProvider: Received identification results from Google Vision API"
          )

          Logger.debug(
            "GoogleVisionProvider: Raw API response: #{inspect(body, pretty: true, limit: :infinity)}"
          )

          parse_response(body, response)

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

  # Build request using image URL (memory efficient - no base64 encoding needed)
  defp build_vision_request(image_url) when is_binary(image_url) and byte_size(image_url) < 2048 do
    # Looks like a URL, use imageUri
    if String.starts_with?(image_url, "http") do
      Logger.debug("GoogleVisionProvider: Using image URL: #{image_url}")

      %{
        "requests" => [
          %{
            "image" => %{
              "source" => %{
                "imageUri" => image_url
              }
            },
            "features" => [
              %{
                "type" => "LABEL_DETECTION",
                "maxResults" => 10
              },
              %{
                "type" => "WEB_DETECTION",
                "maxResults" => 10
              }
            ]
          }
        ]
      }
    else
      # Treat as file path, read and encode
      build_vision_request_from_file(image_url)
    end
  end

  # Build request from raw image data (fallback, uses more memory)
  defp build_vision_request(image_data) when is_binary(image_data) do
    Logger.debug("GoogleVisionProvider: Encoding image data (#{byte_size(image_data)} bytes)")
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
            },
            %{
              "type" => "WEB_DETECTION",
              "maxResults" => 10
            }
          ]
        }
      ]
    }
  end

  defp build_vision_request_from_file(file_path) do
    Logger.debug("GoogleVisionProvider: Reading and encoding file: #{file_path}")
    image_data = File.read!(file_path)
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
            },
            %{
              "type" => "WEB_DETECTION",
              "maxResults" => 10
            }
          ]
        }
      ]
    }
  end

  defp parse_response(%{"responses" => [response | _]}, full_response) do
    Logger.info("GoogleVisionProvider: Response body: #{inspect(full_response)}")

    case response do
      %{"webDetection" => %{"bestGuessLabels" => [%{"label" => best_guess} | _]}} ->
        species_result =
          SpeciesResult.new(%{
            common_name: best_guess,
            scientific_name: nil,
            confidence: 1.0,
            provider: "google_vision",
            provider_id: nil,
            taxonomy: nil,
            raw_response: full_response.body
          })

        Logger.info(
          "GoogleVisionProvider: Found best guess species match: #{species_result.species_name}"
        )

        {:ok, species_result}

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
