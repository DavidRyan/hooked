package com.hooked.mcp.tools

import com.hooked.mcp.models.SkunkSummary
import com.hooked.mcp.tables.SkunksTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

fun Server.registerListSkunksTool(userId: UUID) {
    addTool(
        name = "list_skunks",
        description = """
            List no-catch fishing trips (skunks) — sessions where nothing was caught.
            Sorted by date descending. Optionally filter by date range (YYYY-MM-DD).
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("date_from") {
                    put("type", "string")
                    put("description", "Start date inclusive, format YYYY-MM-DD")
                }
                putJsonObject("date_to") {
                    put("type", "string")
                    put("description", "End date inclusive, format YYYY-MM-DD")
                }
            },
            required = emptyList()
        )
    ) { request ->
        val args = request.params.arguments ?: emptyMap()
        val dateFrom = args["date_from"]?.jsonPrimitive?.contentOrNull
        val dateTo = args["date_to"]?.jsonPrimitive?.contentOrNull

        val skunks: List<SkunkSummary> = transaction {
            SkunksTable
                .select(SkunksTable.id, SkunksTable.location, SkunksTable.fishedAt,
                    SkunksTable.notes, SkunksTable.enrichmentStatus)
                .where {
                    var cond: Op<Boolean> = SkunksTable.userId eq userId
                    dateFrom?.let { from -> cond = cond and (SkunksTable.fishedAt greaterEq LocalDateTime.parse("${from}T00:00:00")) }
                    dateTo?.let { to -> cond = cond and (SkunksTable.fishedAt lessEq LocalDateTime.parse("${to}T23:59:59")) }
                    cond
                }
                .orderBy(SkunksTable.fishedAt, SortOrder.DESC)
                .map { row ->
                    SkunkSummary(
                        id = row[SkunksTable.id].toString(),
                        location = row[SkunksTable.location],
                        fishedAt = row[SkunksTable.fishedAt]?.toString(),
                        notes = row[SkunksTable.notes],
                        enrichmentStatus = row[SkunksTable.enrichmentStatus] ?: false
                    )
                }
        }

        CallToolResult(content = listOf(TextContent(
            "Found ${skunks.size} skunk trip(s):\n${Json.encodeToString(skunks)}"
        )))
    }
}
