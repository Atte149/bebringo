package org.dueattendant149.bebringo.domain.reading

import org.dueattendant149.bebringo.domain.session.AnswerChecker

/**
 * Честная оценка знакомости текста.
 *
 * Точной лемматизации румынского здесь нет, но для A1 хватает нормализации
 * и сравнения по общему корню: «casă» узнаёт «case», «tren» — «trenul».
 */
object ReadingCoverage {

    private val tokenRegex = Regex("[\\p{L}\\p{N}]+")

    fun tokens(text: String): List<String> =
        tokenRegex.findAll(text).map { it.value }.toList()

    fun coverage(text: String, knownWords: Set<String>): Float {
        val words = tokens(text).filter { it.length > 1 }
        if (words.isEmpty()) return 0f
        val known = words.count { isKnown(it, knownWords) }
        return known.toFloat() / words.size
    }

    fun isKnown(rawWord: String, knownWords: Set<String>): Boolean {
        if (knownWords.isEmpty()) return false
        val word = normalize(rawWord)
        if (word.length <= 1) return true
        if (word in knownWords) return true

        val wordStem = stem(word)
        if (wordStem.length < 3) return false
        return knownWords.any { known -> stem(known) == wordStem }
    }

    /** Нормализация для сравнения: нижний регистр, без диакритики и пунктуации. */
    fun normalize(rawWord: String): String =
        AnswerChecker.stripDiacritics(AnswerChecker.normalize(rawWord))
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

    /**
     * Очень простой стеммер для A1: срезает частые румынские окончания,
     * чтобы «casă», «case», «casele» сходились к одному корню.
     */
    private fun stem(word: String): String {
        val suffixes = listOf(
            "urilor", "urile", "ului", "ilor", "lor", "uri", "ele", "ile",
            "ul", "ua", "le", "ii", "ea", "a", "e", "i", "u",
        )
        for (suffix in suffixes) {
            if (word.endsWith(suffix) && word.length - suffix.length >= 3) {
                return word.dropLast(suffix.length)
            }
        }
        return word
    }
}
