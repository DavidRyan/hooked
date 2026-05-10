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

fun Server.registerGetMonthlyBreakdownTool(userId: UUID) {
    addTool(
        name = "get_monthly_breakdown",
        description = """
            Show catch counts grouped by month (Jan–Dec), aggregated across all years.
            Reveals seasonal fishing patterns. Optionally filter to a specific species.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("species") {
                    put("type", "string")
                    put("description", "Optional: filter to a specific species (case-insensitive partial match)")
                }
            },
            required = emptyList()
        )
    ) { request ->
        val speciesFilter = request.params.arguments?.get("species")?.jsonPrimitive?.contentOrNull

        val months: List<Int> = transaction {
            CatchesTable
                .select(CatchesTable.caughtAt)
                .where {
                    var cond: Op<Boolean> = CatchesTable.userId eq userId
                    speciesFilter?.let { f -> cond = cond and (CatchesTable.species.lowerCase() like "%${f.lowercase()}%") }
                    cond
                }
                .mapNotNull { it[CatchesTable.caughtAt]?.monthValue }
        }

        if (months.isEmpty()) {
            val msg = if (speciesFilter != null) "No catches found for species: $speciesFilter"
                      else "No catch data available."
            return@addTool CallToolResult(content = listOf(TextContent(msg)))
        }

        val monthNames = listOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val counts = months.groupingBy { it }.eachCount()
        val maxCount = counts.values.maxOrNull() ?: 1

        val text = buildString {
            val title = if (speciesFilter != null) "Monthly Breakdown for '$speciesFilter'"
                        else "Monthly Catch Breakdown"
            appendLine("$title (${months.size} total catches across all years):")
            appendLine()
            for (m in 1..12) {
                val count = counts[m] ?: 0
                val bar = "█".repeat((count * 20 / maxCount))
                appendLine("${monthNames[m - 1].padEnd(10)} ${count.toString().padStart(3)}  $bar")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
