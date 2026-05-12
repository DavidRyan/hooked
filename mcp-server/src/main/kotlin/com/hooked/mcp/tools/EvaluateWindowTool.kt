package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import com.hooked.mcp.tables.SkunksTable
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
 * Given a planned trip window (location, month, hour band), find historical analogs from
 * the user's catches AND skunks. Returns hit rate, species mix, and average conditions.
 *
 * Answers: "I'm fishing Petenwell tomorrow 6-10am — what does my history say?"
 */
fun Server.registerEvaluateWindowTool(userId: UUID) {
    addTool(
        name = "evaluate_window",
        description = """
            Evaluate a planned fishing window against the user's own history. Filter by any
            combination of location (partial match), month (1-12), and hour band (start/end).
            Returns: number of historical analogs (catches + skunks), per-species hit rate,
            and average conditions during productive windows. Skunks act as the negative
            signal — including them gives a real hit rate, not just catch counts.
            Great for trip-planning questions: "is Petenwell a morning bite in October?"
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "Optional. Partial location match (e.g. 'petenwell')")
                }
                putJsonObject("month") {
                    put("type", "integer")
                    put("description", "Optional. 1-12 to constrain by calendar month.")
                }
                putJsonObject("hour_start") {
                    put("type", "integer")
                    put("description", "Optional. Earliest hour of the window (0-23).")
                }
                putJsonObject("hour_end") {
                    put("type", "integer")
                    put("description", "Optional. Latest hour of the window (0-23, inclusive).")
                }
            },
            required = emptyList()
        )
    ) { request ->
        val args = request.params.arguments
        val locPattern = args?.get("location")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?.let { "%${it.lowercase()}%" }
        val month = args?.get("month")?.jsonPrimitive?.intOrNull?.takeIf { it in 1..12 }
        val hourStart = args?.get("hour_start")?.jsonPrimitive?.intOrNull?.coerceIn(0, 23)
        val hourEnd = args?.get("hour_end")?.jsonPrimitive?.intOrNull?.coerceIn(0, 23)

        fun inHourBand(h: Int): Boolean = when {
            hourStart == null && hourEnd == null -> true
            hourStart != null && hourEnd != null && hourStart <= hourEnd -> h in hourStart..hourEnd
            hourStart != null && hourEnd != null && hourStart > hourEnd -> h >= hourStart || h <= hourEnd
            hourStart != null -> h >= hourStart
            hourEnd != null -> h <= hourEnd
            else -> true
        }

        data class CatchRow(
            val species: String?,
            val location: String?,
            val hour: Int?,
            val mon: Int?,
            val airTemp: Double?,
            val pressure: Double?,
            val windSpeed: Double?,
            val weatherMain: String?
        )
        data class SkunkRow(val hour: Int?, val mon: Int?)

        val catches: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.species, CatchesTable.location, CatchesTable.caughtAt, CatchesTable.weatherData)
                .where {
                    var cond = CatchesTable.userId eq userId
                    if (locPattern != null) cond = cond and (CatchesTable.location.lowerCase() like locPattern)
                    cond
                }
                .map { row ->
                    val w = row[CatchesTable.weatherData]?.jsonObject
                    val main = w?.get("main")?.jsonObject
                    val wind = w?.get("wind")?.jsonObject
                    val weatherArr = w?.get("weather")?.jsonArray?.firstOrNull()?.jsonObject
                    CatchRow(
                        species = row[CatchesTable.species],
                        location = row[CatchesTable.location],
                        hour = row[CatchesTable.caughtAt]?.hour,
                        mon = row[CatchesTable.caughtAt]?.monthValue,
                        airTemp = main?.get("temp")?.jsonPrimitive?.doubleOrNull,
                        pressure = main?.get("pressure")?.jsonPrimitive?.doubleOrNull,
                        windSpeed = wind?.get("speed")?.jsonPrimitive?.doubleOrNull,
                        weatherMain = weatherArr?.get("main")?.jsonPrimitive?.contentOrNull
                    )
                }
                .filter { (month == null || it.mon == month) && (it.hour == null || inHourBand(it.hour)) }
        }

        val skunks: List<SkunkRow> = transaction {
            SkunksTable
                .select(SkunksTable.location, SkunksTable.fishedAt)
                .where {
                    var cond = SkunksTable.userId eq userId
                    if (locPattern != null) cond = cond and (SkunksTable.location.lowerCase() like locPattern)
                    cond
                }
                .map { row ->
                    SkunkRow(
                        hour = row[SkunksTable.fishedAt]?.hour,
                        mon = row[SkunksTable.fishedAt]?.monthValue
                    )
                }
                .filter { (month == null || it.mon == month) && (it.hour == null || inHourBand(it.hour)) }
        }

        val totalSessions = catches.size + skunks.size
        if (totalSessions == 0) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No matching history for this window. Try widening the filters."
            )))
        }

        val hitRate = catches.size.toDouble() / totalSessions
        val speciesCounts = catches.mapNotNull { it.species?.trim()?.takeIf { s -> s.isNotEmpty() } }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        val temps = catches.mapNotNull { it.airTemp }
        val winds = catches.mapNotNull { it.windSpeed }
        val pressures = catches.mapNotNull { it.pressure }
        val weatherMains = catches.mapNotNull { it.weatherMain }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }

        val text = buildString {
            val filterParts = mutableListOf<String>()
            locPattern?.let { filterParts += "location like '${it.removeSurrounding("%")}'" }
            month?.let { filterParts += "month=$it" }
            if (hourStart != null || hourEnd != null) {
                filterParts += "hours=${hourStart ?: "*"}-${hourEnd ?: "*"}"
            }
            appendLine("Window: ${if (filterParts.isEmpty()) "(no filters)" else filterParts.joinToString(", ")}")
            appendLine()
            appendLine("HISTORICAL ANALOGS:")
            appendLine("  ${catches.size} catches + ${skunks.size} skunks = $totalSessions sessions")
            appendLine("  Hit rate: ${(hitRate * 100).roundToInt()}% of sessions produced a catch")
            appendLine()
            if (speciesCounts.isNotEmpty()) {
                appendLine("WHAT YOU CAUGHT IN THIS WINDOW:")
                speciesCounts.take(6).forEach { (sp, n) ->
                    appendLine("  $sp — $n")
                }
                appendLine()
            }
            if (temps.isNotEmpty()) {
                appendLine("PRODUCTIVE CONDITIONS:")
                appendLine("  Air temp: %.0f°F – %.0f°F (avg %.0f°F)".format(temps.min(), temps.max(), temps.average()))
                if (pressures.isNotEmpty()) {
                    appendLine("  Pressure: %.0f – %.0f hPa (avg %.0f)".format(pressures.min(), pressures.max(), pressures.average()))
                }
                if (winds.isNotEmpty()) {
                    appendLine("  Wind: avg %.1f mph (range %.0f – %.0f)".format(winds.average(), winds.min(), winds.max()))
                }
            }
            if (weatherMains.isNotEmpty()) {
                appendLine("  Top weather: ${weatherMains.take(3).joinToString(", ") { "${it.key} (${it.value})" }}")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
