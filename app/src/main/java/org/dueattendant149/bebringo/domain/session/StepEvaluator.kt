package org.dueattendant149.bebringo.domain.session

import org.dueattendant149.bebringo.domain.model.AnswerEvent
import org.dueattendant149.bebringo.domain.model.AssembleStep
import org.dueattendant149.bebringo.domain.model.DialogueStep
import org.dueattendant149.bebringo.domain.model.DictationStep
import org.dueattendant149.bebringo.domain.model.ErrorType
import org.dueattendant149.bebringo.domain.model.ImageStep
import org.dueattendant149.bebringo.domain.model.InsightStep
import org.dueattendant149.bebringo.domain.model.ListeningStep
import org.dueattendant149.bebringo.domain.model.MistakeNote
import org.dueattendant149.bebringo.domain.model.RecognitionStep
import org.dueattendant149.bebringo.domain.model.ReflectionStep
import org.dueattendant149.bebringo.domain.model.SprintStep
import org.dueattendant149.bebringo.domain.model.Step
import org.dueattendant149.bebringo.domain.model.TimerPracticeStep
import org.dueattendant149.bebringo.domain.model.TransformStep
import org.dueattendant149.bebringo.domain.model.TypeInStep
import org.dueattendant149.bebringo.domain.model.WeaveStep

/** Результат проверки одного шага. */
data class StepEvaluation(
    val correct: Boolean,
    val expected: String,
    val errorType: ErrorType?,
    val note: MistakeNote?,
    val warningNote: String? = null,
)

/**
 * Проверка ответа на шаг. Отделена от UI, чтобы её можно было тестировать
 * и чтобы экран не знал, как устроен тот или иной тип задания.
 */
object StepEvaluator {

    fun evaluate(
        step: Step,
        selectedOption: Int?,
        textAnswer: String,
        pickedTokens: List<String>,
        requireDiacritics: Boolean = false,
    ): StepEvaluation = when (step) {
        is InsightStep, is TimerPracticeStep, is WeaveStep, is SprintStep -> StepEvaluation(
            correct = true,
            expected = "",
            errorType = null,
            note = null,
        )

        is ReflectionStep -> {
            val words = textAnswer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val enough = words.size >= step.minWords
            StepEvaluation(
                correct = enough,
                expected = "Минимум слов: ${step.minWords}",
                errorType = if (enough) null else ErrorType.SKIPPED,
                note = if (enough) null else MistakeNote(
                    prompt = textAnswer.ifBlank { "Пустой ответ" },
                    correct = "Минимум ${step.minWords} слов(а)",
                    explanationRu = "Разверните мысль чуть подробнее (сейчас слов: ${words.size}).",
                    errorType = ErrorType.SKIPPED,
                ),
            )
        }

        is RecognitionStep -> choiceEvaluation(
            selected = selectedOption,
            options = step.optionsRu,
            expected = step.translationRu,
        )

        is ListeningStep -> choiceEvaluation(
            selected = selectedOption,
            options = step.optionsRu,
            expected = step.translationRu,
        )

        is ImageStep -> choiceEvaluation(
            selected = selectedOption,
            options = step.optionsRo,
            expected = step.wordRo,
        )

        is DialogueStep -> choiceEvaluation(
            selected = selectedOption,
            options = step.optionsRu,
            expected = step.correctOption,
        )

        is AssembleStep -> textEvaluation(
            actual = pickedTokens.joinToString(" "),
            expected = step.answerRo,
            alternatives = step.acceptableAnswers,
            requireDiacritics = requireDiacritics,
        )

        is TypeInStep -> textEvaluation(
            actual = textAnswer,
            expected = step.answerRo,
            alternatives = step.acceptableAnswers,
            requireDiacritics = requireDiacritics,
        )

        is DictationStep -> textEvaluation(
            actual = textAnswer,
            expected = step.answerRo,
            alternatives = step.acceptableAnswers,
            requireDiacritics = requireDiacritics,
        )

        is TransformStep -> textEvaluation(
            actual = textAnswer,
            expected = step.answerRo,
            alternatives = step.acceptableAnswers,
            requireDiacritics = requireDiacritics,
        )
    }

    /** Тип упражнения для телеметрии. */
    fun exerciseType(step: Step): String = when (step) {
        is InsightStep -> "insight"
        is TimerPracticeStep -> "timer"
        is ReflectionStep -> "reflection"
        is RecognitionStep -> "recognition"
        is ListeningStep -> "listening"
        is ImageStep -> "image"
        is AssembleStep -> "assemble"
        is TypeInStep -> "typein"
        is DictationStep -> "dictation"
        is TransformStep -> "transform"
        is WeaveStep -> "weave"
        is SprintStep -> "sprint"
        is DialogueStep -> "dialogue"
    }

    fun toAnswerEvent(
        step: Step,
        evaluation: StepEvaluation,
        responseMs: Long,
        isFirstAttempt: Boolean = true,
    ) = AnswerEvent(
        stepId = step.id,
        exerciseType = exerciseType(step),
        lemma = step.lemmaHint(),
        correct = evaluation.correct,
        errorType = evaluation.errorType,
        responseMs = responseMs,
        lemmaId = step.lemmaId,
        isProductive = step.isProductive,
        isAssessed = step.isAssessed,
        isFirstAttempt = isFirstAttempt,
    )

    private fun Step.lemmaHint(): String = when (this) {
        is InsightStep -> title
        is TimerPracticeStep -> title
        is ReflectionStep -> prompt
        is RecognitionStep -> wordRo
        is ListeningStep -> audioText
        is ImageStep -> wordRo
        is AssembleStep -> answerRo
        is TypeInStep -> answerRo
        is DictationStep -> audioText
        is TransformStep -> sourceRo
        is WeaveStep -> ""
        is SprintStep -> ""
        is DialogueStep -> title
    }

    private fun choiceEvaluation(
        selected: Int?,
        options: List<String>,
        expected: String,
    ): StepEvaluation {
        val chosen = selected?.let { options.getOrNull(it) }
        val correct = chosen == expected
        return StepEvaluation(
            correct = correct,
            expected = expected,
            errorType = if (correct) null else if (chosen == null) ErrorType.SKIPPED else ErrorType.WRONG_WORD,
            note = if (correct) null else MistakeNote(
                prompt = chosen ?: "Нет ответа",
                correct = expected,
                explanationRu = "Правильный вариант — «$expected».",
                errorType = if (chosen == null) ErrorType.SKIPPED else ErrorType.WRONG_WORD,
            ),
        )
    }

    private fun textEvaluation(
        actual: String,
        expected: String,
        alternatives: List<String>,
        requireDiacritics: Boolean,
    ): StepEvaluation {
        val result = AnswerChecker.checkAny(expected, alternatives, actual, requireDiacritics)
        val isDiacriticsWarning = result.verdict == AnswerVerdict.CORRECT_WITHOUT_DIACRITICS
        val warningText = if (isDiacriticsWarning) {
            "Ответ засчитан! Обратите внимание на диакритику: «${result.expected}»"
        } else {
            null
        }
        val note = when {
            !result.isCorrect -> MistakeNote(
                prompt = actual.ifBlank { "Пустой ответ" },
                correct = result.expected,
                explanationRu = explanationFor(result.errorType),
                errorType = result.errorType ?: ErrorType.WRONG_WORD,
            )
            isDiacriticsWarning -> MistakeNote(
                prompt = actual,
                correct = result.expected,
                explanationRu = "Ответ верный, но без диакритики: «${result.expected}».",
                errorType = ErrorType.DIACRITICS,
            )
            else -> null
        }

        return StepEvaluation(
            correct = result.isCorrect,
            expected = result.expected,
            errorType = result.errorType,
            note = note,
            warningNote = warningText,
        )
    }

    private fun explanationFor(type: ErrorType?): String = when (type) {
        ErrorType.DIACRITICS -> "Не хватает диакритики: ă, â, î, ș или ț."
        ErrorType.WORD_ORDER -> "Слова те же, но порядок другой."
        ErrorType.WRONG_FORM -> "Слово верное, форма неверная."
        ErrorType.SKIPPED -> "Ответа не было."
        ErrorType.WRONG_WORD, null -> "Другое слово."
    }
}
