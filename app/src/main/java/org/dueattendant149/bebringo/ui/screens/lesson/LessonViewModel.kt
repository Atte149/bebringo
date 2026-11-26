package org.dueattendant149.bebringo.ui.screens.lesson

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.domain.entry.EventTypes
import org.dueattendant149.bebringo.domain.model.AnswerEvent
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.ErrorType
import org.dueattendant149.bebringo.domain.model.LessonResult
import org.dueattendant149.bebringo.domain.session.AnswerChecker
import org.dueattendant149.bebringo.domain.srs.CardDirection
import org.dueattendant149.bebringo.domain.srs.Rating
import org.dueattendant149.bebringo.widget.BebringoWidgets
import java.time.LocalDate
import java.util.UUID

/**
 * Побочные эффекты урока: телеметрия, FSRS, промежуточный и финальный прогресс блока,
 * стрик, коллекция фраз и обновление домашнего виджета.
 */
class LessonViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionId: String = UUID.randomUUID().toString()
    private val sessionStartedAt: Long = System.currentTimeMillis()
    private var finishedBlockId: String? = null

    /** Записывает ответ: телеметрия, карточка FSRS и сохранение промежуточного шага. */
    fun recordAnswer(
        block: Block,
        event: AnswerEvent,
        chapterId: String = "",
        nextStepIndex: Int = 0,
        correctSoFar: Int = 0,
        totalSoFar: Int = 0,
        saveProgress: Boolean = true,
    ) {
        viewModelScope.launch {
            runCatching {
                val supportLevel = ServiceLocator.settings.supportLevel.first()
                val lemmaMap = ServiceLocator.courseRepository.lemmaMap()
                val normalizedTarget = AnswerChecker.normalize(event.lemma)
                val resolvedLemmaId = event.lemmaId?.takeIf { it in lemmaMap }
                    ?: lemmaMap.entries.firstOrNull { (_, lemma) ->
                        AnswerChecker.normalize(lemma.lemma) == normalizedTarget
                    }?.key
                    ?: lemmaMap.entries.firstOrNull { (_, lemma) ->
                        normalizedTarget.split(' ').contains(AnswerChecker.normalize(lemma.lemma))
                    }?.key

                if (saveProgress && nextStepIndex in 1 until block.total) {
                    ServiceLocator.progressRepository.save(
                        blockId = block.id,
                        chapterId = chapterId,
                        stepIndex = nextStepIndex,
                        closed = false,
                        correct = correctSoFar,
                        total = totalSoFar,
                    )
                }

                ServiceLocator.telemetryRepository.record(
                    sessionId = sessionId,
                    blockId = block.id,
                    stepIndex = block.steps.indexOfFirst { it.id == event.stepId }.coerceAtLeast(0),
                    exerciseType = event.exerciseType,
                    lemma = resolvedLemmaId ?: event.lemma,
                    result = if (event.correct) "correct" else "wrong",
                    errorType = event.errorType,
                    supportLevel = supportLevel,
                    responseMs = event.responseMs,
                    sessionLenMin = minutesSinceStart(),
                    streakAlive = true,
                )

                ServiceLocator.logEvent(
                    type = EventTypes.LESSON_STEP,
                    sessionId = sessionId,
                    payload = mapOf(
                        "blockId" to block.id,
                        "exerciseType" to event.exerciseType,
                        "result" to if (event.correct) "correct" else "wrong",
                    ),
                )

                // FSRS обновляется только по оцениваемому шагу и только по первой попытке:
                // теория и повторные попытки внутри урока не должны сдвигать расписание.
                if (event.isAssessed && event.isFirstAttempt && resolvedLemmaId != null) {
                    val direction = if (event.isProductive) {
                        CardDirection.PRODUCTIVE
                    } else {
                        CardDirection.RECEPTIVE
                    }
                    val rating = when {
                        !event.correct -> Rating.AGAIN
                        event.errorType == ErrorType.DIACRITICS || event.responseMs > 12_000L -> Rating.HARD
                        event.responseMs < 3_500L -> Rating.EASY
                        else -> Rating.GOOD
                    }
                    ServiceLocator.srsRepository.recordReviewForLemma(
                        lemmaId = resolvedLemmaId,
                        direction = direction,
                        rating = rating,
                        now = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    /** Закрывает блок идемпотентно: прогресс, стрик, коллекция выученных фраз и виджеты. */
    fun finish(block: Block, chapterId: String, result: LessonResult, onDone: () -> Unit) {
        if (finishedBlockId == block.id) {
            onDone()
            return
        }
        finishedBlockId = block.id

        viewModelScope.launch {
            runCatching {
                ServiceLocator.progressRepository.save(
                    blockId = block.id,
                    chapterId = chapterId,
                    stepIndex = 0,
                    closed = true,
                    correct = result.correct,
                    total = result.total,
                )
                ServiceLocator.streakRepository.markBlockClosed(LocalDate.now())
                ServiceLocator.logEvent(
                    type = EventTypes.BLOCK_CLOSED,
                    sessionId = sessionId,
                    payload = mapOf("blockId" to block.id, "source" to "lesson"),
                )

                val lemmaMap = ServiceLocator.courseRepository.lemmaMap()
                val practicedLemmas = block.steps
                    .mapNotNull { step -> step.lemmaId?.let { lemmaMap[it] } }
                    .distinctBy { it.id }
                    .ifEmpty { lemmaMap.values.take(2) }

                practicedLemmas.take(3).forEach { lemma ->
                    ServiceLocator.collectionRepository.add(
                        textRo = lemma.exampleRo ?: lemma.lemma,
                        translationRu = lemma.exampleRu ?: lemma.translationRu,
                    )
                }

                BebringoWidgets.updateAll(getApplication())
            }.also {
                onDone()
            }
        }
    }

    /** Завершает отдельную сессию повторения: стрик и виджет, без записи прогресса курса. */
    fun finishReview(block: Block, onDone: () -> Unit) {
        if (finishedBlockId == block.id) {
            onDone()
            return
        }
        finishedBlockId = block.id

        viewModelScope.launch {
            runCatching {
                ServiceLocator.streakRepository.markBlockClosed(LocalDate.now())
                ServiceLocator.logEvent(
                    type = EventTypes.REVIEW_DONE,
                    sessionId = sessionId,
                )
                BebringoWidgets.updateAll(getApplication())
            }.also {
                onDone()
            }
        }
    }

    private fun minutesSinceStart(): Int =
        (((System.currentTimeMillis() - sessionStartedAt) / 60_000L).toInt() + 1)
}
