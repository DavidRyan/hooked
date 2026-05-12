package com.hooked.mcp

import com.hooked.mcp.resources.registerFishingProfileResource
import com.hooked.mcp.tables.UsersTable
import com.hooked.mcp.tools.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun main() = runBlocking {
    val userEmail = System.getenv("USER_EMAIL")
        ?: error("USER_EMAIL environment variable is required")
    val openWeatherApiKey = System.getenv("OPENWEATHER_API_KEY")
    val tavilyApiKey = System.getenv("TAVILY_API_KEY")

    DatabaseConfig.connect()

    val userId: UUID = transaction {
        UsersTable
            .select(UsersTable.id)
            .where { UsersTable.email eq userEmail }
            .firstOrNull()
            ?.get(UsersTable.id)
            ?: error("No user found for email: $userEmail")
    }

    System.err.println("[hooked-mcp] Connected. Serving data for: $userEmail ($userId)")
    if (openWeatherApiKey == null) {
        System.err.println("[hooked-mcp] OPENWEATHER_API_KEY not set — get_live_weather tool will be unavailable")
    }

    val server = Server(
        serverInfo = Implementation(name = "hooked-mcp-server", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
                resources = ServerCapabilities.Resources(listChanged = false, subscribe = false)
            )
        )
    )

    // Data access tools
    server.registerListCatchesTool(userId)
    server.registerGetCatchTool(userId)
    server.registerGetCatchStatsTool(userId)
    server.registerListSkunksTool(userId)
    server.registerSearchByLocationTool(userId)

    // Pattern analysis tools
    server.registerGetSpeciesProfileTool(userId)
    server.registerGetBestConditionsTool(userId)
    server.registerGetMonthlyBreakdownTool(userId)
    server.registerGetPersonalRecordsTool(userId)
    server.registerComparePeriodsTool(userId)
    server.registerGetConditionalBreakdownTool(userId)
    server.registerGetLocationProfileTool(userId)
    server.registerFindOutlierCatchesTool(userId)
    server.registerEvaluateWindowTool(userId)
    server.registerCompareLocationsTool(userId)
    server.registerGetDawnDuskIndexTool(userId)

    // Weather tools
    server.registerGetCatchWeatherTool(userId)
    server.registerGetLiveWeatherTool(openWeatherApiKey)

    // Non-history knowledge tools (work even with no catches in DB)
    server.registerGetSolunarTool()
    server.registerGetSpeciesEcologyTool()
    server.registerWebSearchTool(tavilyApiKey)

    // Resources
    server.registerFishingProfileResource(userId, userEmail)

    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )

    // Server.connect() in mcp-kotlin-sdk 0.8.3 returns once the session is wired up
    // and continues processing in the background. Keep main alive until the session
    // ends, otherwise the JVM exits immediately after registering tools.
    val done = CompletableDeferred<Unit>()
    server.onClose { done.complete(Unit) }
    server.connect(transport)
    done.await()

    System.err.println("[hooked-mcp] Server shut down.")
}
