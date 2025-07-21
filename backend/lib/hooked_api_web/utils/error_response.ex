defmodule HookedApiWeb.Utils.ErrorResponse do
  @moduledoc """
  Utility module for creating standardized error responses.
  """

  @doc """
  Creates a validation error response from an Ecto changeset.
  """
  def validation_error(changeset) do
    errors = extract_changeset_errors(changeset)

    %{
      error: "validation_failed",
      message: "The provided data failed validation",
      details: errors,
      code: "VALIDATION_ERROR"
    }
  end

  @doc """
  Creates an authentication error response.
  """
  def authentication_error(message \\ "Authentication failed") do
    %{
      error: "authentication_failed",
      message: message,
      code: "AUTH_ERROR"
    }
  end

  @doc """
  Creates an authorization error response.
  """
  def authorization_error(message \\ "Authorization required") do
    %{
      error: "authorization_required",
      message: message,
      code: "AUTH_REQUIRED"
    }
  end

  @doc """
  Creates a not found error response.
  """
  def not_found_error(resource \\ "Resource") do
    %{
      error: "not_found",
      message: "#{resource} not found",
      code: "NOT_FOUND"
    }
  end

  @doc """
  Creates a server error response.
  """
  def server_error(message \\ "Internal server error") do
    %{
      error: "server_error",
      message: message,
      code: "SERVER_ERROR"
    }
  end

  @doc """
  Creates a custom error response.
  """
  def custom_error(error_type, message, code \\ nil) do
    response = %{
      error: error_type,
      message: message
    }

    if code do
      Map.put(response, :code, code)
    else
      response
    end
  end

  # Private helper to extract errors from Ecto changeset
  defp extract_changeset_errors(changeset) do
    Enum.reduce(changeset.errors, %{}, fn {field, {message, opts}}, acc ->
      formatted_message = format_error_message(message, opts)

      case Map.get(acc, field) do
        nil -> Map.put(acc, field, [formatted_message])
        existing -> Map.put(acc, field, existing ++ [formatted_message])
      end
    end)
  end

  # Format error message with interpolated values
  defp format_error_message(message, opts) do
    Enum.reduce(opts, message, fn {key, value}, acc ->
      String.replace(acc, "%{#{key}}", to_string(value))
    end)
  end
end
