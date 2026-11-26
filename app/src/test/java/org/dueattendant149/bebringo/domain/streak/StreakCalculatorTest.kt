package org.dueattendant149.bebringo.domain.streak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 11, 8)

    private fun day(offset: Int, blocks: Int = 1, freeze: Boolean = false, repaired: Boolean = false) = StreakDay(
        day = today.minusDays(offset.toLong()),
        closedBlocks = blocks,
        usedFreeze = freeze,
        repaired = repaired,
    )

    @Test
    fun `три дня подряд дают стрик три и не тратят заморозки после начала истории`() {
        val result = StreakCalculator.current(
            days = listOf(day(0), day(1), day(2)),
            today = today,
        )
        assertEquals(3, result.days)
        assertEquals(2, result.freezesLeft)
    }

    @Test
    fun `сегодня без занятий стрик не обнуляется если вчера был`() {
        val result = StreakCalculator.current(
            days = listOf(day(1), day(2), day(3)),
            today = today,
        )
        assertEquals(3, result.days)
    }

    @Test
    fun `один пропуск закрывается заморозкой и предлагает ремонт вчерашнего дня`() {
        val result = StreakCalculator.current(
            days = listOf(day(0), day(2), day(3)),
            today = today,
        )
        assertEquals(3, result.days)
        assertEquals(1, result.freezesLeft)
        assertTrue(result.canRepairYesterday)
    }

    @Test
    fun `два пропуска подряд закрываются двумя заморозками`() {
        val result = StreakCalculator.current(
            days = listOf(day(0), day(3), day(4)),
            today = today,
        )
        assertEquals(3, result.days)
        assertEquals(0, result.freezesLeft)
    }

    @Test
    fun `три пропуска подряд рвут стрик`() {
        val result = StreakCalculator.current(
            days = listOf(day(0), day(4), day(5)),
            today = today,
        )
        assertEquals(1, result.days)
    }

    @Test
    fun `ремонт закрывает разрыв и не тратит заморозку`() {
        val result = StreakCalculator.current(
            days = listOf(day(0), day(1, blocks = 0, repaired = true), day(2), day(3)),
            today = today,
        )
        assertEquals(3, result.days)
        assertEquals(2, result.freezesLeft)
        assertFalse(result.canRepairYesterday)
    }

    @Test
    fun `пустая история даёт ноль`() {
        val result = StreakCalculator.current(emptyList(), today)
        assertEquals(0, result.days)
        assertEquals(null, result.lastClosedDay)
    }

    @Test
    fun `последний закрытый день возвращается`() {
        val result = StreakCalculator.current(
            days = listOf(day(0), day(1)),
            today = today,
        )
        assertEquals(today, result.lastClosedDay)
    }
}
