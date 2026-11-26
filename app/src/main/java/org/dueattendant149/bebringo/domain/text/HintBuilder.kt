package org.dueattendant149.bebringo.domain.text

import org.dueattendant149.bebringo.domain.model.TypeInStep

/**
 * Пошаговые подсказки для ввода слова.
 *
 * Важно: первая ступень не должна содержать ответ целиком. Старые YAML-подсказки
 * часто были готовым ответом, поэтому такие подсказки отбрасываются и заменяются
 * структурной подсказкой.
 */
object HintBuilder {

    fun tiers(step: TypeInStep): List<String> {
        val answer = step.answerRo.trim()
        val words = answer.split(Regex("\\s+")).filter { it.isNotBlank() }
        val firstLetter = answer.firstOrNull { it.isLetter() }
            ?.uppercaseChar()
            ?.toString()
            .orEmpty()

        val structure = buildString {
            append("Слов: ${words.size}.")
            if (firstLetter.isNotEmpty()) {
                append(" Первая буква: «$firstLetter».")
            }
        }

        val safeAuthoredHint = step.hint
            ?.trim()
            ?.takeIf { it.isNotBlank() && !containsAnswer(it, answer) }

        val firstTier = listOfNotNull(safeAuthoredHint, structure).joinToString(" ")
        val transliteration = RomanianTranslit.toCyrillic(answer)

        return listOf(
            firstTier,
            "Произношение: $transliteration",
            "Полный ответ: $answer",
        )
    }

    /** Грубая проверка, не выдаёт ли подсказка ответ (с точностью до регистра и пунктуации). */
    fun containsAnswer(hint: String, answer: String): Boolean {
        val cleanHint = clean(hint)
        val cleanAnswer = clean(answer)
        if (cleanHint.isEmpty() || cleanAnswer.isEmpty()) return false
        return cleanHint.contains(cleanAnswer) || cleanAnswer.contains(cleanHint)
    }

    private fun clean(value: String): String =
        value
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "")
}
