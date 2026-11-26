package org.dueattendant149.bebringo.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.domain.model.TrackMeta
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BlockProgressBar
import org.dueattendant149.bebringo.ui.components.ChapterDots
import org.dueattendant149.bebringo.ui.components.GroupedListCard
import org.dueattendant149.bebringo.ui.components.GroupedListRow
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Главный экран «Мой день» (Material Design 3).
 *
 * Фокус на одном главном действии — карточке текущего блока с маскотом-бобром,
 * прогрессом главы и сгруппированным списком практик (Чтение, Чат, Слово дня).
 */
@Composable
fun HomeScreen(
    streakDays: Int,
    blockTitle: String,
    chapterTitle: String,
    stepsLeft: Int,
    stepsTotal: Int,
    chapterDone: Int,
    chapterCurrent: Int,
    chapterTotal: Int,
    minutesToday: Int,
    blocksToday: Int,
    dueCount: Int = 0,
    onContinue: () -> Unit,
    repairAvailable: Boolean = false,
    onRepair: () -> Unit = {},
    onReview: () -> Unit = {},
    onReading: () -> Unit,
    onChat: () -> Unit,
    onWordOfDay: () -> Unit,
    modifier: Modifier = Modifier,
    freezesLeft: Int = 2,
    blockNumber: Int = 1,
    blockSubtitleRu: String = "",
    chapterTitleRu: String = "",
    partIndex: Int = 1,
    partsTotal: Int = 1,
    word: String = "tren",
    wordTranslation: String = "поезд",
    mascotPose: String = "flag_ro",
    tracks: List<TrackMeta> = emptyList(),
    selectedTrackId: String = "all",
    onSelectTrack: (String) -> Unit = {},
) {
    val colors = BebringoTheme.colors
    val stepsDone = (stepsTotal - stepsLeft).coerceIn(0, stepsTotal)
    val heroFraction = if (stepsTotal > 0) stepsDone.toFloat() / stepsTotal else 0f
    val partLabel = if (partsTotal > 1) " · часть $partIndex из $partsTotal" else ""
    val estimatedMinutes = ((stepsLeft * 0.6f).roundToInt()).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        // Верхняя шапка с названием и пилюлями серии/заморозок
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Bebringo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBg,
                )
                Text(
                    text = if (blocksToday > 0) "Отличный темп сегодня!" else "Спокойный шаг вперёд",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(colors.tertiaryContainer, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "🔥 $streakDays дн.",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onTertiaryContainer,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(colors.surfaceContainerHigh, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "❄️ $freezesLeft",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.muted,
                    )
                }
            }
        }

        // Панель фильтров треков (модульная архитектура «Мой день»)
        if (tracks.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                FilterChip(
                    selected = selectedTrackId == "all",
                    onClick = { onSelectTrack("all") },
                    label = { Text("Все треки") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryContainer,
                        selectedLabelColor = colors.onPrimaryContainer,
                    ),
                )
                tracks.forEach { track ->
                    FilterChip(
                        selected = selectedTrackId == track.id,
                        onClick = { onSelectTrack(track.id) },
                        label = { Text(track.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryContainer,
                            selectedLabelColor = colors.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        // Герой-карточка текущего блока (R1)
        BebringoCard(
            containerColor = colors.surfaceContainerLow,
            borderColor = colors.outline,
            onClick = onContinue,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .background(colors.primaryContainer, RoundedCornerShape(Radius.pill))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Блок $blockNumber$partLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = blockTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onBg,
                    )
                    if (blockSubtitleRu.isNotBlank()) {
                        Text(
                            text = blockSubtitleRu,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = if (stepsDone > 0) {
                            "Продолжить с шага ${stepsDone + 1} из $stepsTotal · ещё ~$estimatedMinutes мин"
                        } else {
                            "Осталось шагов: $stepsLeft из $stepsTotal · ~$estimatedMinutes мин"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Mascot(
                    pose = if (blocksToday > 0 && stepsDone == 0) {
                        MascotPose.CELEBRATE
                    } else {
                        MascotPose.fromId(mascotPose)
                    },
                    size = 92.dp,
                )
            }

            if (stepsDone > 0) {
                Spacer(Modifier.height(Spacing.m))
                BlockProgressBar(fraction = heroFraction)
            }

            Spacer(Modifier.height(Spacing.l))

            PrimaryButton(
                text = if (stepsDone > 0) "Продолжить урок" else "Начать урок",
                onClick = onContinue,
            )
        }

        // Карточка ремонта стрика (показывается только при реальном пропуске вчера)
        if (repairAvailable) {
            BebringoCard(
                containerColor = colors.warningContainer,
                borderColor = colors.warning.copy(alpha = 0.4f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    Mascot(pose = MascotPose.LANTERN, size = 60.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Серия остановилась вчера",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onBg,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "Можно закрыть вчерашний пропуск и спокойно продолжить без потери ритма.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.m))
                PrimaryButton(text = "Восстановить вчерашний день", onClick = onRepair)
            }
        }

        // Карточка прогресса главы
        BebringoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Глава · $chapterTitle",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBg,
                    )
                    if (chapterTitleRu.isNotBlank()) {
                        Text(
                            text = chapterTitleRu,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
                Text(
                    text = "$chapterDone / $chapterTotal",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
            Spacer(Modifier.height(Spacing.m))
            ChapterDots(
                done = chapterDone,
                current = chapterCurrent,
                total = chapterTotal,
            )
        }

        // M3 сгруппированный контейнер практик (вместо отдельных рамок на каждую строку)
        GroupedListCard {
            if (dueCount > 0) {
                GroupedListRow(
                    title = "Повторение",
                    emoji = "🔁",
                    subtitle = "Слова из прошлых уроков · карточек: $dueCount",
                    badgeText = "Повторить",
                    onClick = onReview,
                )
            }
            GroupedListRow(
                title = "Чтение с переводом по тапу",
                emoji = "📖",
                subtitle = "Короткие тексты A1 · перевод по тапу",
                onClick = onReading,
            )
            GroupedListRow(
                title = "Диалог с собеседником",
                emoji = "💬",
                subtitle = "La gară · покупка билета",
                onClick = onChat,
            )
            GroupedListRow(
                title = "Слово дня",
                emoji = "🔊",
                subtitle = "$word — $wordTranslation",
                badgeText = "Учить",
                onClick = onWordOfDay,
                showDivider = false,
            )
        }

        // Итог активности за сегодня
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainer, RoundedCornerShape(Radius.md))
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Сегодня в практике",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            Text(
                text = "$minutesToday мин · частей: $blocksToday",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
        }
    }
}
