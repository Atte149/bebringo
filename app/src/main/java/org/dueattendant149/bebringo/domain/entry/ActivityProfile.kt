package org.dueattendant149.bebringo.domain.entry

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Хронотип по медиане первого входа в день. */
enum class Chronotype {
    /** Мало данных для вывода. */
    UNKNOWN,

    /** Медиана первого входа до 10:00. */
    EARLY_BIRD,

    /** Между. */
    MIXED,

    /** Медиана первого входа с 18:00 и позже. */
    NIGHT_OWL,
}

/** Типичное окно активности: начало и длина в часах (может пересекать полночь). */
data class TypicalWindow(val startHour: Int, val lengthHours: Int)

/**
 * Профиль активности пользователя: когда он обычно заходит сам.
 *
 * Это ядро правила «молчать, если придёт сам»: [pOrganic] оценивает вероятность
 * органического входа в слот, а [chronotype] и [typicalWindow] описывают привычку
 * целиком. Все данные — затухающие: вес наблюдений старше недели падает на 10%
 * за неделю, поэтому ушедшая в прошлое привычка не держит модель заложником.
 *
 * Класс чистый и неизменяемый: приём timestamp'ов вместо `Clock` держит
 * детерминизм в тестах.
 */
data class ActivityProfile(
    /** Счётчики входов по «день недели × час» (7×24 = 168), с затуханием. */
    val histogram: List<Double> = List(HISTOGRAM_SIZE) { 0.0 },
    /** Сколько разных дней каждого дня недели мы наблюдали (7), с затуханием. */
    val weekdayObserved: List<Double> = List(DAYS_PER_WEEK) { 0.0 },
    /** Час первого входа по календарным дням (ISO-дата → час), для хронотипа. */
    val firstOpenHours: Map<String, Int> = emptyMap(),
    /** Момент последнего применения затухания. */
    val lastDecayAt: Long = 0L,
) {

    /** Приводит счётчики к моменту [nowMs], применяя недельное затухание ×0.9. */
    fun decayed(nowMs: Long): ActivityProfile {
        if (lastDecayAt == 0L || nowMs <= lastDecayAt) return copy(lastDecayAt = nowMs.coerceAtLeast(lastDecayAt))
        val weeks = (nowMs - lastDecayAt) / WEEK_MS.toDouble()
        val factor = Math.pow(DECAY_PER_WEEK, weeks)
        return copy(
            histogram = histogram.map { it * factor },
            weekdayObserved = weekdayObserved.map { it * factor },
            lastDecayAt = nowMs,
        )
    }

    /** Регистрирует органический вход; сначала применяется затухание. */
    fun recordOpen(timestampMs: Long): ActivityProfile {
        val decayed = decayed(timestampMs)
        val dateTime = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
        val weekdayIndex = dateTime.dayOfWeek.value - 1
        val hour = dateTime.hour
        val dayKey = dateTime.toLocalDate().toString()

        val histogram = decayed.histogram.toMutableList()
        histogram[weekdayIndex * HOURS_PER_DAY + hour] += 1.0

        val weekdayObserved = decayed.weekdayObserved.toMutableList()
        var firstOpenHours = decayed.firstOpenHours
        if (dayKey !in firstOpenHours) {
            // Новый наблюдаемый день: фиксируем первый вход и засчитываем день недели.
            weekdayObserved[weekdayIndex] += 1.0
            firstOpenHours = (firstOpenHours + (dayKey to hour)).trimToRecentDays()
        }

        return decayed.copy(
            histogram = histogram,
            weekdayObserved = weekdayObserved,
            firstOpenHours = firstOpenHours,
        )
    }

    /**
     * Оценка вероятности органического входа в слот [weekday] (1..7) × [hour] (0..23).
     *
     * Доля наблюдавшихся входов в этот слот (±[toleranceHours] часов) от числа
     * наблюдавшихся таких дней недели, со сглаживанием вниз: при нулевых
     * наблюдениях даёт 0.0, при одном входе за один день — около 0.5.
     */
    fun pOrganic(weekday: Int, hour: Int, toleranceHours: Int = 1): Double {
        val weekdayIndex = (weekday - 1).coerceIn(0, DAYS_PER_WEEK - 1)
        var numerator = 0.0
        for (offset in -toleranceHours..toleranceHours) {
            val h = Math.floorMod(hour + offset, HOURS_PER_DAY)
            numerator += histogram[weekdayIndex * HOURS_PER_DAY + h]
        }
        val observed = weekdayObserved[weekdayIndex]
        return (numerator / (observed + 1.0)).coerceIn(0.0, 1.0)
    }

    /** Хронотип по медиане часов первого входа; нужно минимум [MIN_DAYS_FOR_CHRONOTYPE] дней. */
    fun chronotype(): Chronotype {
        val hours = firstOpenHours.values.sorted()
        if (hours.size < MIN_DAYS_FOR_CHRONOTYPE) return Chronotype.UNKNOWN
        val median = hours[hours.size / 2]
        return when {
            median <= EARLY_BIRD_MAX_HOUR -> Chronotype.EARLY_BIRD
            median >= NIGHT_OWL_MIN_HOUR -> Chronotype.NIGHT_OWL
            else -> Chronotype.MIXED
        }
    }

    /**
     * Самое плотное непрерывное окно (по часам суток), покрывающее 80% входов.
     * null — когда данных меньше [MIN_MASS_FOR_WINDOW] входов.
     */
    fun typicalWindow(): TypicalWindow? {
        val byHour = DoubleArray(HOURS_PER_DAY)
        for (weekdayIndex in 0 until DAYS_PER_WEEK) {
            for (hour in 0 until HOURS_PER_DAY) {
                byHour[hour] += histogram[weekdayIndex * HOURS_PER_DAY + hour]
            }
        }
        val total = byHour.sum()
        if (total < MIN_MASS_FOR_WINDOW) return null

        val target = total * WINDOW_MASS_FRACTION
        // Скользящее окно по «удвоенным» суткам — чтобы окна через полночь находились.
        var bestStart = 0
        var bestLength = HOURS_PER_DAY
        for (start in 0 until HOURS_PER_DAY) {
            var mass = 0.0
            for (length in 1..HOURS_PER_DAY) {
                mass += byHour[(start + length - 1) % HOURS_PER_DAY]
                if (mass >= target && length < bestLength) {
                    bestStart = start
                    bestLength = length
                    break
                }
            }
        }
        return TypicalWindow(startHour = bestStart, lengthHours = bestLength)
    }

    private fun Map<String, Int>.trimToRecentDays(): Map<String, Int> =
        entries.sortedBy { it.key }.takeLast(MAX_FIRST_OPEN_DAYS).associate { it.key to it.value }

    companion object {
        const val HOURS_PER_DAY = 24
        const val DAYS_PER_WEEK = 7
        const val HISTOGRAM_SIZE = HOURS_PER_DAY * DAYS_PER_WEEK
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        const val DECAY_PER_WEEK = 0.9
        const val MIN_DAYS_FOR_CHRONOTYPE = 7
        const val EARLY_BIRD_MAX_HOUR = 9
        const val NIGHT_OWL_MIN_HOUR = 18
        const val MIN_MASS_FOR_WINDOW = 4.0
        const val WINDOW_MASS_FRACTION = 0.8
        const val MAX_FIRST_OPEN_DAYS = 56

        /** Пустой профиль; точка отсчёта затухания — первый реальный вход. */
        fun empty() = ActivityProfile()

        fun weekdayOf(timestampMs: Long): Int =
            Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).dayOfWeek.value

        fun hourOf(timestampMs: Long): Int =
            Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).hour

        fun dayOf(timestampMs: Long): LocalDate =
            Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
