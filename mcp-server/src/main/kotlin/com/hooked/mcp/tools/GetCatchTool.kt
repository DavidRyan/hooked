package com.hooked.mcp.tools

import com.hooked.mcp.models.CatchDetail
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

fun Server.registerGetCatchTool(userId: UUID) {
    addTool(
        name = "get_catch",
        description = """
            Get full details for a single catch by its UUID.
            Includes species, location, coordinates, notes, image URL,
            weather conditions at time of catch, and enrichment status.
            Use list_catches to find catch IDs.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("id") {
                    put("type", "string")
                    put("description", "UUID of the catch to retrieve")
                }
            },
            required = listOf("id")
        )
    ) { request ->
        val catchId = request.params.arguments?.get("id")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'id' parameter is required")), isError = true
            )

        val catchUuid = runCatching { UUID.fromString(catchId) }.getOrElse {
            return@addTool CallToolResult(
                content = listOf(TextContent("Error: '$catchId' is not a valid UUID")), isError = true
            )
        }

        val detail: CatchDetail? = transaction {
            CatchesTable.selectAll()
                .where { (CatchesTable.id eq catchUuid) and (CatchesTable.userId eq userId) }
                .firstOrNull()
                ?.let { row ->
                    CatchDetail(
                        id = row[CatchesTable.id].toString(),
                        species = row[CatchesTable.species],
                        location = row[CatchesTable.location],
                        latitude = row[CatchesTable.latitude],
                        longitude = row[CatchesTable.longitude],
                        caughtAt = row[CatchesTable.caughtAt]?.toString(),
                        notes = row[CatchesTable.notes],
                        weatherData = row[CatchesTable.weatherData],
                        imageUrl = row[CatchesTable.imageUrl],
                        enrichmentStatus = row[CatchesTable.enrichmentStatus],
                        insertedAt = row[CatchesTable.insertedAt].toString(),
                        updatedAt = row[CatchesTable.updatedAt].toString()
                    )
                }
        }

        if (detail == null) {
            CallToolResult(content = listOf(TextContent("No catch found with id: $catchId")), isError = true)
        } else {
            CallToolResult(content = listOf(TextContent(Json.encodeToString(detail))))
        }
    }
}
