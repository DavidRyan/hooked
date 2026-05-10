package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import com.hooked.mcp.tables.SkunksTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Server.registerSearchByLocationTool(userId: UUID) {
    addTool(
        name = "search_by_location",
        description = """
            Search catches (and optionally skunks) by location keyword.
            Case-insensitive partial match — e.g. "lake" finds "Lake Hartwell", "Catfish Lake", etc.
            Returns a human-readable summary with IDs for follow-up lookups.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keyword") {
                    put("type", "string")
                    put("description", "Location keyword to search for (case-insensitive partial match)")
                }
                putJsonObject("include_skunks") {
                    put("type", "boolean")
                    put("description", "If true, also search no-catch trips for this location. Default: false.")
                }
            },
            required = listOf("keyword")
        )
    ) { request ->
        val args = request.params.arguments ?: emptyMap()
        val keyword = args["keyword"]?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'keyword' is required")), isError = true
            )
        val includeSkunks = args["include_skunks"]?.jsonPrimitive?.booleanOrNull ?: false
        val pattern = "%${keyword.lowercase()}%"

        data class Result(val type: String, val id: String, val species: String?,
                          val location: String?, val date: String?, val notes: String?)

        val results = mutableListOf<Result>()

        transaction {
            CatchesTable
                .select(CatchesTable.id, CatchesTable.species, CatchesTable.location,
                    CatchesTable.caughtAt, CatchesTable.notes)
                .where { (CatchesTable.userId eq userId) and (CatchesTable.location.lowerCase() like pattern) }
                .orderBy(CatchesTable.caughtAt, SortOrder.DESC)
                .forEach { row ->
                    results.add(Result("catch", row[CatchesTable.id].toString(),
                        row[CatchesTable.species], row[CatchesTable.location],
                        row[CatchesTable.caughtAt]?.toString(), row[CatchesTable.notes]))
                }

            if (includeSkunks) {
                SkunksTable
                    .select(SkunksTable.id, SkunksTable.location, SkunksTable.fishedAt, SkunksTable.notes)
                    .where { (SkunksTable.userId eq userId) and (SkunksTable.location.lowerCase() like pattern) }
                    .orderBy(SkunksTable.fishedAt, SortOrder.DESC)
                    .forEach { row ->
                        results.add(Result("skunk", row[SkunksTable.id].toString(),
                            null, row[SkunksTable.location],
                            row[SkunksTable.fishedAt]?.toString(), row[SkunksTable.notes]))
                    }
            }
        }

        val text = buildString {
            appendLine("Found ${results.size} result(s) for location keyword '$keyword':")
            results.forEach { r ->
                append("- [${r.type.uppercase()}] ${r.date ?: "unknown date"} @ ${r.location ?: "unknown"}")
                r.species?.let { append(" | $it") }
                if (!r.notes.isNullOrBlank()) append(" | has notes")
                appendLine()
                appendLine("  ID: ${r.id}")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
