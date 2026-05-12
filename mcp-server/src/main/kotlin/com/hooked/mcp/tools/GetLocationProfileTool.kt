package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Inverse of get_species_profile: for a given location, return what works there.
 * Answers "what should I fish for at Petenwell?" / "is Lake Mendota a morning or
 * evening spot?" / "what conditions have been productive at Castle Rock?"
 */
fun Server.registerGetLocationProfileTool(userId: UUID) {
    addTool(
        name = "get_location_profile",
        description = """
            Profile a specific fishing location: which species you've caught there, the best
            months and times of day at that spot, and the typical weather conditions when
            you've been productive. Use this for spot-planning questions like
            "what should I target at Petenwell tomorrow?" or "is Devil's Lake a morning bite?"
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "Location name (case-insensitive partial match, e.g. 'mendota' or 'petenwell')")
                }
            },
            required = listOf("location")
        )
    ) { request ->
        val query = request.params.arguments?.get("location")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'location' parameter is required")), isError = true
            )
        val pattern = "%${query.lowercase()}%"

        data class CatchRow(
            val species: String?,
            val location: String?,
            val hour: Int?,
            val month: Int?,
            val weatherMain: String?,
            val pressureTrend: String?,
            val airTemp: Double?,
            val windSpeed: Double?
        )

        val rows: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.species, CatchesTable.location, CatchesTable.caughtAt, CatchesTable.weatherData)
                .where {
                    (CatchesTable.userId eq userId) and
                        (CatchesTable.location.lowerCase() like pattern)
                }
                .map { row ->
                    val w = row[CatchesTable.weatherData]?.jsonObject
                    val main = w?.get("main")?.jsonObject
                    val wind = w?.get("wind")?.jsonObject
                    val weatherArr = w?.get("weather")?.jsonArray?.firstOrNull()?.jsonObject
                    val pressure = main?.get("pressure")?.jsonPrimitive?.doubleOrNull
                    val derivedTrend = pressure?.let {
                        when {
                            it < 1010 -> "falling"
                            it < 1020 -> "stable"
                            else -> "rising"
                        }
                    }
                    CatchRow(
                        species = row[CatchesTable.species]?.trim()?.takeIf { it.isNotEmpty() },
                        location = row[CatchesTable.location],
                        hour = row[CatchesTable.caughtAt]?.hour,
                        month = row[CatchesTable.caughtAt]?.monthValue,
                        weatherMain = weatherArr?.get("main")?.jsonPrimitive?.contentOrNull,
                        pressureTrend = w?.get("pressure_trend")?.jsonPrimitive?.contentOrNull ?: derivedTrend,
                        airTemp = main?.get("temp")?.jsonPrimitive?.doubleOrNull,
                        windSpeed = wind?.get("speed")?.jsonPrimitive?.doubleOrNull
                    )
                }
        }

        if (rows.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("No catches found at locations matching '$query'."))
            )
        }

        val canonicalLocation = rows.mapNotNull { it.location }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: query

        val speciesCounts = rows.mapNotNull { it.species }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }

        val hourCounts = rows.mapNotNull { it.hour }
            .groupingBy { hourBand(it) }.eachCount().entries.sortedByDescending { it.value }

        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val monthCounts = rows.mapNotNull { it.month }
            .groupingBy { monthNames[it - 1] }.eachCount().entries.sortedByDescending { it.value }

        val weatherCounts = rows.mapNotNull { it.weatherMain }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }

        val trendCounts = rows.mapNotNull { it.pressureTrend }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }

        val temps = rows.mapNotNull { it.airTemp }
        val winds = rows.mapNotNull { it.windSpeed }

        // Top species per dominant time band — helps "morning bites X, evening bites Y".
        val speciesByBand = rows
            .filter { it.species != null && it.hour != null }
            .groupBy { hourBand(it.hour!!) }
            .mapValues { (_, list) ->
                list.mapNotNull { it.species }
                    .groupingBy { it }.eachCount().entries
                    .sortedByDescending { it.value }.take(2)
                    .joinToString(", ") { "${it.key}(${it.value})" }
            }

        val text = buildString {
            appendLine("Location Profile: $canonicalLocation (${rows.size} catches)")
            appendLine()
            appendLine("SPECIES MIX:")
            speciesCounts.take(6).forEach { (sp, n) ->
                val pct = (n * 100.0 / rows.size).roundToInt()
                appendLine("  $sp — $n ($pct%)")
            }
            appendLine()
            if (hourCounts.isNotEmpty()) {
                appendLine("TIME OF DAY:")
                hourCounts.forEach { (band, n) ->
                    appendLine("  $band — $n catches")
                }
                appendLine()
            }
            if (speciesByBand.isNotEmpty()) {
                appendLine("WHAT BITES WHEN:")
                speciesByBand.entries.sortedBy { bandOrder(it.key) }.forEach { (band, sp) ->
                    appendLine("  $band: $sp")
                }
                appendLine()
            }
            if (monthCounts.isNotEmpty()) {
                appendLine("BEST MONTHS:")
                monthCounts.take(4).forEach { (m, n) -> appendLine("  $m — $n catches") }
                appendLine()
            }
            if (weatherCounts.isNotEmpty()) {
                appendLine("TYPICAL CONDITIONS:")
                weatherCounts.take(4).forEach { (w, n) -> appendLine("  $w — $n catches") }
                appendLine()
            }
            if (trendCounts.isNotEmpty()) {
                appendLine("PRESSURE TREND DISTRIBUTION:")
                trendCounts.forEach { (t, n) -> appendLine("  $t — $n catches") }
                appendLine()
            }
            if (temps.isNotEmpty()) {
                appendLine("AIR TEMP: %.0f°F – %.0f°F (avg %.1f°F)".format(temps.min(), temps.max(), temps.average()))
            }
            if (winds.isNotEmpty()) {
                appendLine("WIND SPEED AT CATCH: avg %.1f mph (range %.1f – %.1f)".format(
                    winds.average(), winds.min(), winds.max()))
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}

private fun hourBand(h: Int): String = when (h) {
    in 0..4   -> "Pre-dawn (0-5)"
    in 5..7   -> "Dawn (5-8)"
    in 8..11  -> "Morning (8-12)"
    in 12..16 -> "Afternoon (12-17)"
    in 17..20 -> "Evening (17-21)"
    else      -> "Night (21-24)"
}

private fun bandOrder(band: String): Int = when (band) {
    "Pre-dawn (0-5)"      -> 0
    "Dawn (5-8)"          -> 1
    "Morning (8-12)"      -> 2
    "Afternoon (12-17)"   -> 3
    "Evening (17-21)"     -> 4
    else                  -> 5
}
