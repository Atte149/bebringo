package org.dueattendant149.bebringo.ui.screens.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dueattendant149.bebringo.audio.TtsPlayer
import org.dueattendant149.bebringo.domain.model.AnswerEvent
import org.dueattendant149.bebringo.domain.model.AssembleStep
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.DialogueStep
import org.dueattendant149.bebringo.domain.model.DictationStep
import org.dueattendant149.bebringo.domain.model.ImageStep
import org.dueattendant149.bebringo.domain.model.InsightStep
import org.dueattendant149.bebringo.domain.model.LessonResult
import org.dueattendant149.bebringo.domain.model.ListeningStep
import org.dueattendant149.bebringo.domain.model.MistakeNote
import org.dueattendant149.bebringo.domain.model.RecognitionStep
import org.dueattendant149.bebringo.domain.model.ReflectionStep
import org.dueattendant149.bebringo.domain.model.SprintStep
import org.dueattendant149.bebringo.domain.model.Step
import org.dueattendant149.bebringo.domain.model.TextSpan
import org.dueattendant149.bebringo.domain.model.TimerPracticeStep
import org.dueattendant149.bebringo.domain.model.TransformStep
import org.dueattendant149.bebringo.domain.model.TypeInStep
import org.dueattendant149.bebringo.domain.model.WeaveStep
import org.dueattendant149.bebringo.domain.session.StepEvaluation
import org.dueattendant149.bebringo.domain.session.StepEvaluator
import org.dueattendant149.bebringo.domain.text.HintBuilder
import org.dueattendant149.bebringo.ui.components.AudioButton
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTextField
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components. BlockProgressBar
import org.dueattendant149.bebringo.ui.components.ChoiceRow
import org.dueattendant149.bebringo.ui.components.DiacriticsBar
import org.dueattendant149.bebringo.ui.components.FeedbackCard
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.components.SecondaryButton
import org.dueattendant149.bebringo.ui.components.SuccessFeedbackCard
import org.dueattendant149.bebringo.ui.components.WordTile
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing
import kotlin.random.Random

/**
 * Экран прохождения блока (Material Design 3).
 *
 * Особенности:
 * - поддерживает 13 типов шагов (10 языковых + `InsightStep`, `TimerPracticeStep`, `ReflectionStep`);
 * - в `AssembleStep` плитки перемешиваются, а выбранные слова можно вернуть обратно тапом;
 * - ошибка показывает разбор и не возвращается в тот же урок: повторение уходит в FSRS на другой день;
 * - при случайном нажатии «Назад» посреди блока спрашивает подтверждение (прогресс уже сохранён).
 */
@Composable
fun LessonScreen(
    block: Block,
    onBack: () -> Unit,
    onAnswer: (AnswerEvent, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onFinished: (LessonResult) -> Unit,
    requireDiacritics: Boolean = false,
    initialStepIndex: Int = 0,
    soundEnabled: Boolean = true,
    autoPlayAudio: Boolean = false,
    onToggleSound: () -> Unit = {},
    finishLabel: String = "Завершить блок",
) {
    val colors = BebringoTheme.colors
    var queueIndex by rememberSaveable(block.id) {
        mutableIntStateOf(initialStepIndex.coerceIn(0, (block.steps.size - 1).coerceAtLeast(0)))
    }
    var selectedOption by rememberSaveable(block.id, queueIndex) { mutableStateOf<Int?>(null) }
    var textAnswer by rememberSaveable(block.id, queueIndex) { mutableStateOf("") }
    val pickedIndices = remember(block.id, queueIndex) { mutableStateListOf<Int>() }
    var checked by rememberSaveable(block.id, queueIndex) { mutableStateOf(false) }
    var evaluation by remember(block.id, queueIndex) { mutableStateOf<StepEvaluation?>(null) }
    var correctCount by rememberSaveable(block.id) { mutableIntStateOf(0) }
    val mistakes = remember(block.id) { mutableStateListOf<MistakeNote>() }
    var stepStartedAt by remember(block.id, queueIndex) { mutableLongStateOf(System.currentTimeMillis()) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val step = block.steps.getOrElse(queueIndex) { block.steps.last() }
    val isLastStep = queueIndex >= block.steps.size - 1
    val isSingleTapStep = step is InsightStep || step is WeaveStep || step is SprintStep || step is TimerPracticeStep

    val context = LocalContext.current
    val tts = remember { TtsPlayer(context) }
    DisposableEffect(Unit) { onDispose { tts.release() } }
    // При переходе на новый шаг старая фраза не должна доигрывать.
    DisposableEffect(step.id) { onDispose { tts.stop() } }

    BackHandler(enabled = queueIndex > 0 && !showExitDialog) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выйти из урока?") },
            text = {
                Text("Текущий шаг уже сохранён — вы сможете продолжить этот блок с того же места на главном экране.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    },
                ) {
                    Text("Выйти", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Остаться", color = colors.primary)
                }
            },
        )
    }

    fun advance() {
        if (queueIndex >= block.steps.size - 1) {
            onFinished(
                LessonResult(
                    correct = correctCount.coerceAtMost(block.total),
                    total = block.total,
                    mistakes = mistakes.distinctBy { it.correct }.take(3),
                ),
            )
        } else {
            queueIndex += 1
            selectedOption = null
            textAnswer = ""
            pickedIndices.clear()
            checked = false
            evaluation = null
            stepStartedAt = System.currentTimeMillis()
        }
    }

    fun check() {
        val pickedWords = if (step is AssembleStep) {
            pickedIndices.mapNotNull { idx -> step.tokens.getOrNull(idx) }
        } else {
            emptyList()
        }
        val result = StepEvaluator.evaluate(
            step = step,
            selectedOption = selectedOption,
            textAnswer = textAnswer,
            pickedTokens = pickedWords,
            requireDiacritics = requireDiacritics,
        )
        evaluation = result
        checked = true

        if (result.correct) {
            correctCount++
        }
        result.note?.let { mistakes.add(it) }

        val event = StepEvaluator.toAnswerEvent(
            step = step,
            evaluation = result,
            responseMs = System.currentTimeMillis() - stepStartedAt,
            isFirstAttempt = true,
        )
        val nextSavedStep = (queueIndex + 1).coerceAtMost((block.total - 1).coerceAtLeast(0))
        val answeredSoFar = (queueIndex + 1).coerceAtMost(block.total)
        onAnswer(event, nextSavedStep, correctCount, answeredSoFar)

        if (isSingleTapStep && result.correct) {
            advance()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .imePadding(),
    ) {
        BebringoTopBar(
            onBack = {
                if (queueIndex > 0) showExitDialog = true else onBack()
            },
            stepCurrent = (queueIndex + 1).coerceAtMost(block.steps.size),
            stepTotal = block.steps.size,
            action = {
                IconButton(onClick = onToggleSound) {
                    Icon(
                        imageVector = if (soundEnabled) {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        },
                        contentDescription = if (soundEnabled) "Выключить звук" else "Включить звук",
                        tint = colors.onBg,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            when (step) {
                is InsightStep -> InsightContent(step, tts)
                is TimerPracticeStep -> TimerPracticeContent(step)
                is ReflectionStep -> ReflectionContent(step, textAnswer) { textAnswer = it }
                is RecognitionStep -> RecognitionContent(step, selectedOption, tts) { selectedOption = it }
                is ListeningStep -> ListeningContent(step, selectedOption, tts, soundEnabled, autoPlayAudio) {
                    selectedOption = it
                }
                is ImageStep -> ImageContent(step, selectedOption) { selectedOption = it }
                is AssembleStep -> AssembleContent(step, pickedIndices)
                is TypeInStep -> TypeInContent(step, textAnswer) { textAnswer = it }
                is DictationStep -> DictationContent(step, textAnswer, tts, soundEnabled, autoPlayAudio) {
                    textAnswer = it
                }
                is TransformStep -> TransformContent(step, textAnswer) { textAnswer = it }
                is WeaveStep -> WeaveContent(step)
                is SprintStep -> SprintContent(step)
                is DialogueStep -> DialogueContent(step, selectedOption, tts) { selectedOption = it }
            }

            if (checked) {
                val eval = evaluation
                if (eval != null) {
                    if (!eval.correct) {
                        eval.note?.let { note ->
                            FeedbackCard(
                                prompt = note.prompt,
                                correction = note.correct,
                                explanation = note.explanationRu,
                            )
                        }
                    } else if (eval.warningNote != null) {
                        SuccessFeedbackCard(
                            title = "Засчитано без диакритики",
                            subtitle = eval.warningNote,
                            isWarning = true,
                        )
                    } else {
                        SuccessFeedbackCard(
                            title = "Верно!",
                            subtitle = eval.expected.takeIf { it.isNotBlank() }?.let { "Эталон: $it" },
                            isWarning = false,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
        ) {
            PrimaryButton(
                text = when {
                    isSingleTapStep && isLastStep -> finishLabel
                    isSingleTapStep -> "Понятно, дальше"
                    checked && isLastStep -> finishLabel
                    checked -> "Продолжить"
                    else -> "Проверить"
                },
                onClick = { if (checked) advance() else check() },
            )
        }
    }
}

@Composable
private fun StepPrompt(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = BebringoTheme.colors.onBg,
    )
}

@Composable
private fun PromptCard(text: String) {
    val colors = BebringoTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh, RoundedCornerShape(Radius.md))
            .padding(Spacing.l),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = colors.onBg)
    }
}

@Composable
private fun OptionsColumn(
    options: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        options.forEachIndexed { index, option ->
            ChoiceRow(
                text = option,
                selected = selected == index,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun InsightContent(step: InsightStep, tts: TtsPlayer) {
    val colors = BebringoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Mascot(pose = MascotPose.fromId(step.mascotPose), size = 80.dp)
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .background(colors.primaryContainer, RoundedCornerShape(Radius.pill))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Теория · 40 сек",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBg,
            )
        }
    }

    BebringoCard(containerColor = colors.surfaceContainerLow) {
        Text(
            text = parseSimpleBoldMarkdown(step.bodyMarkdown, colors.primary),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBg,
        )
    }

    if (step.examples.isNotEmpty()) {
        BebringoCard(containerColor = colors.surfaceContainer) {
            Text(
                text = "Примеры",
                style = MaterialTheme.typography.labelLarge,
                color = colors.muted,
            )
            Spacer(Modifier.height(Spacing.s))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                step.examples.forEach { ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.primary,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.onBg,
                            )
                            if (ex.secondary.isNotBlank()) {
                                Text(
                                    text = ex.secondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.muted,
                                )
                            }
                        }
                        AudioButton(
                            label = "🔊",
                            onClick = { tts.speak(ex.primary) },
                        )
                    }
                }
            }
        }
    }

    if (!step.tip.isNullOrBlank()) {
        BebringoCard(
            containerColor = colors.tertiaryContainer,
            borderColor = colors.tertiary.copy(alpha = 0.35f),
        ) {
            Text(
                text = "💡 ${step.tip}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onTertiaryContainer,
            )
        }
    }
}

private fun parseSimpleBoldMarkdown(
    raw: String,
    highlightColor: androidx.compose.ui.graphics.Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    val parts = raw.split("**")
    parts.forEachIndexed { idx, segment ->
        if (idx % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
                append(segment)
            }
        } else {
            append(segment)
        }
    }
}

@Composable
private fun TimerPracticeContent(step: TimerPracticeStep) {
    val colors = BebringoTheme.colors
    var remainingSec by rememberSaveable(step.id) { mutableIntStateOf(step.durationSec) }
    var running by rememberSaveable(step.id) { mutableStateOf(false) }

    LaunchedEffect(running, remainingSec) {
        if (running && remainingSec > 0) {
            delay(1000L)
            remainingSec -= 1
        } else if (remainingSec == 0) {
            running = false
        }
    }

    val elapsed = step.durationSec - remainingSec
    val currentCue = step.cues.lastOrNull { it.atSec <= elapsed }?.text

    StepPrompt(step.title)
    BebringoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Mascot(pose = MascotPose.fromId(step.mascotPose), size = 88.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "%02d:%02d".format(remainingSec / 60, remainingSec % 60),
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.primary,
                )
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBg,
                )
            }
        }
        Spacer(Modifier.height(Spacing.m))
        BlockProgressBar(
            fraction = if (step.durationSec > 0) elapsed.toFloat() / step.durationSec else 1f,
        )
        if (!currentCue.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = currentCue,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = colors.primary,
            )
        }
        Spacer(Modifier.height(Spacing.m))
        SecondaryButton(
            text = if (running) "Пауза" else if (remainingSec == 0) "Повторить таймер" else "Запустить таймер",
            onClick = {
                if (remainingSec == 0) {
                    remainingSec = step.durationSec
                    running = true
                } else {
                    running = !running
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReflectionContent(step: ReflectionStep, answer: String, onChange: (String) -> Unit) {
    val colors = BebringoTheme.colors
    val wordCount = answer.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val checkedItems = remember(step.id) { mutableStateListOf<Int>() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Mascot(pose = MascotPose.fromId(step.mascotPose), size = 68.dp)
        StepPrompt(step.prompt)
    }

    BebringoTextField(
        value = answer,
        onValueChange = onChange,
        placeholder = step.placeholder,
    )

    Text(
        text = "Слов: $wordCount (рекомендуемый минимум: ${step.minWords})",
        style = MaterialTheme.typography.labelMedium,
        color = if (wordCount >= step.minWords) colors.success else colors.muted,
    )

    if (step.checklist.isNotEmpty()) {
        Text(
            text = "Чеклист самопроверки:",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBg,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            step.checklist.forEachIndexed { idx, item ->
                val selected = idx in checkedItems
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) checkedItems.remove(idx) else checkedItems.add(idx)
                    },
                    label = { Text(item) },
                )
            }
        }
    }
}

@Composable
private fun RecognitionContent(
    step: RecognitionStep,
    selected: Int?,
    tts: TtsPlayer,
    onSelect: (Int) -> Unit,
) {
    val colors = BebringoTheme.colors
    StepPrompt("Что это значит?")
    BebringoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = step.wordRo,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBg,
                modifier = Modifier.weight(1f),
            )
            AudioButton(
                label = "🔊",
                onClick = { tts.speak(step.wordRo) },
            )
        }
    }
    OptionsColumn(step.optionsRu, selected, onSelect)
}

@Composable
private fun ListeningContent(
    step: ListeningStep,
    selected: Int?,
    tts: TtsPlayer,
    soundEnabled: Boolean,
    autoPlayAudio: Boolean,
    onSelect: (Int) -> Unit,
) {
    val colors = BebringoTheme.colors
    val isPlaying by tts.isPlaying.collectAsState()
    var showHint by remember(step.id) { mutableStateOf(false) }
    val systemSilent = remember(step.id) { tts.isSilentMode() }

    LaunchedEffect(step.id, soundEnabled, autoPlayAudio, systemSilent) {
        if (soundEnabled && autoPlayAudio && !systemSilent) tts.speak(step.audioText)
    }

    StepPrompt("Послушайте и выберите перевод")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Mascot(
            pose = if (isPlaying) MascotPose.THUMBS_UP else MascotPose.WAVE,
            size = 64.dp,
        )
        if (soundEnabled) {
            AudioButton(
                label = "Прослушать фразу",
                isPlaying = isPlaying,
                onClick = { tts.speak(step.audioText) },
            )
        } else {
            Text(
                text = "🔇 Звук выключен",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
    }

    when {
        !soundEnabled -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.warningContainer, RoundedCornerShape(Radius.md))
                    .padding(horizontal = Spacing.m, vertical = Spacing.s),
            ) {
                Text(
                    text = "Звук выключен — вот фраза: «${step.audioText}»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBg,
                )
            }
        }

        showHint -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainer, RoundedCornerShape(Radius.md))
                    .padding(horizontal = Spacing.m, vertical = Spacing.s),
            ) {
                Text(
                    text = "Фраза: «${step.audioText}»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                )
            }
        }

        else -> {
            TextButton(
                onClick = { showHint = true },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "Не слышно звук? Показать фразу",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            }
            if (systemSilent) {
                Text(
                    text = "Телефон в беззвучном режиме — нажмите «Прослушать фразу».",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            } else if (!autoPlayAudio) {
                Text(
                    text = "Автозапуск выключен — нажмите «Прослушать фразу».",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            }
        }
    }

    OptionsColumn(step.optionsRu, selected, onSelect)
}

@Composable
private fun ImageContent(step: ImageStep, selected: Int?, onSelect: (Int) -> Unit) {
    val colors = BebringoTheme.colors
    StepPrompt("Выберите подходящее слово")
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Mascot(pose = MascotPose.READING, size = 132.dp)
    }
    Text(
        text = "Образ: ${step.imageKey}",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.muted,
    )
    OptionsColumn(step.optionsRo, selected, onSelect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssembleContent(step: AssembleStep, pickedIndices: MutableList<Int>) {
    val colors = BebringoTheme.colors
    // Перемешиваем индексы токенов детерминированно по id шага, чтобы слова не шли в порядке ответа
    val shuffledOrder = remember(step.id) {
        val indices = step.tokens.indices.toList()
        val shuffled = indices.shuffled(Random(step.id.hashCode()))
        if (shuffled == indices && indices.size > 1) shuffled.reversed() else shuffled
    }

    StepPrompt("Составьте фразу из слов")
    PromptCard(step.promptRu)

    // Область собранного ответа — тап по плитке возвращает слово обратно в банк!
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 84.dp)
            .background(colors.surfaceContainerLow, RoundedCornerShape(Radius.md))
            .padding(Spacing.m),
        contentAlignment = if (pickedIndices.isEmpty()) Alignment.CenterStart else Alignment.TopStart,
    ) {
        if (pickedIndices.isEmpty()) {
            Text(
                text = "Нажимайте на слова ниже, чтобы собрать фразу (нажмите на выбранное слово, чтобы убрать)…",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                pickedIndices.forEachIndexed { posInPicked, tokenIndex ->
                    val tokenText = step.tokens.getOrElse(tokenIndex) { "" }
                    WordTile(
                        text = tokenText,
                        pickedStyle = true,
                        onClick = { pickedIndices.removeAt(posInPicked) },
                    )
                }
            }
        }
    }

    // Банк доступных слов (с дистракторами)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        shuffledOrder.forEach { tokenIndex ->
            val token = step.tokens[tokenIndex]
            val used = tokenIndex in pickedIndices
            WordTile(
                text = token,
                used = used,
                onClick = { if (!used) pickedIndices.add(tokenIndex) },
            )
        }
    }
}

@Composable
private fun TypeInContent(step: TypeInStep, answer: String, onChange: (String) -> Unit) {
    val colors = BebringoTheme.colors
    var hintTier by remember(step.id) { mutableIntStateOf(0) }
    val hints = remember(step.id) { HintBuilder.tiers(step) }

    StepPrompt("Напишите по-румынски")
    PromptCard(step.promptRu)
    BebringoTextField(value = answer, onValueChange = onChange, placeholder = "Введите ответ…")
    DiacriticsBar(onInsert = { onChange(answer + it) })

    if (hintTier > 0) {
        BebringoCard(containerColor = colors.surfaceContainer) {
            Text(
                text = "Подсказка $hintTier из ${hints.size}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = hints[hintTier - 1],
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBg,
            )
            if (hintTier == hints.size) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Это полный ответ — попробуйте сначала вспомнить сами.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.warning,
                )
            }
        }
    }

    if (hintTier < hints.size) {
        TextButton(onClick = { hintTier += 1 }) {
            Text(
                text = if (hintTier == 0) "Подсказка" else "Ещё подсказка",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun DictationContent(
    step: DictationStep,
    answer: String,
    tts: TtsPlayer,
    soundEnabled: Boolean,
    autoPlayAudio: Boolean,
    onChange: (String) -> Unit,
) {
    val colors = BebringoTheme.colors
    val isPlaying by tts.isPlaying.collectAsState()
    var showHint by remember(step.id) { mutableStateOf(false) }
    val systemSilent = remember(step.id) { tts.isSilentMode() }

    LaunchedEffect(step.id, soundEnabled, autoPlayAudio, systemSilent) {
        if (soundEnabled && autoPlayAudio && !systemSilent) tts.speak(step.audioText)
    }

    StepPrompt("Напишите, что слышите")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (soundEnabled) {
            AudioButton(
                label = "Прослушать",
                isPlaying = isPlaying,
                onClick = { tts.speak(step.audioText) },
            )
        } else {
            Text(
                text = "🔇 Звук выключен",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
    }

    when {
        !soundEnabled -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.warningContainer, RoundedCornerShape(Radius.md))
                    .padding(horizontal = Spacing.m, vertical = Spacing.s),
            ) {
                Text(
                    text = "Звук выключен — подсказка: «${step.audioText}»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBg,
                )
            }
        }

        showHint -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainer, RoundedCornerShape(Radius.md))
                    .padding(horizontal = Spacing.m, vertical = Spacing.s),
            ) {
                Text(
                    text = "Подсказка: «${step.audioText}»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                )
            }
        }

        else -> {
            TextButton(
                onClick = { showHint = true },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "Не слышно звук? Показать подсказку",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            }
            if (systemSilent) {
                Text(
                    text = "Телефон в беззвучном режиме — нажмите «Прослушать».",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            } else if (!autoPlayAudio) {
                Text(
                    text = "Автозапуск выключен — нажмите «Прослушать».",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            }
        }
    }

    BebringoTextField(value = answer, onValueChange = onChange, placeholder = "Введите ответ…")
    DiacriticsBar(onInsert = { onChange(answer + it) })
}

@Composable
private fun TransformContent(step: TransformStep, answer: String, onChange: (String) -> Unit) {
    StepPrompt(step.instructionRu)
    PromptCard(step.sourceRo)
    BebringoTextField(value = answer, onValueChange = onChange, placeholder = "Введите изменённую форму…")
    DiacriticsBar(onInsert = { onChange(answer + it) })
}

@Composable
private fun WeaveContent(step: WeaveStep) {
    val colors = BebringoTheme.colors
    StepPrompt("Прочитайте текст")
    BebringoCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            step.paragraphs.forEach { paragraph ->
                Text(
                    text = buildAnnotated(paragraph),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBg,
                )
            }
        }
    }
}

private fun buildAnnotated(spans: List<TextSpan>): androidx.compose.ui.text.AnnotatedString =
    buildAnnotatedString {
        spans.forEach { span ->
            if (span.isRussian) {
                withStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFF8C8880),
                        fontStyle = FontStyle.Italic,
                    ),
                ) { append(span.text) }
            } else {
                append(span.text)
            }
        }
    }

@Composable
private fun SprintContent(step: SprintStep) {
    val colors = BebringoTheme.colors
    StepPrompt("Пробегите текст глазами")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${step.timeLimitSec} сек",
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted,
        )
        Text(text = "▶", style = MaterialTheme.typography.bodyLarge, color = colors.primary)
    }
    BebringoCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            step.paragraphs.forEach { paragraph ->
                Text(text = paragraph, style = MaterialTheme.typography.bodyLarge, color = colors.onBg)
            }
        }
    }
}

@Composable
private fun DialogueContent(step: DialogueStep, selected: Int?, tts: TtsPlayer, onSelect: (Int) -> Unit) {
    val colors = BebringoTheme.colors
    val isPlaying by tts.isPlaying.collectAsState()
    StepPrompt("Послушайте диалог (нажмите на реплику для звука)")
    AudioButton(
        label = "Весь диалог",
        isPlaying = isPlaying,
        onClick = { tts.speak(step.lines.joinToString(". ") { it.textRo }) },
    )
    BebringoCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            step.lines.forEach { line ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tts.speak(line.textRo) }
                        .padding(vertical = 2.dp),
                ) {
                    Text(text = line.speakerRo, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    Text(text = line.textRo, style = MaterialTheme.typography.bodyLarge, color = colors.onBg)
                    if (line.translationRu != null) {
                        Text(text = line.translationRu, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(Spacing.s))
    StepPrompt(step.questionRu)
    OptionsColumn(step.optionsRu, selected, onSelect)
}
