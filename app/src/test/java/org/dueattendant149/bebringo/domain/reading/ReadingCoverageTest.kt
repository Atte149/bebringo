package org.dueattendant149.bebringo.domain.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCoverageTest {

    @Test
    fun `нормализация убирает диакритику и пунктуацию`() {
        assertEquals("casa", ReadingCoverage.normalize("Casă!"))
        assertEquals("sunt", ReadingCoverage.normalize("Sunt,"))
    }

    @Test
    fun `формы одного слова считаются знакомыми`() {
        val known = setOf("casa", "mare", "tren")
        assertTrue(ReadingCoverage.isKnown("casele", known))
        assertTrue(ReadingCoverage.isKnown("trenul", known))
        assertTrue(ReadingCoverage.isKnown("mari", known))
        assertFalse(ReadingCoverage.isKnown("furtună", known))
    }

    @Test
    fun `покрытие считается по фактическим словам`() {
        val coverage = ReadingCoverage.coverage(
            text = "Casa este mare.",
            knownWords = setOf("casa", "mare"),
        )
        assertEquals(2f / 3f, coverage, 0.001f)
    }
}
