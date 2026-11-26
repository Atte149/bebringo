package org.dueattendant149.bebringo.ui.screens.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.domain.model.MistakeNote
import org.dueattendant149.bebringo.ui.components.FeedbackCard
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/**
 * Разбор урока или его части (Material Design 3).
 * Показывает не больше трёх ошибок/заметок по диакритике и маскота-бобра в позе праздника.
 */
@Composable
fun BlockReviewScreen(
    correct: Int,
    total: Int,
    mistakes: List<MistakeNote>,
    onContinue: () -> Unit,
    partIndex: Int = 1,
    partsTotal: Int = 1,
    onContinueLesson: (() -> Unit)? = null,
) {
    val colors = BebringoTheme.colors
    val perfect = mistakes.isEmpty() && correct >= total
    val isFinalPart = partIndex >= partsTotal

    // Назад с разбора не должен возвращать в уже пройденный урок.
    BackHandler(onBack = onContinue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Mascot(
            pose = if (perfect) MascotPose.CELEBRATE else MascotPose.THUMBS_UP,
            size = 148.dp,
        )

        Text(
            text = when {
                partsTotal > 1 && !isFinalPart -> "Часть $partIndex из $partsTotal завершена"
                partsTotal > 1 -> "Урок завершён!"
                else -> "Блок завершён!"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBg,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .background(colors.primaryContainer, RoundedCornerShape(Radius.pill))
                .padding(horizontal = Spacing.xl, vertical = Spacing.s),
        ) {
            Text(
                text = "Верно с первой попытки: $correct из $total",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
            )
        }

        if (mistakes.isEmpty()) {
            Text(
                text = "Ни одной помарки — слова добавлены в интервальное повторение FSRS.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Короткий разбор на заметку",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBg,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                mistakes.take(3).forEach { mistake ->
                    FeedbackCard(
                        prompt = mistake.prompt,
                        correction = mistake.correct,
                        explanation = mistake.explanationRu,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.l))

        if (onContinueLesson != null) {
            PrimaryButton(text = "Продолжить урок", onClick = onContinueLesson)
            TextButton(onClick = onContinue) {
                Text(text = "На главный экран", color = colors.muted)
            }
        } else {
            PrimaryButton(text = "На главный экран", onClick = onContinue)
        }
    }
}
