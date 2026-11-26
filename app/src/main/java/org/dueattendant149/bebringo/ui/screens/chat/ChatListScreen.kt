package org.dueattendant149.bebringo.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.domain.model.ChatScenario
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components.MascotPlaceholder
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Список сценариев разговора. */
@Composable
fun ChatListScreen(
    scenarios: List<ChatScenario>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        BebringoTopBar(onBack = onBack, title = "Чат")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(
                text = "Разговор по сценарию. Ошибки не исправляются на ходу — в конце будет разбор.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )

            scenarios.forEach { scenario ->
                BebringoCard(onClick = { onOpen(scenario.id) }) {
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
                                text = scenario.titleRu,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                            )
                        }
                        Text(text = "›", style = MaterialTheme.typography.headlineSmall, color = colors.muted)
                    }
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = "${scenario.blocksPassed} из ${scenario.blocksTotal} пройдено",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}
