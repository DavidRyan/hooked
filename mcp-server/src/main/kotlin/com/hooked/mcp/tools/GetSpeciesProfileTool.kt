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

fun Server.registerGetSpeciesProfileTool(userId: UUID) {
    addTool(
        name = "get_species_profile",
        description = """
            Get a detailed profile for a specific fish species from the user's catch history.
            Returns: total caught, best locations, best months of the year, and typical weather
            conditions when this species was caught.
            Great for questions like "when and where do I usually catch bass?"
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("species") {
                    put("type", "string")
                    put("description", "Species name to profile (case-insensitive partial match, e.g. 'bass')")
                }
            },
            required = listOf("species")
        )
    ) { request ->
        val speciesQuery = request.params.arguments?.get("species")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'species' parameter is required")), isError = true
            )

        val pattern = "%${speciesQuery.lowercase()}%"

        data class CatchRow(val location: String?, val month: Int?,
                            val weatherDesc: String?, val tempF: Double?)

        val rows: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.location, CatchesTable.caughtAt, CatchesTable.weatherData)
                .where { (CatchesTable.userId eq userId) and (CatchesTable.species.lowerCase() like pattern) }
                .map { row ->
                    val weather = row[CatchesTable.weatherData]
                    CatchRow(
                        location = row[CatchesTable.location],
                        month = row[CatchesTable.caughtAt]?.monthValue,
                        weatherDesc = weather?.jsonObject?.get("weather")
                            ?.jsonArray?.firstOrNull()?.jsonObject?.get("description")
                            ?.jsonPrimitive?.contentOrNull,
                        tempF = weather?.jsonObject?.get("main")
                            ?.jsonObject?.get("temp")?.jsonPrimitive?.doubleOrNull
                    )
                }
        }

        if (rows.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("No catches found matching species: $speciesQuery"))
            )
        }

        val topLocations = rows.mapNotNull { it.location?.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)

        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthCounts = rows.mapNotNull { it.month }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }

        val weatherDescs = rows.mapNotNull { it.weatherDesc?.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)

        val temps = rows.mapNotNull { it.tempF }
        val avgTemp = if (temps.isNotEmpty()) temps.average() else null

        val text = buildString {
            appendLine("Species Profile: $speciesQuery (${rows.size} catch(es))")
            appendLine()
            appendLine("TOP LOCATIONS:")
            topLocations.forEach { (loc, count) -> appendLine("  $loc — $count catch(es)") }
            appendLine()
            appendLine("BEST MONTHS:")
            monthCounts.take(4).forEach { (month, count) ->
                appendLine("  ${monthNames[month - 1]} — $count catch(es)")
            }
            appendLine()
            if (weatherDescs.isNotEmpty()) {
                appendLine("TYPICAL CONDITIONS:")
                weatherDescs.forEach { (desc, count) -> appendLine("  $desc — $count catch(es)") }
            }
            avgTemp?.let { appendLine("\nAVERAGE TEMP AT CATCH: %.1f°F".format(it)) }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
