package com.hooked.chat

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject

class McpClient(private val config: Config) {
    data class Config(
        val jarPath: String,
        val userEmail: String,
        val databaseUrl: String,
        val openWeatherKey: String?,
        val tavilyKey: String?
    )

    private lateinit var process: Process
    private lateinit var client: Client

    suspend fun connect() {
        process = ProcessBuilder("java", "-jar", config.jarPath)
            .apply {
                environment()["USER_EMAIL"] = config.userEmail
                environment()["DATABASE_URL"] = config.databaseUrl
                config.openWeatherKey?.let { environment()["OPENWEATHER_API_KEY"] = it }
                config.tavilyKey?.let { environment()["TAVILY_API_KEY"] = it }
                redirectErrorStream(false)  // MCP server writes diagnostics to stderr
            }.start()

        // Pipe MCP server stderr to our stderr so we can see its logs
        Thread { process.errorStream.copyTo(System.err) }.start()

        client = Client(clientInfo = Implementation("hooked-chat-server", "1.0.0"))
        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered()
        )
        client.connect(transport)
    }

    suspend fun listTools(): List<Tool> = client.listTools()?.tools ?: emptyList()

    suspend fun callTool(name: String, args: JsonObject): String {
        val result = client.callTool(name, args)
        return result?.content?.filterIsInstance<TextContent>()
            ?.joinToString("\n") { it.text } ?: "(no result)"
    }

    fun close() {
        runCatching { process.destroy() }
    }
}
