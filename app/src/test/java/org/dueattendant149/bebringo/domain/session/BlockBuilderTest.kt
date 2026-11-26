package org.dueattendant149.bebringo.domain.session

import org.dueattendant149.bebringo.domain.model.AssembleStep
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.Gender
import org.dueattendant149.bebringo.domain.model.Lemma
import org.dueattendant149.bebringo.domain.model.ListeningStep
import org.dueattendant149.bebringo.domain.model.PartOfSpeech
import org.dueattendant149.bebringo.domain.model.RecognitionStep
import org.dueattendant149.bebringo.domain.srs.CardDirection
import org.dueattendant149.bebringo.domain.srs.SrsCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockBuilderTest {

    private val lemma = Lemma(
        id = "l1",
        lemma = "tren",
        translationRu = "поезд",
        pos = PartOfSpeech.NOUN,
        gender = Gender.NEUTER,
        plural = "trenuri",
    )

    private val builder = DefaultBlockBuilder(lemmaProvider = { id -> if (id == "l1") lemma else null })

    private fun validBlock() = Block(
        id = "b1",
        titleRo = "La gară",
        steps = listOf(
            ListeningStep("1", "Bună", "Привет", listOf("Привет", "Пока", "Да", "Нет")),
            RecognitionStep("2", "tren", "поезд", listOf("поезд", "билет", "вокзал", "касса")),
            AssembleStep("3", "Я хочу билет", listOf("vreau", "un", "bilet"), "Vreau un bilet"),
            RecognitionStep("4", "bilet", "билет", listOf("билет", "поезд", "вокзал", "касса")),
            RecognitionStep("5", "gară", "вокзал", listOf("вокзал", "поезд", "билет", "касса")),
        ),
    )

    @Test
    fun `правильный блок не имеет нарушений`() {
        assertEquals(emptyList<String>(), builder.validate(validBlock()))
    }

    @Test
    fun `блок без продуктивного шага невалиден`() {
        val block = validBlock().let { block ->
            block.copy(steps = block.steps.map { step ->
                if (step.isProductive) RecognitionStep("x", "tren", "поезд", listOf("поезд")) else step
            })
        }
        assertTrue(builder.validate(block).any { it.contains("продуктивного") })
    }

    @Test
    fun `блок без аудио невалиден`() {
        val block = validBlock().let { block ->
            block.copy(steps = block.steps.map { step ->
                if (step.isAudio) RecognitionStep("x", "tren", "поезд", listOf("поезд")) else step
            })
        }
        assertTrue(builder.validate(block).any { it.contains("аудио") })
    }

    @Test
    fun `блок неверного размера невалиден`() {
        val block = validBlock().let { it.copy(steps = it.steps.take(3)) }
        assertTrue(builder.validate(block).any { it.contains("шагов") })
    }

    @Test
    fun `повторения подменяют шаги и не ломают размер`() {
        val due = listOf(
            SrsCard("c1", "l1", CardDirection.RECEPTIVE, 1.0, 5.0, 0),
            SrsCard("c2", "l1", CardDirection.PRODUCTIVE, 1.0, 5.0, 0),
        )

        val built = builder.build(validBlock(), due)

        assertEquals(5, built.steps.size)
        assertTrue(builder.validate(built).isEmpty())
        assertTrue(built.steps.any { it.id == "review-c1" || it.id == "review-c2" })
    }

    @Test
    fun `без повторов блок остаётся авторским`() {
        val authored = validBlock()
        assertEquals(authored, builder.build(authored, emptyList()))
    }

    @Test
    fun `сессия повторения не дублирует лемму`() {
        val second = lemma.copy(id = "l2", lemma = "bilet", translationRu = "билет")
        val reviewBuilder = DefaultBlockBuilder(
            lemmaProvider = { id -> if (id == "l1") lemma else second },
            allLemmasProvider = { listOf(lemma, second) },
        )
        val due = listOf(
            SrsCard("c1", "l1", CardDirection.RECEPTIVE, 1.0, 5.0, 0),
            SrsCard("c2", "l1", CardDirection.PRODUCTIVE, 1.0, 5.0, 0),
            SrsCard("c3", "l2", CardDirection.RECEPTIVE, 1.0, 5.0, 0),
        )

        val review = reviewBuilder.buildReviewBlock(due)

        assertEquals(2, review.steps.size)
        assertEquals(listOf("l1", "l2"), review.steps.map { it.lemmaId })
    }
}
