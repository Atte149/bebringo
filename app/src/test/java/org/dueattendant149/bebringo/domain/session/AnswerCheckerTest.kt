package org.dueattendant149.bebringo.domain.session

import org.dueattendant149.bebringo.domain.model.ErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {

    @Test
    fun `точное совпадение засчитывается`() {
        val result = AnswerChecker.check("Bună dimineața!", "bună dimineața")
        assertEquals(AnswerVerdict.CORRECT, result.verdict)
        assertTrue(result.isCorrect)
    }

    @Test
    fun `ответ без диакритики засчитывается с пометкой`() {
        val result = AnswerChecker.check("Bună dimineața!", "buna dimineata")
        assertEquals(AnswerVerdict.CORRECT_WITHOUT_DIACRITICS, result.verdict)
        assertEquals(ErrorType.DIACRITICS, result.errorType)
        assertTrue(result.isCorrect)
    }

    @Test
    fun `строгий режим не прощает диакритику`() {
        val result = AnswerChecker.check("masă", "masa", requireDiacritics = true)
        assertEquals(AnswerVerdict.WRONG, result.verdict)
        assertEquals(ErrorType.DIACRITICS, result.errorType)
    }

    @Test
    fun `перестановка слов распознаётся как порядок`() {
        val result = AnswerChecker.check("Vreau să cumpăr un bilet", "un bilet cumpăr să vreau")
        assertEquals(AnswerVerdict.WRONG, result.verdict)
        assertEquals(ErrorType.WORD_ORDER, result.errorType)
    }

    @Test
    fun `неверная форма слова распознаётся`() {
        val result = AnswerChecker.check("Cățelul latră", "Cățel latră")
        assertEquals(AnswerVerdict.WRONG, result.verdict)
        assertEquals(ErrorType.WRONG_FORM, result.errorType)
    }

    @Test
    fun `другое слово распознаётся как неверное`() {
        val result = AnswerChecker.check("tren", "bilet")
        assertEquals(AnswerVerdict.WRONG, result.verdict)
        assertEquals(ErrorType.WRONG_WORD, result.errorType)
    }

    @Test
    fun `пустой ответ помечается как пропуск`() {
        val result = AnswerChecker.check("tren", "   ")
        assertEquals(AnswerVerdict.WRONG, result.verdict)
        assertEquals(ErrorType.SKIPPED, result.errorType)
    }

    @Test
    fun `пунктуация и регистр не мешают`() {
        val result = AnswerChecker.check("La gară!", "  la gara.  ")
        assertTrue(result.isCorrect)
        assertFalse(result.verdict == AnswerVerdict.WRONG)
    }

    @Test
    fun `альтернативные допустимые ответы засчитываются`() {
        val result = AnswerChecker.checkAny(
            expectedPrimary = "Mă numesc Ana",
            alternatives = listOf("Eu sunt Ana"),
            actual = "Eu sunt Ana!",
        )
        assertEquals(AnswerVerdict.CORRECT, result.verdict)
        assertTrue(result.isCorrect)
    }
}
