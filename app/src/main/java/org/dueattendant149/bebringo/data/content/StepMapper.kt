package org.dueattendant149.bebringo.data.content

import org.dueattendant149.bebringo.domain.model.AssembleStep
import org.dueattendant149.bebringo.domain.model.DialogueLine
import org.dueattendant149.bebringo.domain.model.DialogueStep
import org.dueattendant149.bebringo.domain.model.DictationStep
import org.dueattendant149.bebringo.domain.model.ImageStep
import org.dueattendant149.bebringo.domain.model.InsightExample
import org.dueattendant149.bebringo.domain.model.InsightStep
import org.dueattendant149.bebringo.domain.model.ListeningStep
import org.dueattendant149.bebringo.domain.model.RecognitionStep
import org.dueattendant149.bebringo.domain.model.ReflectionStep
import org.dueattendant149.bebringo.domain.model.SprintStep
import org.dueattendant149.bebringo.domain.model.Step
import org.dueattendant149.bebringo.domain.model.TextSpan
import org.dueattendant149.bebringo.domain.model.TimerCue
import org.dueattendant149.bebringo.domain.model.TimerPracticeStep
import org.dueattendant149.bebringo.domain.model.TransformStep
import org.dueattendant149.bebringo.domain.model.TypeInStep
import org.dueattendant149.bebringo.domain.model.WeaveStep

/** Ошибка в описании шага: курсом нельзя пользоваться, пока она не исправлена. */
class ContentException(message: String) : Exception(message)

/**
 * Преобразование YAML-описания в доменный шаг.
 *
 * Валидация жёсткая: лучше упасть при загрузке курса, чем показать пользователю
 * пустой экран посреди урока.
 */
object StepMapper {

    fun toStep(id: String, def: StepDef): Step = when (def.type.lowercase()) {
        "insight" -> InsightStep(
            id = id,
            title = def.title ?: def.prompt.require("title"),
            bodyMarkdown = def.body ?: def.instruction.require("body"),
            examples = def.examples.map { InsightExample(it.primary, it.secondary) },
            tip = def.tip ?: def.hint,
            mascotPose = def.mascotPose ?: "insight",
            lemmaId = def.lemma,
        )

        "timer" -> TimerPracticeStep(
            id = id,
            title = def.title ?: def.prompt.require("title"),
            instruction = def.instruction ?: def.body.require("instruction"),
            durationSec = def.durationSec.takeIf { it > 0 } ?: def.timeLimit,
            cues = def.cues.map { TimerCue(it.atSec, it.text) },
            mascotPose = def.mascotPose ?: "meditate",
            lemmaId = def.lemma,
        )

        "reflection" -> ReflectionStep(
            id = id,
            prompt = def.prompt.require("prompt"),
            minWords = def.minWords.coerceAtLeast(1),
            checklist = def.checklist,
            placeholder = def.placeholder ?: "Напишите свой ответ или наблюдение...",
            mascotPose = def.mascotPose ?: "pencil",
            lemmaId = def.lemma,
        )

        "recognition" -> RecognitionStep(
            id = id,
            wordRo = def.word.require("word"),
            translationRu = def.translation.require("translation"),
            optionsRu = def.options.requireList("options"),
            lemmaId = def.lemma,
        )

        "listening" -> ListeningStep(
            id = id,
            audioText = def.audio.require("audio"),
            translationRu = def.translation.require("translation"),
            optionsRu = def.options.requireList("options"),
            lemmaId = def.lemma,
        )

        "image" -> ImageStep(
            id = id,
            imageKey = def.image ?: def.word.require("word"),
            wordRo = def.word.require("word"),
            optionsRo = def.options.requireList("options"),
            lemmaId = def.lemma,
        )

        "assemble" -> AssembleStep(
            id = id,
            promptRu = def.prompt.require("prompt"),
            tokens = def.tokens.requireList("tokens"),
            answerRo = def.answer.require("answer"),
            acceptableAnswers = def.answers,
            lemmaId = def.lemma,
        )

        "typein" -> TypeInStep(
            id = id,
            promptRu = def.prompt.require("prompt"),
            answerRo = def.answer.require("answer"),
            hint = def.hint,
            acceptableAnswers = def.answers,
            lemmaId = def.lemma,
        )

        "dictation" -> DictationStep(
            id = id,
            audioText = def.audio.require("audio"),
            answerRo = def.answer.require("answer"),
            acceptableAnswers = def.answers,
            lemmaId = def.lemma,
        )

        "transform" -> TransformStep(
            id = id,
            sourceRo = def.source.require("source"),
            instructionRu = def.instruction.require("instruction"),
            answerRo = def.answer.require("answer"),
            acceptableAnswers = def.answers,
            lemmaId = def.lemma,
        )

        "weave" -> WeaveStep(
            id = id,
            paragraphs = def.paragraphs.map { paragraph -> paragraph.map { it.toSpan() } },
            lemmaId = def.lemma,
        )

        "sprint" -> SprintStep(
            id = id,
            paragraphs = def.paragraphs.map { paragraph -> paragraph.joinToString("") { it.text } },
            timeLimitSec = def.timeLimit,
            lemmaId = def.lemma,
        )

        "dialogue" -> DialogueStep(
            id = id,
            title = def.prompt ?: "Диалог",
            lines = def.lines.map { DialogueLine(it.speaker, it.text, it.ru) },
            questionRu = def.question.require("question"),
            optionsRu = def.options.requireList("options"),
            lemmaId = def.lemma,
        )

        else -> throw ContentException("Неизвестный тип шага: ${def.type}")
    }

    private fun SpanDef.toSpan(): TextSpan = TextSpan(text = text, isRussian = ru, lemmaId = lemma)

    private fun String?.require(field: String): String =
        this?.takeIf { it.isNotBlank() }
            ?: throw ContentException("В шаге не заполнено поле «$field»")

    private fun List<String>.requireList(field: String): List<String> {
        if (isEmpty()) throw ContentException("В шаге пустой список «$field»")
        return this
    }
}
