package com.hooked.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hooked.chat.model.ChatEffect
import com.hooked.chat.model.ChatIntent
import com.hooked.core.presentation.toast.ToastManager
import com.hooked.theme.Colors
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    starterPrompt: String? = null,
    viewModel: ChatViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Auto-send starter prompt once after connection settles
    LaunchedEffect(starterPrompt, state.isConnecting) {
        if (!starterPrompt.isNullOrBlank() && !state.isConnecting && state.messages.isEmpty()) {
            viewModel.sendIntent(ChatIntent.SendMessage(starterPrompt))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                ChatEffect.ScrollToBottom -> {
                    val target = state.messages.lastIndex.coerceAtLeast(0)
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(target)
                    }
                }
                is ChatEffect.ShowError -> toastManager.showError(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Colors.text
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Colors.text
                )
            )
        },
        containerColor = Colors.base
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.messages.isEmpty() && !state.isAssistantThinking) {
                EmptyChatState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        MessageBubble(message = msg)
                    }
                    if (state.isAssistantThinking) {
                        item("thinking") { ThinkingIndicator(toolName = state.activeToolCall) }
                    }
                }
            }

            ChatInputBar(
                value = state.inputText,
                onValueChange = { viewModel.sendIntent(ChatIntent.UpdateInput(it)) },
                onSend = { viewModel.sendIntent(ChatIntent.SendMessage(state.inputText)) },
                enabled = !state.isConnecting
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    if (isUser) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                    else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                )
                .background(if (isUser) Colors.primary else Colors.surface1)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isUser) Colors.onPrimary else Colors.text
            )
        }
    }
}

@Composable
private fun ThinkingIndicator(toolName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Colors.surface1)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Colors.primary
            )
            Text(
                text = if (toolName.isNullOrBlank()) "Thinking…" else "Looking up $toolName…",
                style = MaterialTheme.typography.bodyMedium,
                color = Colors.subtext1
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .background(Colors.surface0)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask the fishing expert…", color = Colors.subtext0) },
            shape = RoundedCornerShape(20.dp),
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Colors.surface1,
                unfocusedContainerColor = Colors.surface1,
                focusedBorderColor = Colors.overlay1,
                unfocusedBorderColor = Colors.overlay0,
                focusedTextColor = Colors.text,
                unfocusedTextColor = Colors.text,
                cursorColor = Colors.primary
            )
        )

        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (enabled && value.isNotBlank()) Colors.primary else Colors.surface2)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (enabled && value.isNotBlank()) Colors.onPrimary else Colors.subtext1
            )
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ask the fishing expert",
            style = MaterialTheme.typography.headlineMedium,
            color = Colors.text,
            textAlign = TextAlign.Center
        )
        Text(
            text = "It knows your full catch history. Try \"where do I usually catch bass?\" or \"what conditions worked best last summer?\"",
            style = MaterialTheme.typography.bodyLarge,
            color = Colors.subtext1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
