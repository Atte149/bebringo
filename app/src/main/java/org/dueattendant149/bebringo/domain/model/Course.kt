package org.dueattendant149.bebringo.domain.model

/** Тип обучающего трека в модульном движке Bebringo. */
enum class TrackKind(val labelRu: String) {
    LANGUAGE("Язык"),
    MINDFULNESS("Практика"),
    CULTURE("Культура"),
    WRITING("Письмо");

    companion object {
        fun fromString(raw: String?): TrackKind = when (raw?.trim()?.lowercase()) {
            "mindfulness", "meditation" -> MINDFULNESS
            "culture", "philosophy", "buddhism" -> CULTURE
            "writing", "craft" -> WRITING
            else -> LANGUAGE
        }
    }
}

/** Возможности движка, активные для конкретного трека. */
data class TrackCapabilities(
    val tts: Boolean = true,
    val diacritics: Boolean = true,
    val fsrs: Boolean = true,
    val llmCoach: Boolean = false,
)

/** Метаданные обучающего трека (курса). */
data class TrackMeta(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val kind: TrackKind = TrackKind.LANGUAGE,
    val mascotPose: String = "flag_ro",
    val ttsLocale: String? = "ro-RO",
    val specialChars: List<String> = listOf("ă", "â", "î", "ș", "ț"),
    val capabilities: TrackCapabilities = TrackCapabilities(),
)

/** Лемма или концепт: единица словаря и памяти FSRS. */
data class Lemma(
    val id: String,
    val lemma: String,
    val translationRu: String,
    val pos: PartOfSpeech,
    val gender: Gender? = null,
    val plural: String? = null,
    val exampleRo: String? = null,
    val exampleRu: String? = null,
    val tags: Set<String> = emptySet(),
    val trackId: String = "ro_a1",
)

enum class PartOfSpeech { NOUN, VERB, ADJECTIVE, ADVERB, PRONOUN, NUMERAL, PREPOSITION, CONJUNCTION, INTERJECTION, PHRASE, CONCEPT }

enum class Gender { MASCULINE, FEMININE, NEUTER }

/** Блок: ровно 5 шагов, 3–4 минуты. */
data class Block(
    val id: String,
    val titleRo: String,
    val steps: List<Step>,
    val titleRu: String = "",
    val trackId: String = "ro_a1",
) {
    val total: Int get() = steps.size
}

/** Сцена: урок из 2 блоков (10 шагов). */
data class Scene(
    val id: String,
    val titleRo: String,
    val blocks: List<Block>,
    val titleRu: String = "",
)

/** Глава: 4–6 сцен и босс. */
data class Chapter(
    val id: String,
    val titleRo: String,
    val titleRu: String,
    val scenes: List<Scene>,
    val isBossCleared: Boolean = false,
)

/** Юнит: одна грамматическая или смысловая тема. */
data class Unit(
    val id: String,
    val titleRu: String,
    val chapters: List<Chapter>,
    val trackId: String = "ro_a1",
)

/** Прогресс пользователя по блоку. */
data class BlockProgress(
    val blockId: String,
    val chapterId: String,
    val currentStepIndex: Int,
    val closed: Boolean,
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
) {
    val fraction: Float
        get() = if (totalAnswers == 0) 0f else correctAnswers.toFloat() / totalAnswers
}

/** Текущее состояние стрика. */
data class StreakState(
    val days: Int,
    val lastClosedDay: Long?,
    val freezesLeft: Int,
    val repairAvailableUntil: Long? = null,
)
