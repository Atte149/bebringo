package org.dueattendant149.bebringo.domain.text

/**
 * Приблизительная транслитерация румынского в кириллицу для подсказок.
 *
 * Это не официальная система транслитерации, а именно помощь изучающему:
 * «Bună seara» → «Бунэ сяра». Диакритика и дифтонги передаются так, как
 * русскоязычный ученик скорее всего услышит слово.
 */
object RomanianTranslit {

    /** Диграфы проверяются раньше одиночных букв, поэтому порядок важен. */
    private val digraphs = listOf(
        "che" to "ке",
        "chi" to "ки",
        "ghe" to "ге",
        "ghi" to "ги",
        "ce" to "че",
        "ci" to "чи",
        "ge" to "дже",
        "gi" to "джи",
        "ch" to "к",
        "gh" to "г",
        "ea" to "я",
        "ia" to "я",
        "oa" to "оа",
        "ie" to "ие",
        "iu" to "ю",
    )

    private val letters = mapOf(
        'a' to "а",
        'ă' to "э",
        'â' to "ы",
        'î' to "ы",
        'b' to "б",
        'c' to "к",
        'd' to "д",
        'e' to "е",
        'f' to "ф",
        'g' to "г",
        'h' to "х",
        'i' to "и",
        'j' to "ж",
        'k' to "к",
        'l' to "л",
        'm' to "м",
        'n' to "н",
        'o' to "о",
        'p' to "п",
        'q' to "к",
        'r' to "р",
        's' to "с",
        'ș' to "ш",
        't' to "т",
        'ț' to "ц",
        'u' to "у",
        'v' to "в",
        'w' to "в",
        'x' to "кс",
        'y' to "и",
        'z' to "з",
    )

    fun toCyrillic(text: String): String =
        text.split(' ').joinToString(" ") { token -> transliterateToken(token) }

    private fun transliterateToken(token: String): String {
        if (token.isBlank()) return token

        val leading = token.takeWhile { !it.isLetterOrDigit() }
        val trailing = token.drop(leading.length).takeLastWhile { !it.isLetterOrDigit() }
        val core = token.substring(leading.length, token.length - trailing.length)
        if (core.isEmpty()) return token

        val lower = core.lowercase()
        val out = StringBuilder(lower.length + 4)
        var index = 0
        while (index < lower.length) {
            val rest = lower.substring(index)
            val pair = digraphs.firstOrNull { rest.startsWith(it.first) }
            if (pair != null) {
                out.append(pair.second)
                index += pair.first.length
            } else {
                out.append(letters[lower[index]] ?: lower[index].toString())
                index += 1
            }
        }

        // Финальное -i в румынском обычно не отдельный слог: lei → лей, frați → фраць.
        if (lower.endsWith('i') && lower.length > 2 && !lower.endsWith("ii")) {
            val before = lower[lower.length - 2]
            val replacement = if (before in "aeiouăâî") "й" else "ь"
            out.setLength(out.length - 1)
            out.append(replacement)
        } else if (lower.endsWith("ii") && lower.length > 3) {
            out.setLength(out.length - 1)
            out.append("й")
        }

        if (core.first().isUpperCase() && out.isNotEmpty()) {
            out.setCharAt(0, out.first().uppercaseChar())
        }

        return leading + out + trailing
    }
}
