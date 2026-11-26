package org.dueattendant149.bebringo.domain.streak

import java.time.LocalDate
import java.time.YearMonth

/** Один день занятий. */
data class StreakDay(
    val day: LocalDate,
    val closedBlocks: Int,
    val usedFreeze: Boolean = false,
    val repaired: Boolean = false,
)

data class StreakResult(
    val days: Int,
    val freezesLeft: Int,
    val lastClosedDay: LocalDate?,
    val blocksToday: Int = 0,
    val canRepairYesterday: Boolean = false,
)

/**
 * Подсчёт стрика.
 *
 * Правила:
 * - день засчитывается, если закрыт хотя бы один блок;
 * - пропуск можно закрыть заморозкой, не больше двух в календарный месяц;
 * - заморозки списываются только тогда, когда они действительно соединяют стрик с предыдущим закрытым днём;
 * - ремонт за вчера (`repaired = true`) соединяет разрыв без списания заморозки и сразу снимает плашку ремонта.
 */
object StreakCalculator {

    fun current(
        days: List<StreakDay>,
        today: LocalDate,
        freezesPerMonth: Int = 2,
    ): StreakResult {
        if (days.isEmpty()) return StreakResult(0, freezesPerMonth, null, 0, false)

        val byDay = days.associateBy { it.day }
        val earliest = days.minOf { it.day }

        val repairedDays = days.filter { it.repaired }.map { it.day }.toSet()
        val committedByMonth = mutableMapOf<YearMonth, Int>()
        days.filter { it.usedFreeze }.forEach { day ->
            committedByMonth.merge(YearMonth.from(day.day), 1, Int::plus)
        }

        val lastClosed = days.filter { it.closedBlocks > 0 }.maxOfOrNull { it.day }
        val blocksToday = byDay[today]?.closedBlocks ?: 0

        var streak = 0
        var cursor = if (blocksToday > 0) today else today.minusDays(1)
        val pendingFreezes = mutableListOf<LocalDate>()

        var guard = 0
        while (cursor >= earliest && guard++ < 3650) {
            val entry = byDay[cursor]
            if (entry != null && entry.closedBlocks > 0) {
                // Подтверждаем отложенные заморозки, так как за разрывом найден закрытый день
                pendingFreezes.forEach { fDay ->
                    if (byDay[fDay]?.usedFreeze != true) {
                        committedByMonth.merge(YearMonth.from(fDay), 1, Int::plus)
                    }
                }
                pendingFreezes.clear()
                streak++
                cursor = cursor.minusDays(1)
                continue
            }

            if (cursor in repairedDays) {
                cursor = cursor.minusDays(1)
                continue
            }

            val month = YearMonth.from(cursor)
            val alreadyCommitted = committedByMonth[month] ?: 0
            val pendingInMonth = pendingFreezes.count { YearMonth.from(it) == month && byDay[it]?.usedFreeze != true }
            if (alreadyCommitted + pendingInMonth < freezesPerMonth) {
                pendingFreezes.add(cursor)
                cursor = cursor.minusDays(1)
            } else {
                // Разрыв слишком длинный — отложенные заморозки не сгорают впустую
                pendingFreezes.clear()
                break
            }
        }

        val currentMonth = YearMonth.from(today)
        val spentThisMonth = committedByMonth[currentMonth] ?: 0
        val freezesLeft = (freezesPerMonth - spentThisMonth).coerceAtLeast(0)

        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val yesterdayEntry = byDay[yesterday]
        val yesterdayMissed = (yesterdayEntry?.closedBlocks ?: 0) == 0 &&
            yesterdayEntry?.repaired != true &&
            yesterdayEntry?.usedFreeze != true
        val hadStreakTwoDaysAgo = (byDay[twoDaysAgo]?.closedBlocks ?: 0) > 0
        // Плашка ремонта показывается, если вчера был пропуск после активного дня и пользователь хочет восстановить его вручную
        val canRepair = yesterdayMissed && hadStreakTwoDaysAgo

        return StreakResult(
            days = streak,
            freezesLeft = freezesLeft,
            lastClosedDay = lastClosed,
            blocksToday = blocksToday,
            canRepairYesterday = canRepair,
        )
    }
}
