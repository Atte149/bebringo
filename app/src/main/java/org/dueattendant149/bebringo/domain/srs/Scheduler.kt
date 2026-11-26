package org.dueattendant149.bebringo.domain.srs

/**
 * Планировщик повторений.
 *
 * Отделён интерфейсом, чтобы позже заменить реализацию на `rs-fsrs-java`
 * без изменения экранов и репозиториев.
 */
interface Scheduler {

    /** Целевая вероятность вспоминания. 0.9 — разумный компромисс между нагрузкой и прочностью. */
    val desiredRetention: Double

    /** Пересчитать карточку после ответа. */
    fun review(card: SrsCard, rating: Rating, now: Long): SrsCard

    /** Вероятность вспомнить карточку к моменту [now]. */
    fun retrievability(card: SrsCard, now: Long): Double

    /** Когда карточка станет доступна снова. */
    fun nextDueAt(card: SrsCard): Long

    /**
     * Порция карточек на сегодня.
     *
     * Просроченное не отдаётся отдельным «долгом»: сортируем по хрупкости
     * и берём столько, сколько влезает в сессию.
     */
    fun dueCards(cards: List<SrsCard>, now: Long, limit: Int): List<SrsCard>
}
