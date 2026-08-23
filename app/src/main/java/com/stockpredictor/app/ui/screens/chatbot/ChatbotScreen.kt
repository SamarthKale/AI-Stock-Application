package com.stockpredictor.app.ui.screens.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.data.local.entity.ChatMessageEntity
import com.stockpredictor.app.data.local.entity.ChatRole
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.ClayTextField
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun ChatbotScreen(
    onBack: () -> Unit,
    viewModel: ChatbotViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Ask AI", onBack = onBack)
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is UiState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
                is UiState.Empty -> EmptyState(message = "Ask a question to start the conversation.", modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.fillMaxSize())
                is UiState.Success -> MessageList(messages = s.data, isSending = isSending, modifier = Modifier.fillMaxSize())
            }
        }
        if (sendError != null) {
            Text(
                text = sendError ?: "",
                color = ClayColor.AccentCoral,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Xs),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(ClaySpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClayTextField(
                value = draft,
                onValueChange = { draft = it },
                label = "Message",
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(ClaySpacing.Sm))
            ClayButton(
                text = "Send",
                loading = isSending,
                onClick = {
                    viewModel.sendMessage(draft)
                    draft = ""
                },
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessageEntity>,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1 + if (isSending) 1 else 0).coerceAtLeast(0))
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = ClaySpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
        contentPadding = PaddingValues(vertical = ClaySpacing.Md),
    ) {
        items(messages, key = { it.id }) { message -> ChatBubble(message) }
        if (isSending) {
            item(key = "typing-indicator") { TypingIndicator() }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.role == ChatRole.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        ClayCard(
            shape = ClayShapes.Medium,
            padding = ClaySpacing.Md,
            backgroundColor = if (isUser) ClayColor.AccentPrimaryPressed else ClayColor.ClayBase,
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
        ) {
            Text(text = message.content, color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        ClayCard(shape = ClayShapes.Medium, padding = ClaySpacing.Md) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(ClayDimens.SpinnerSize), color = ClayColor.TextSecondary, strokeWidth = ClayDimens.SpinnerStroke)
                Spacer(modifier = Modifier.width(ClaySpacing.Sm))
                Text("Assistant is typing…", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
