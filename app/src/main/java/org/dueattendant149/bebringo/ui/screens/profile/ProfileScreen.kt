package org.dueattendant149.bebringo.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.GroupedListCard
import org.dueattendant149.bebringo.ui.components.GroupedListRow
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Профиль (Material Design 3): сгруппированные секции настроек, ИИ, бэкапа и лаборатории. */
@Composable
fun ProfileScreen(
    streakDays: Int,
    onLab: () -> Unit,
    onBackup: () -> Unit,
    onTelemetry: () -> Unit,
    onLlm: () -> Unit = {},
    onDiacritics: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBg,
        )

        BebringoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Mascot(pose = MascotPose.AVATAR, size = 72.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Модульный путь обучения",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBg,
                    )
                    Text(
                        text = "Серия практики: $streakDays дн. · Румынский A1",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✓", color = colors.onPrimaryContainer)
                }
            }
        }

        Text(
            text = "Обучение и оформление",
            style = MaterialTheme.typography.labelLarge,
            color = colors.muted,
        )

        GroupedListCard {
            GroupedListRow(
                title = "Диакритика, FSRS и Material You",
                emoji = "ă",
                subtitle = "Строгость диакритики, уровень подпорки, удержание памяти и тема",
                onClick = onDiacritics,
                showDivider = false,
            )
        }

        Text(
            text = "ИИ и резервные копии",
            style = MaterialTheme.typography.labelLarge,
            color = colors.muted,
        )

        GroupedListCard {
            GroupedListRow(
                title = "ИИ-собеседник (OpenAI API)",
                emoji = "🤖",
                subtitle = "Настройка модели для диалогов и разбора рефлексии",
                onClick = onLlm,
            )
            GroupedListRow(
                title = "Резервная копия и импорт",
                emoji = "☁️",
                subtitle = "Локальный JSON, восстановление и WebDAV",
                onClick = onBackup,
                showDivider = false,
            )
        }

        Text(
            text = "Исследование и метрики",
            style = MaterialTheme.typography.labelLarge,
            color = colors.muted,
        )

        GroupedListCard {
            GroupedListRow(
                title = "Лаборатория механик",
                emoji = "🧪",
                subtitle = "Приверженность за 28 дней и личные эксперименты",
                onClick = onLab,
            )
            GroupedListRow(
                title = "Телеметрия (CSV)",
                emoji = "📊",
                subtitle = "Локальный журнал ответов и выгрузка",
                onClick = onTelemetry,
                showDivider = false,
            )
        }

        Spacer(Modifier.height(Spacing.l))
    }
}
