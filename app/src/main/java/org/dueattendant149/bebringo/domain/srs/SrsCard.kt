package org.dueattendant149.bebringo.domain.srs

/** Оценка ответа пользователя в терминах FSRS. */
enum class Rating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

/** Направление карточки: узнавание и производство — разные навыки. */
enum class CardDirection {
    RECEPTIVE,
    PRODUCTIVE,
}

/** Состояние карточки в планировщике. */
data class SrsCard(
    val id: String,
    val lemmaId: String,
    val direction: CardDirection,
    val stability: Double,
    val difficulty: Double,
    val dueAt: Long,
    val lastReviewAt: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val isNew: Boolean get() = reps == 0
}
