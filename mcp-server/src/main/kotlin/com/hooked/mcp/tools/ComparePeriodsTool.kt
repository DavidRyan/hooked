package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

fun Server.registerComparePeriodsTool(userId: UUID) {
    addTool(
        name = "compare_periods",
        description = """
            Compare two date ranges side by side: catch counts, species diversity,
            unique locations, and top species for each period.
            Example: compare this year vs last year, or summer vs fall.
            All dates use format YYYY-MM-DD.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("period_a_from") { put("type", "string"); put("description", "Period A start (YYYY-MM-DD)") }
                putJsonObject("period_a_to")   { put("type", "string"); put("description", "Period A end (YYYY-MM-DD)") }
                putJsonObject("period_b_from") { put("type", "string"); put("description", "Period B start (YYYY-MM-DD)") }
                putJsonObject("period_b_to")   { put("type", "string"); put("description", "Period B end (YYYY-MM-DD)") }
            },
            required = listOf("period_a_from", "period_a_to", "period_b_from", "period_b_to")
        )
    ) { request ->
        val args = request.params.arguments ?: emptyMap()

        fun parseDate(key: String, endOfDay: Boolean = false): LocalDateTime? {
            val raw = args[key]?.jsonPrimitive?.contentOrNull ?: return null
            return runCatching {
                LocalDateTime.parse("${raw}T${if (endOfDay) "23:59:59" else "00:00:00"}")
            }.getOrNull()
        }

        val aFrom = parseDate("period_a_from") ?: return@addTool CallToolResult(
            content = listOf(TextContent("Error: invalid or missing period_a_from")), isError = true)
        val aTo = parseDate("period_a_to", true) ?: return@addTool CallToolResult(
            content = listOf(TextContent("Error: invalid or missing period_a_to")), isError = true)
        val bFrom = parseDate("period_b_from") ?: return@addTool CallToolResult(
            content = listOf(TextContent("Error: invalid or missing period_b_from")), isError = true)
        val bTo = parseDate("period_b_to", true) ?: return@addTool CallToolResult(
            content = listOf(TextContent("Error: invalid or missing period_b_to")), isError = true)

        data class PeriodStats(val total: Int, val uniqueSpecies: Int,
                               val uniqueLocations: Int, val topSpecies: List<Pair<String, Int>>)

        fun fetchStats(from: LocalDateTime, to: LocalDateTime): PeriodStats = transaction {
            val rows = CatchesTable
                .select(CatchesTable.species, CatchesTable.location)
                .where {
                    (CatchesTable.userId eq userId) and
                    (CatchesTable.caughtAt greaterEq from) and
                    (CatchesTable.caughtAt lessEq to)
                }.toList()

            val species = rows.mapNotNull { it[CatchesTable.species]?.trim() }.filter { it.isNotEmpty() }
            val locations = rows.mapNotNull { it[CatchesTable.location]?.trim() }.filter { it.isNotEmpty() }
            val topSpecies = species.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(3).map { Pair(it.key, it.value) }
            PeriodStats(rows.size, species.toSet().size, locations.toSet().size, topSpecies)
        }

        val a = fetchStats(aFrom, aTo)
        val b = fetchStats(bFrom, bTo)

        val text = buildString {
            appendLine("Period Comparison")
            appendLine("  Period A: $aFrom → $aTo")
            appendLine("  Period B: $bFrom → $bTo")
            appendLine()
            appendLine("TOTAL CATCHES:    A=${a.total}  vs  B=${b.total}" +
                if (a.total != b.total) "  (${if (a.total > b.total) "A" else "B"} wins)" else "  (tie)")
            appendLine("UNIQUE SPECIES:   A=${a.uniqueSpecies}  vs  B=${b.uniqueSpecies}")
            appendLine("UNIQUE LOCATIONS: A=${a.uniqueLocations}  vs  B=${b.uniqueLocations}")
            appendLine()
            appendLine("TOP SPECIES — Period A:")
            if (a.topSpecies.isEmpty()) appendLine("  (no catches)")
            else a.topSpecies.forEach { (s, c) -> appendLine("  $s ($c)") }
            appendLine()
            appendLine("TOP SPECIES — Period B:")
            if (b.topSpecies.isEmpty()) appendLine("  (no catches)")
            else b.topSpecies.forEach { (s, c) -> appendLine("  $s ($c)") }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
