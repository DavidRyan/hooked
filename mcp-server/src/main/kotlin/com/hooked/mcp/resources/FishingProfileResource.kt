package com.hooked.mcp.resources

import com.hooked.mcp.tables.CatchesTable
import com.hooked.mcp.tables.SkunksTable
import com.hooked.mcp.tables.UsersTable
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ResourceContents
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.Server
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Server.registerFishingProfileResource(userId: UUID, userEmail: String) {
    addResource(
        uri = "fishing://profile",
        name = "My Fishing Profile",
        description = "Summary of the user's fishing history: total catches, top species, top locations, and most recent catch. Read this for background context about the user's fishing habits.",
        mimeType = "text/plain"
    ) { _ ->
        val text = transaction {
            val catchRows = CatchesTable
                .select(CatchesTable.species, CatchesTable.location, CatchesTable.caughtAt)
                .where { CatchesTable.userId eq userId }
                .toList()

            val skunkCount = SkunksTable
                .select(SkunksTable.id)
                .where { SkunksTable.userId eq userId }
                .count()

            val user = UsersTable
                .select(UsersTable.firstName, UsersTable.lastName)
                .where { UsersTable.id eq userId }
                .firstOrNull()

            val firstName = user?.get(UsersTable.firstName)
            val lastName = user?.get(UsersTable.lastName)

            val topSpecies = catchRows
                .mapNotNull { it[CatchesTable.species]?.trim() }.filter { it.isNotEmpty() }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(3)

            val topLocations = catchRows
                .mapNotNull { it[CatchesTable.location]?.trim() }.filter { it.isNotEmpty() }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(3)

            val lastCatch = catchRows
                .mapNotNull { it[CatchesTable.caughtAt] }
                .maxOrNull()

            buildString {
                appendLine("FISHING PROFILE")
                appendLine("User: ${listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { userEmail }}")
                appendLine("Email: $userEmail")
                appendLine()
                appendLine("TOTALS:")
                appendLine("  Total catches: ${catchRows.size}")
                appendLine("  Total skunks (no-catch trips): $skunkCount")
                appendLine()
                appendLine("TOP SPECIES:")
                if (topSpecies.isEmpty()) appendLine("  (no species recorded yet)")
                else topSpecies.forEach { (s, c) -> appendLine("  $s ($c catches)") }
                appendLine()
                appendLine("TOP LOCATIONS:")
                if (topLocations.isEmpty()) appendLine("  (no locations recorded yet)")
                else topLocations.forEach { (l, c) -> appendLine("  $l ($c catches)") }
                appendLine()
                lastCatch?.let { appendLine("LAST CATCH: $it") }
                    ?: appendLine("LAST CATCH: none recorded")
            }
        }

        ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    uri = "fishing://profile",
                    mimeType = "text/plain",
                    text = text
                )
            )
        )
    }
}
