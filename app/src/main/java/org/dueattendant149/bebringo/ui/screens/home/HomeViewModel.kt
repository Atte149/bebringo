package org.dueattendant149.bebringo.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.domain.entry.EventTypes
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.TrackMeta
import org.dueattendant149.bebringo.widget.BebringoWidgets
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loading: Boolean = true,
    val streakDays: Int = 0,
    val freezesLeft: Int = 2,
    val blockNumber: Int = 1,
    val blockTitle: String = "",
    val blockSubtitleRu: String = "",
    val chapterTitle: String = "",
    val chapterTitleRu: String = "",
    val stepsLeft: Int = 5,
    val stepsTotal: Int = 5,
    val partIndex: Int = 1,
    val partsTotal: Int = 1,
    val chapterDone: Int = 0,
    val chapterCurrent: Int = 0,
    val chapterTotal: Int = 4,
    val minutesToday: Int = 0,
    val blocksToday: Int = 0,
    val dueCount: Int = 0,
    val nextBlockId: String? = null,
    val mascotPose: String = "flag_ro",
    val tracks: List<TrackMeta> = emptyList(),
    val selectedTrackId: String = "all",
    val word: String = "",
    val wordTranslation: String = "",
    val error: String? = null,
    val repairAvailable: Boolean = false,
)

/**
 * Состояние главного экрана «Мой день».
 *
 * Собирает текущий/незаконченный блок по выбранному треку, реальный прогресс главы,
 * честную статистику за сегодня, серию дней и слово дня.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun selectTrack(trackId: String) {
        viewModelScope.launch {
            ServiceLocator.settings.setSelectedTrackFilter(trackId)
            refresh()
        }
    }

    /** Закрывает вчерашний день задним числом, если разрыв случился только вчера. */
    fun repairStreak() {
        viewModelScope.launch {
            runCatching {
                ServiceLocator.streakRepository.repair(LocalDate.now().minusDays(1))
                ServiceLocator.logEvent(EventTypes.STREAK_REPAIRED)
                BebringoWidgets.updateAll(getApplication())
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val course = ServiceLocator.courseRepository.course()
                val selectedTrack = ServiceLocator.settings.selectedTrackFilter.first()
                val unfinished = ServiceLocator.progressRepository.unfinished()
                val closedIds = ServiceLocator.progressRepository.closedBlockIds()
                val today = LocalDate.now()
                val streak = ServiceLocator.streakRepository.state(today)
                val dueCards = ServiceLocator.srsRepository
                    .due(System.currentTimeMillis(), limit = 10)
                    .filter { !it.isNew }

                val trackBlocks = course.blocksForTrack(selectedTrack).ifEmpty { course.blocks }
                val unfinishedBlock = unfinished?.let { u ->
                    trackBlocks.firstOrNull { it.id == u.blockId }
                }
                val block: Block? = unfinishedBlock
                    ?: trackBlocks.firstOrNull { it.id !in closedIds }
                    ?: trackBlocks.firstOrNull()

                val blockNumber = if (block != null) {
                    (trackBlocks.indexOfFirst { it.id == block.id } + 1).coerceAtLeast(1)
                } else {
                    1
                }

                val chapter = course.units
                    .flatMap { it.chapters }
                    .firstOrNull { ch -> ch.scenes.any { scene -> scene.blocks.any { it.id == block?.id } } }

                // Урок — это сцена из нескольких блоков. Счётчик шагов и прогресс
                // считаем по всей сцене, чтобы 10 шагов не выглядели как 5.
                val scene = chapter?.scenes?.firstOrNull { sc -> sc.blocks.any { it.id == block?.id } }
                val sceneBlocks = scene?.blocks.orEmpty().ifEmpty { listOfNotNull(block) }
                val partIndex = sceneBlocks.indexOfFirst { it.id == block?.id }
                    .let { if (it >= 0) it + 1 else 1 }
                val partsTotal = sceneBlocks.size.coerceAtLeast(1)
                val sessionTotalSteps = sceneBlocks.sumOf { it.total }.coerceAtLeast(1)
                val sessionDoneBefore = sceneBlocks
                    .take((partIndex - 1).coerceAtLeast(0))
                    .sumOf { it.total }

                val chapterBlocks = chapter?.scenes?.flatMap { it.blocks }.orEmpty()
                val chapterDone = chapterBlocks.count { it.id in closedIds }
                val chapterCurrent = chapterBlocks.indexOfFirst { it.id == block?.id }.coerceAtLeast(0)
                val chapterTotal = chapterBlocks.size.coerceAtLeast(1)

                val startOfTodayMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val minutesToday = ServiceLocator.telemetryRepository.minutesSince(
                    fromTs = startOfTodayMs,
                    blocksToday = streak.blocksToday,
                )

                val dayOfYear = today.dayOfYear
                val lemma = course.lemmas.values.elementAtOrNull(dayOfYear % course.lemmas.size.coerceAtLeast(1))
                val activeTrackMeta = course.trackById(block?.trackId ?: "ro_a1") ?: course.track

                val savedStepIndex = if (unfinishedBlock != null) (unfinished?.stepIndex ?: 0) else 0
                val sessionDone = (sessionDoneBefore + savedStepIndex)
                    .coerceIn(0, sessionTotalSteps)
                val remainingSteps = (sessionTotalSteps - sessionDone).coerceIn(1, sessionTotalSteps)

                _state.update {
                    HomeUiState(
                        loading = false,
                        streakDays = streak.days,
                        freezesLeft = streak.freezesLeft,
                        blockNumber = blockNumber,
                        blockTitle = block?.titleRo ?: "",
                        blockSubtitleRu = block?.titleRu ?: "",
                        chapterTitle = chapter?.titleRo ?: "",
                        chapterTitleRu = chapter?.titleRu ?: "",
                        stepsLeft = remainingSteps,
                        stepsTotal = sessionTotalSteps,
                        partIndex = partIndex,
                        partsTotal = partsTotal,
                        chapterDone = chapterDone,
                        chapterCurrent = chapterCurrent,
                        chapterTotal = chapterTotal,
                        blocksToday = streak.blocksToday,
                        minutesToday = minutesToday,
                        dueCount = dueCards.size,
                        nextBlockId = block?.id,
                        mascotPose = activeTrackMeta.mascotPose,
                        tracks = course.tracks,
                        selectedTrackId = selectedTrack,
                        word = lemma?.lemma ?: "tren",
                        wordTranslation = lemma?.translationRu ?: "поезд",
                        repairAvailable = streak.canRepairYesterday,
                    )
                }
                BebringoWidgets.updateAll(getApplication())
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message) }
            }
        }
    }
}
