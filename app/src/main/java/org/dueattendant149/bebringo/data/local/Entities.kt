package org.dueattendant149.bebringo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Событие слоя рекомендации входа (append-only).
 *
 * `payload` и `snapshot` — JSON; снапшот повторяет [org.dueattendant149.bebringo.domain.entry.EventSnapshot].
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val type: String,
    val sessionId: String?,
    val payload: String?,
    val snapshot: String?,
)

/**
 * Профиль активности: единственная строка с сериализованным
 * [org.dueattendant149.bebringo.domain.entry.ActivityProfile].
 */
@Entity(tableName = "activity_profile")
data class ActivityProfileEntity(
    @PrimaryKey val id: Int = 1,
    val profileJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "lemmas")
data class LemmaEntity(
    @PrimaryKey val id: String,
    val lemma: String,
    val translationRu: String,
    val pos: String,
    val gender: String?,
    val plural: String?,
    val exampleRo: String?,
    val exampleRu: String?,
    val tags: String,
)

@Entity(tableName = "srs_cards")
data class SrsCardEntity(
    @PrimaryKey val id: String,
    val lemmaId: String,
    val direction: String,
    val stability: Double,
    val difficulty: Double,
    val dueAt: Long,
    val lastReviewAt: Long?,
    val reps: Int,
    val lapses: Int,
)

@Entity(tableName = "block_progress")
data class BlockProgressEntity(
    @PrimaryKey val blockId: String,
    val chapterId: String,
    val stepIndex: Int,
    val closed: Boolean,
    val correct: Int,
    val total: Int,
    val updatedAt: Long,
)

@Entity(tableName = "telemetry")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val sessionId: String,
    val blockId: String,
    val stepIndex: Int,
    val exerciseType: String,
    val lemma: String,
    val result: String,
    val errorType: String?,
    val supportLevel: Int,
    val responseMs: Long,
    val sessionLenMin: Int,
    val streakAlive: Boolean,
    val readingId: String?,
    val chatTurn: Int?,
)

@Entity(tableName = "streak_days")
data class StreakDayEntity(
    @PrimaryKey val day: String,
    val closedBlocks: Int,
    val usedFreeze: Boolean,
    val repaired: Boolean = false,
)

@Entity(tableName = "collection")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val textRo: String,
    val translationRu: String?,
    val createdAt: Long,
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val titleRu: String,
    val achieved: Boolean,
)
