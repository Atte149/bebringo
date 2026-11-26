package org.dueattendant149.bebringo.domain.text

import org.dueattendant149.bebringo.domain.model.TypeInStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HintBuilderTest {

    @Test
    fun `транслитерация передаёт основные румынские звуки`() {
        assertEquals("Бунэ сяра!", RomanianTranslit.toCyrillic("Bună seara!"))
        assertEquals("Мэ нумеск Анна.", RomanianTranslit.toCyrillic("Mă numesc Anna."))
        assertEquals("Шединца есте ла ора зече.", RomanianTranslit.toCyrillic("Ședința este la ora zece."))
        assertEquals("Ун чеай, те рог.", RomanianTranslit.toCyrillic("Un ceai, te rog."))
        assertEquals("фраць", RomanianTranslit.toCyrillic("frați"))
        assertEquals("лей", RomanianTranslit.toCyrillic("lei"))
    }

    @Test
    fun `первая подсказка не содержит ответ целиком`() {
        val step = TypeInStep(
            id = "s1",
            promptRu = "Добрый вечер!",
            answerRo = "Bună seara!",
            hint = "Bună seara",
        )

        val tiers = HintBuilder.tiers(step)

        assertEquals(3, tiers.size)
        assertFalse(HintBuilder.containsAnswer(tiers.first(), step.answerRo))
        assertTrue(tiers.first().contains("Слов: 2"))
        assertTrue(tiers[1].contains("Бунэ сяра"))
        assertTrue(tiers[2].contains("Bună seara!"))
    }

    @Test
    fun `безопасная авторская подсказка сохраняется`() {
        val step = TypeInStep(
            id = "s2",
            promptRu = "У меня есть сестра.",
            answerRo = "Am o soră.",
            hint = "Глагол «иметь» + неопределённый артикль + существительное.",
        )

        val first = HintBuilder.tiers(step).first()

        assertTrue(first.startsWith("Глагол «иметь»"))
        assertTrue(first.contains("Слов: 3"))
    }
}
