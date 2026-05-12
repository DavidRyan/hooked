package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Surface the most unusual catches: catches whose conditions are atypical for that
 * species relative to the user's own catch history. Computes a per-species mean and
 * standard deviation across hour-of-day, air temp, and pressure, then z-scores each
 * catch and returns the highest-deviation ones.
 *
 * Real-world value: "you've caught a Walleye at noon in 78°F water once — that's
 * atypical, here's what was different that day."
 */
fun Server.registerFindOutlierCatchesTool(userId: UUID) {
    addTool(
        name = "find_outlier_catches",
        description = """
            Find catches whose conditions are most unusual for that species, given the user's
            own catch history. Useful for surprising-pattern questions like "any catches that
            don't fit my usual pattern?" or "have I ever caught X in weird conditions?"
            Returns the top N most atypical catches, each with the species, location, date,
            and which dimension (hour, temp, pressure) was the outlier.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("species") {
                    put("type", "string")
                    put("description", "Optional: limit to a single species (case-insensitive partial match). Omit for all species.")
                }
                putJsonObject("limit") {
                    put("type", "integer")
                    put("description", "Maximum number of outliers to return. Default 8.")
                }
            },
            required = emptyList()
        )
    ) { request ->
        val speciesQuery = request.params.arguments?.get("species")?.jsonPrimitive?.contentOrNull
        val limit = request.params.arguments?.get("limit")?.jsonPrimitive?.intOrNull ?: 8

        data class CatchRow(
            val species: String,
            val location: String?,
            val caughtAt: String?,
            val hour: Int?,
            val airTemp: Double?,
            val pressure: Double?
        )

        val rows: List<CatchRow> = transaction {
            CatchesTable
                .select(
                    CatchesTable.species,
                    CatchesTable.location,
                    CatchesTable.caughtAt,
                    CatchesTable.weatherData
                )
                .where { CatchesTable.userId eq userId }
                .mapNotNull { row ->
                    val sp = row[CatchesTable.species]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    val w = row[CatchesTable.weatherData]?.jsonObject
                    val main = w?.get("main")?.jsonObject
                    CatchRow(
                        species = sp,
                        location = row[CatchesTable.location],
                        caughtAt = row[CatchesTable.caughtAt]?.toString(),
                        hour = row[CatchesTable.caughtAt]?.hour,
                        airTemp = main?.get("temp")?.jsonPrimitive?.doubleOrNull,
                        pressure = main?.get("pressure")?.jsonPrimitive?.doubleOrNull
                    )
                }
        }

        if (rows.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent("No catch data available.")))
        }

        val filtered = if (!speciesQuery.isNullOrBlank()) {
            val q = speciesQuery.lowercase()
            rows.filter { it.species.lowercase().contains(q) }
        } else rows

        if (filtered.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No catches found matching species '$speciesQuery'."
            )))
        }

        // Compute mean + stdev per species per dimension. Need at least 3 catches of a
        // species to bother computing — fewer than that and "outlier" isn't meaningful.
        data class Stats(val mean: Double, val stdev: Double)
        fun statsOf(xs: List<Double>): Stats {
            if (xs.size < 2) return Stats(xs.firstOrNull() ?: 0.0, 0.0)
            val m = xs.average()
            val v = xs.sumOf { (it - m) * (it - m) } / (xs.size - 1)
            return Stats(m, sqrt(v))
        }

        val perSpecies = filtered.groupBy { it.species }
        val statsBySpecies: Map<String, Triple<Stats, Stats, Stats>> = perSpecies.mapValues { (_, list) ->
            val hours = list.mapNotNull { it.hour?.toDouble() }
            val temps = list.mapNotNull { it.airTemp }
            val pressures = list.mapNotNull { it.pressure }
            Triple(statsOf(hours), statsOf(temps), statsOf(pressures))
        }

        data class Scored(
            val row: CatchRow,
            val score: Double,
            val drivers: List<String>
        )

        val scored = filtered.mapNotNull { r ->
            val (hStats, tStats, pStats) = statsBySpecies[r.species] ?: return@mapNotNull null
            val sampleSize = perSpecies[r.species]?.size ?: 0
            if (sampleSize < 3) return@mapNotNull null

            val drivers = mutableListOf<String>()
            var max = 0.0
            r.hour?.let {
                if (hStats.stdev > 0) {
                    val z = abs(it - hStats.mean) / hStats.stdev
                    if (z > 1.5) drivers += "hour=$it (usually ~${hStats.mean.toInt()})"
                    if (z > max) max = z
                }
            }
            r.airTemp?.let {
                if (tStats.stdev > 0) {
                    val z = abs(it - tStats.mean) / tStats.stdev
                    if (z > 1.5) drivers += "air ${it.toInt()}°F (usually ~${tStats.mean.toInt()}°F)"
                    if (z > max) max = z
                }
            }
            r.pressure?.let {
                if (pStats.stdev > 0) {
                    val z = abs(it - pStats.mean) / pStats.stdev
                    if (z > 1.5) drivers += "pressure ${it.toInt()} hPa (usually ~${pStats.mean.toInt()})"
                    if (z > max) max = z
                }
            }
            if (max < 1.5 || drivers.isEmpty()) null else Scored(r, max, drivers)
        }.sortedByDescending { it.score }

        if (scored.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No clear outliers found. Either catches are clustered tightly, or species sample sizes are too small (need >=3 of a species)."
            )))
        }

        val text = buildString {
            appendLine("Outlier catches (z-score > 1.5 vs that species' own history):")
            appendLine()
            scored.take(limit).forEach { s ->
                val date = s.row.caughtAt ?: "?"
                val loc = s.row.location ?: "?"
                appendLine("  ${s.row.species} @ $loc on $date")
                appendLine("    drivers: ${s.drivers.joinToString("; ")}")
                appendLine("    max z-score: %.1f".format(s.score))
            }
            appendLine()
            appendLine("Each driver line shows the deviation from this species' own mean across your catches.")
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
