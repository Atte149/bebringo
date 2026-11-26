package org.dueattendant149.bebringo.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.domain.model.ChatMessage
import org.dueattendant149.bebringo.domain.model.ChatScenario
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTextField
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components.MascotPlaceholder
import org.dueattendant149.bebringo.ui.components.SecondaryButton
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/**
 * Чат с носителем.
 *
 * Если ключ не задан, экран честно говорит об этом и не притворяется, что связь есть.
 */
@Composable
fun ChatScreen(
    scenario: ChatScenario,
    state: ChatUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onFinish: () -> Unit,
    onDismissReview: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val colors = BebringoTheme.colors
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .imePadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = "Чат")

        BebringoCard(modifier = Modifier.padding(horizontal = Spacing.screen)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MascotPlaceholder(size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scenario.titleRo,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBg,
                    )
                    Text(
                        text = "${scenario.blocksPassed} из ${scenario.blocksTotal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.m))

        if (!state.configured) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.Center,
            ) {
                BebringoCard {
                    Text(
                        text = "Чат не настроен",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBg,
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = "Чтобы говорить с носителем, укажите адрес модели, название и ключ в настройках. " +
                            "Без этого приложение работает как обычно.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(Spacing.l))
                    SecondaryButton(text = "Открыть настройки", onClick = onOpenSettings)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                items(state.messages) { message -> MessageBubble(message) }
                if (state.sending) {
                    item {
                        Text(
                            text = "Носитель печатает…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
            }
        }

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.error,
                modifier = Modifier.padding(horizontal = Spacing.screen),
            )
        }

        if (state.configured) {
            Row(
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(horizontal = Spacing.screen, vertical = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalAlignment = Alignment.Bottom,
            ) {
                BebringoTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = "Написать сообщение…",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.primary, CircleShape)
                        .clickable {
                            if (draft.isNotBlank()) {
                                onSend(draft)
                                draft = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "↑",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onPrimary,
                    )
                }
            }

            if (state.canFinish) {
                TextButton(onClick = onFinish, modifier = Modifier.padding(horizontal = Spacing.screen)) {
                    Text(text = "Закончить и разобрать", color = colors.primary)
                }
            }
        }
    }

    if (state.review != null) {
        AlertDialog(
            onDismissRequest = onDismissReview,
            confirmButton = {
                TextButton(onClick = onDismissReview) { Text("Понятно") }
            },
            title = { Text("Разбор") },
            text = { Text(state.review) },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val colors = BebringoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!message.fromUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(colors.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "•", color = colors.muted)
            }
            Spacer(Modifier.size(Spacing.s))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (message.fromUser) colors.primary else colors.surface,
                    RoundedCornerShape(Radius.md),
                )
                .padding(Spacing.m),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.fromUser) colors.onPrimary else colors.onBg,
            )
            Text(
                text = message.timeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (message.fromUser) colors.onPrimary.copy(alpha = 0.7f) else colors.muted,
            )
        }
    }
}
