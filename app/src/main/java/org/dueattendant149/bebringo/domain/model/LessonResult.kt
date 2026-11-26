package org.dueattendant149.bebringo.domain.model

/** Итог урока: сколько верно и список ошибок/заметок для разбора. */
data class LessonResult(
    val correct: Int,
    val total: Int,
    val mistakes: List<MistakeNote>,
)

/** Событие ответа: пишется в телеметрию и обновляет карточку FSRS. */
data class AnswerEvent(
    val stepId: String,
    val exerciseType: String,
    val lemma: String,
    val correct: Boolean,
    val errorType: ErrorType?,
    val responseMs: Long,
    val lemmaId: String? = null,
    val isProductive: Boolean = false,
    /** Оцениваемый шаг: теорию и чтение нельзя записывать в FSRS как повторение. */
    val isAssessed: Boolean = true,
    /** Повторная попытка внутри того же урока не должна перезаписывать расписание FSRS. */
    val isFirstAttempt: Boolean = true,
)
