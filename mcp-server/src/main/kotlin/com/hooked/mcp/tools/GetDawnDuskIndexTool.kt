package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.math.roundToInt

/**
 * For each species the user has caught, compute a dawn-vs-dusk-vs-midday index based
 * on hour of day. Classifies each species as a dawn / dusk / twilight / midday / anytime
 * specialist by comparing actual band share to a uniform-time baseline.
 */
fun Server.registerGetDawnDuskIndexTool(userId: UUID) {
    addTool(
        name = "get_dawn_dusk_index",
        description = """
            For each species, compute how skewed it is toward dawn (5-8am), dusk (5-9pm),
            and midday (10am-3pm). Returns a per-species classification (dawn / dusk /
            twilight / midday / anytime) plus the underlying band shares. Use this for
            light-driven biology questions: "which fish do I catch at dawn?" or
            "is it true walleye are a twilight bite?"
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = emptyList()
        )
    ) { _ ->
        data class CatchRow(val species: String, val hour: Int)

        val rows: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.species, CatchesTable.caughtAt)
                .where { CatchesTable.userId eq userId }
                .mapNotNull { row ->
                    val sp = row[CatchesTable.species]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    val hr = row[CatchesTable.caughtAt]?.hour ?: return@mapNotNull null
                    CatchRow(sp, hr)
                }
        }

        if (rows.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent("No catch data with timestamps.")))
        }

        // Bands as fractions of 24 hours (used for expected baseline).
        val dawnHours = (5..7).toSet()      // 3 hours = 12.5%
        val duskHours = (17..20).toSet()    // 4 hours = 16.7%
        val middayHours = (10..14).toSet()  // 5 hours = 20.8%

        val dawnExpected = dawnHours.size / 24.0
        val duskExpected = duskHours.size / 24.0
        val middayExpected = middayHours.size / 24.0

        data class Profile(
            val species: String,
            val total: Int,
            val dawnPct: Double,
            val duskPct: Double,
            val middayPct: Double,
            val classification: String
        )

        val profiles = rows.groupBy { it.species }.mapNotNull { (sp, list) ->
            if (list.size < 3) return@mapNotNull null
            val n = list.size.toDouble()
            val dawnFrac = list.count { it.hour in dawnHours } / n
            val duskFrac = list.count { it.hour in duskHours } / n
            val middayFrac = list.count { it.hour in middayHours } / n

            val dawnLift = dawnFrac / dawnExpected
            val duskLift = duskFrac / duskExpected
            val middayLift = middayFrac / middayExpected

            val classification = when {
                dawnLift >= 2.0 && duskLift >= 2.0 -> "twilight specialist"
                dawnLift >= 2.0                     -> "dawn specialist"
                duskLift >= 2.0                     -> "dusk specialist"
                middayLift >= 2.0                   -> "midday feeder"
                dawnLift >= 1.4 || duskLift >= 1.4  -> "leans twilight"
                middayLift >= 1.4                   -> "leans midday"
                else                                -> "anytime"
            }

            Profile(sp, list.size, dawnFrac, duskFrac, middayFrac, classification)
        }.sortedByDescending { it.total }

        if (profiles.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "Not enough catches per species to compute a dawn/dusk index (need >=3 of a species)."
            )))
        }

        val text = buildString {
            appendLine("Dawn / dusk / midday index (species with >=3 catches)")
            appendLine("Bands: Dawn 5-8am | Midday 10am-3pm | Dusk 5-9pm | Baseline shares: 12.5% / 20.8% / 16.7%")
            appendLine()
            profiles.forEach { p ->
                appendLine("  ${p.species} (n=${p.total}) — ${p.classification}")
                appendLine("    Dawn ${(p.dawnPct * 100).roundToInt()}%  •  Midday ${(p.middayPct * 100).roundToInt()}%  •  Dusk ${(p.duskPct * 100).roundToInt()}%")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
