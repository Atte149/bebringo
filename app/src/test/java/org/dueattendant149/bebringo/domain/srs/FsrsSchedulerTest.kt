package org.dueattendant149.bebringo.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsSchedulerTest {

    private val scheduler = FsrsScheduler()
    private val day = 86_400_000L
    private val now = 1_700_000_000_000L

    private fun newCard() = SrsCard(
        id = "c1",
        lemmaId = "l1",
        direction = CardDirection.RECEPTIVE,
        stability = 0.0,
        difficulty = 0.0,
        dueAt = now,
    )

    @Test
    fun `новая карточка получает стабильность по оценке`() {
        val again = scheduler.review(newCard(), Rating.AGAIN, now)
        val good = scheduler.review(newCard(), Rating.GOOD, now)
        val easy = scheduler.review(newCard(), Rating.EASY, now)

        assertTrue(again.stability < good.stability)
        assertTrue(good.stability < easy.stability)
        assertEquals(1, good.reps)
    }

    @Test
    fun `интервал при целевом удержании девяносто процентов равен стабильности`() {
        val card = scheduler.review(newCard(), Rating.GOOD, now)
        val intervalDays = (card.dueAt - now) / day
        assertEquals(card.stability.toLong(), intervalDays)
    }

    @Test
    fun `оценка again уменьшает стабильность`() {
        val good = scheduler.review(newCard(), Rating.GOOD, now)
        val afterAgain = scheduler.review(good, Rating.AGAIN, now + day)
        assertTrue(afterAgain.stability < good.stability)
        assertEquals(1, afterAgain.lapses)
    }

    @Test
    fun `оценка easy растягивает интервал сильнее good`() {
        val good = scheduler.review(newCard(), Rating.GOOD, now)
        val easy = scheduler.review(newCard(), Rating.EASY, now)
        assertTrue(easy.stability > good.stability)
    }

    @Test
    fun `вероятность вспоминания падает со временем`() {
        val card = scheduler.review(newCard(), Rating.GOOD, now)
        val soon = scheduler.retrievability(card, now + day)
        val later = scheduler.retrievability(card, now + card.stability.toLong() * day)
        assertTrue(soon > later)
        assertEquals(1.0, scheduler.retrievability(card, now), 0.01)
    }

    @Test
    fun `dueCards сначала отдаёт самое хрупкое`() {
        val strong = scheduler.review(newCard(), Rating.EASY, now)
        val weak = scheduler.review(newCard(), Rating.AGAIN, now)

        val due = scheduler.dueCards(
            cards = listOf(strong.copy(id = "strong"), weak.copy(id = "weak")),
            now = now + 30 * day,
            limit = 1,
        )

        assertEquals("weak", due.first().id)
    }

    @Test
    fun `dueCards уважает лимит`() {
        val cards = (1..10).map { index ->
            newCard().copy(id = "c$index", dueAt = now - day)
        }
        assertEquals(3, scheduler.dueCards(cards, now, 3).size)
        assertEquals(0, scheduler.dueCards(cards, now, 0).size)
    }
}
