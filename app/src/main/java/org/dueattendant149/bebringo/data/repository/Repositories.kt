package org.dueattendant149.bebringo.data.repository

import org.dueattendant149.bebringo.data.content.CourseLoader
import org.dueattendant149.bebringo.data.content.LoadedCourse
import org.dueattendant149.bebringo.data.local.BlockProgressDao
import org.dueattendant149.bebringo.data.local.BlockProgressEntity
import org.dueattendant149.bebringo.data.local.CollectionDao
import org.dueattendant149.bebringo.data.local.CollectionEntity
import org.dueattendant149.bebringo.data.local.LemmaDao
import org.dueattendant149.bebringo.data.local.LemmaEntity
import org.dueattendant149.bebringo.data.local.SkillDao
import org.dueattendant149.bebringo.data.local.SkillEntity
import org.dueattendant149.bebringo.data.local.SrsCardEntity
import org.dueattendant149.bebringo.data.local.SrsDao
import org.dueattendant149.bebringo.data.local.StreakDao
import org.dueattendant149.bebringo.data.local.StreakDayEntity
import org.dueattendant149.bebringo.data.local.TelemetryDao
import org.dueattendant149.bebringo.data.local.TelemetryEntity
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.ErrorType
import org.dueattendant149.bebringo.domain.model.Gender
import org.dueattendant149.bebringo.domain.model.Lemma
import org.dueattendant149.bebringo.domain.model.PartOfSpeech
import org.dueattendant149.bebringo.domain.srs.CardDirection
import org.dueattendant149.bebringo.domain.srs.Rating
import org.dueattendant149.bebringo.domain.srs.Scheduler
import org.dueattendant149.bebringo.domain.srs.SrsCard
import org.dueattendant149.bebringo.domain.streak.StreakCalculator
import org.dueattendant149.bebringo.domain.streak.StreakDay
import org.dueattendant149.bebringo.domain.streak.StreakResult
import java.io.File
import java.time.LocalDate
import kotlin.math.ceil

/** Курс и модульные треки: разбираются и синхронизируют леммы/концепты с БД. */
class CourseRepository(
    private val loader: CourseLoader,
    private val lemmaDao: LemmaDao,
) {
    @Volatile
    private var cached: LoadedCourse? = null

    suspend fun course(): LoadedCourse {
        cached?.let { return it }
        val loaded = loader.loadAllTracks()
        lemmaDao.upsertAll(loaded.lemmas.values.map { it.toEntity() })
        cached = loaded
        return loaded
    }

    suspend fun reload(): LoadedCourse {
        cached = null
        return course()
    }

    suspend fun lemmaMap(): Map<String, Lemma> = course().lemmas

    suspend fun block(id: String): Block? = course().blockById(id)

    suspend fun firstBlock(): Block? = course().blocks.firstOrNull()

    suspend fun nextBlock(after: String?): Block? {
        val blocks = course().blocks
        if (after == null) return blocks.firstOrNull()
        val index = blocks.indexOfFirst { it.id == after }
        return blocks.getOrNull(index + 1) ?: blocks.firstOrNull()
    }

    /** Возвращает первый ещё не закрытый блок курса (с учётом фильтра трека). */
    suspend fun nextOpenBlock(closedBlockIds: Set<String>, trackFilter: String = "all"): Block? {
        val blocks = course().blocksForTrack(trackFilter)
        return blocks.firstOrNull { it.id !in closedBlockIds } ?: blocks.firstOrNull()
    }
}

/** Прогресс по блокам. */
class ProgressRepository(private val dao: BlockProgressDao) {

    suspend fun unfinished(): BlockProgressEntity? = dao.unfinished()

    suspend fun lastClosed(): BlockProgressEntity? = dao.lastClosed()

    suspend fun closedBlockIds(): Set<String> =
        dao.snapshot().filter { it.closed }.map { it.blockId }.toSet()

    suspend fun byBlock(blockId: String): BlockProgressEntity? = dao.byBlock(blockId)

    suspend fun save(
        blockId: String,
        chapterId: String,
        stepIndex: Int,
        closed: Boolean,
        correct: Int,
        total: Int,
    ) {
        dao.upsert(
            BlockProgressEntity(
                blockId = blockId,
                chapterId = chapterId,
                stepIndex = stepIndex,
                closed = closed,
                correct = correct,
                total = total,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun closedCount(): Int = dao.closedCount()

    fun all() = dao.all()
}

/** Карточки повторения FSRS: две на лемму, рецептивная и продуктивная. */
class SrsRepository(
    private val dao: SrsDao,
    private val scheduler: Scheduler,
) {
    suspend fun ensureCards(lemmaIds: List<String>) {
        lemmaIds.distinct().forEach { lemmaId ->
            CardDirection.entries.forEach { direction ->
                if (dao.find(lemmaId, direction.name) == null) {
                    dao.upsert(
                        SrsCardEntity(
                            id = "$lemmaId-${direction.name}",
                            lemmaId = lemmaId,
                            direction = direction.name,
                            stability = 0.0,
                            difficulty = 0.0,
                            dueAt = System.currentTimeMillis(),
                            lastReviewAt = null,
                            reps = 0,
                            lapses = 0,
                        ),
                    )
                }
            }
        }
    }

    suspend fun due(now: Long, limit: Int): List<SrsCard> {
        val allCards = dao.all().map { it.toDomain() }
        // Сначала карточки, которые уже побывали в работе и срок повторения которых наступил,
        // отсортированные планировщиком FSRS по минимальному retrievability.
        val reviewedDue = scheduler.dueCards(allCards.filter { !it.isNew }, now, limit)
        if (reviewedDue.size >= limit) return reviewedDue
        val remaining = limit - reviewedDue.size
        val newDue = scheduler.dueCards(allCards.filter { it.isNew }, now, remaining)
        return reviewedDue + newDue
    }

    suspend fun review(card: SrsCard, rating: Rating, now: Long) {
        val updated = scheduler.review(card, rating, now)
        dao.upsert(updated.toEntity())
    }

    suspend fun recordReviewForLemma(
        lemmaId: String,
        direction: CardDirection,
        rating: Rating,
        now: Long = System.currentTimeMillis(),
    ) {
        ensureCards(listOf(lemmaId))
        val entity = dao.find(lemmaId, direction.name) ?: return
        review(entity.toDomain(), rating, now)
    }

    fun learnedCount() = dao.learnedCount()

    /** Леммы, у которых уже была любая карточка: по ним считается знакомость чтения. */
    suspend fun learnedLemmaIds(): List<String> = dao.learnedLemmaIds()

    private fun SrsCardEntity.toDomain() = SrsCard(
        id = id,
        lemmaId = lemmaId,
        direction = CardDirection.valueOf(direction),
        stability = stability,
        difficulty = difficulty,
        dueAt = dueAt,
        lastReviewAt = lastReviewAt,
        reps = reps,
        lapses = lapses,
    )

    private fun SrsCard.toEntity() = SrsCardEntity(
        id = id,
        lemmaId = lemmaId,
        direction = direction.name,
        stability = stability,
        difficulty = difficulty,
        dueAt = dueAt,
        lastReviewAt = lastReviewAt,
        reps = reps,
        lapses = lapses,
    )
}

/** Стрик по дням. */
class StreakRepository(private val dao: StreakDao) {

    suspend fun markBlockClosed(today: LocalDate) {
        val key = today.toString()
        val existing = dao.byDay(key)
        dao.upsert(
            StreakDayEntity(
                day = key,
                closedBlocks = (existing?.closedBlocks ?: 0) + 1,
                usedFreeze = existing?.usedFreeze ?: false,
                repaired = existing?.repaired ?: false,
            ),
        )
    }

    suspend fun useFreeze(day: LocalDate) {
        val key = day.toString()
        val existing = dao.byDay(key)
        dao.upsert(
            StreakDayEntity(
                day = key,
                closedBlocks = existing?.closedBlocks ?: 0,
                usedFreeze = true,
                repaired = existing?.repaired ?: false,
            ),
        )
    }

    /**
     * Ремонт разрыва: вчерашний день закрывается задним числом.
     * Допустим только в пределах 48 часов, то есть ровно за вчера.
     */
    suspend fun repair(day: LocalDate) {
        val key = day.toString()
        val existing = dao.byDay(key)
        dao.upsert(
            StreakDayEntity(
                day = key,
                closedBlocks = existing?.closedBlocks ?: 0,
                usedFreeze = existing?.usedFreeze ?: false,
                repaired = true,
            ),
        )
    }

    suspend fun allDays(): List<StreakDay> =
        dao.all().map { entry ->
            StreakDay(
                day = LocalDate.parse(entry.day),
                closedBlocks = entry.closedBlocks,
                usedFreeze = entry.usedFreeze,
                repaired = entry.repaired,
            )
        }

    suspend fun state(today: LocalDate): StreakResult {
        val days = allDays()
        return StreakCalculator.current(days, today)
    }
}

/** Телеметрия: локально, с выгрузкой в CSV. */
class TelemetryRepository(
    private val dao: TelemetryDao,
    private val exportDir: File,
) {
    suspend fun record(
        sessionId: String,
        blockId: String,
        stepIndex: Int,
        exerciseType: String,
        lemma: String,
        result: String,
        errorType: ErrorType?,
        supportLevel: Int,
        responseMs: Long,
        sessionLenMin: Int,
        streakAlive: Boolean,
        readingId: String? = null,
        chatTurn: Int? = null,
    ) {
        dao.insert(
            TelemetryEntity(
                ts = System.currentTimeMillis(),
                sessionId = sessionId,
                blockId = blockId,
                stepIndex = stepIndex,
                exerciseType = exerciseType,
                lemma = lemma,
                result = result,
                errorType = errorType?.csv,
                supportLevel = supportLevel,
                responseMs = responseMs,
                sessionLenMin = sessionLenMin,
                streakAlive = streakAlive,
                readingId = readingId,
                chatTurn = chatTurn,
            ),
        )
    }

    fun count() = dao.count()

    suspend fun minutesSince(fromTs: Long, blocksToday: Int = 0): Int {
        val events = dao.since(fromTs)
        if (events.isEmpty() && blocksToday == 0) return 0
        val activeMs = events.sumOf { it.responseMs.coerceIn(1_500L, 60_000L) }
        val fromTelemetry = ceil(activeMs / 60_000.0).toInt()
        return maxOf(fromTelemetry, blocksToday * 3)
    }

    suspend fun exportCsv(): File {
        val events = dao.all()
        exportDir.mkdirs()
        val file = File(exportDir, "bebringo-telemetry.csv")
        file.printWriter().use { writer ->
            writer.println(
                "ts,session_id,block_id,step_index,exercise_type,lemma,result," +
                    "error_type,support_level,response_ms,session_len_min,streak_alive,reading_id,chat_turn",
            )
            events.forEach { event ->
                writer.println(
                    listOf(
                        event.ts, event.sessionId, event.blockId, event.stepIndex,
                        event.exerciseType, event.lemma.csvEscape(), event.result,
                        event.errorType ?: "", event.supportLevel, event.responseMs,
                        event.sessionLenMin, event.streakAlive, event.readingId ?: "",
                        event.chatTurn ?: "",
                    ).joinToString(","),
                )
            }
        }
        return file
    }

    suspend fun clear() = dao.clear()

    private fun String.csvEscape(): String {
        if (none { it == ',' || it == '"' || it == '\n' }) return this
        return "\"" + replace("\"", "\"\"") + "\""
    }
}

/** Коллекция собственных фраз (без дубликатов). */
class CollectionRepository(private val dao: CollectionDao) {
    fun all() = dao.all()
    fun count() = dao.count()
    suspend fun add(textRo: String, translationRu: String? = null) {
        val trimmed = textRo.trim()
        if (trimmed.isEmpty() || dao.exists(trimmed)) return
        dao.insert(
            CollectionEntity(
                textRo = trimmed,
                translationRu = translationRu?.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}

/** Умения. */
class SkillRepository(private val dao: SkillDao) {
    fun all() = dao.all()
    suspend fun seed(skills: List<Pair<String, String>>) {
        syncWithProgress(skills, closedBlocks = 0)
    }

    suspend fun syncWithProgress(skills: List<Pair<String, String>>, closedBlocks: Int) {
        val unlockedCount = (closedBlocks / 2).coerceIn(0, skills.size)
        dao.upsertAll(skills.mapIndexed { index, (id, title) ->
            SkillEntity(id = id, titleRu = title, achieved = index < unlockedCount)
        })
    }

    suspend fun markAchieved(skillId: String, titleRu: String) {
        dao.upsertAll(listOf(SkillEntity(id = skillId, titleRu = titleRu, achieved = true)))
    }
}

internal fun Lemma.toEntity() = LemmaEntity(
    id = id,
    lemma = lemma,
    translationRu = translationRu,
    pos = pos.name.lowercase(),
    gender = gender?.name?.lowercase()?.take(1),
    plural = plural,
    exampleRo = exampleRo,
    exampleRu = exampleRu,
    tags = tags.joinToString(","),
)

internal fun LemmaEntity.toDomain() = Lemma(
    id = id,
    lemma = lemma,
    translationRu = translationRu,
    pos = runCatching { PartOfSpeech.valueOf(pos.uppercase()) }.getOrDefault(PartOfSpeech.NOUN),
    gender = when (gender?.lowercase()) {
        "m", "masculine" -> Gender.MASCULINE
        "f", "feminine" -> Gender.FEMININE
        "n", "neuter" -> Gender.NEUTER
        else -> null
    },
    plural = plural,
    exampleRo = exampleRo,
    exampleRu = exampleRu,
    tags = tags.split(',').filter { it.isNotBlank() }.toSet(),
)
