package com.hooked.chat

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.ChatCompletionCreateParams
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(): Unit = runBlocking<Unit> {
    val mcpJar = System.getenv("MCP_SERVER_JAR") ?: error("MCP_SERVER_JAR required")
    val databaseUrl = System.getenv("DATABASE_URL") ?: error("DATABASE_URL required")
    val openAiKey = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY required")
    val hookedApiUrl = System.getenv("HOOKED_API_URL") ?: error("HOOKED_API_URL required (e.g. http://localhost:4000/api)")
    val openWeatherKey = System.getenv("OPENWEATHER_API_KEY")
    val tavilyKey = System.getenv("TAVILY_API_KEY")
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val model = System.getenv("OPENAI_MODEL") ?: "gpt-4o"

    // Single shared OpenAI client. Per-user state (history, MCP subprocess) lives
    // inside each WS session.
    val openAi: OpenAIClient = OpenAIOkHttpClient.builder().apiKey(openAiKey).build()
    val tokenValidator = TokenValidator(hookedApiUrl)

    val systemPrompt = """
        You are a fishing analyst chatting inside the Hooked app. You have tools to query
        the user's complete catch history, weather conditions for each catch, and aggregated
        patterns. The user knows obvious facts already — your job is to find what they don't
        already know.

        ANALYSIS RULES (these matter most):
        - Avoid obvious answers. "You catch most bass in summer" is useless. Look for
          conditional patterns: pressure trend, time of day, wind direction, water temp,
          location-vs-species interactions. Surface the surprising.
        - Always call multiple tools before answering pattern questions. Chain at least 2:
          e.g. get_species_profile + get_best_conditions, or list_catches (filtered) +
          get_catch_weather. Triangulate before claiming a pattern.
        - Compare. "X is 3x more common when Y" is a real insight; "X happens often" is not.
          Look at conditional rates, not raw counts.
        - For weather: pull from weather.main.temp, weather.weather[0].main, weather.wind.speed,
          weather.main.pressure. Compare productive vs unproductive conditions.
        - When sample size is small (e.g. <5 catches of a species), say so explicitly rather
          than overclaiming. "Only 3 Walleye so far — pattern is suggestive, not proven."
        - If asked for a recommendation (when/where to fish), interpret the data — don't just
          report it. Tie it to actionable conditions ("falling pressure + dawn = best window").

        BEYOND THE USER'S DATA (use freely when the data is thin or to validate patterns):
        - Use your built-in knowledge of fish biology, behavior, and ecology. Species spawning
          temperatures, dawn/dusk crepuscular feeding, pre-frontal pressure effects, water-temp
          activity windows — say "biologically, X tends to Y" when the data agrees, or when the
          user's history is too small to be conclusive.
        - get_solunar (sun/moon/feeding periods for a date), get_species_ecology (species
          biology lookup), and get_live_weather (current conditions) are non-history tools you
          should reach for when the user asks predictive or general questions.
        - When personal data and general biology disagree, say so. "Most Bass feed at dawn —
          but in your log, midday has produced 60% more. Could be your local lake's pattern."
        - web_search (if available) for current local fishing reports, stocking news, hatch
          info, or species you don't have data on. Cite urls returned.

        VOICE AND LENGTH:
        - Be terse. 1–3 sentences, ~60 words max, unless explicitly asked for more detail.
        - Lead with the punchline / specific number / surprising fact.
        - No preamble ("Based on your data…", "Great question"). No bullets. No markdown.
        - Reference specific data points (species names, numbers, spot names, conditions).
        - It's OK to admit "the data doesn't show a clear pattern there" when true.
    """.trimIndent()

    System.err.println("[chat-server] Booting on port $port, Hooked API at $hookedApiUrl")

    embeddedServer(Netty, port = port) {
        install(WebSockets)
        routing {
            webSocket("/chat") {
                val token = call.request.queryParameters["token"]
                if (token.isNullOrBlank()) {
                    System.err.println("[chat-server] WS rejected: missing token")
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
                    return@webSocket
                }

                val user = tokenValidator.validate(token)
                if (user == null) {
                    System.err.println("[chat-server] WS rejected: invalid token")
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                    return@webSocket
                }

                System.err.println("[chat-server] WS connected for ${user.email}")

                val mcpClient = McpClient(
                    McpClient.Config(
                        jarPath = mcpJar,
                        userEmail = user.email,
                        databaseUrl = databaseUrl,
                        openWeatherKey = openWeatherKey,
                        tavilyKey = tavilyKey
                    )
                )

                try {
                    mcpClient.connect()
                    val tools = mcpClient.listTools().map { it.toOpenAiTool() }
                    System.err.println("[chat-server] MCP up with ${tools.size} tools for ${user.email}")

                    val agentLoop = AgentLoop(openAi, tools, mcpClient)
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
                } catch (e: Exception) {
                    System.err.println("[chat-server] Session error for ${user.email}: ${e.message}")
                    runCatching {
                        send("""{"type":"error","message":${Json.encodeToString(e.message ?: "session error")}}""")
                    }
                } finally {
                    System.err.println("[chat-server] WS closed for ${user.email}; tearing down MCP")
                    runCatching { mcpClient.close() }
                }
            }
        }
    }.start(wait = true)
}
