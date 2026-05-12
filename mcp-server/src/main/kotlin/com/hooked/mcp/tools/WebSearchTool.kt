package com.hooked.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Web search via Tavily. Used to fetch info not in the user's catch history:
 * stocking reports, current fishing reports, hatch info, regulations, etc.
 *
 * Requires TAVILY_API_KEY env var. If absent, the tool returns a gentle error
 * so the LLM knows to fall back to baked-in knowledge.
 */
fun Server.registerWebSearchTool(tavilyApiKey: String?) {
    addTool(
        name = "web_search",
        description = """
            Search the web for fishing-related information not in the user's catch history:
            local fishing reports, hatches, stocking reports, regulations, species you
            don't have data on, or current conditions at a destination. Returns the top
            results with titles, content snippets, and URLs. Cite URLs when you use a result.
            Only call this when the user's personal data + your built-in knowledge aren't
            enough — it costs an external API call.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "Search query — be specific (location, species, dates)")
                }
                putJsonObject("max_results") {
                    put("type", "integer")
                    put("description", "Maximum results to return (default 5, max 10)")
                }
            },
            required = listOf("query")
        )
    ) { request ->
        if (tavilyApiKey.isNullOrBlank()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "Web search not configured (set TAVILY_API_KEY). Fall back to built-in knowledge."
            )))
        }

        val args = request.params.arguments
        val query = args?.get("query")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'query' parameter is required")), isError = true
            )
        val maxResults = (args["max_results"]?.jsonPrimitive?.intOrNull ?: 5).coerceIn(1, 10)

        val body = buildJsonObject {
            put("query", query)
            put("max_results", maxResults)
            put("search_depth", "basic")
        }.toString()

        val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        val req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.tavily.com/search"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $tavilyApiKey")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = try {
            http.send(req, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "Web search failed: ${e.message}"
            )), isError = true)
        }

        if (response.statusCode() != 200) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "Tavily returned ${response.statusCode()}: ${response.body().take(200)}"
            )), isError = true)
        }

        val parsed = try {
            Json { ignoreUnknownKeys = true }
                .decodeFromString<TavilyResponse>(response.body())
        } catch (e: Exception) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "Couldn't parse web search response: ${e.message}"
            )), isError = true)
        }

        if (parsed.results.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent("No results for: $query")))
        }

        val text = buildString {
            appendLine("Web search: \"$query\" — ${parsed.results.size} results")
            parsed.answer?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine("DIRECT ANSWER:")
                appendLine("  $it")
            }
            appendLine()
            parsed.results.forEachIndexed { i, r ->
                appendLine("${i + 1}. ${r.title}")
                appendLine("   ${r.url}")
                r.content.lineSequence().take(3).forEach { line ->
                    if (line.isNotBlank()) appendLine("   ${line.trim()}")
                }
                appendLine()
            }
        }
        CallToolResult(content = listOf(TextContent(text)))
    }
}

@Serializable
private data class TavilyResponse(
    val query: String? = null,
    val answer: String? = null,
    val results: List<TavilyResult> = emptyList()
)

@Serializable
private data class TavilyResult(
    val title: String = "",
    val url: String = "",
    val content: String = ""
)
