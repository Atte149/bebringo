package org.dueattendant149.bebringo.domain.session

import org.dueattendant149.bebringo.domain.model.ErrorType

/** Итог проверки ответа. */
enum class AnswerVerdict {
    CORRECT,
    CORRECT_WITHOUT_DIACRITICS,
    WRONG,
}

data class AnswerResult(
    val verdict: AnswerVerdict,
    val errorType: ErrorType?,
    val expected: String,
) {
    val isCorrect: Boolean get() = verdict != AnswerVerdict.WRONG
}

/**
 * Сравнение ответа пользователя с эталоном (и списком допустимых синонимов/вариантов).
 *
 * Правила:
 * - регистр, лишние пробелы и пунктуация не важны;
 * - отсутствие диакритики засчитывается, но помечается: `masă` и `masa` — разные слова,
 *   и человек должен это видеть, при этом не терять прогресс из-за клавиатуры;
 * - если слова те же, но порядок другой — это `word_order`;
 * - если слова похожи, но форма неверная — `wrong_form`;
 * - иначе `wrong_word`.
 */
object AnswerChecker {

    fun checkAny(
        expectedPrimary: String,
        alternatives: List<String>,
        actual: String,
        requireDiacritics: Boolean = false,
    ): AnswerResult {
        val candidates = (listOf(expectedPrimary) + alternatives).filter { it.isNotBlank() }.distinct()
        val results = candidates.map { candidate -> check(candidate, actual, requireDiacritics) }
        results.firstOrNull { it.verdict == AnswerVerdict.CORRECT }?.let { return it }
        results.firstOrNull { it.verdict == AnswerVerdict.CORRECT_WITHOUT_DIACRITICS }?.let { return it }
        return results.firstOrNull() ?: check(expectedPrimary, actual, requireDiacritics)
    }

    fun check(
        expected: String,
        actual: String,
        requireDiacritics: Boolean = false,
    ): AnswerResult {
        val normalizedExpected = normalize(expected)
        val normalizedActual = normalize(actual)

        if (normalizedExpected == normalizedActual) {
            return AnswerResult(AnswerVerdict.CORRECT, null, expected)
        }

        val expectedBare = stripDiacritics(normalizedExpected)
        val actualBare = stripDiacritics(normalizedActual)

        if (expectedBare == actualBare) {
            return if (requireDiacritics) {
                AnswerResult(AnswerVerdict.WRONG, ErrorType.DIACRITICS, expected)
            } else {
                AnswerResult(AnswerVerdict.CORRECT_WITHOUT_DIACRITICS, ErrorType.DIACRITICS, expected)
            }
        }

        val expectedTokens = expectedBare.split(' ').filter { it.isNotEmpty() }
        val actualTokens = actualBare.split(' ').filter { it.isNotEmpty() }

        if (expectedTokens.sorted() == actualTokens.sorted()) {
            return AnswerResult(AnswerVerdict.WRONG, ErrorType.WORD_ORDER, expected)
        }

        if (expectedTokens.size == actualTokens.size && looksLikeInflection(expectedTokens, actualTokens)) {
            return AnswerResult(AnswerVerdict.WRONG, ErrorType.WRONG_FORM, expected)
        }

        return AnswerResult(
            verdict = AnswerVerdict.WRONG,
            errorType = if (actual.isBlank()) ErrorType.SKIPPED else ErrorType.WRONG_WORD,
            expected = expected,
        )
    }

    /** Нормализация: нижний регистр, единичные пробелы, без пунктуации по краям. */
    fun normalize(value: String): String =
        value
            .lowercase()
            .replace(Regex("[.,!?;:»«\"'()\\[\\]—–]"), "")
            .replace(Regex("[\\u00A0\\s]+"), " ")
            .trim()

    /** Убирает диакритику: ă→a, â→a, î→i, ș→s, ț→t. */
    fun stripDiacritics(value: String): String {
        val builder = StringBuilder(value.length)
        value.forEach { char ->
            builder.append(
                when (char) {
                    'ă', 'â', 'á', 'à' -> 'a'
                    'î', 'í', 'ì' -> 'i'
                    'ș', 'ş' -> 's'
                    'ț', 'ţ' -> 't'
                    'é', 'è' -> 'e'
                    'ó', 'ò' -> 'o'
                    'ú', 'ù' -> 'u'
                    else -> char
                },
            )
        }
        return builder.toString()
    }

    /**
     * Похоже ли, что различие в окончании, а не в самом слове.
     * Сравниваем общий префикс токенов: `cățel` и `cățeii` — форма, `cățel` и `tren` — слово.
     */
    private fun looksLikeInflection(expected: List<String>, actual: List<String>): Boolean {
        var similar = 0
        for (index in expected.indices) {
            if (commonPrefixRatio(expected[index], actual[index]) >= 0.6) similar++
        }
        return similar == expected.size
    }

    private fun commonPrefixRatio(first: String, second: String): Double {
        if (first.isEmpty() || second.isEmpty()) return 0.0
        var common = 0
        val limit = minOf(first.length, second.length)
        while (common < limit && first[common] == second[common]) common++
        return common.toDouble() / maxOf(first.length, second.length)
    }
}
