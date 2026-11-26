package org.dueattendant149.bebringo.ui.navigation

import org.dueattendant149.bebringo.domain.model.LessonResult

/**
 * Держит результаты частей текущего урока между экранами.
 *
 * Урок в Bebringo — это сцена из нескольких блоков. Навигационные аргументы
 * не годятся для списка ошибок, поэтому результаты частей живут здесь.
 */
object SessionHolder {
    var blockId: String = ""
    var chapterId: String = ""
    var result: LessonResult? = null

    private val blockResults = mutableListOf<Pair<String, LessonResult>>()
    private val chapterByBlock = mutableMapOf<String, String>()

    var sceneId: String = ""
    var sceneTitle: String = ""

    /** Начинает урок для сцены. Если сцена другая — прошлые результаты сбрасываются. */
    fun begin(sceneId: String, sceneTitle: String) {
        if (this.sceneId != sceneId) {
            clear()
            this.sceneId = sceneId
            this.sceneTitle = sceneTitle
        }
    }

    fun put(blockId: String, chapterId: String, result: LessonResult) {
        this.blockId = blockId
        this.chapterId = chapterId
        this.result = result
        chapterByBlock[blockId] = chapterId
        blockResults.removeAll { it.first == blockId }
        blockResults.add(blockId to result)
    }

    /** Суммарный результат всех частей урока. */
    fun combinedResult(): LessonResult? {
        if (blockResults.isEmpty()) return null
        return LessonResult(
            correct = blockResults.sumOf { it.second.correct },
            total = blockResults.sumOf { it.second.total },
            mistakes = blockResults
                .flatMap { it.second.mistakes }
                .distinctBy { it.correct }
                .take(3),
        )
    }

    fun completedBlocks(): List<Pair<String, String>> =
        blockResults.map { (id, _) -> id to chapterByBlock[id].orEmpty() }

    fun clear() {
        blockId = ""
        chapterId = ""
        result = null
        blockResults.clear()
        chapterByBlock.clear()
        sceneId = ""
        sceneTitle = ""
    }
}
