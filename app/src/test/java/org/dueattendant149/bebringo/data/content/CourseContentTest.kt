package org.dueattendant149.bebringo.data.content

import org.dueattendant149.bebringo.domain.model.AssembleStep
import org.dueattendant149.bebringo.domain.model.InsightStep
import org.dueattendant149.bebringo.domain.model.ReflectionStep
import org.dueattendant149.bebringo.domain.model.TimerPracticeStep
import org.dueattendant149.bebringo.domain.model.TrackKind
import org.dueattendant149.bebringo.domain.session.DefaultBlockBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Проверка встроенного курса и модульного парсера треков на JVM.
 *
 * Если контент сломан, сборка падает здесь, а не на телефоне посреди урока.
 */
class CourseContentTest {

    private val raw: String by lazy {
        File("src/main/assets/course/a1.yml").readText()
    }

    private val course: LoadedCourse by lazy { CourseParser.parse(raw) }

    @Test
    fun `курс парсится и содержит метаданные трека v2`() {
        assertTrue(course.blocks.isNotEmpty())
        assertTrue(course.lemmas.isNotEmpty())
        assertTrue(course.units.isNotEmpty())
        assertEquals("ro_a1", course.track.id)
        assertEquals(TrackKind.LANGUAGE, course.track.kind)
        assertTrue(course.track.capabilities.diacritics)
    }

    @Test
    fun `в курсе достаточно блоков и лемм`() {
        assertTrue("Блоков: ${course.blocks.size}", course.blocks.size >= 16)
        assertTrue("Лемм: ${course.lemmas.size}", course.lemmas.size >= 50)
    }

    @Test
    fun `каждый блок проходит проверку инвариантов`() {
        val builder = DefaultBlockBuilder(
            lemmaProvider = { id -> course.lemmas[id] },
            allLemmasProvider = { course.lemmas.values.toList() },
        )
        val violations = course.blocks.flatMap { block ->
            builder.validate(block).map { "${block.id}: $it" }
        }
        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun `идентификаторы блоков уникальны`() {
        val ids = course.blocks.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `в каждом блоке пять шагов`() {
        course.blocks.forEach { block ->
            assertEquals("${block.id}: ${block.steps.size}", 5, block.steps.size)
        }
    }

    @Test
    fun `в блоке нет повторяющихся оцениваемых лемм`() {
        course.blocks.forEach { block ->
            val assessed = block.steps.filter { it.isAssessed }.mapNotNull { it.lemmaId }
            assertEquals(
                "${block.id}: оцениваемые леммы повторяются — $assessed",
                assessed.size,
                assessed.distinct().size,
            )
        }
    }

    @Test
    fun `в сцене нет повторяющихся оцениваемых лемм`() {
        course.units.forEach { unit ->
            unit.chapters.forEach { chapter ->
                chapter.scenes.forEach { scene ->
                    val assessed = scene.blocks
                        .flatMap { it.steps }
                        .filter { it.isAssessed }
                        .mapNotNull { it.lemmaId }
                    assertEquals(
                        "${scene.id}: оцениваемые леммы повторяются — $assessed",
                        assessed.size,
                        assessed.distinct().size,
                    )
                }
            }
        }
    }

    @Test
    fun `все ответы непустые и assemble содержит дистракторы`() {
        course.blocks.forEach { block ->
            block.steps.forEach { step ->
                val text = when (step) {
                    is org.dueattendant149.bebringo.domain.model.TypeInStep -> step.answerRo
                    is AssembleStep -> {
                        val answerWordCount = step.answerRo.trim().split(Regex("\\s+")).size
                        assertTrue(
                            "${block.id}/${step.id}: в assemble должны быть слова-дистракторы",
                            step.tokens.size > answerWordCount,
                        )
                        step.answerRo
                    }
                    is org.dueattendant149.bebringo.domain.model.TransformStep -> step.answerRo
                    is org.dueattendant149.bebringo.domain.model.DictationStep -> step.answerRo
                    else -> null
                }
                if (text != null) {
                    assertTrue("${block.id}/${step.id} пустой ответ", text.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `новые типы шагов insight timer и reflection корректно парсятся`() {
        val mindfulnessYaml = """
            id: mindfulness_base
            title: Практика осознанности
            subtitle: Дыхание, внимание и покой
            kind: mindfulness
            mascotPose: meditate
            ttsLocale: ru-RU
            specialChars: []
            capabilities:
              tts: false
              diacritics: false
              fsrs: true
              llmCoach: false
            lemmas:
              - id: anapanasati
                lemma: Анапанасати
                ru: осознанность к дыханию
                pos: concept
            units:
              - id: m1
                titleRu: Неделя 1
                chapters:
                  - id: m1-c1
                    titleRo: Анапанасати
                    titleRu: Возвращение к дыханию
                    scenes:
                      - id: m1-s1
                        titleRo: Пауза
                        titleRu: Первая пауза
                        blocks:
                          - id: m1-b1
                            titleRo: Точка опоры
                            titleRu: Точка опоры
                            steps:
                              - type: insight
                                lemma: anapanasati
                                title: Дыхание как якорь
                                body: Когда ум блуждает, верните внимание к вдоху и выдоху.
                                examples:
                                  - primary: Вдох — я знаю, что вдыхаю
                                    secondary: Выдох — я знаю, что выдыхаю
                                tip: Не меняйте ритм дыхания специально.
                              - type: recognition
                                lemma: anapanasati
                                word: Анапанасати
                                translation: осознанность к дыханию
                                options: [осознанность к дыханию, задержка дыхания, визуализация, чтение мантр]
                              - type: timer
                                lemma: anapanasati
                                title: Минута квадратного дыхания
                                instruction: Следуйте за ритмом дыхания
                                durationSec: 60
                                cues:
                                  - atSec: 0
                                    text: Сделайте мягкий вдох
                                  - atSec: 30
                                    text: Отпустите напряжение плеч
                              - type: listening
                                lemma: anapanasati
                                audio: Верните внимание к кончику носа
                                translation: точка наблюдения за дыханием
                                options: [точка наблюдения за дыханием, задержка дыхания, счёт вслух, быстрый вдох]
                              - type: reflection
                                lemma: anapanasati
                                prompt: Что вы заметили за эту минуту?
                                checklist:
                                  - Отметил состояние тела
                                  - Заметил хотя бы одну мысль без оценки
                                minWords: 5
        """.trimIndent()

        val parsed = CourseParser.parse(mindfulnessYaml)
        assertEquals(TrackKind.MINDFULNESS, parsed.track.kind)
        val steps = parsed.blocks.single().steps
        assertEquals(5, steps.size)
        val insight = steps[0] as? InsightStep
        assertNotNull(insight)
        assertEquals("Дыхание как якорь", insight!!.title)
        assertEquals(1, insight.examples.size)

        val timer = steps[2] as? TimerPracticeStep
        assertNotNull(timer)
        assertEquals(60, timer!!.durationSec)
        assertEquals(2, timer.cues.size)

        val reflection = steps[4] as? ReflectionStep
        assertNotNull(reflection)
        assertEquals(2, reflection!!.checklist.size)
        assertEquals(5, reflection.minWords)
    }
}
