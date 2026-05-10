package com.hooked.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

fun Server.registerGetLiveWeatherTool(apiKey: String?) {
    addTool(
        name = "get_live_weather",
        description = """
            Get current weather conditions at a location by name.
            Useful for planning a fishing trip — check conditions before heading out.
            Returns temperature, sky conditions, wind, humidity, and visibility.
            Examples: "Lake Hartwell SC", "Chattahoochee River GA", "Savannah GA"
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "Location name to get weather for (city, lake, river, etc.)")
                }
            },
            required = listOf("location")
        )
    ) { request ->
        if (apiKey == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    "Live weather unavailable: OPENWEATHER_API_KEY environment variable is not set."
                )), isError = true
            )
        }

        val location = request.params.arguments?.get("location")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'location' is required")), isError = true
            )

        val encodedLocation = URLEncoder.encode(location, "UTF-8")
        val url = URI.create(
            "https://api.openweathermap.org/data/2.5/weather?q=$encodedLocation&appid=$apiKey&units=imperial"
        )

        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        val response = runCatching {
            client.send(
                HttpRequest.newBuilder(url).GET().timeout(Duration.ofSeconds(15)).build(),
                HttpResponse.BodyHandlers.ofString()
            )
        }.getOrElse { e ->
            return@addTool CallToolResult(
                content = listOf(TextContent("Error fetching weather: ${e.message}")), isError = true
            )
        }

        when (response.statusCode()) {
            404 -> return@addTool CallToolResult(
                content = listOf(TextContent("Location not found: '$location'. Try a more specific name, e.g. 'Atlanta GA'."))
            )
            200 -> Unit
            else -> return@addTool CallToolResult(
                content = listOf(TextContent("Weather API error (${response.statusCode()})")), isError = true
            )
        }

        val json = Json.parseToJsonElement(response.body()).jsonObject
        val main = json["main"]?.jsonObject
        val wind = json["wind"]?.jsonObject
        val clouds = json["clouds"]?.jsonObject
        val weatherDesc = json["weather"]?.jsonArray?.firstOrNull()?.jsonObject
        val cityName = json["name"]?.jsonPrimitive?.contentOrNull
        val country = json["sys"]?.jsonObject?.get("country")?.jsonPrimitive?.contentOrNull

        fun windDirection(deg: Int): String {
            val dirs = listOf("N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW")
            return dirs[((deg + 11.25) / 22.5).toInt() % 16]
        }

        val text = buildString {
            appendLine("Current Weather: ${cityName ?: location}${country?.let { ", $it" } ?: ""}")
            appendLine()
            weatherDesc?.get("description")?.jsonPrimitive?.contentOrNull
                ?.let { appendLine("CONDITIONS: ${it.replaceFirstChar { c -> c.uppercase() }}") }
            main?.get("temp")?.jsonPrimitive?.doubleOrNull?.let { temp ->
                val feels = main["feels_like"]?.jsonPrimitive?.doubleOrNull
                appendLine("TEMPERATURE: %.1f°F${feels?.let { " (feels like %.1f°F)".format(it) } ?: ""}".format(temp))
            }
            main?.get("humidity")?.jsonPrimitive?.intOrNull?.let { appendLine("HUMIDITY: $it%") }
            wind?.get("speed")?.jsonPrimitive?.doubleOrNull?.let { speed ->
                append("WIND: %.1f mph".format(speed))
                wind["deg"]?.jsonPrimitive?.intOrNull?.let { append(" from ${windDirection(it)}") }
                appendLine()
            }
            clouds?.get("all")?.jsonPrimitive?.intOrNull?.let { appendLine("CLOUD COVER: $it%") }
            json["visibility"]?.jsonPrimitive?.intOrNull?.let {
                appendLine("VISIBILITY: ${"%.1f".format(it / 1000.0)} km")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
