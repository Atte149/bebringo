package org.dueattendant149.bebringo.domain.model

/**
 * Шаг блока. Тринадцать типов (10 языковых + 3 универсальных для любых треков).
 *
 * Инвариант языкового блока: минимум один продуктивный шаг и минимум один аудиошаг.
 * Каждый шаг может ссылаться на `lemmaId` (слово или концепт) для точного обновления FSRS.
 */
sealed interface Step {
    val id: String

    /** Явная привязка к лемме/концепту словаря для FSRS. */
    val lemmaId: String? get() = null

    /** Продуктивный шаг: пользователь строит форму сам, а не узнаёт её. */
    val isProductive: Boolean get() = false

    /** Аудиошаг: задействует восприятие на слух. */
    val isAudio: Boolean get() = false

    /**
     * Шаг оценивается как ответ пользователя и обновляет FSRS.
     * Теория, таймер, вплетение и пробег — не ответы, их нельзя считать повторением.
     */
    val isAssessed: Boolean get() = true
}

/** Пример внутри карточки теории/инсайта. */
data class InsightExample(
    val primary: String,
    val secondary: String,
)

/** Подсказка по времени внутри таймер-практики. */
data class TimerCue(
    val atSec: Int,
    val text: String,
)

/**
 * Теория / Инсайт (40 секунд): вводит новое правило, лемму или концепт
 * ПЕРЕД тем, как тестировать пользователя.
 */
data class InsightStep(
    override val id: String,
    val title: String,
    val bodyMarkdown: String,
    val examples: List<InsightExample> = emptyList(),
    val tip: String? = null,
    val mascotPose: String = "insight",
    override val lemmaId: String? = null,
) : Step {
    override val isAssessed: Boolean get() = false
}

/**
 * Таймер-практика: медитация, дыхательное упражнение, наблюдение или фрирайтинг на время.
 */
data class TimerPracticeStep(
    override val id: String,
    val title: String,
    val instruction: String,
    val durationSec: Int = 60,
    val cues: List<TimerCue> = emptyList(),
    val mascotPose: String = "meditate",
    override val lemmaId: String? = null,
) : Step {
    override val isAssessed: Boolean get() = false
}

/**
 * Свободная рефлексия / письменное упражнение с чеклистом самопроверки и опциональным разбором ИИ.
 */
data class ReflectionStep(
    override val id: String,
    val prompt: String,
    val minWords: Int = 5,
    val checklist: List<String> = emptyList(),
    val placeholder: String = "Напишите свой ответ или наблюдение...",
    val mascotPose: String = "pencil",
    override val lemmaId: String? = null,
) : Step {
    override val isProductive: Boolean get() = true
}

/** Узнавание: слово/термин → выбор значения. */
data class RecognitionStep(
    override val id: String,
    val wordRo: String,
    val translationRu: String,
    val optionsRu: List<String>,
    override val lemmaId: String? = null,
) : Step

/** Слух: аудио → выбор значения. */
data class ListeningStep(
    override val id: String,
    val audioText: String,
    val translationRu: String,
    val optionsRu: List<String>,
    override val lemmaId: String? = null,
) : Step {
    override val isAudio: Boolean get() = true
}

/** Образ: картинка/схема → слово, без перевода на экране. */
data class ImageStep(
    override val id: String,
    val imageKey: String,
    val wordRo: String,
    val optionsRo: List<String>,
    override val lemmaId: String? = null,
) : Step

/** Сборка: плитки слов → фраза по русскому промпту. */
data class AssembleStep(
    override val id: String,
    val promptRu: String,
    val tokens: List<String>,
    val answerRo: String,
    val acceptableAnswers: List<String> = emptyList(),
    override val lemmaId: String? = null,
) : Step {
    override val isProductive: Boolean get() = true
}

/** Ввод с нуля: печать ответа по промпту. */
data class TypeInStep(
    override val id: String,
    val promptRu: String,
    val answerRo: String,
    val hint: String? = null,
    val acceptableAnswers: List<String> = emptyList(),
    override val lemmaId: String? = null,
) : Step {
    override val isProductive: Boolean get() = true
}

/** Диктовка: аудио → печать. */
data class DictationStep(
    override val id: String,
    val audioText: String,
    val answerRo: String,
    val acceptableAnswers: List<String> = emptyList(),
    override val lemmaId: String? = null,
) : Step {
    override val isProductive: Boolean get() = true
    override val isAudio: Boolean get() = true
}

/** Трансформация: изменить форму (число, артикль, время, стиль). */
data class TransformStep(
    override val id: String,
    val sourceRo: String,
    val instructionRu: String,
    val answerRo: String,
    val acceptableAnswers: List<String> = emptyList(),
    override val lemmaId: String? = null,
) : Step {
    override val isProductive: Boolean get() = true
}

/** Фрагмент текста: изучаемый или вплетённый переводной токен. */
data class TextSpan(
    val text: String,
    val isRussian: Boolean = false,
    val lemmaId: String? = null,
)

/** Вплетение: абзац с русскими вставками и тапом по слову. */
data class WeaveStep(
    override val id: String,
    val paragraphs: List<List<TextSpan>>,
    override val lemmaId: String? = null,
) : Step {
    override val isAssessed: Boolean get() = false
}

/** Пробег: текст на скорость узнавания. */
data class SprintStep(
    override val id: String,
    val paragraphs: List<String>,
    val timeLimitSec: Int = 60,
    override val lemmaId: String? = null,
) : Step {
    override val isAssessed: Boolean get() = false
}

/** Реплика диалога. */
data class DialogueLine(
    val speakerRo: String,
    val textRo: String,
    val translationRu: String? = null,
)

/** Диалог: аудиосцена → вопрос → варианты. */
data class DialogueStep(
    override val id: String,
    val title: String,
    val lines: List<DialogueLine>,
    val questionRu: String,
    val optionsRu: List<String>,
    val correctOption: String = optionsRu.firstOrNull().orEmpty(),
    override val lemmaId: String? = null,
) : Step {
    override val isAudio: Boolean get() = true
}
