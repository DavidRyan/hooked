package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Server.registerGetCatchWeatherTool(userId: UUID) {
    addTool(
        name = "get_catch_weather",
        description = """
            Get the weather conditions stored for a specific catch in a human-readable format.
            Returns temperature, sky conditions, humidity, wind speed and direction, visibility.
            Weather is recorded at the time the catch was submitted.
            Use list_catches to find catch IDs.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("id") {
                    put("type", "string")
                    put("description", "UUID of the catch")
                }
            },
            required = listOf("id")
        )
    ) { request ->
        val catchId = request.params.arguments?.get("id")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'id' is required")), isError = true
            )

        val catchUuid = runCatching { UUID.fromString(catchId) }.getOrElse {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: '$catchId' is not a valid UUID")), isError = true
            )
        }

        val row = transaction {
            CatchesTable
                .select(CatchesTable.weatherData, CatchesTable.species,
                    CatchesTable.location, CatchesTable.caughtAt)
                .where { (CatchesTable.id eq catchUuid) and (CatchesTable.userId eq userId) }
                .firstOrNull()
        } ?: return@addTool CallToolResult(
            content = listOf(TextContent("No catch found with id: $catchId")), isError = true
        )

        val weatherData = row[CatchesTable.weatherData]
        val species = row[CatchesTable.species]
        val location = row[CatchesTable.location]
        val caughtAt = row[CatchesTable.caughtAt]

        if (weatherData == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    "No weather data available for this catch (enrichment may not have run yet).\n" +
                    "Catch: ${species ?: "Unknown"} at ${location ?: "Unknown"} on $caughtAt"
                ))
            )
        }

        val w = weatherData.jsonObject
        val main = w["main"]?.jsonObject
        val wind = w["wind"]?.jsonObject
        val clouds = w["clouds"]?.jsonObject
        val weatherArr = w["weather"]?.jsonArray?.firstOrNull()?.jsonObject

        val tempF = main?.get("temp")?.jsonPrimitive?.doubleOrNull
        val feelsLikeF = main?.get("feels_like")?.jsonPrimitive?.doubleOrNull
        val humidity = main?.get("humidity")?.jsonPrimitive?.intOrNull
        val pressure = main?.get("pressure")?.jsonPrimitive?.intOrNull
        val description = weatherArr?.get("description")?.jsonPrimitive?.contentOrNull
        val windSpeed = wind?.get("speed")?.jsonPrimitive?.doubleOrNull
        val windDeg = wind?.get("deg")?.jsonPrimitive?.intOrNull
        val cloudiness = clouds?.get("all")?.jsonPrimitive?.intOrNull
        val visibility = w["visibility"]?.jsonPrimitive?.intOrNull
        val cityName = w["name"]?.jsonPrimitive?.contentOrNull

        fun windDirection(deg: Int): String {
            val dirs = listOf("N","NNE","NE","ENE","E","ESE","SE","SSE",
                              "S","SSW","SW","WSW","W","WNW","NW","NNW")
            return dirs[((deg + 11.25) / 22.5).toInt() % 16]
        }

        val text = buildString {
            appendLine("Weather for: ${species ?: "Unknown"} at ${location ?: "Unknown"} on $caughtAt")
            cityName?.let { appendLine("Weather location: $it") }
            appendLine()
            description?.let { appendLine("CONDITIONS: ${it.replaceFirstChar { c -> c.uppercase() }}") }
            tempF?.let { appendLine("TEMPERATURE: %.1f°F${feelsLikeF?.let { f -> " (feels like %.1f°F)".format(f) } ?: ""}".format(it)) }
            humidity?.let { appendLine("HUMIDITY: $it%") }
            pressure?.let { appendLine("PRESSURE: $it hPa") }
            windSpeed?.let { speed ->
                append("WIND: %.1f mph".format(speed))
                windDeg?.let { deg -> append(" from ${windDirection(deg)}") }
                appendLine()
            }
            cloudiness?.let { appendLine("CLOUD COVER: $it%") }
            visibility?.let { appendLine("VISIBILITY: ${"%.1f".format(it / 1000.0)} km") }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
