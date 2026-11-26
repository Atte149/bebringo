package org.dueattendant149.bebringo.data.content

import android.content.Context
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.Chapter
import org.dueattendant149.bebringo.domain.model.Gender
import org.dueattendant149.bebringo.domain.model.Lemma
import org.dueattendant149.bebringo.domain.model.PartOfSpeech
import org.dueattendant149.bebringo.domain.model.Scene
import org.dueattendant149.bebringo.domain.model.TrackCapabilities
import org.dueattendant149.bebringo.domain.model.TrackKind
import org.dueattendant149.bebringo.domain.model.TrackMeta
import org.dueattendant149.bebringo.domain.model.Unit
import java.io.File

/** Готовый курс (или объединённый набор треков) в доменных моделях. */
data class LoadedCourse(
    val title: String,
    val lemmas: Map<String, Lemma>,
    val units: List<Unit>,
    val blocks: List<Block>,
    val track: TrackMeta = TrackMeta(id = "ro_a1", title = title),
    val tracks: List<TrackMeta> = listOf(track),
) {
    fun blockById(id: String): Block? = blocks.firstOrNull { it.id == id }
    fun trackById(id: String): TrackMeta? = tracks.firstOrNull { it.id == id }
    fun blocksForTrack(trackId: String): List<Block> =
        if (trackId == "all" || trackId.isBlank()) blocks else blocks.filter { it.trackId == trackId }
}

/**
 * Разбор YAML-курса / трека.
 *
 * Отделён от чтения assets, чтобы проверяться юнит-тестом на JVM без Android.
 */
object CourseParser {

    private val yaml = Yaml.default

    fun parse(raw: String): LoadedCourse {
        val file = yaml.decodeFromString<CourseFile>(raw)
        return build(file)
    }

    fun merge(courses: List<LoadedCourse>): LoadedCourse {
        if (courses.isEmpty()) {
            return LoadedCourse(
                title = "Bebringo",
                lemmas = emptyMap(),
                units = emptyList(),
                blocks = emptyList(),
            )
        }
        if (courses.size == 1) return courses.first()
        val primary = courses.first()
        return LoadedCourse(
            title = primary.title,
            lemmas = courses.fold(emptyMap()) { acc, c -> acc + c.lemmas },
            units = courses.flatMap { it.units },
            blocks = courses.flatMap { it.blocks },
            track = primary.track,
            tracks = courses.flatMap { it.tracks }.distinctBy { it.id },
        )
    }

    private fun build(file: CourseFile): LoadedCourse {
        val trackId = file.id.ifBlank { "ro_a1" }
        val trackMeta = TrackMeta(
            id = trackId,
            title = file.title,
            subtitle = file.subtitle,
            kind = TrackKind.fromString(file.kind),
            mascotPose = file.mascotPose,
            ttsLocale = file.ttsLocale,
            specialChars = file.specialChars,
            capabilities = TrackCapabilities(
                tts = file.capabilities.tts,
                diacritics = file.capabilities.diacritics,
                fsrs = file.capabilities.fsrs,
                llmCoach = file.capabilities.llmCoach,
            ),
        )

        val lemmas = file.lemmas.mapIndexed { index, def ->
            val id = def.id ?: "${def.lemma}-$index"
            id to Lemma(
                id = id,
                lemma = def.lemma,
                translationRu = def.ru,
                pos = def.pos.toPartOfSpeech(),
                gender = def.gender?.toGender(),
                plural = def.plural,
                exampleRo = def.example,
                exampleRu = def.exampleRu,
                tags = def.tags.toSet(),
                trackId = trackId,
            )
        }.toMap()

        val units = file.units.map { unitDef ->
            Unit(
                id = unitDef.id,
                titleRu = unitDef.titleRu,
                trackId = trackId,
                chapters = unitDef.chapters.map { chapterDef ->
                    Chapter(
                        id = chapterDef.id,
                        titleRo = chapterDef.titleRo,
                        titleRu = chapterDef.titleRu,
                        scenes = chapterDef.scenes.map { sceneDef ->
                            Scene(
                                id = sceneDef.id,
                                titleRo = sceneDef.titleRo,
                                titleRu = sceneDef.titleRu,
                                blocks = sceneDef.blocks.map { blockDef ->
                                    Block(
                                        id = blockDef.id,
                                        titleRo = blockDef.titleRo,
                                        titleRu = blockDef.titleRu.ifBlank { chapterDef.titleRu },
                                        trackId = trackId,
                                        steps = blockDef.steps.mapIndexed { index, stepDef ->
                                            StepMapper.toStep("${blockDef.id}-$index", stepDef)
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }

        val blocks = units.flatMap { unit ->
            unit.chapters.flatMap { chapter -> chapter.scenes.flatMap { it.blocks } }
        }

        return LoadedCourse(
            title = file.title,
            lemmas = lemmas,
            units = units,
            blocks = blocks,
            track = trackMeta,
            tracks = listOf(trackMeta),
        )
    }

    private fun String.toPartOfSpeech(): PartOfSpeech = when (lowercase()) {
        "noun" -> PartOfSpeech.NOUN
        "verb" -> PartOfSpeech.VERB
        "adjective", "adj" -> PartOfSpeech.ADJECTIVE
        "adverb", "adv" -> PartOfSpeech.ADVERB
        "pronoun" -> PartOfSpeech.PRONOUN
        "numeral" -> PartOfSpeech.NUMERAL
        "preposition" -> PartOfSpeech.PREPOSITION
        "conjunction" -> PartOfSpeech.CONJUNCTION
        "interjection" -> PartOfSpeech.INTERJECTION
        "phrase" -> PartOfSpeech.PHRASE
        "concept", "term", "practice" -> PartOfSpeech.CONCEPT
        else -> PartOfSpeech.NOUN
    }

    private fun String.toGender(): Gender? = when (lowercase()) {
        "m", "masculine" -> Gender.MASCULINE
        "f", "feminine" -> Gender.FEMININE
        "n", "neuter" -> Gender.NEUTER
        else -> null
    }
}

/** Читает курс и пользовательские треки из assets и локальной директории `tracks/`. */
class CourseLoader(private val context: Context) {
    fun load(assetName: String = "course/a1.yml"): LoadedCourse {
        val raw = context.assets.open(assetName).bufferedReader().use { it.readText() }
        return CourseParser.parse(raw)
    }

    fun loadAllTracks(): LoadedCourse {
        val assetFiles = runCatching {
            context.assets.list("course")?.filter { it.endsWith(".yml") || it.endsWith(".yaml") }.orEmpty()
        }.getOrDefault(listOf("a1.yml"))

        val orderedAssets = buildList {
            if ("a1.yml" in assetFiles) add("a1.yml")
            addAll(assetFiles.filter { it != "a1.yml" }.sorted())
        }

        val parsedFromAssets = orderedAssets.mapNotNull { fileName ->
            runCatching { load("course/$fileName") }.getOrNull()
        }

        val userDir = File(context.filesDir, "tracks")
        val parsedFromUserDir = if (userDir.exists() && userDir.isDirectory) {
            userDir.listFiles { f -> f.isFile && (f.name.endsWith(".yml") || f.name.endsWith(".yaml")) }
                ?.sortedBy { it.name }
                ?.mapNotNull { file ->
                    runCatching { CourseParser.parse(file.readText()) }.getOrNull()
                }.orEmpty()
        } else {
            emptyList()
        }

        return CourseParser.merge(parsedFromAssets + parsedFromUserDir)
    }
}
