package org.dueattendant149.bebringo.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.ChoiceRow
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.components.ProgressBarWithLabel
import org.dueattendant149.bebringo.ui.components.SelectableChip
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

@Composable
private fun OnboardingScaffold(
    title: String,
    onSkip: (() -> Unit)?,
    content: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    val colors = BebringoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Bebringo",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBg,
            )
            if (onSkip != null) {
                TextButton(onClick = onSkip) {
                    Text(text = "Пропустить", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
            } else {
                Spacer(Modifier.height(Spacing.xl))
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBg,
        )

        content()

        Spacer(Modifier.height(Spacing.s))
        bottom()
    }
}

@Composable
fun OnboardingGoalScreen(
    onNext: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val colors = BebringoTheme.colors
    var selected by remember { mutableStateOf(OnboardingDraft.goal) }

    val options = listOf(
        "study" to ("🏫" to "Учёба и работа"),
        "travel" to ("🏝" to "Путешествия и жизнь"),
        "interest" to ("❤️" to "Интерес к языку и культуре"),
        "life" to ("🌱" to "Личное развитие"),
    )

    OnboardingScaffold(
        title = "Для чего вы начинаете обучение?",
        onSkip = onSkip,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Mascot(pose = MascotPose.FLAG_RO, size = 104.dp)
                Text(
                    text = "Короткие блоки по 3–4 минуты: румынский язык и модульные практики на каждый день.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                options.forEach { (key, pair) ->
                    ChoiceRow(
                        text = pair.second,
                        emoji = pair.first,
                        selected = selected == key,
                        onClick = {
                            selected = key
                            OnboardingDraft.goal = key
                        },
                    )
                }
            }
        },
        bottom = {
            PrimaryButton(text = "Дальше", onClick = { onNext(selected) })
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingAnchorScreen(
    onNext: (time: String, after: String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = BebringoTheme.colors
    var time by remember { mutableStateOf(OnboardingDraft.anchorTime) }
    var after by remember { mutableStateOf(OnboardingDraft.anchorAfter) }

    OnboardingScaffold(
        title = "Когда вам удобно заниматься?",
        onSkip = null,
        content = {
            Text(
                text = "Приложение напомнит один раз в день в выбранное окно. В остальное время — полная тишина.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text("Время", style = MaterialTheme.typography.labelMedium, color = colors.muted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    listOf("утром", "днём", "вечером").forEach { option ->
                        SelectableChip(
                            text = option,
                            selected = time == option,
                            onClick = {
                                time = option
                                OnboardingDraft.anchorTime = option
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text("Сразу после", style = MaterialTheme.typography.labelMedium, color = colors.muted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    listOf("кофе", "завтрака", "обеда", "конца работы").forEach { option ->
                        SelectableChip(
                            text = option,
                            selected = after == option,
                            onClick = {
                                after = option
                                OnboardingDraft.anchorAfter = option
                            },
                        )
                    }
                }
            }

            BebringoCard(containerColor = colors.primaryContainer) {
                Text(
                    text = "«Я занимаюсь $time, сразу после $after.»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onPrimaryContainer,
                )
            }
        },
        bottom = {
            PrimaryButton(text = "Дальше", onClick = { onNext(time, after) })
            Spacer(Modifier.height(Spacing.s))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Назад", color = colors.muted)
            }
        },
    )
}

@Composable
fun OnboardingPlacementScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = BebringoTheme.colors
    var selectedIdx by remember { mutableIntStateOf(0) }
    val options = listOf("Добрый день!", "До свидания!", "Спасибо!", "Извините!")

    OnboardingScaffold(
        title = "Проверим, с чего начать",
        onSkip = null,
        content = {
            Text(
                text = "Короткая проверка знакомых слов. Если вы только начинаете — пойдём с самого первого блока.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBg,
            )

            ProgressBarWithLabel(fraction = 0.25f, label = "Мини-проверка")

            BebringoCard {
                Text(
                    text = "Bună ziua!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBg,
                )
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = "Что это значит?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.m))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    options.forEachIndexed { idx, option ->
                        ChoiceRow(
                            text = option,
                            selected = selectedIdx == idx,
                            onClick = { selectedIdx = idx },
                        )
                    }
                }
            }
        },
        bottom = {
            PrimaryButton(text = "Продолжить", onClick = onNext)
            Spacer(Modifier.height(Spacing.s))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Назад", color = colors.muted)
            }
        },
    )
}

@Composable
fun OnboardingHeadStartScreen(
    onNext: () -> Unit,
) {
    val colors = BebringoTheme.colors

    OnboardingScaffold(
        title = "У вас уже есть фора",
        onSkip = null,
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Mascot(pose = MascotPose.READING, size = 84.dp)
                Text(
                    text = "Интернациональные слова в румынском понятны сразу — мы опираемся на них в первых сценах.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                    modifier = Modifier.weight(1f),
                )
            }

            BebringoCard {
                ProgressBarWithLabel(fraction = 0.4f, label = "Блок 1 · «Bună ziua»")
                Spacer(Modifier.height(Spacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    SelectableChip(text = "telefon", selected = true, onClick = {})
                    SelectableChip(text = "cultură", selected = true, onClick = {})
                    SelectableChip(text = "programator", selected = true, onClick = {})
                }
            }

            Text(
                text = "telefon — телефон\ncultură — культура\nprogramator — программист",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBg,
            )
        },
        bottom = {
            PrimaryButton(text = "Дальше", onClick = onNext)
        },
    )
}

@Composable
fun OnboardingReadyScreen(
    onFinish: () -> Unit,
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding()
            .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(pose = MascotPose.CELEBRATE, size = 148.dp)

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = "Всё готово к старту!",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBg,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.l))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf(
                "Блоки по 3–4 минуты с карточками теории и практикой",
                "Интервальное повторение FSRS подстраивается под вашу память",
                "Спокойная серия дней с двумя заморозками в месяц",
            ).forEach { line ->
                Text(
                    text = "· $line",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))

        PrimaryButton(text = "Перейти к первому блоку", onClick = onFinish)
    }
}
