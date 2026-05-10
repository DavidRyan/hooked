package com.hooked.chat

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatCompletionCreateParams
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val mcpJar = System.getenv("MCP_SERVER_JAR") ?: error("MCP_SERVER_JAR required")
    val userEmail = System.getenv("USER_EMAIL") ?: error("USER_EMAIL required")
    val databaseUrl = System.getenv("DATABASE_URL") ?: error("DATABASE_URL required")
    val openAiKey = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY required")
    val openWeatherKey = System.getenv("OPENWEATHER_API_KEY")
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val model = System.getenv("OPENAI_MODEL") ?: "gpt-4o"

    // Start MCP server subprocess and connect
    val mcpClient = McpClient(McpClient.Config(mcpJar, userEmail, databaseUrl, openWeatherKey))
    mcpClient.connect()
    System.err.println("[chat-server] MCP client connected")

    // Fetch and convert tools once at startup
    val tools = mcpClient.listTools().map { it.toOpenAiTool() }
    System.err.println("[chat-server] Loaded ${tools.size} MCP tools")

    val openAi: OpenAIClient = OpenAIOkHttpClient.builder().apiKey(openAiKey).build()
    val agentLoop = AgentLoop(openAi, tools, mcpClient)

    val systemPrompt = """
        You are a helpful fishing assistant for the Hooked app. You have access to the user's
        complete fishing history through tools. Use tools to look up data before answering questions
        about their catches, locations, species, patterns, and conditions. Be conversational and
        specific — reference actual data from their history rather than giving generic advice.
    """.trimIndent()

    // Register shutdown hook before blocking on server start
    Runtime.getRuntime().addShutdownHook(Thread { mcpClient.close() })

    embeddedServer(Netty, port = port) {
        install(WebSockets)
        routing {
            webSocket("/chat") {
                System.err.println("[chat-server] WebSocket connected")

                // Fresh conversation history per session
                val paramsBuilder = ChatCompletionCreateParams.builder()
                    .model(model)
                    .tools(tools)
                paramsBuilder.addSystemMessage(systemPrompt)

                for (frame in incoming) {
                    val userText = (frame as? Frame.Text)?.readText() ?: continue
                    try {
                        val response = agentLoop.run(paramsBuilder, userText) { event -> send(event) }
                        send("""{"type":"message","content":${Json.encodeToString(response)}}""")
                    } catch (e: Exception) {
                        send("""{"type":"error","message":${Json.encodeToString(e.message ?: "error")}}""")
                    }
                }
                System.err.println("[chat-server] WebSocket disconnected")
            }
        }
    }.start(wait = true)
}
