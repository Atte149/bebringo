package org.dueattendant149.bebringo.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.data.sample.SampleData
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.Lemma
import org.dueattendant149.bebringo.domain.model.LessonResult
import org.dueattendant149.bebringo.domain.reading.ReadingCoverage
import org.dueattendant149.bebringo.domain.session.DefaultBlockBuilder
import org.dueattendant149.bebringo.ui.screens.chat.ChatListScreen
import org.dueattendant149.bebringo.ui.screens.chat.ChatScreen
import org.dueattendant149.bebringo.ui.screens.chat.ChatViewModel
import org.dueattendant149.bebringo.ui.screens.common.TelemetryScreen
import org.dueattendant149.bebringo.ui.screens.common.WordOfDayScreen
import org.dueattendant149.bebringo.ui.screens.home.HomeScreen
import org.dueattendant149.bebringo.ui.screens.home.HomeViewModel
import org.dueattendant149.bebringo.ui.screens.lab.LabScreen
import org.dueattendant149.bebringo.ui.screens.lesson.BlockReviewScreen
import org.dueattendant149.bebringo.ui.screens.lesson.LessonScreen
import org.dueattendant149.bebringo.ui.screens.lesson.LessonViewModel
import org.dueattendant149.bebringo.ui.screens.onboarding.OnboardingAnchorScreen
import org.dueattendant149.bebringo.ui.screens.onboarding.OnboardingDraft
import org.dueattendant149.bebringo.ui.screens.onboarding.OnboardingGoalScreen
import org.dueattendant149.bebringo.ui.screens.onboarding.OnboardingHeadStartScreen
import org.dueattendant149.bebringo.ui.screens.onboarding.OnboardingPlacementScreen
import org.dueattendant149.bebringo.ui.screens.onboarding.OnboardingReadyScreen
import org.dueattendant149.bebringo.ui.screens.profile.BackupScreen
import org.dueattendant149.bebringo.ui.screens.profile.LearningSettingsScreen
import org.dueattendant149.bebringo.ui.screens.profile.LlmSettingsScreen
import org.dueattendant149.bebringo.ui.screens.profile.ProfileScreen
import org.dueattendant149.bebringo.ui.screens.profile.SettingsViewModel
import org.dueattendant149.bebringo.ui.screens.progress.ProgressScreen
import org.dueattendant149.bebringo.ui.screens.progress.StatsViewModel
import org.dueattendant149.bebringo.ui.screens.reading.ReaderScreen
import org.dueattendant149.bebringo.ui.screens.reading.ReadingListScreen
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import java.time.LocalDate

/**
 * Граф навигации Bebringo.
 *
 * Все экраны подключены к реальным репозиториям, FSRS, телеметрии и `SettingsStore`.
 */
@Composable
fun BebringoNavHost(
    navController: NavHostController,
    startDestination: String = Routes.ONBOARDING_GOAL,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // --- Онбординг ---
        composable(Routes.ONBOARDING_GOAL) {
            val scope = rememberCoroutineScope()
            OnboardingGoalScreen(
                onNext = { selected ->
                    OnboardingDraft.goal = selected
                    navController.navigate(Routes.ONBOARDING_ANCHOR)
                },
                onSkip = {
                    scope.launch {
                        ServiceLocator.settings.completeOnboarding(
                            goal = OnboardingDraft.goal,
                            anchorTime = OnboardingDraft.anchorTime,
                            anchorAfter = OnboardingDraft.anchorAfter,
                        )
                    }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING_GOAL) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING_ANCHOR) {
            OnboardingAnchorScreen(
                onNext = { time, after ->
                    OnboardingDraft.anchorTime = time
                    OnboardingDraft.anchorAfter = after
                    navController.navigate(Routes.ONBOARDING_PLACEMENT)
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ONBOARDING_PLACEMENT) {
            OnboardingPlacementScreen(
                onNext = { navController.navigate(Routes.ONBOARDING_HEAD_START) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ONBOARDING_HEAD_START) {
            OnboardingHeadStartScreen(
                onNext = { navController.navigate(Routes.ONBOARDING_READY) },
            )
        }

        composable(Routes.ONBOARDING_READY) {
            val scope = rememberCoroutineScope()
            OnboardingReadyScreen(
                onFinish = {
                    scope.launch {
                        ServiceLocator.settings.completeOnboarding(
                            goal = OnboardingDraft.goal,
                            anchorTime = OnboardingDraft.anchorTime,
                            anchorAfter = OnboardingDraft.anchorAfter,
                        )
                    }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING_GOAL) { inclusive = true }
                    }
                },
            )
        }

        // --- Вкладки ---
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) { viewModel.refresh() }

            HomeScreen(
                streakDays = state.streakDays,
                freezesLeft = state.freezesLeft,
                blockNumber = state.blockNumber,
                blockTitle = state.blockTitle,
                blockSubtitleRu = state.blockSubtitleRu,
                chapterTitle = state.chapterTitle,
                chapterTitleRu = state.chapterTitleRu,
                stepsLeft = state.stepsLeft,
                stepsTotal = state.stepsTotal,
                partIndex = state.partIndex,
                partsTotal = state.partsTotal,
                chapterDone = state.chapterDone,
                chapterCurrent = state.chapterCurrent,
                chapterTotal = state.chapterTotal,
                minutesToday = state.minutesToday,
                blocksToday = state.blocksToday,
                dueCount = state.dueCount,
                onReview = { navController.navigate(Routes.REVIEW_SESSION) },
                word = state.word,
                wordTranslation = state.wordTranslation,
                mascotPose = state.mascotPose,
                tracks = state.tracks,
                selectedTrackId = state.selectedTrackId,
                onSelectTrack = { viewModel.selectTrack(it) },
                onContinue = {
                    val blockId = state.nextBlockId
                    if (blockId != null) navController.navigate(Routes.lesson(blockId))
                },
                repairAvailable = state.repairAvailable,
                onRepair = { viewModel.repairStreak() },
                onReading = { navController.navigate(Routes.READING_LIST) },
                onChat = { navController.navigate(Routes.CHAT_LIST) },
                onWordOfDay = { navController.navigate(Routes.WORD_OF_DAY) },
            )
        }

        composable(Routes.PROGRESS) {
            val stats: StatsViewModel = viewModel()
            val statsState by stats.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { stats.refresh() }

            ProgressScreen(
                coverage = statsState.coverage,
                readingTitle = statsState.readingTitle,
                skills = statsState.skills,
                collectionCount = statsState.collectionCount,
                collectionPreview = statsState.collectionPreview,
            )
        }

        composable(Routes.PROFILE) {
            val stats: StatsViewModel = viewModel()
            val statsState by stats.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { stats.refresh() }

            ProfileScreen(
                streakDays = statsState.streakDays,
                onLab = { navController.navigate(Routes.LAB) },
                onBackup = { navController.navigate(Routes.BACKUP) },
                onTelemetry = { navController.navigate(Routes.TELEMETRY) },
                onLlm = { navController.navigate(Routes.LLM_SETTINGS) },
                onDiacritics = { navController.navigate(Routes.DIACRITICS_SETTINGS) },
            )
        }

        // --- Урок ---
        composable(
            route = Routes.LESSON,
            arguments = listOf(navArgument("blockId") { type = NavType.StringType }),
        ) { entry ->
            val blockId = entry.arguments?.getString("blockId").orEmpty()
            LessonRoute(
                blockId = blockId,
                onBack = { navController.popBackStack() },
                onFinished = { block, chapterId, result ->
                    SessionHolder.put(block.id, chapterId, result)
                    navController.navigate(Routes.REVIEW)
                },
            )
        }

        composable(Routes.REVIEW) {
            val holder = SessionHolder
            val result = holder.result
            val lessonViewModel: LessonViewModel = viewModel()
            var block by remember(holder.blockId) { mutableStateOf<Block?>(null) }
            var nextBlock by remember(holder.blockId) { mutableStateOf<Block?>(null) }
            var partIndex by remember(holder.blockId) { mutableIntStateOf(1) }
            var partsTotal by remember(holder.blockId) { mutableIntStateOf(1) }

            LaunchedEffect(holder.blockId) {
                val course = runCatching { ServiceLocator.courseRepository.course() }.getOrNull()
                block = course?.blockById(holder.blockId)
                if (block != null && result != null) {
                    lessonViewModel.finish(block!!, holder.chapterId, result) {}
                }

                val scene = course?.units
                    ?.flatMap { it.chapters }
                    ?.flatMap { it.scenes }
                    ?.firstOrNull { scene -> scene.blocks.any { it.id == holder.blockId } }
                val sceneBlocks = scene?.blocks.orEmpty()
                partIndex = sceneBlocks.indexOfFirst { it.id == holder.blockId }
                    .let { if (it >= 0) it + 1 else 1 }
                partsTotal = sceneBlocks.size.coerceAtLeast(1)
                nextBlock = sceneBlocks.dropWhile { it.id != holder.blockId }.getOrNull(1)
            }

            val combined = holder.combinedResult() ?: result
            BlockReviewScreen(
                correct = combined?.correct ?: 0,
                total = combined?.total ?: 5,
                mistakes = combined?.mistakes.orEmpty(),
                partIndex = partIndex,
                partsTotal = partsTotal,
                onContinueLesson = nextBlock?.let { next ->
                    {
                        // Убираем и разбор, и уже пройденную часть, чтобы «назад»
                        // не возвращал в завершённый урок.
                        navController.navigate(Routes.lesson(next.id)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                },
                onContinue = {
                    SessionHolder.clear()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        // --- Отдельная сессия повторения ---
        composable(Routes.REVIEW_SESSION) {
            val colors = BebringoTheme.colors
            val reviewViewModel: LessonViewModel = viewModel()
            val scope = rememberCoroutineScope()
            val soundEnabled by ServiceLocator.settings.soundEnabled
                .collectAsStateWithLifecycle(initialValue = true)
            val autoPlayAudio by ServiceLocator.settings.autoPlayAudio
                .collectAsStateWithLifecycle(initialValue = false)
            val strictDiacritics by ServiceLocator.settings.strictDiacritics
                .collectAsStateWithLifecycle(initialValue = false)

            var block by remember { mutableStateOf<Block?>(null) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                SessionHolder.clear()
                runCatching {
                    val course = ServiceLocator.courseRepository.course()
                    val due = ServiceLocator.srsRepository
                        .due(System.currentTimeMillis(), limit = 10)
                        .filter { !it.isNew }
                    val builder = DefaultBlockBuilder(
                        lemmaProvider = { id -> course.lemmas[id] },
                        allLemmasProvider = { course.lemmas.values },
                    )
                    builder.buildReviewBlock(due)
                }.onSuccess { block = it }.onFailure { error = it.message }
            }

            val current = block
            when {
                error != null -> Box(
                    modifier = Modifier.fillMaxSize().background(colors.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Не удалось собрать повторение: $error", color = colors.error)
                }

                current == null -> Box(
                    modifier = Modifier.fillMaxSize().background(colors.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }

                current.steps.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().background(colors.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "На сегодня повторений нет. Возвращайтесь после следующего урока.",
                        color = colors.muted,
                    )
                }

                else -> LessonScreen(
                    block = current,
                    onBack = { navController.popBackStack() },
                    requireDiacritics = strictDiacritics,
                    soundEnabled = soundEnabled,
                    autoPlayAudio = autoPlayAudio,
                    onToggleSound = {
                        scope.launch { ServiceLocator.settings.setSoundEnabled(!soundEnabled) }
                    },
                    finishLabel = "Завершить повторение",
                    onAnswer = { event, nextStepIndex, correctSoFar, totalSoFar ->
                        reviewViewModel.recordAnswer(
                            block = current,
                            event = event,
                            chapterId = "",
                            nextStepIndex = nextStepIndex,
                            correctSoFar = correctSoFar,
                            totalSoFar = totalSoFar,
                            saveProgress = false,
                        )
                    },
                    onFinished = {
                        reviewViewModel.finishReview(current) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }

        // --- Чтение ---
        composable(Routes.READING_LIST) {
            var knownWords by remember { mutableStateOf<Set<String>>(emptySet()) }
            LaunchedEffect(Unit) {
                knownWords = runCatching { knownReadingWords() }.getOrDefault(emptySet())
            }
            ReadingListScreen(
                readings = SampleData.readings,
                knownWords = knownWords,
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Routes.reader(id)) },
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("readingId") { type = NavType.StringType }),
        ) { entry ->
            val readingId = entry.arguments?.getString("readingId")
            val reading = SampleData.readings.firstOrNull { it.id == readingId }
                ?: SampleData.readings.first()
            val scope = rememberCoroutineScope()
            var lemmas by remember { mutableStateOf<Map<String, Lemma>>(emptyMap()) }
            var knownWords by remember { mutableStateOf<Set<String>>(emptySet()) }
            LaunchedEffect(Unit) {
                lemmas = runCatching { ServiceLocator.courseRepository.lemmaMap() }.getOrDefault(emptyMap())
                knownWords = runCatching { knownReadingWords() }.getOrDefault(emptySet())
            }
            ReaderScreen(
                reading = reading,
                onBack = { navController.popBackStack() },
                lemmas = lemmas,
                knownWords = knownWords,
                onAddWordToSrs = { wordRo, translationRu ->
                    scope.launch {
                        ServiceLocator.collectionRepository.add(wordRo, translationRu)
                        val matchId = lemmas.entries.firstOrNull {
                            it.value.lemma.equals(wordRo, ignoreCase = true)
                        }?.key ?: lemmas.entries.firstOrNull {
                            ReadingCoverage.isKnown(
                                wordRo,
                                setOf(ReadingCoverage.normalize(it.value.lemma)),
                            )
                        }?.key ?: "reader-${wordRo.lowercase()}"
                        ServiceLocator.srsRepository.ensureCards(listOf(matchId))
                    }
                },
            )
        }

        // --- Чат и прочие разделы ---
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                scenarios = SampleData.chatScenarios,
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Routes.chat(id)) },
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType }),
        ) { entry ->
            val scenarioId = entry.arguments?.getString("scenarioId")
            val scenario = SampleData.chatScenarios.firstOrNull { it.id == scenarioId }
                ?: SampleData.chatScenarios.first()

            val chatViewModel: ChatViewModel = viewModel()
            val chatState by chatViewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(scenario.id) { chatViewModel.start(scenario) }

            ChatScreen(
                scenario = scenario,
                state = chatState,
                onBack = { navController.popBackStack() },
                onSend = { text -> chatViewModel.send(scenario, text) },
                onFinish = { chatViewModel.finish() },
                onDismissReview = { chatViewModel.dismissReview() },
                onOpenSettings = { navController.navigate(Routes.LLM_SETTINGS) },
            )
        }

        composable(Routes.WORD_OF_DAY) {
            val scope = rememberCoroutineScope()
            var lemma by remember { mutableStateOf<Lemma?>(null) }
            LaunchedEffect(Unit) {
                runCatching {
                    val course = ServiceLocator.courseRepository.course()
                    val dayOfYear = LocalDate.now().dayOfYear
                    lemma = course.lemmas.values.elementAtOrNull(dayOfYear % course.lemmas.size.coerceAtLeast(1))
                }
            }
            val currentLemma = lemma
            WordOfDayScreen(
                word = currentLemma?.lemma ?: SampleData.wordOfDay,
                translation = currentLemma?.translationRu ?: SampleData.wordOfDayTranslation,
                exampleRo = currentLemma?.exampleRo,
                exampleRu = currentLemma?.exampleRu,
                onBack = { navController.popBackStack() },
                onAddToSrs = {
                    scope.launch {
                        val id = currentLemma?.id ?: "tren"
                        ServiceLocator.srsRepository.ensureCards(listOf(id))
                        ServiceLocator.collectionRepository.add(
                            textRo = currentLemma?.exampleRo ?: currentLemma?.lemma ?: SampleData.wordOfDay,
                            translationRu = currentLemma?.exampleRu ?: currentLemma?.translationRu ?: SampleData.wordOfDayTranslation,
                        )
                    }
                },
            )
        }

        composable(Routes.LAB) {
            val scope = rememberCoroutineScope()
            val quietStreaks by ServiceLocator.settings.labQuietStreaks.collectAsStateWithLifecycle(initialValue = true)
            val dailyReview by ServiceLocator.settings.labDailyReview.collectAsStateWithLifecycle(initialValue = false)
            val autoSupport by ServiceLocator.settings.labAutoSupport.collectAsStateWithLifecycle(initialValue = true)
            var adherence by remember { mutableFloatStateOf(0f) }
            var exportMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                runCatching {
                    val today = LocalDate.now()
                    val cutoff = today.minusDays(27)
                    val activeDays = ServiceLocator.streakRepository.allDays()
                        .count { it.day >= cutoff && (it.closedBlocks > 0 || it.repaired) }
                    adherence = (activeDays / 28f).coerceIn(0f, 1f)
                }
            }

            LabScreen(
                adherence = adherence,
                onBack = { navController.popBackStack() },
                quietStreaks = quietStreaks,
                onQuietStreaksChange = { value -> scope.launch { ServiceLocator.settings.setLabQuietStreaks(value) } },
                dailyReview = dailyReview,
                onDailyReviewChange = { value -> scope.launch { ServiceLocator.settings.setLabDailyReview(value) } },
                autoSupport = autoSupport,
                onAutoSupportChange = { value -> scope.launch { ServiceLocator.settings.setLabAutoSupport(value) } },
                exportMessage = exportMessage,
                onExport = {
                    scope.launch {
                        runCatching {
                            val file = ServiceLocator.telemetryRepository.exportCsv()
                            exportMessage = "CSV выгружен: ${file.name}"
                        }.onFailure {
                            exportMessage = "Ошибка экспорта: ${it.message}"
                        }
                    }
                },
            )
        }

        composable(Routes.BACKUP) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

            BackupScreen(
                state = settingsState,
                onChange = { url, user, password -> settingsViewModel.updateWebdav(url, user, password) },
                onSave = { settingsViewModel.saveWebdav() },
                onExport = { settingsViewModel.exportNow() },
                onUpload = { settingsViewModel.uploadNow() },
                onImportUri = { uri -> settingsViewModel.importFromUri(uri) },
                onRestoreLatestLocal = { settingsViewModel.restoreLatestLocal() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.LLM_SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

            LlmSettingsScreen(
                state = settingsState,
                onChange = { endpoint, model, key -> settingsViewModel.updateLlm(endpoint, model, key) },
                onSave = { settingsViewModel.saveLlm() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DIACRITICS_SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

            LearningSettingsScreen(
                state = settingsState,
                onStrictChange = { settingsViewModel.setStrictDiacritics(it) },
                onDynamicColorChange = { settingsViewModel.setDynamicColor(it) },
                onSoundChange = { settingsViewModel.setSoundEnabled(it) },
                onAutoPlayChange = { settingsViewModel.setAutoPlayAudio(it) },
                onSupportLevelChange = { settingsViewModel.setSupportLevel(it) },
                onRetentionChange = { settingsViewModel.setDesiredRetention(it) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TELEMETRY) {
            val scope = rememberCoroutineScope()
            val count by ServiceLocator.telemetryRepository.count().collectAsStateWithLifecycle(initialValue = 0)
            var statusMessage by remember { mutableStateOf<String?>(null) }

            TelemetryScreen(
                eventCount = count,
                onBack = { navController.popBackStack() },
                statusMessage = statusMessage,
                onExportCsv = {
                    scope.launch {
                        runCatching {
                            val file = ServiceLocator.telemetryRepository.exportCsv()
                            val eventsFile = ServiceLocator.eventRepository.exportCsv()
                            statusMessage = "Экспортировано: ${file.name}, ${eventsFile.name}"
                        }.onFailure {
                            statusMessage = "Ошибка: ${it.message}"
                        }
                    }
                },
                onClear = {
                    scope.launch {
                        ServiceLocator.telemetryRepository.clear()
                        statusMessage = "История телеметрии очищена"
                    }
                },
            )
        }
    }
}

/** Слова, которые уже встречались в уроках: по ним считается знакомость чтения. */
private suspend fun knownReadingWords(): Set<String> {
    val lemmas = ServiceLocator.courseRepository.lemmaMap()
    return ServiceLocator.srsRepository.learnedLemmaIds()
        .mapNotNull { lemmas[it]?.lemma }
        .flatMap { it.split(Regex("\\s+")) }
        .filter { it.isNotBlank() }
        .toSet()
}

/** Загружает блок из курса и запускает урок. */
@Composable
private fun LessonRoute(
    blockId: String,
    onBack: () -> Unit,
    onFinished: (Block, String, LessonResult) -> Unit,
) {
    val colors = BebringoTheme.colors
    var block by remember(blockId) { mutableStateOf<Block?>(null) }
    var chapterId by remember(blockId) { mutableStateOf("") }
    var initialStepIndex by remember(blockId) { mutableIntStateOf(0) }
    var finishLabel by remember(blockId) { mutableStateOf("Завершить урок") }
    var error by remember(blockId) { mutableStateOf<String?>(null) }
    val strictDiacritics by ServiceLocator.settings.strictDiacritics.collectAsStateWithLifecycle(initialValue = false)
    val soundEnabled by ServiceLocator.settings.soundEnabled.collectAsStateWithLifecycle(initialValue = true)
    val autoPlayAudio by ServiceLocator.settings.autoPlayAudio.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val viewModel: LessonViewModel = viewModel()

    LaunchedEffect(blockId) {
        runCatching {
            val course = ServiceLocator.courseRepository.course()
            val authored = course.blockById(blockId) ?: course.blocks.first()
            val savedProgress = ServiceLocator.progressRepository.byBlock(authored.id)
            val resumeStep = if (savedProgress != null && !savedProgress.closed) savedProgress.stepIndex else 0

            val chapter = course.units
                .flatMap { it.chapters }
                .firstOrNull { ch -> ch.scenes.any { scene -> scene.blocks.any { it.id == authored.id } } }
            val scene = chapter?.scenes?.firstOrNull { sc -> sc.blocks.any { it.id == authored.id } }
            val sceneIndex = scene?.blocks?.indexOfFirst { it.id == authored.id } ?: -1
            finishLabel = if (scene != null && sceneIndex >= 0 && sceneIndex < scene.blocks.size - 1) {
                "Завершить часть"
            } else {
                "Завершить урок"
            }
            // Урок — это сцена из нескольких блоков. Результаты частей копятся в SessionHolder.
            SessionHolder.begin(scene?.id.orEmpty(), scene?.titleRo.orEmpty())
            // Новый урок не подменяет авторские шаги повторениями: повторение живёт
            // отдельной сессией и приходит в другой день.
            block = authored
            chapterId = chapter?.id.orEmpty()
            initialStepIndex = resumeStep
        }.onFailure { error = it.message }
    }

    val current = block
    when {
        error != null -> Box(
            modifier = Modifier.fillMaxSize().background(colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Не удалось загрузить блок: $error", color = colors.error)
        }

        current == null -> Box(
            modifier = Modifier.fillMaxSize().background(colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = colors.primary)
        }

        else -> LessonScreen(
            block = current,
            onBack = onBack,
            requireDiacritics = strictDiacritics,
            initialStepIndex = initialStepIndex,
            soundEnabled = soundEnabled,
            autoPlayAudio = autoPlayAudio,
            onToggleSound = {
                scope.launch { ServiceLocator.settings.setSoundEnabled(!soundEnabled) }
            },
            finishLabel = finishLabel,
            onAnswer = { event, nextStepIndex, correctSoFar, totalSoFar ->
                viewModel.recordAnswer(
                    block = current,
                    event = event,
                    chapterId = chapterId,
                    nextStepIndex = nextStepIndex,
                    correctSoFar = correctSoFar,
                    totalSoFar = totalSoFar,
                )
            },
            onFinished = { result -> onFinished(current, chapterId, result) },
        )
    }
}
