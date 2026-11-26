package org.dueattendant149.bebringo.domain.session

import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.Lemma
import org.dueattendant149.bebringo.domain.model.RecognitionStep
import org.dueattendant149.bebringo.domain.model.Step
import org.dueattendant149.bebringo.domain.model.TypeInStep
import org.dueattendant149.bebringo.domain.srs.CardDirection
import org.dueattendant149.bebringo.domain.srs.SrsCard

/**
 * Сборка блока на день.
 *
 * Авторский блок — это сценарная основа. Планировщик может подменить до двух шагов
 * на повторение из SRS, но не имеет права сломать инварианты.
 */
interface BlockBuilder {

    /**
     * @param authored авторский блок сцены
     * @param due карточки, которые пора повторить
     */
    fun build(authored: Block, due: List<SrsCard>): Block

    /** Проверка инвариантов. Возвращает список нарушений, пустой — если всё в порядке. */
    fun validate(block: Block): List<String>
}

class DefaultBlockBuilder(
    private val lemmaProvider: (String) -> Lemma?,
    private val maxReviewSteps: Int = 2,
    private val allLemmasProvider: () -> Collection<Lemma> = { emptyList() },
) : BlockBuilder {

    override fun build(authored: Block, due: List<SrsCard>): Block {
        if (due.isEmpty()) return authored

        val reviewSteps = due
            .take(maxReviewSteps)
            .mapNotNull { card -> reviewStepFor(card) }

        if (reviewSteps.isEmpty()) return authored

        // Подменяем шаги узнавания/ввода с конца блока, сохраняя инварианты
        // (минимум 1 продуктивный и минимум 1 аудиошаг).
        val result = authored.steps.toMutableList()
        var replaced = 0
        for (index in result.indices.reversed()) {
            if (replaced >= reviewSteps.size) break
            val step = result[index]
            if (step is RecognitionStep || step is TypeInStep) {
                val candidate = reviewSteps[replaced]
                val productiveAfter = result.indices.count { i ->
                    val s = if (i == index) candidate else result[i]
                    s.isProductive
                }
                val audioAfter = result.indices.count { i ->
                    val s = if (i == index) candidate else result[i]
                    s.isAudio
                }
                if (productiveAfter >= 1 && audioAfter >= 1) {
                    result[index] = candidate
                    replaced++
                }
            }
        }

        return authored.copy(steps = result)
    }

    /**
     * Отдельная сессия повторения: только созревшие карточки, без новых шагов урока.
     * Одна карточка на лемму, чтобы не показывать одно слово дважды подряд.
     */
    fun buildReviewBlock(cards: List<SrsCard>, title: String = "Повторение"): Block {
        val steps = cards
            .distinctBy { it.lemmaId }
            .take(MAX_REVIEW_STEPS)
            .mapNotNull { card -> reviewStepFor(card) }
        return Block(
            id = "review-${System.currentTimeMillis()}",
            titleRo = title,
            steps = steps,
        )
    }

    override fun validate(block: Block): List<String> = buildList {
        if (block.steps.size != REQUIRED_STEPS) {
            add("В блоке ${block.steps.size} шагов, нужно $REQUIRED_STEPS")
        }
        if (block.steps.none { it.isProductive }) {
            add("Нет продуктивного шага: блок нельзя закрыть одним узнаванием")
        }
        if (block.steps.none { it.isAudio }) {
            add("Нет аудиошага: слух перестанет тренироваться")
        }
    }

    private fun reviewStepFor(card: SrsCard): Step? {
        val lemma = lemmaProvider(card.lemmaId) ?: return null
        return when (card.direction) {
            CardDirection.RECEPTIVE -> RecognitionStep(
                id = "review-${card.id}",
                wordRo = lemma.lemma,
                translationRu = lemma.translationRu,
                optionsRu = optionsFor(lemma),
                lemmaId = lemma.id,
            )

            CardDirection.PRODUCTIVE -> TypeInStep(
                id = "review-${card.id}",
                promptRu = lemma.translationRu,
                answerRo = lemma.lemma,
                lemmaId = lemma.id,
            )
        }
    }

    private fun optionsFor(lemma: Lemma): List<String> {
        val pool = allLemmasProvider()
        val samePosDistractors = pool
            .filter { it.id != lemma.id && it.pos == lemma.pos && it.translationRu != lemma.translationRu }
            .map { it.translationRu }
            .distinct()
            .shuffled()
        val otherDistractors = pool
            .filter { it.id != lemma.id && it.translationRu != lemma.translationRu }
            .map { it.translationRu }
            .distinct()
            .shuffled()
        val fallback = listOf("поезд", "билет", "вокзал", "кофе", "дом", "время")

        return (listOf(lemma.translationRu) + samePosDistractors + otherDistractors + fallback)
            .distinct()
            .take(4)
            .shuffled()
    }

    private companion object {
        const val REQUIRED_STEPS = 5
        const val MAX_REVIEW_STEPS = 10
    }
}
