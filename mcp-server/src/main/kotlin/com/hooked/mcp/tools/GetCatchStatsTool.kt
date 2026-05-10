package com.hooked.mcp.tools

import com.hooked.mcp.models.CatchStats
import com.hooked.mcp.models.DateRange
import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Server.registerGetCatchStatsTool(userId: UUID) {
    addTool(
        name = "get_catch_stats",
        description = """
            Get aggregated fishing statistics: total catch count, species breakdown,
            unique species and location counts, most productive location, and date range.
            Good for a quick overall summary of the user's fishing history.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = emptyList()
        )
    ) { _ ->
        val stats: CatchStats = transaction {
            val rows = CatchesTable
                .select(CatchesTable.species, CatchesTable.location, CatchesTable.caughtAt)
                .where { CatchesTable.userId eq userId }
                .toList()

            val speciesBreakdown = rows
                .mapNotNull { it[CatchesTable.species]?.trim() }
                .filter { it.isNotEmpty() }
                .groupingBy { it }.eachCount().toSortedMap()

            val locationCounts = rows
                .mapNotNull { it[CatchesTable.location]?.trim() }
                .filter { it.isNotEmpty() }
                .groupingBy { it }.eachCount()

            val mostProductive = locationCounts.maxByOrNull { it.value }

            val dates = rows.mapNotNull { it[CatchesTable.caughtAt] }
            val dateRange = if (dates.isNotEmpty()) DateRange(
                earliest = dates.min().toString(), latest = dates.max().toString()
            ) else null

            CatchStats(
                totalCatches = rows.size,
                speciesBreakdown = speciesBreakdown,
                uniqueSpecies = speciesBreakdown.keys.size,
                uniqueLocations = locationCounts.keys.size,
                mostProductiveLocation = mostProductive?.key,
                mostProductiveLocationCount = mostProductive?.value ?: 0,
                dateRange = dateRange
            )
        }

        CallToolResult(content = listOf(TextContent(Json.encodeToString(stats))))
    }
}
