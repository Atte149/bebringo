package org.dueattendant149.bebringo.domain.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ActivityProfileTest {

    private fun ts(date: LocalDate, hour: Int): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, 0), ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val monday: LocalDate = LocalDate.of(2026, 11, 16) // понедельник
    private val nextMonday: LocalDate = monday.plusDays(7)

    @Test
    fun `empty profile has no habits`() {
        val profile = ActivityProfile.empty()
        assertEquals(0.0, profile.pOrganic(weekday = 1, hour = 21), 1e-9)
        assertEquals(Chronotype.UNKNOWN, profile.chronotype())
        assertNull(profile.typicalWindow())
    }

    @Test
    fun `open is counted in the right slot`() {
        var profile = ActivityProfile.empty()
        profile = profile.recordOpen(ts(monday, 21))

        assertEquals(1.0, profile.histogram[(1 - 1) * 24 + 21], 1e-9)
        assertTrue(profile.pOrganic(weekday = 1, hour = 21) > 0.3)
        assertEquals(0.0, profile.pOrganic(weekday = 1, hour = 9), 1e-9)
    }

    @Test
    fun `same day twice counts the day once`() {
        var profile = ActivityProfile.empty()
        profile = profile.recordOpen(ts(monday, 21))
        profile = profile.recordOpen(ts(monday, 22))

        // Второй вход слегка затухает первый (часы между записями) — единица с точностью до затухания.
        assertEquals(1.0, profile.weekdayObserved[0], 0.01)
        assertEquals(1, profile.firstOpenHours.size)
        assertEquals(21, profile.firstOpenHours[monday.toString()])
    }

    @Test
    fun `pOrganic grows with repeated habit`() {
        var profile = ActivityProfile.empty()
        for (week in 0 until 4) {
            profile = profile.recordOpen(ts(monday.plusDays(week * 7L), 21))
        }
        // Четыре входа из четырёх наблюдённых понедельников ≈ 4/5 после сглаживания.
        assertTrue(profile.pOrganic(weekday = 1, hour = 21) > 0.6)
    }

    @Test
    fun `week old observations decay`() {
        var fresh = ActivityProfile.empty().recordOpen(ts(monday, 21))
        fresh = fresh.decayed(ts(monday, 21))

        val old = ActivityProfile.empty().recordOpen(ts(monday, 21))
            .decayed(ts(nextMonday, 21))

        val freshMass = fresh.histogram[(1 - 1) * 24 + 21]
        val oldMass = old.histogram[(1 - 1) * 24 + 21]
        assertTrue(oldMass < freshMass)
        assertEquals(freshMass * ActivityProfile.DECAY_PER_WEEK, oldMass, 1e-6)
    }

    @Test
    fun `chronotype detects night owl`() {
        var profile = ActivityProfile.empty()
        for (day in 0 until 7) {
            profile = profile.recordOpen(ts(monday.plusDays(day.toLong()), 22))
        }
        assertEquals(Chronotype.NIGHT_OWL, profile.chronotype())
    }

    @Test
    fun `chronotype detects early bird`() {
        var profile = ActivityProfile.empty()
        for (day in 0 until 7) {
            profile = profile.recordOpen(ts(monday.plusDays(day.toLong()), 7))
        }
        assertEquals(Chronotype.EARLY_BIRD, profile.chronotype())
    }

    @Test
    fun `chronotype stays unknown with few days`() {
        var profile = ActivityProfile.empty()
        for (day in 0 until 3) {
            profile = profile.recordOpen(ts(monday.plusDays(day.toLong()), 22))
        }
        assertEquals(Chronotype.UNKNOWN, profile.chronotype())
    }

    @Test
    fun `typical window covers the dense evening hours`() {
        var profile = ActivityProfile.empty()
        for (day in 0 until 10) {
            profile = profile.recordOpen(ts(monday.plusDays(day.toLong()), 21))
            profile = profile.recordOpen(ts(monday.plusDays(day.toLong()), 22))
        }
        val window = profile.typicalWindow()
        requireNotNull(window)
        // Окно должно покрывать 21-22 (допускается пересечение полночи).
        val covers = (0 until window.lengthHours).any { offset ->
            (window.startHour + offset) % 24 == 21
        }
        assertTrue(covers)
    }

    @Test
    fun `typical window is null without enough mass`() {
        val profile = ActivityProfile.empty().recordOpen(ts(monday, 21))
        assertNull(profile.typicalWindow())
    }

    @Test
    fun `recordOpen after decay does not resurrect old mass`() {
        var profile = ActivityProfile.empty().recordOpen(ts(monday, 21))
        profile = profile.recordOpen(ts(nextMonday, 21))
        // Обе записи на месте, затухание применено ко всей старой массе.
        assertTrue(profile.histogram[(1 - 1) * 24 + 21] > 1.0)
        assertTrue(profile.histogram[(1 - 1) * 24 + 21] < 2.0)
    }

    @Test
    fun `first open hours map stays bounded`() {
        var profile = ActivityProfile.empty()
        for (day in 0 until ActivityProfile.MAX_FIRST_OPEN_DAYS + 20) {
            profile = profile.recordOpen(ts(monday.plusDays(day.toLong()), 20))
        }
        assertTrue(profile.firstOpenHours.size <= ActivityProfile.MAX_FIRST_OPEN_DAYS)
    }

    @Test
    fun `day closed flag is independent of profile`() {
        // Sanity-связка с моделью событий: weekday/hour консистентны с ts.
        val t = ts(monday, 21)
        assertEquals(1, ActivityProfile.weekdayOf(t))
        assertEquals(21, ActivityProfile.hourOf(t))
        assertEquals(monday, ActivityProfile.dayOf(t))
        assertFalse(ActivityProfile.dayOf(t).isAfter(monday))
    }
}
