package org.dueattendant149.bebringo.ui.screens.progress

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
import org.dueattendant149.bebringo.domain.model.Skill
import java.time.LocalDate

data class StatsUiState(
    val streakDays: Int = 0,
    val freezesLeft: Int = 2,
    val coverage: Float = 0f,
    val learned: Int = 0,
    val closedBlocks: Int = 0,
    val totalBlocks: Int = 16,
    val collectionCount: Int = 0,
    val collectionPreview: List<Pair<String, String>> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val readingTitle: String = "",
)

/** Общая статистика для экранов «Прогресс» и «Профиль». */
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val streak = ServiceLocator.streakRepository.state(LocalDate.now())
                val learnedCards = ServiceLocator.srsRepository.learnedCount().first()
                val collectionItems = ServiceLocator.collectionRepository.all().first()
                val closedBlocks = ServiceLocator.progressRepository.closedCount()
                val course = ServiceLocator.courseRepository.course()
                val totalBlocks = course.blocks.size.coerceAtLeast(1)
                val totalLemmas = course.lemmas.size.coerceAtLeast(1)

                // На нулевом прогрессе 0 умений: умения открываются только по мере закрытия блоков (1 умение за каждые 2 блока)
                ServiceLocator.skillRepository.syncWithProgress(DEFAULT_SKILLS, closedBlocks)
                val unlockedSkillCount = (closedBlocks / 2).coerceIn(0, DEFAULT_SKILLS.size)
                val skills = ServiceLocator.skillRepository.all().first().mapIndexed { index, entity ->
                    Skill(
                        id = entity.id,
                        titleRu = entity.titleRu,
                        achieved = index < unlockedSkillCount,
                    )
                }

                val wordCoverage = (learnedCards.toFloat() / totalLemmas).coerceIn(0f, 1f)
                val blockCoverage = (closedBlocks.toFloat() / totalBlocks).coerceIn(0f, 1f)
                val combinedCoverage = maxOf(wordCoverage, blockCoverage)

                _state.update {
                    it.copy(
                        streakDays = streak.days,
                        freezesLeft = streak.freezesLeft,
                        learned = learnedCards,
                        closedBlocks = closedBlocks,
                        totalBlocks = totalBlocks,
                        coverage = combinedCoverage,
                        collectionCount = collectionItems.size,
                        collectionPreview = collectionItems.take(5).map { item ->
                            item.textRo to (item.translationRu ?: "")
                        },
                        skills = skills,
                        readingTitle = "Чтение A1 · закрыто блоков: $closedBlocks из $totalBlocks",
                    )
                }
            }
        }
    }

    private companion object {
        val DEFAULT_SKILLS = listOf(
            "s1" to "Представиться и рассказать о себе",
            "s2" to "Заказать в кафе и оплатить счёт",
            "s3" to "Спросить дорогу и понять ответ",
            "s4" to "Купить билет на поезд до Клужа",
            "s5" to "Рассказать о семье и доме",
            "s6" to "Объяснить, что случилось в прошлом",
            "s7" to "Поговорить о работе и команде",
            "s8" to "Прочитать короткий рассказ со словарём",
        )
    }
}
