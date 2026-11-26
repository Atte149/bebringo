package org.dueattendant149.bebringo.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.dueattendant149.bebringo.data.local.BebringoDatabase
import org.dueattendant149.bebringo.data.local.BlockProgressEntity
import org.dueattendant149.bebringo.data.local.CollectionEntity
import org.dueattendant149.bebringo.data.local.SkillEntity
import org.dueattendant149.bebringo.data.local.SrsCardEntity
import org.dueattendant149.bebringo.data.local.StreakDayEntity
import org.dueattendant149.bebringo.data.local.TelemetryEntity
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class BackupPayload(
    val version: Int = 2,
    val createdAt: Long,
    val telemetry: List<TelemetryRecord> = emptyList(),
    val progress: List<ProgressRecord> = emptyList(),
    val srs: List<SrsRecord> = emptyList(),
    val streak: List<StreakRecord> = emptyList(),
    val collection: List<CollectionRecord> = emptyList(),
    val skills: List<SkillRecord> = emptyList(),
)

@Serializable
data class TelemetryRecord(
    val ts: Long, val sessionId: String, val blockId: String, val stepIndex: Int,
    val exerciseType: String, val lemma: String, val result: String, val errorType: String?,
    val supportLevel: Int, val responseMs: Long, val sessionLenMin: Int,
    val streakAlive: Boolean, val readingId: String?, val chatTurn: Int?,
)

@Serializable
data class ProgressRecord(
    val blockId: String, val chapterId: String, val stepIndex: Int,
    val closed: Boolean, val correct: Int, val total: Int,
)

@Serializable
data class SrsRecord(
    val id: String, val lemmaId: String, val direction: String,
    val stability: Double, val difficulty: Double, val dueAt: Long,
    val lastReviewAt: Long?, val reps: Int, val lapses: Int,
)

@Serializable
data class StreakRecord(
    val day: String,
    val closedBlocks: Int,
    val usedFreeze: Boolean,
    val repaired: Boolean = false,
)

@Serializable
data class CollectionRecord(val textRo: String, val translationRu: String?)

@Serializable
data class SkillRecord(val id: String, val titleRu: String, val achieved: Boolean)

/**
 * Резервная копия.
 *
 * Два пути: файл (ручной экспорт/импорт) и WebDAV (автоматически раз в сутки).
 * Все дисковые и сетевые операции выполняются на `Dispatchers.IO`, а импорт — в транзакции Room.
 */
class BackupManager(
    private val context: Context,
    private val database: BebringoDatabase,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val payload = collect()
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "bebringo-backup-${payload.createdAt}.json")
        file.writeText(json.encodeToString(BackupPayload.serializer(), payload))
        file
    }

    /** Импорт из произвольного файла, выбранного пользователем. */
    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Не удалось прочитать файл")
        val payload = json.decodeFromString(BackupPayload.serializer(), raw)
        apply(payload)
    }

    /** Импорт из локального файла бэкапа. */
    suspend fun importFile(file: File) = withContext(Dispatchers.IO) {
        val raw = file.readText()
        val payload = json.decodeFromString(BackupPayload.serializer(), raw)
        apply(payload)
    }

    /** Список сохранённых локальных копий. */
    suspend fun listLocalBackups(): List<File> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "backups")
        dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    /**
     * Отправляет копию на WebDAV.
     * URL должен указывать на файл, например `.../remote.php/dav/files/user/bebringo.json`.
     */
    suspend fun uploadToWebdav(url: String, user: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        runCatching {
            val payload = collect()
            val body = json.encodeToString(BackupPayload.serializer(), payload)
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .apply { if (user.isNotBlank()) header("Authorization", Credentials.basic(user, password)) }
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    /** Оставляет семь последних локальных копий. */
    suspend fun pruneLocal(keep: Int = 7) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "backups")
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return@withContext
        files.drop(keep).forEach { it.delete() }
    }

    private suspend fun collect(): BackupPayload {
        val telemetry = database.telemetryDao().all().map {
            TelemetryRecord(
                it.ts, it.sessionId, it.blockId, it.stepIndex, it.exerciseType,
                it.lemma, it.result, it.errorType, it.supportLevel, it.responseMs,
                it.sessionLenMin, it.streakAlive, it.readingId, it.chatTurn,
            )
        }
        val progress = database.blockProgressDao().snapshot().map {
            ProgressRecord(it.blockId, it.chapterId, it.stepIndex, it.closed, it.correct, it.total)
        }
        val collection = database.collectionDao().snapshot().map {
            CollectionRecord(it.textRo, it.translationRu)
        }
        val srs = database.srsDao().all().map {
            SrsRecord(it.id, it.lemmaId, it.direction, it.stability, it.difficulty, it.dueAt, it.lastReviewAt, it.reps, it.lapses)
        }
        val streak = database.streakDao().all().map {
            StreakRecord(it.day, it.closedBlocks, it.usedFreeze, it.repaired)
        }
        val skills = database.skillDao().snapshot().map {
            SkillRecord(it.id, it.titleRu, it.achieved)
        }

        return BackupPayload(
            createdAt = System.currentTimeMillis(),
            telemetry = telemetry,
            srs = srs,
            streak = streak,
            progress = progress,
            collection = collection,
            skills = skills,
        )
    }

    private suspend fun apply(payload: BackupPayload) {
        database.withTransaction {
            database.telemetryDao().clear()
            database.srsDao().clear()
            database.blockProgressDao().clear()
            database.collectionDao().clear()
            database.streakDao().clear()
            if (payload.skills.isNotEmpty()) {
                database.skillDao().clear()
            }

            payload.telemetry.forEach { record ->
                database.telemetryDao().insert(
                    TelemetryEntity(
                        ts = record.ts, sessionId = record.sessionId, blockId = record.blockId,
                        stepIndex = record.stepIndex, exerciseType = record.exerciseType,
                        lemma = record.lemma, result = record.result, errorType = record.errorType,
                        supportLevel = record.supportLevel, responseMs = record.responseMs,
                        sessionLenMin = record.sessionLenMin, streakAlive = record.streakAlive,
                        readingId = record.readingId, chatTurn = record.chatTurn,
                    ),
                )
            }
            payload.srs.forEach { record ->
                database.srsDao().upsert(
                    SrsCardEntity(
                        id = record.id, lemmaId = record.lemmaId, direction = record.direction,
                        stability = record.stability, difficulty = record.difficulty, dueAt = record.dueAt,
                        lastReviewAt = record.lastReviewAt, reps = record.reps, lapses = record.lapses,
                    ),
                )
            }
            payload.progress.forEach { record ->
                database.blockProgressDao().upsert(
                    BlockProgressEntity(
                        blockId = record.blockId, chapterId = record.chapterId, stepIndex = record.stepIndex,
                        closed = record.closed, correct = record.correct, total = record.total,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            payload.collection.distinctBy { it.textRo.trim() }.forEach { record ->
                database.collectionDao().insert(
                    CollectionEntity(
                        textRo = record.textRo.trim(), translationRu = record.translationRu?.trim(),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
            payload.streak.forEach { record ->
                database.streakDao().upsert(
                    StreakDayEntity(
                        day = record.day, closedBlocks = record.closedBlocks,
                        usedFreeze = record.usedFreeze, repaired = record.repaired,
                    ),
                )
            }
            if (payload.skills.isNotEmpty()) {
                database.skillDao().upsertAll(
                    payload.skills.map { SkillEntity(it.id, it.titleRu, it.achieved) },
                )
            }
        }
    }
}
