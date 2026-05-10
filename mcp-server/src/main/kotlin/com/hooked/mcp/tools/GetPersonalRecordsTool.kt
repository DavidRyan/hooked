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
import java.time.LocalDate
import java.util.UUID

fun Server.registerGetPersonalRecordsTool(userId: UUID) {
    addTool(
        name = "get_personal_records",
        description = """
            Get personal fishing records and milestones: best single day (most catches),
            most common species, rarest species, most productive location,
            longest catch streak (consecutive days with at least one catch),
            and longest skunk streak (consecutive days with only skunks).
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = emptyList()
        )
    ) { _ ->
        data class CatchRow(val date: LocalDate?, val species: String?, val location: String?)

        val (catches, skunkDates) = transaction {
            val c = CatchesTable
                .select(CatchesTable.caughtAt, CatchesTable.species, CatchesTable.location)
                .where { CatchesTable.userId eq userId }
                .orderBy(CatchesTable.caughtAt, SortOrder.ASC)
                .map { row ->
                    CatchRow(
                        date = row[CatchesTable.caughtAt]?.toLocalDate(),
                        species = row[CatchesTable.species]?.trim(),
                        location = row[CatchesTable.location]?.trim()
                    )
                }
            val s = SkunksTable
                .select(SkunksTable.fishedAt)
                .where { SkunksTable.userId eq userId }
                .mapNotNull { it[SkunksTable.fishedAt]?.toLocalDate() }
            Pair(c, s)
        }

        val bestDay = catches.mapNotNull { it.date }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }

        val speciesCounts = catches.mapNotNull { it.species }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount()
        val locationCounts = catches.mapNotNull { it.location }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount()

        fun longestStreak(days: Collection<LocalDate>): Pair<Int, LocalDate?> {
            if (days.isEmpty()) return Pair(0, null)
            val sorted = days.sorted()
            var maxStreak = 1; var curStreak = 1; var maxEnd: LocalDate? = sorted.first()
            for (i in 1 until sorted.size) {
                if (sorted[i] == sorted[i - 1].plusDays(1)) {
                    curStreak++
                    if (curStreak > maxStreak) { maxStreak = curStreak; maxEnd = sorted[i] }
                } else curStreak = 1
            }
            return Pair(maxStreak, maxEnd)
        }

        val (catchStreak, catchStreakEnd) = longestStreak(catches.mapNotNull { it.date }.toSortedSet())
        val (skunkStreak, skunkStreakEnd) = longestStreak(skunkDates.toSortedSet())

        val text = buildString {
            appendLine("Personal Records (${catches.size} total catches, ${skunkDates.size} skunks)")
            appendLine()
            bestDay?.let { (day, count) -> appendLine("BEST DAY: $day with $count catch(es)") }
            speciesCounts.maxByOrNull { it.value }?.let { (s, c) -> appendLine("MOST CAUGHT: $s ($c times)") }
            speciesCounts.minByOrNull { it.value }?.let { (s, c) -> appendLine("RAREST CAUGHT: $s ($c time(s))") }
            locationCounts.maxByOrNull { it.value }?.let { (l, c) -> appendLine("MOST PRODUCTIVE LOCATION: $l ($c catches)") }
            appendLine("UNIQUE SPECIES: ${speciesCounts.size}")
            appendLine("UNIQUE LOCATIONS: ${locationCounts.size}")
            appendLine()
            appendLine("LONGEST CATCH STREAK: $catchStreak consecutive day(s)" +
                (catchStreakEnd?.let { " (ending $it)" } ?: ""))
            appendLine("LONGEST SKUNK STREAK: $skunkStreak consecutive day(s)" +
                (skunkStreakEnd?.let { " (ending $it)" } ?: ""))
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}
