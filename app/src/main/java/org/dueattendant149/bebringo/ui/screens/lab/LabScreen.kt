package org.dueattendant149.bebringo.ui.screens.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components.GroupedListCard
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.components.ProgressBarWithLabel
import org.dueattendant149.bebringo.ui.components.SuccessFeedbackCard
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

/**
 * Лаборатория механик (Material Design 3).
 *
 * Показывает реальную приверженность за 28 дней и сохраняет переключатели экспериментов в `SettingsStore`.
 */
@Composable
fun LabScreen(
    adherence: Float,
    onBack: () -> Unit,
    onExport: () -> Unit,
    quietStreaks: Boolean = true,
    onQuietStreaksChange: (Boolean) -> Unit = {},
    dailyReview: Boolean = false,
    onDailyReviewChange: (Boolean) -> Unit = {},
    autoSupport: Boolean = true,
    onAutoSupportChange: (Boolean) -> Unit = {},
    exportMessage: String? = null,
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        BebringoTopBar(onBack = onBack, title = "Лаборатория")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            BebringoCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    Mascot(pose = MascotPose.THINKING, size = 72.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Доля дней с сессией (28 дней)",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onBg,
                        )
                        Text(
                            text = "Включайте по одной механике на две недели и оценивайте личный эффект.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.m))
                ProgressBarWithLabel(fraction = adherence, label = "Приверженность")
            }

            GroupedListCard {
                LabToggleRow(
                    title = "Спокойный ремонт серии",
                    subtitle = "Возможность закрыть вчерашний пропуск одним касанием",
                    checked = quietStreaks,
                    onCheckedChange = onQuietStreaksChange,
                    showDivider = true,
                )
                LabToggleRow(
                    title = "Слово дня с самопроверкой на виджете",
                    subtitle = "Перевод на домашнем виджете открывается по нажатию",
                    checked = autoSupport,
                    onCheckedChange = onAutoSupportChange,
                    showDivider = true,
                )
                LabToggleRow(
                    title = "Усиленное повторение FSRS в блоках",
                    subtitle = "До двух карточек с низким удержанием в конце каждого блока",
                    checked = dailyReview,
                    onCheckedChange = onDailyReviewChange,
                    showDivider = false,
                )
            }

            if (!exportMessage.isNullOrBlank()) {
                SuccessFeedbackCard(title = exportMessage)
            }

            PrimaryButton(text = "Выгрузить телеметрию (CSV)", onClick = onExport)

            Spacer(Modifier.height(Spacing.l))
        }
    }
}

@Composable
private fun LabToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean,
) {
    val colors = BebringoTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = Spacing.m)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.onBg)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                ),
            )
        }
        if (showDivider) {
            HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)
        }
    }
}
