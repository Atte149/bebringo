package org.dueattendant149.bebringo.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.audio.TtsPlayer
import org.dueattendant149.bebringo.ui.components.AudioButton
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.components.SecondaryButton
import org.dueattendant149.bebringo.ui.components.SuccessFeedbackCard
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Слово дня: микро-вход с озвучкой, примером и добавлением в очередь FSRS. */
@Composable
fun WordOfDayScreen(
    word: String,
    translation: String,
    onBack: () -> Unit,
    exampleRo: String? = null,
    exampleRu: String? = null,
    onAddToSrs: () -> Unit = {},
) {
    val colors = BebringoTheme.colors
    val context = LocalContext.current
    val tts = remember { TtsPlayer(context) }
    DisposableEffect(Unit) { onDispose { tts.release() } }
    var added by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = "Слово дня")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Mascot(pose = MascotPose.SCHOLAR, size = 140.dp)

            BebringoCard(containerColor = colors.surfaceContainerLow) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.onBg,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    AudioButton(
                        label = "Прослушать произношение",
                        onClick = { tts.speak(word) },
                    )
                }
            }

            if (!exampleRo.isNullOrBlank()) {
                BebringoCard(containerColor = colors.surfaceContainer) {
                    Text(
                        text = "В контексте",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = exampleRo,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBg,
                    )
                    if (!exampleRu.isNullOrBlank()) {
                        Text(
                            text = exampleRu,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = colors.muted,
                        )
                    }
                }
            }

            if (added) {
                SuccessFeedbackCard(
                    title = "Добавлено в интервальное повторение",
                    subtitle = "Слово появится в ближайших блоках и попадёт в коллекцию фраз.",
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.l)) {
            PrimaryButton(
                text = if (added) "✓ В очереди FSRS" else "Добавить в повторение",
                enabled = !added,
                onClick = {
                    onAddToSrs()
                    added = true
                },
            )
        }
    }
}

/** Заглушка разделов, которые появятся позже. */
@Composable
fun StubSectionScreen(
    title: String,
    description: String,
    onBack: () -> Unit,
) {
    val colors = BebringoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = title)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(pose = MascotPose.THINKING, size = 112.dp)
            Spacer(Modifier.height(Spacing.l))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Телеметрия: локальные события, экспорт в CSV и очистка. */
@Composable
fun TelemetryScreen(
    eventCount: Int,
    onBack: () -> Unit,
    statusMessage: String? = null,
    onExportCsv: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = "Телеметрия")

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            BebringoCard {
                Text(
                    text = "Событий записано: $eventCount",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBg,
                )
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = "Каждое событие содержит время ответа, тип шага, уровень подпорки и тип помарки. Всё хранится строго локально на этом устройстве.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                SuccessFeedbackCard(
                    title = statusMessage,
                )
            }

            PrimaryButton(text = "Выгрузить CSV", onClick = onExportCsv)
            SecondaryButton(text = "Очистить историю", onClick = onClear)
        }
    }
}
