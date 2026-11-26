package org.dueattendant149.bebringo.ui.screens.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.audio.TtsPlayer
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.domain.model.Lemma
import org.dueattendant149.bebringo.domain.model.Reading
import org.dueattendant149.bebringo.domain.model.ReadingSource
import org.dueattendant149.bebringo.domain.model.SupportLevel
import org.dueattendant149.bebringo.domain.reading.ReadingCoverage
import org.dueattendant149.bebringo.ui.components.AudioButton
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components.BlockProgressBar
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Список текстов. Порядок — по интересу, не по длине. */
@Composable
fun ReadingListScreen(
    readings: List<Reading>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    knownWords: Set<String> = emptySet(),
) {
    val colors = BebringoTheme.colors
    val sortedReadings = readings.sortedByDescending { reading ->
        ReadingCoverage.coverage(
            reading.paragraphs.joinToString(" ") { it.textRo },
            knownWords,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        BebringoTopBar(onBack = onBack, title = "Чтение")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Mascot(pose = MascotPose.READING, size = 68.dp)
                Text(
                    text = "Тексты на настоящем румынском. Нажимайте на любое слово в тексте для перевода и озвучки.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.weight(1f),
                )
            }

            sortedReadings.forEach { reading ->
                val coverage = ReadingCoverage.coverage(
                    reading.paragraphs.joinToString(" ") { it.textRo },
                    knownWords,
                )
                val formatLabel = if (reading.source == ReadingSource.ORIGINAL) {
                    "короткий текст"
                } else {
                    "отрывок"
                }
                BebringoCard(onClick = { onOpen(reading.id) }) {
                    Text(
                        text = reading.titleRo,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBg,
                    )
                    Text(
                        text = "${reading.author} · ${reading.wordCount} слов · $formatLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(Spacing.m))
                    BlockProgressBar(fraction = coverage)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = if (knownWords.isEmpty()) {
                            "Знакомо: 0% · слова появятся после уроков"
                        } else {
                            "Знакомо: ${(coverage * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                }
            }
        }
    }
}

private val ReadingGlossary = mapOf(
    "dimineața" to "утром; утро",
    "grigore" to "Григоре (имя)",
    "se" to "ся (возвратная частица)",
    "trezea" to "просыпался (от a se trezi)",
    "întotdeauna" to "всегда",
    "cu" to "с",
    "un" to "один (неопред. артикль м./ср. р.)",
    "companion" to "компаньон, спутник",
    "care" to "который",
    "plutea" to "парил, плыл в воздухе",
    "la" to "в, на, у",
    "metru" to "метр",
    "deasupra" to "над",
    "patului" to "кровати (род. падеж от pat)",
    "acesta" to "этот, тот",
    "îi" to "ему / ей",
    "spunea" to "говорил (от a spune)",
    "ce" to "что",
    "are" to "имеет (are nevoie = нуждается)",
    "nevoie" to "нужда, необходимость",
    "înainte" to "раньше, перед тем как",
    "ca" to "чтобы, как",
    "el" to "он",
    "să" to "частица сослагательного наклонения",
    "știe" to "знает (от a ști)",
    "trecuseră" to "прошли (давнопрошедшее от a trece)",
    "patruzeci" to "сорок",
    "și" to "и",
    "trei" to "три",
    "de" to "из, от (связка числительного)",
    "ani" to "годы, лет",
    "când" to "когда",
    "sistema" to "система",
    "funcționa" to "работала, функционировала",
    "nimeni" to "никто",
    "mai" to "уже, ещё",
    "amintea" to "помнил (от a-și aminti)",
    "cum" to "как",
    "era" to "было, был, была",
    "fără" to "без",
    "ea" to "она, неё",
    "mama" to "мама",
    "spăla" to "мыла (от a spăla)",
    "vasele" to "посуду (мн. ч. с арт.)",
    "vorbea" to "говорила (от a vorbi)",
    "singură" to "одна, сама с собой",
    "nu" to "не",
    "tristă" to "грустная",
    "doar" to "только, просто",
    "obosită" to "уставшая",
    "după" to "после",
    "furtună" to "буря, гроза",
    "cerul" to "небо",
    "rămase" to "осталось",
    "senin" to "ясное, безоблачное",
    "oamenii" to "люди",
    "ieșiră" to "вышли",
    "din" to "из",
    "case" to "дома",
    "priviră" to "посмотрели",
    "spre" to "к, в сторону",
    "zare" to "горизонт, даль",
    "orașul" to "город",
    "este" to "есть, является",
    "plin" to "полный",
    "oameni" to "люди",
    "așteaptă" to "ждут",
    "vindecarea" to "исцеление, выздоровление",
    "eu" to "я",
    "sunt" to "есть (я)",
    "unul" to "один",
    "dintre" to "из числа, среди",
    "ei" to "они, них",
    "beau" to "пью",
    "mănânc" to "ем",
    "pâine" to "хлеб",
    "merg" to "иду",
    "întorc" to "возвращаюсь",
    "vorbesc" to "говорю",
    "place" to "нравится",
    "oraș" to "город",
    "mea" to "моя",
    "meu" to "мой",
    "acasă" to "дома",
    "într-o" to "в (одной)",
    "o" to "одна / неопред. артикль ж. р.",
)

/**
 * Читалка с интерактивным выбором любого слова и мгновенным добавлением в FSRS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
    reading: Reading,
    onBack: () -> Unit,
    lemmas: Map<String, Lemma> = emptyMap(),
    knownWords: Set<String> = emptySet(),
    onAddWordToSrs: (wordRo: String, translationRu: String) -> Unit = { _, _ -> },
) {
    val colors = BebringoTheme.colors
    val supportValue by ServiceLocator.settings.supportLevel.collectAsState(initial = 1)
    var supportOverride by remember { mutableStateOf<SupportLevel?>(null) }
    val support = supportOverride ?: SupportLevel.of(supportValue)
    var tappedWord by remember { mutableStateOf<String?>(null) }
    var addedWords by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val tts = remember { TtsPlayer(context) }
    DisposableEffect(Unit) { onDispose { tts.release() } }

    fun lookupTranslation(rawWord: String): String? {
        val clean = ReadingCoverage.normalize(rawWord)
        ReadingGlossary.entries.firstOrNull { ReadingCoverage.normalize(it.key) == clean }
            ?.let { return it.value }
        lemmas.values.firstOrNull { ReadingCoverage.normalize(it.lemma) == clean }
            ?.let { return it.translationRu }
        // «case» → «casă»: сравниваем по корню, а не только по точной форме.
        lemmas.values.firstOrNull { ReadingCoverage.isKnown(clean, setOf(ReadingCoverage.normalize(it.lemma))) }
            ?.let { return it.translationRu }
        ReadingGlossary.entries.firstOrNull {
            ReadingCoverage.isKnown(clean, setOf(ReadingCoverage.normalize(it.key)))
        }?.let { return it.value }
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        BebringoTopBar(
            onBack = onBack,
            title = reading.titleRo,
            action = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(colors.primaryContainer)
                        .clickable {
                            val next = SupportLevel.of((support.value + 1) % 4)
                            supportOverride = next
                            scope.launch { ServiceLocator.settings.setSupportLevel(next.value) }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Подпорка ${support.value}/3",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimaryContainer,
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
            Text(
                text = "${reading.author} · ${reading.wordCount} слов · нажмите на любое слово для перевода",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            if (knownWords.isNotEmpty()) {
                Text(
                    text = "Подчёркнутые слова ещё не встречались в уроках.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
            }

            reading.paragraphs.forEach { paragraph ->
                BebringoCard(containerColor = colors.surfaceContainerLow) {
                    val words = paragraph.textRo.split(' ').filter { it.isNotBlank() }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        words.forEach { rawToken ->
                            val stripped = rawToken.trim(',', '.', '!', '?', ';', ':', '—', '«', '»', '"')
                            val wovenReplacement = if (support == SupportLevel.WOVEN) {
                                paragraph.wovenRu.entries
                                    .firstOrNull { it.key.equals(stripped, ignoreCase = true) }
                                    ?.value
                            } else {
                                null
                            }
                            val isSelected = tappedWord?.equals(stripped, ignoreCase = true) == true
                            val isUnknown = knownWords.isNotEmpty() &&
                                stripped.isNotBlank() &&
                                !ReadingCoverage.isKnown(stripped, knownWords)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isSelected -> colors.primaryContainer
                                            isUnknown && wovenReplacement == null ->
                                                colors.warningContainer.copy(alpha = 0.45f)
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        },
                                    )
                                    .clickable {
                                        if (stripped.isNotBlank()) {
                                            tappedWord = stripped
                                        }
                                    }
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = wovenReplacement ?: rawToken,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontStyle = if (wovenReplacement != null) FontStyle.Italic else FontStyle.Normal,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        textDecoration = if (isUnknown && !isSelected && wovenReplacement == null) {
                                            TextDecoration.Underline
                                        } else {
                                            null
                                        },
                                    ),
                                    color = when {
                                        isSelected -> colors.onPrimaryContainer
                                        wovenReplacement != null -> colors.primary
                                        else -> colors.onBg
                                    },
                                )
                            }
                        }
                    }

                    if (support == SupportLevel.GLOSS || support == SupportLevel.PARALLEL) {
                        paragraph.translationRu?.let { translation ->
                            Spacer(Modifier.height(Spacing.m))
                            Text(
                                text = translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                                fontStyle = FontStyle.Italic,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xxl))
        }

        val currentWord = tappedWord
        if (currentWord != null) {
            val resolvedTranslation = lookupTranslation(currentWord)
            val alreadyAdded = resolvedTranslation != null && currentWord.lowercase() in addedWords
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.surfaceContainerHigh,
                        RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
                    )
                    .padding(Spacing.l),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                        ) {
                            Text(
                                text = currentWord,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.onBg,
                            )
                            AudioButton(
                                label = "🔊",
                                onClick = { tts.speak(currentWord) },
                            )
                        }
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.muted,
                            modifier = Modifier
                                .clickable { tappedWord = null }
                                .padding(Spacing.xs),
                        )
                    }

                    if (resolvedTranslation != null) {
                        Text(
                            text = resolvedTranslation,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                        )

                        Spacer(Modifier.height(Spacing.xs))

                        PrimaryButton(
                            text = if (alreadyAdded) {
                                "✓ Добавлено в коллекцию и FSRS"
                            } else {
                                "Добавить в повторение FSRS"
                            },
                            enabled = !alreadyAdded,
                            onClick = {
                                onAddWordToSrs(currentWord, resolvedTranslation)
                                addedWords = addedWords + currentWord.lowercase()
                            },
                        )
                    } else {
                        Text(
                            text = "Перевода пока нет в словаре — слово появится в уроках позже.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
            }
        }
    }
}
