package org.dueattendant149.bebringo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LemmaDao {
    @Upsert
    suspend fun upsertAll(lemmas: List<LemmaEntity>)

    @Query("SELECT * FROM lemmas WHERE id = :id")
    suspend fun byId(id: String): LemmaEntity?

    @Query("SELECT * FROM lemmas")
    suspend fun all(): List<LemmaEntity>

    @Query("SELECT COUNT(*) FROM lemmas")
    suspend fun count(): Int
}

@Dao
interface SrsDao {
    @Upsert
    suspend fun upsert(card: SrsCardEntity)

    @Upsert
    suspend fun upsertAll(cards: List<SrsCardEntity>)

    @Query("SELECT * FROM srs_cards")
    suspend fun all(): List<SrsCardEntity>

    @Query("SELECT * FROM srs_cards WHERE dueAt <= :now ORDER BY dueAt LIMIT :limit")
    suspend fun due(now: Long, limit: Int): List<SrsCardEntity>

    @Query("SELECT * FROM srs_cards WHERE lemmaId = :lemmaId AND direction = :direction LIMIT 1")
    suspend fun find(lemmaId: String, direction: String): SrsCardEntity?

    @Query("SELECT COUNT(*) FROM srs_cards WHERE reps > 0")
    fun learnedCount(): Flow<Int>

    @Query("SELECT DISTINCT lemmaId FROM srs_cards WHERE reps > 0")
    suspend fun learnedLemmaIds(): List<String>

    @Query("DELETE FROM srs_cards")
    suspend fun clear()
}

@Dao
interface BlockProgressDao {
    @Upsert
    suspend fun upsert(progress: BlockProgressEntity)

    @Query("SELECT * FROM block_progress WHERE blockId = :blockId")
    suspend fun byBlock(blockId: String): BlockProgressEntity?

    @Query("SELECT * FROM block_progress WHERE closed = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun unfinished(): BlockProgressEntity?

    @Query("SELECT * FROM block_progress WHERE closed = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun lastClosed(): BlockProgressEntity?

    @Query("SELECT * FROM block_progress")
    fun all(): Flow<List<BlockProgressEntity>>

    @Query("SELECT * FROM block_progress")
    suspend fun snapshot(): List<BlockProgressEntity>

    @Query("SELECT COUNT(*) FROM block_progress WHERE closed = 1")
    suspend fun closedCount(): Int

    @Query("DELETE FROM block_progress")
    suspend fun clear()
}

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY ts DESC")
    suspend fun all(): List<EventEntity>

    @Query("SELECT * FROM events WHERE type = :type ORDER BY ts DESC LIMIT 1")
    suspend fun lastOfType(type: String): EventEntity?

    @Query("SELECT COUNT(*) FROM events WHERE type = :type AND ts >= :fromTs")
    suspend fun countSince(type: String, fromTs: Long): Int

    @Query("SELECT COUNT(*) FROM events")
    fun count(): Flow<Int>

    @Query("DELETE FROM events")
    suspend fun clear()
}

@Dao
interface ActivityProfileDao {
    @Query("SELECT * FROM activity_profile WHERE id = 1")
    suspend fun get(): ActivityProfileEntity?

    @Upsert
    suspend fun upsert(entity: ActivityProfileEntity)

    @Query("DELETE FROM activity_profile")
    suspend fun clear()
}

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insert(event: TelemetryEntity)

    @Query("SELECT * FROM telemetry ORDER BY ts DESC")
    suspend fun all(): List<TelemetryEntity>

    @Query("SELECT * FROM telemetry WHERE ts >= :fromTs ORDER BY ts DESC")
    suspend fun since(fromTs: Long): List<TelemetryEntity>

    @Query("SELECT COUNT(*) FROM telemetry")
    fun count(): Flow<Int>

    @Query("DELETE FROM telemetry")
    suspend fun clear()
}

@Dao
interface StreakDao {
    @Upsert
    suspend fun upsert(day: StreakDayEntity)

    @Query("SELECT * FROM streak_days ORDER BY day DESC")
    suspend fun all(): List<StreakDayEntity>

    @Query("SELECT * FROM streak_days WHERE day = :day")
    suspend fun byDay(day: String): StreakDayEntity?

    @Query("DELETE FROM streak_days")
    suspend fun clear()
}

@Dao
interface CollectionDao {
    @Insert
    suspend fun insert(item: CollectionEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM collection WHERE textRo = :textRo)")
    suspend fun exists(textRo: String): Boolean

    @Query("SELECT * FROM collection ORDER BY createdAt DESC")
    fun all(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collection")
    suspend fun snapshot(): List<CollectionEntity>

    @Query("SELECT COUNT(*) FROM collection")
    fun count(): Flow<Int>

    @Query("DELETE FROM collection")
    suspend fun clear()
}

@Dao
interface SkillDao {
    @Upsert
    suspend fun upsertAll(skills: List<SkillEntity>)

    @Query("SELECT * FROM skills ORDER BY id")
    fun all(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills")
    suspend fun snapshot(): List<SkillEntity>

    @Query("SELECT COUNT(*) FROM skills WHERE achieved = 1")
    suspend fun achievedCount(): Int

    @Query("DELETE FROM skills")
    suspend fun clear()
}
