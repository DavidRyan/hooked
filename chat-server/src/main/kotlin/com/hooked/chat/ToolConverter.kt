package com.hooked.chat

import com.openai.core.JsonValue
import com.openai.models.ChatCompletionTool
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun Tool.toOpenAiTool(): ChatCompletionTool =
    ChatCompletionTool.builder()
        .function(
            FunctionDefinition.builder()
                .name(name)
                .description(description ?: "")
                .parameters(
                    FunctionParameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty("properties",
                            JsonValue.from(inputSchema.properties?.toPlain() ?: emptyMap<String, Any?>()))
                        .putAdditionalProperty("required",
                            JsonValue.from(inputSchema.required ?: emptyList<String>()))
                        .build()
                )
                .build()
        )
        .build()

private fun JsonElement.toPlain(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> if (isString) content
        else content.toBooleanStrictOrNull()
            ?: content.toLongOrNull()
            ?: content.toDoubleOrNull()
            ?: content
    is JsonArray -> map { it.toPlain() }
    is JsonObject -> entries.associate { (k, v) -> k to v.toPlain() }
}
