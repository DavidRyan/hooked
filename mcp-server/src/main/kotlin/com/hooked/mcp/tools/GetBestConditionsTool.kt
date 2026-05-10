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

fun Server.registerGetBestConditionsTool(userId: UUID) {
    addTool(
        name = "get_best_conditions",
        description = """
            Analyze all catches to find what conditions correlate with the most success.
            Returns: most productive times of day, seasons, months, and weather conditions
            (temperature ranges, sky conditions, wind) based on historical catch data.
            Great for planning future trips.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = emptyList()
        )
    ) { _ ->
        data class CatchRow(val hour: Int?, val month: Int?,
                            val weatherDesc: String?, val tempF: Double?, val windSpeed: Double?)

        val rows: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.caughtAt, CatchesTable.weatherData)
                .where { CatchesTable.userId eq userId }
                .map { row ->
                    val weather = row[CatchesTable.weatherData]?.jsonObject
                    CatchRow(
                        hour = row[CatchesTable.caughtAt]?.hour,
                        month = row[CatchesTable.caughtAt]?.monthValue,
                        weatherDesc = weather?.get("weather")?.jsonArray
                            ?.firstOrNull()?.jsonObject?.get("description")?.jsonPrimitive?.contentOrNull,
                        tempF = weather?.get("main")?.jsonObject?.get("temp")?.jsonPrimitive?.doubleOrNull,
                        windSpeed = weather?.get("wind")?.jsonObject?.get("speed")?.jsonPrimitive?.doubleOrNull
                    )
                }
        }

        if (rows.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent("No catch data available.")))
        }

        fun hourLabel(h: Int) = when {
            h < 6  -> "Night (12am–6am)"
            h < 12 -> "Morning (6am–12pm)"
            h < 17 -> "Afternoon (12pm–5pm)"
            h < 21 -> "Evening (5pm–9pm)"
            else   -> "Night (9pm–12am)"
        }
        fun season(m: Int) = when (m) {
            in 3..5  -> "Spring"; in 6..8  -> "Summer"
            in 9..11 -> "Fall";   else     -> "Winter"
        }
        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

        val timeOfDayCounts = rows.mapNotNull { it.hour }.groupingBy { hourLabel(it) }.eachCount()
            .entries.sortedByDescending { it.value }
        val seasonCounts = rows.mapNotNull { it.month }.groupingBy { season(it) }.eachCount()
            .entries.sortedByDescending { it.value }
        val monthCounts = rows.mapNotNull { it.month }.groupingBy { monthNames[it - 1] }.eachCount()
            .entries.sortedByDescending { it.value }
        val weatherCounts = rows.mapNotNull { it.weatherDesc?.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
        val temps = rows.mapNotNull { it.tempF }
        val winds = rows.mapNotNull { it.windSpeed }

        val text = buildString {
            appendLine("Best Conditions Analysis (${rows.size} total catches)")
            appendLine()
            appendLine("TIME OF DAY:")
            timeOfDayCounts.forEach { (label, count) ->
                appendLine("  $label — $count catches (${(count * 100.0 / rows.size).toInt()}%)")
            }
            appendLine()
            appendLine("SEASON:")
            seasonCounts.forEach { (season, count) ->
                appendLine("  $season — $count catches (${(count * 100.0 / rows.size).toInt()}%)")
            }
            appendLine()
            appendLine("BEST MONTHS:")
            monthCounts.take(4).forEach { (month, count) -> appendLine("  $month — $count catches") }
            appendLine()
            if (weatherCounts.isNotEmpty()) {
                appendLine("TOP WEATHER CONDITIONS:")
                weatherCounts.forEach { (desc, count) -> appendLine("  $desc — $count catches") }
                appendLine()
            }
            if (temps.isNotEmpty()) {
                appendLine("TEMPERATURE RANGE AT CATCHES: %.0f°F – %.0f°F (avg %.1f°F)".format(
                    temps.min(), temps.max(), temps.average()))
            }
            if (winds.isNotEmpty()) appendLine("AVG WIND SPEED AT CATCHES: %.1f mph".format(winds.average()))
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
