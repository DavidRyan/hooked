package com.hooked.mcp.tools

import com.hooked.mcp.models.CatchSummary
import com.hooked.mcp.tables.CatchesTable
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

fun Server.registerListCatchesTool(userId: UUID) {
    addTool(
        name = "list_catches",
        description = """
            List fishing catches for this user, sorted by date descending (most recent first).
            All parameters are optional — call with no args to get all catches.
            Use species to filter by fish type (e.g. "bass", "trout").
            Use date_from/date_to (YYYY-MM-DD) to filter by date range.
            Use location to filter by location name keyword.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("species") {
                    put("type", "string")
                    put("description", "Filter by species (case-insensitive partial match)")
                }
                putJsonObject("date_from") {
                    put("type", "string")
                    put("description", "Start date inclusive, format YYYY-MM-DD")
                }
                putJsonObject("date_to") {
                    put("type", "string")
                    put("description", "End date inclusive, format YYYY-MM-DD")
                }
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "Filter by location keyword (case-insensitive partial match)")
                }
            },
            required = emptyList()
        )
    ) { request ->
        val args = request.params.arguments ?: emptyMap()
        val speciesFilter = args["species"]?.jsonPrimitive?.contentOrNull
        val dateFrom = args["date_from"]?.jsonPrimitive?.contentOrNull
        val dateTo = args["date_to"]?.jsonPrimitive?.contentOrNull
        val locationFilter = args["location"]?.jsonPrimitive?.contentOrNull

        val catches: List<CatchSummary> = transaction {
            CatchesTable
                .select(CatchesTable.id, CatchesTable.species, CatchesTable.location,
                    CatchesTable.caughtAt, CatchesTable.notes, CatchesTable.imageUrl)
                .where {
                    var cond: Op<Boolean> = CatchesTable.userId eq userId
                    speciesFilter?.let { f -> cond = cond and (CatchesTable.species.lowerCase() like "%${f.lowercase()}%") }
                    locationFilter?.let { f -> cond = cond and (CatchesTable.location.lowerCase() like "%${f.lowercase()}%") }
                    dateFrom?.let { from -> cond = cond and (CatchesTable.caughtAt greaterEq LocalDateTime.parse("${from}T00:00:00")) }
                    dateTo?.let { to -> cond = cond and (CatchesTable.caughtAt lessEq LocalDateTime.parse("${to}T23:59:59")) }
                    cond
                }
                .orderBy(CatchesTable.caughtAt, SortOrder.DESC)
                .map { row ->
                    CatchSummary(
                        id = row[CatchesTable.id].toString(),
                        species = row[CatchesTable.species],
                        location = row[CatchesTable.location],
                        caughtAt = row[CatchesTable.caughtAt]?.toString(),
                        hasNotes = !row[CatchesTable.notes].isNullOrBlank(),
                        imageUrl = row[CatchesTable.imageUrl]
                    )
                }
        }

        CallToolResult(content = listOf(TextContent(
            "Found ${catches.size} catch(es):\n${Json.encodeToString(catches)}"
        )))
    }
}
