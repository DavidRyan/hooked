package com.hooked.chat

import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionTool
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class AgentLoop(
    private val openAi: OpenAIClient,
    private val tools: List<ChatCompletionTool>,
    private val mcpClient: McpClient
) {
    /**
     * Run the agentic loop for a single user turn.
     * [paramsBuilder] accumulates conversation history across turns — pass the same
     * builder for the lifetime of a WebSocket session.
     */
    suspend fun run(
        paramsBuilder: ChatCompletionCreateParams.Builder,
        userMessage: String,
        onEvent: suspend (String) -> Unit
    ): String {
        paramsBuilder.addUserMessage(userMessage)

        while (true) {
            val response = withContext(Dispatchers.IO) {
                openAi.chat().completions().create(paramsBuilder.build())
            }

            val message = response.choices().first().message()

            // Add assistant message to history (including any tool_calls)
            paramsBuilder.addMessage(message)

            val toolCalls = message.toolCalls().orElse(emptyList())
            if (toolCalls.isEmpty()) {
                return message.content().orElse("(no response)")
            }

            // Execute each tool call and add results to history
            for (toolCall in toolCalls) {
                val name = toolCall.function().name()
                onEvent("""{"type":"tool_call","name":${Json.encodeToString(name)}}""")

                val args = Json.parseToJsonElement(toolCall.function().arguments()).jsonObject
                val result = runCatching { mcpClient.callTool(name, args) }
                    .getOrElse { e -> "Error calling $name: ${e.message}" }

                paramsBuilder.addMessage(
                    ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCall.id())
                        .content(result)
                        .build()
                )
            }
            // Loop: call OpenAI again with tool results
        }
    }
}
