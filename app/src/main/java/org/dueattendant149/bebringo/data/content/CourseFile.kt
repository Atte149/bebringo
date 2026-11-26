package org.dueattendant149.bebringo.data.content

import kotlinx.serialization.Serializable

/**
 * YAML-схема трека/курса (v2, обратно совместима с v1).
 *
 * Один и тот же формат используется для языковых курсов, практик осознанности,
 * введения в культуру/философию и тренажёров писательского мастерства.
 */
@Serializable
data class CourseFile(
    val id: String = "ro_a1",
    val title: String = "A1",
    val subtitle: String = "Румынский язык · уровень A1",
    val kind: String = "language",
    val mascotPose: String = "flag_ro",
    val ttsLocale: String? = "ro-RO",
    val specialChars: List<String> = listOf("ă", "â", "î", "ș", "ț"),
    val capabilities: CapabilitiesDef = CapabilitiesDef(),
    val lemmas: List<LemmaDef> = emptyList(),
    val units: List<UnitDef> = emptyList(),
)

@Serializable
data class CapabilitiesDef(
    val tts: Boolean = true,
    val diacritics: Boolean = true,
    val fsrs: Boolean = true,
    val llmCoach: Boolean = false,
)

@Serializable
data class LemmaDef(
    val id: String? = null,
    val lemma: String,
    val ru: String,
    val pos: String = "noun",
    val gender: String? = null,
    val plural: String? = null,
    val example: String? = null,
    val exampleRu: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class UnitDef(
    val id: String,
    val titleRu: String,
    val chapters: List<ChapterDef> = emptyList(),
)

@Serializable
data class ChapterDef(
    val id: String,
    val titleRo: String,
    val titleRu: String,
    val scenes: List<SceneDef> = emptyList(),
)

@Serializable
data class SceneDef(
    val id: String,
    val titleRo: String,
    val titleRu: String = "",
    val blocks: List<BlockDef> = emptyList(),
)

@Serializable
data class BlockDef(
    val id: String,
    val titleRo: String,
    val titleRu: String = "",
    val steps: List<StepDef> = emptyList(),
)

/**
 * Описание одного шага в YAML.
 *
 * Поддерживает 10 языковых типов и 3 универсальных типа (`insight`, `timer`, `reflection`).
 */
@Serializable
data class StepDef(
    val type: String,
    val lemma: String? = null,
    val word: String? = null,
    val translation: String? = null,
    val prompt: String? = null,
    val answer: String? = null,
    val answers: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val tokens: List<String> = emptyList(),
    val hint: String? = null,
    val audio: String? = null,
    val source: String? = null,
    val instruction: String? = null,
    val image: String? = null,
    val paragraphs: List<List<SpanDef>> = emptyList(),
    val lines: List<LineDef> = emptyList(),
    val question: String? = null,
    val timeLimit: Int = 60,
    // Поля универсальных шагов (insight / timer / reflection)
    val title: String? = null,
    val body: String? = null,
    val tip: String? = null,
    val mascotPose: String? = null,
    val examples: List<ExampleDef> = emptyList(),
    val durationSec: Int = 60,
    val cues: List<CueDef> = emptyList(),
    val minWords: Int = 5,
    val checklist: List<String> = emptyList(),
    val placeholder: String? = null,
)

@Serializable
data class ExampleDef(
    val primary: String,
    val secondary: String = "",
)

@Serializable
data class CueDef(
    val atSec: Int,
    val text: String,
)

@Serializable
data class SpanDef(
    val text: String,
    val ru: Boolean = false,
    val lemma: String? = null,
)

@Serializable
data class LineDef(
    val speaker: String,
    val text: String,
    val ru: String? = null,
)
