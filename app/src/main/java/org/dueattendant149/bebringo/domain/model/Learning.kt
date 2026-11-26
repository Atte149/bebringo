package org.dueattendant149.bebringo.domain.model

/** Уровень русской подпорки при чтении. 0 — чистый румынский, 3 — полный перевод. */
enum class SupportLevel(val value: Int) {
    NONE(0),
    WOVEN(1),
    GLOSS(2),
    PARALLEL(3);

    companion object {
        fun of(value: Int): SupportLevel = entries.firstOrNull { it.value == value } ?: WOVEN
    }
}

/** Абзац текста: румынский оригинал и, при наличии, русский перевод. */
data class ReadingParagraph(
    val textRo: String,
    val translationRu: String? = null,
    /** Русские вставки: ключ — слово оригинала, значение — чем его заменить. */
    val wovenRu: Map<String, String> = emptyMap(),
)

/** Текст для чтения. Размер считается по фактическому тексту, а не хранится вручную. */
data class Reading(
    val id: String,
    val titleRo: String,
    val author: String,
    val paragraphs: List<ReadingParagraph>,
    val source: ReadingSource,
) {
    val wordCount: Int
        get() = paragraphs.sumOf { paragraph ->
            paragraph.textRo.split(Regex("\\s+")).count { it.isNotBlank() }
        }
}

enum class ReadingSource { PUBLIC_DOMAIN, HELION, PURCHASED, ORIGINAL }

/** Умение: одна строка в лестнице «я умею». */
data class Skill(
    val id: String,
    val titleRu: String,
    val achieved: Boolean,
)

/** Сценарий разговора с носителем. */
data class ChatScenario(
    val id: String,
    val titleRo: String,
    val titleRu: String,
    val requiredLemmas: List<String>,
    val blocksTotal: Int,
    val blocksPassed: Int,
)

/** Реплика в чате. */
data class ChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val timeLabel: String,
)

/** Ошибка в разборе блока. */
data class MistakeNote(
    val prompt: String,
    val correct: String,
    val explanationRu: String,
    val errorType: ErrorType,
)

enum class ErrorType(val csv: String) {
    WRONG_FORM("wrong_form"),
    WRONG_WORD("wrong_word"),
    DIACRITICS("diacritics"),
    WORD_ORDER("word_order"),
    SKIPPED("skipped"),
}
