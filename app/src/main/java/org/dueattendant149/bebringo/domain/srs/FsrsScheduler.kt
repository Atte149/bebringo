package org.dueattendant149.bebringo.domain.srs

import kotlin.math.exp
import kotlin.math.pow

/**
 * Реализация FSRS-4.5 на чистых формулах.
 *
 * Почему не HLR как у Duolingo: в открытом бенчмарке (10 тысяч коллекций Anki)
 * HLR даёт log loss 0.469, что хуже константного базлайна 0.395. FSRS-6 — 0.346.
 *
 * Почему не готовая библиотека сразу: `rs-fsrs-java` тянет нативный Rust через JNI.
 * Интерфейс [Scheduler] отделён, поэтому замена на неё не затронет ни экраны, ни базу.
 *
 * Формулы соответствуют FSRS-4.5; веса — дефолтные, оптимизация под пользователя
 * появится, когда наберётся достаточно истории.
 */
class FsrsScheduler(
    override val desiredRetention: Double = 0.9,
    private val weights: DoubleArray = DEFAULT_WEIGHTS,
) : Scheduler {

    init {
        require(desiredRetention in 0.70..0.97) {
            "Целевое удержание вне разумного диапазона: $desiredRetention"
        }
    }

    override fun retrievability(card: SrsCard, now: Long): Double {
        if (card.isNew) return 0.0
        val elapsedDays = ((now - (card.lastReviewAt ?: now)).toDouble() / DAY_MS).coerceAtLeast(0.0)
        val stability = card.stability.coerceAtLeast(MIN_STABILITY)
        return forgettingCurve(elapsedDays, stability)
    }

    override fun review(card: SrsCard, rating: Rating, now: Long): SrsCard {
        val grade = rating.ordinal + 1

        if (card.isNew) {
            val stability = initStability(grade)
            val difficulty = initDifficulty(grade)
            return card.copy(
                stability = stability,
                difficulty = difficulty,
                lastReviewAt = now,
                reps = card.reps + 1,
                lapses = if (rating == Rating.AGAIN) card.lapses + 1 else card.lapses,
                dueAt = now + intervalDays(stability) * DAY_MS,
            )
        }

        val elapsedDays = ((now - (card.lastReviewAt ?: now)).toDouble() / DAY_MS)
            .coerceAtLeast(0.0)
        val retrievabilityNow = forgettingCurve(elapsedDays, card.stability)
        val difficulty = nextDifficulty(card.difficulty, grade)

        val stability = if (rating == Rating.AGAIN) {
            forgetStability(card.difficulty, card.stability, retrievabilityNow)
        } else {
            recallStability(
                difficulty = difficulty,
                stability = card.stability,
                retrievability = retrievabilityNow,
                grade = grade,
            )
        }

        return card.copy(
            stability = stability,
            difficulty = difficulty,
            lastReviewAt = now,
            reps = card.reps + 1,
            lapses = if (rating == Rating.AGAIN) card.lapses + 1 else card.lapses,
            dueAt = now + intervalDays(stability) * DAY_MS,
        )
    }

    override fun nextDueAt(card: SrsCard): Long = card.dueAt

    override fun dueCards(cards: List<SrsCard>, now: Long, limit: Int): List<SrsCard> {
        if (limit <= 0) return emptyList()
        return cards
            .filter { it.dueAt <= now || it.isNew }
            .sortedWith(
                compareBy<SrsCard> { card ->
                    // Сначала самое хрупкое: низкая вероятность вспоминания
                    if (card.isNew) 1.0 else retrievability(card, now)
                }.thenBy { it.stability },
            )
            .take(limit)
    }

    // --- Формулы ---

    private fun forgettingCurve(elapsedDays: Double, stability: Double): Double =
        (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)

    private fun initStability(grade: Int): Double =
        (weights[grade - 1]).coerceAtLeast(MIN_STABILITY)

    private fun initDifficulty(grade: Int): Double =
        (weights[4] - (grade - 3) * weights[5]).coerceIn(1.0, 10.0)

    private fun nextDifficulty(difficulty: Double, grade: Int): Double {
        val delta = difficulty - weights[6] * (grade - 3)
        val reverted = weights[7] * initDifficulty(EASY) + (1 - weights[7]) * delta
        return reverted.coerceIn(1.0, 10.0)
    }

    private fun recallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        grade: Int,
    ): Double {
        val hardPenalty = if (grade == HARD) weights[15] else 1.0
        val easyBonus = if (grade == EASY) weights[16] else 1.0
        val value = stability * (
            1.0 +
                exp(weights[8]) *
                (11.0 - difficulty) *
                stability.pow(-weights[9]) *
                (exp(weights[10] * (1.0 - retrievability)) - 1.0) *
                hardPenalty *
                easyBonus
            )
        return value.coerceIn(MIN_STABILITY, MAX_STABILITY)
    }

    private fun forgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
    ): Double {
        val value = weights[11] *
            difficulty.pow(-weights[12]) *
            ((stability + 1.0).pow(weights[13]) - 1.0) *
            exp(weights[14] * (1.0 - retrievability))
        return value.coerceIn(MIN_STABILITY, MAX_STABILITY)
    }

    /** Интервал в днях, при котором вероятность вспоминания равна [desiredRetention]. */
    private fun intervalDays(stability: Double): Long {
        val raw = stability / FACTOR * (desiredRetention.pow(1.0 / DECAY) - 1.0)
        return raw.coerceIn(1.0, MAX_INTERVAL_DAYS.toDouble()).toLong()
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val DECAY = -0.5
        const val FACTOR = 19.0 / 81.0
        const val MIN_STABILITY = 0.1
        const val MAX_STABILITY = 36_500.0
        const val MAX_INTERVAL_DAYS = 36_500
        const val EASY = 4
        const val HARD = 2

        /** Дефолтные веса FSRS-4.5: стабильность, сложность, затухание, штрафы. */
        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031,
            1.6474, 0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755,
        )
    }
}
