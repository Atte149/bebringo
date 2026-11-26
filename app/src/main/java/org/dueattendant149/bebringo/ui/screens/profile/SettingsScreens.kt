package org.dueattendant149.bebringo.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.audio.TtsPlayer
import org.dueattendant149.bebringo.ui.components.AudioButton
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.BebringoTextField
import org.dueattendant149.bebringo.ui.components.BebringoTopBar
import org.dueattendant149.bebringo.ui.components.GroupedListCard
import org.dueattendant149.bebringo.ui.components.PrimaryButton
import org.dueattendant149.bebringo.ui.components.SecondaryButton
import org.dueattendant149.bebringo.ui.components.SuccessFeedbackCard
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Настройки модели ИИ, к которой подключается чат и разбор рефлексии. */
@Composable
fun LlmSettingsScreen(
    state: SettingsUiState,
    onChange: (endpoint: String, model: String, key: String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = "Модель ИИ")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(
                text = "Подойдёт любой OpenAI-совместимый адрес (`/v1/chat/completions`). Ключ хранится только локально на этом устройстве.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )

            BebringoTextField(
                value = state.llmEndpoint,
                onValueChange = { onChange(it, state.llmModel, state.llmKey) },
                placeholder = "https://api.openai.com",
            )
            BebringoTextField(
                value = state.llmModel,
                onValueChange = { onChange(state.llmEndpoint, it, state.llmKey) },
                placeholder = "gpt-4o-mini",
            )
            BebringoTextField(
                value = state.llmKey,
                onValueChange = { onChange(state.llmEndpoint, state.llmModel, it) },
                placeholder = "API-ключ (sk-...)",
            )

            PrimaryButton(text = "Сохранить", onClick = onSave)

            if (state.message != null) {
                SuccessFeedbackCard(title = state.message)
            }
        }
    }
}

/** Бэкап: локальный экспорт, импорт из файла, восстановление и WebDAV. */
@Composable
fun BackupScreen(
    state: SettingsUiState,
    onChange: (url: String, user: String, password: String) -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    onImportUri: (Uri) -> Unit = {},
    onRestoreLatestLocal: () -> Unit = {},
) {
    val colors = BebringoTheme.colors
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) onImportUri(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = "Резервная копия")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            BebringoCard {
                Text(
                    text = "Локальные копии (${state.localBackupCount} из 7)",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBg,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Экспорт и импорт прогресса, карточек FSRS, коллекции фраз и серии дней в формате JSON.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.m))
                PrimaryButton(
                    text = "Создать локальную копию сейчас",
                    enabled = !state.busy,
                    onClick = onExport,
                )
                Spacer(Modifier.height(Spacing.s))
                SecondaryButton(
                    text = "Восстановить из файла (.json)",
                    enabled = !state.busy,
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                )
                if (state.localBackupCount > 0) {
                    Spacer(Modifier.height(Spacing.s))
                    SecondaryButton(
                        text = "Восстановить последнюю локальную копию",
                        enabled = !state.busy,
                        onClick = onRestoreLatestLocal,
                    )
                }
            }

            BebringoCard {
                Text(text = "Облако WebDAV", style = MaterialTheme.typography.titleLarge, color = colors.onBg)
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = "Отправка копии по протоколу WebDAV (Nextcloud / ownCloud / Яндекс.Диск).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.m))
                BebringoTextField(
                    value = state.webdavUrl,
                    onValueChange = { onChange(it, state.webdavUser, state.webdavPassword) },
                    placeholder = "https://cloud.example.com/remote.php/dav/files/user/bebringo.json",
                )
                Spacer(Modifier.height(Spacing.s))
                BebringoTextField(
                    value = state.webdavUser,
                    onValueChange = { onChange(state.webdavUrl, it, state.webdavPassword) },
                    placeholder = "Логин",
                )
                Spacer(Modifier.height(Spacing.s))
                BebringoTextField(
                    value = state.webdavPassword,
                    onValueChange = { onChange(state.webdavUrl, state.webdavUser, it) },
                    placeholder = "Пароль или пароль приложения",
                )
                Spacer(Modifier.height(Spacing.m))
                PrimaryButton(text = "Сохранить настройки WebDAV", enabled = !state.busy, onClick = onSave)
                Spacer(Modifier.height(Spacing.s))
                SecondaryButton(text = "Отправить на WebDAV сейчас", enabled = !state.busy, onClick = onUpload)
            }

            if (state.message != null) {
                SuccessFeedbackCard(title = state.message)
            }
        }
    }
}

/** Настройки диакритики, русской подпорки, удержания FSRS и Material You Dynamic Color. */
@Composable
fun LearningSettingsScreen(
    state: SettingsUiState,
    onStrictChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit = {},
    onSoundChange: (Boolean) -> Unit = {},
    onAutoPlayChange: (Boolean) -> Unit = {},
    onSupportLevelChange: (Int) -> Unit = {},
    onRetentionChange: (Int) -> Unit = {},
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        BebringoTopBar(onBack = onBack, title = "Обучение и оформление")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            GroupedListCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.m)) {
                        Text(
                            text = "Строгая проверка диакритики",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBg,
                        )
                        Text(
                            text = "Если выключено, ответ без ă, â, î, ș, ț засчитывается с мягким напоминанием.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    Switch(
                        checked = state.strictDiacritics,
                        onCheckedChange = onStrictChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                        ),
                    )
                }

                HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.m)) {
                        Text(
                            text = "Динамические цвета Material You",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBg,
                        )
                        Text(
                            text = "Адаптация палитры под обои Android 12+ (по умолчанию — тёплая лесная палитра Bebringo).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    Switch(
                        checked = state.dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                        ),
                    )
                }
            }

            BebringoCard {
                val context = LocalContext.current
                val tts = remember { TtsPlayer(context) }
                DisposableEffect(Unit) { onDispose { tts.release() } }
                val isPlaying by tts.isPlaying.collectAsState()

                Text(
                    text = "Озвучка и произношение",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBg,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "В приложение встроена студийная офлайн-база румынского языка (${tts.getOfflinePhrasesCount()} фраз). Работает автономно без интернета.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )

                Spacer(Modifier.height(Spacing.m))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.m)) {
                        Text(
                            text = "Звук приложения",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBg,
                        )
                        Text(
                            text = "Выключено — ни урок, ни чтение, ни слова не озвучиваются.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = onSoundChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                        ),
                    )
                }

                Spacer(Modifier.height(Spacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.m)) {
                        Text(
                            text = "Автозапуск аудио в уроке",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBg,
                        )
                        Text(
                            text = "Если выключено, аудиошаг ждёт нажатия «Прослушать».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    Switch(
                        checked = state.autoPlayAudio,
                        onCheckedChange = onAutoPlayChange,
                        enabled = state.soundEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                        ),
                    )
                }

                Spacer(Modifier.height(Spacing.m))
                if (state.soundEnabled) {
                    AudioButton(
                        label = "Проверить звук («Bună dimineața!»)",
                        isPlaying = isPlaying,
                        onClick = { tts.speak("Bună dimineața!") },
                    )
                } else {
                    Text(
                        text = "Звук выключен. Включите «Звук приложения», чтобы проверить озвучку.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }

            BebringoCard {
                Text(
                    text = "Уровень русской подпорки в чтении",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBg,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "0 — чистый оригинал, 1 — вплетение, 2 — перевод трудных мест, 3 — параллельный текст.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    listOf(0, 1, 2, 3).forEach { lvl ->
                        FilterChip(
                            selected = state.supportLevel == lvl,
                            onClick = { onSupportLevelChange(lvl) },
                            label = { Text("Уровень $lvl") },
                        )
                    }
                }
            }

            BebringoCard {
                Text(
                    text = "Целевое удержание FSRS",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBg,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Выше процент — чаще повторения и выше надёжность памяти.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    listOf(85, 90, 95).forEach { pct ->
                        FilterChip(
                            selected = state.desiredRetention == pct,
                            onClick = { onRetentionChange(pct) },
                            label = { Text("$pct%") },
                        )
                    }
                }
            }
        }
    }
}
