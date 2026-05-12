package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import com.hooked.mcp.tables.SkunksTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.math.roundToInt

/**
 * For a given species, rank locations by catches AND by hit rate (catches per
 * session, counting skunks as sessions). Surfaces locations that punch above
 * their weight — e.g. "you've only been to Castle Rock 4 times but caught Bass
 * every time."
 */
fun Server.registerCompareLocationsTool(userId: UUID) {
    addTool(
        name = "compare_locations",
        description = """
            For a single species, compare locations by total catches AND by hit rate
            (catches of this species ÷ total sessions at that location, where a session is
            a catch or a skunk on a given date). Surfaces underused-but-productive spots
            and overused-but-stale spots. Use this for "where should I focus for Bass?"
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("species") {
                    put("type", "string")
                    put("description", "Species name (case-insensitive partial match, e.g. 'bass')")
                }
            },
            required = listOf("species")
        )
    ) { request ->
        val speciesQuery = request.params.arguments?.get("species")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'species' parameter is required")), isError = true
            )
        val pattern = "%${speciesQuery.lowercase()}%"

        data class CatchRow(val location: String?, val date: java.time.LocalDate?, val isTarget: Boolean)
        data class SkunkRow(val location: String?, val date: java.time.LocalDate?)

        val allCatches: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.species, CatchesTable.location, CatchesTable.caughtAt)
                .where { CatchesTable.userId eq userId }
                .map { row ->
                    val sp = row[CatchesTable.species]?.lowercase() ?: ""
                    val isTarget = sp.contains(speciesQuery.lowercase())
                    CatchRow(
                        location = row[CatchesTable.location]?.trim()?.takeIf { it.isNotEmpty() },
                        date = row[CatchesTable.caughtAt]?.toLocalDate(),
                        isTarget = isTarget
                    )
                }
        }

        val allSkunks: List<SkunkRow> = transaction {
            SkunksTable
                .select(SkunksTable.location, SkunksTable.fishedAt)
                .where { SkunksTable.userId eq userId }
                .map { row ->
                    SkunkRow(
                        location = row[SkunksTable.location]?.trim()?.takeIf { it.isNotEmpty() },
                        date = row[SkunksTable.fishedAt]?.toLocalDate()
                    )
                }
        }

        val targetByLocation = allCatches.filter { it.isTarget && it.location != null }
            .groupBy { it.location!! }
            .mapValues { it.value.size }

        if (targetByLocation.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No catches of '$speciesQuery' found at any location."
            )))
        }

        // Sessions = distinct (location, date) pairs across catches + skunks.
        val sessionsByLocation: Map<String, Int> = run {
            val pairs = mutableSetOf<Pair<String, java.time.LocalDate>>()
            allCatches.forEach { c ->
                if (c.location != null && c.date != null) pairs += c.location to c.date
            }
            allSkunks.forEach { s ->
                if (s.location != null && s.date != null) pairs += s.location to s.date
            }
            pairs.groupingBy { it.first }.eachCount()
        }

        data class Row(val location: String, val targetCount: Int, val sessions: Int, val hitRate: Double)

        val rows = targetByLocation.map { (loc, targetCount) ->
            val sessions = sessionsByLocation[loc] ?: targetCount
            val hitRate = targetCount.toDouble() / sessions.coerceAtLeast(1)
            Row(loc, targetCount, sessions, hitRate)
        }

        val byVolume = rows.sortedByDescending { it.targetCount }
        val byHitRate = rows
            .filter { it.sessions >= 2 } // require at least 2 sessions for a meaningful rate
            .sortedByDescending { it.hitRate }

        val text = buildString {
            appendLine("Location comparison for '$speciesQuery' (${rows.size} spots with catches)")
            appendLine()
            appendLine("BY TOTAL CATCHES:")
            byVolume.take(6).forEach { r ->
                appendLine("  ${r.location} — ${r.targetCount} catches across ${r.sessions} sessions (${(r.hitRate * 100).roundToInt()}% hit rate)")
            }
            appendLine()
            if (byHitRate.isNotEmpty()) {
                appendLine("BY HIT RATE (min 2 sessions):")
                byHitRate.take(6).forEach { r ->
                    appendLine("  ${r.location} — ${(r.hitRate * 100).roundToInt()}% (${r.targetCount}/${r.sessions})")
                }
                appendLine()
            }

            // Flag the most interesting comparisons: top-rate vs top-volume.
            val volumeLeader = byVolume.firstOrNull()
            val rateLeader = byHitRate.firstOrNull()
            if (volumeLeader != null && rateLeader != null && rateLeader.location != volumeLeader.location) {
                appendLine("NOTE: Top-volume spot (${volumeLeader.location}, ${(volumeLeader.hitRate * 100).roundToInt()}% rate) differs from top-rate spot (${rateLeader.location}, ${(rateLeader.hitRate * 100).roundToInt()}% rate). The rate leader may be underused.")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
