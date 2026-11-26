package org.dueattendant149.bebringo.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dueattendant149.bebringo.data.local.ActivityProfileDao
import org.dueattendant149.bebringo.data.local.ActivityProfileEntity
import org.dueattendant149.bebringo.data.local.EventDao
import org.dueattendant149.bebringo.data.local.EventEntity
import org.dueattendant149.bebringo.domain.entry.ActivityProfile
import org.dueattendant149.bebringo.domain.entry.DeviceState
import org.dueattendant149.bebringo.domain.entry.EventSnapshot
import org.dueattendant149.bebringo.domain.entry.EventTypes
import java.io.File
import java.time.Instant
import java.time.ZoneId

/** Сериализуемая форма снапшота для хранения в `events.snapshot`. */
@Serializable
private data class SnapshotJson(
    val hour: Int,
    val weekday: Int,
    val isWifi: Boolean? = null,
    val isCharging: Boolean? = null,
    val batterySaver: Boolean? = null,
    val streakLen: Int = 0,
    val dayClosed: Boolean = false,
    val dueCards: Int = 0,
    val daysSinceLastOpen: Int = -1,
    val notifsLast24h: Int = 0,
)

/** Сериализуемая форма профиля активности. */
@Serializable
private data class ProfileJson(
    val histogram: List<Double> = emptyList(),
    val weekdayObserved: List<Double> = emptyList(),
    val firstOpenHours: Map<String, Int> = emptyMap(),
    val lastDecayAt: Long = 0L,
)

/** Срез состояния для снапшота; собирается вызывающей стороной. */
data class SnapshotState(
    val streakLen: Int = 0,
    val dayClosed: Boolean = false,
    val dueCards: Int = 0,
)

/**
 * Слой событий входа: append-only журнал с контекстными снапшотами
 * и инкрементальный профиль активности.
 *
 * Каждое событие фиксируется вместе с контекстом (час, день недели, сеть,
 * стрик, карточки) — это сырые признаки для модели «отправить / помолчать».
 * Профиль обновляется только на `app_open`: это ядро правила «молчать,
 * если придёт сам».
 */
class EventRepository(
    private val eventDao: EventDao,
    private val profileDao: ActivityProfileDao,
    private val exportDir: File,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Пишет событие; вызывается из любого места приложения. */
    suspend fun record(
        type: String,
        sessionId: String? = null,
        payload: Map<String, String> = emptyMap(),
        device: DeviceState = DeviceState(),
        state: SnapshotState = SnapshotState(),
        timestamp: Long = System.currentTimeMillis(),
    ) {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
        val lastOpenTs = eventDao.lastOfType(EventTypes.APP_OPEN)?.ts
        val daysSinceLastOpen = lastOpenTs
            ?.let { ts -> ActivityProfile.dayOf(ts).until(dateTime.toLocalDate()).days }
            ?: -1

        val snapshot = EventSnapshot(
            hour = dateTime.hour,
            weekday = dateTime.dayOfWeek.value,
            isWifi = device.isWifi,
            isCharging = device.isCharging,
            batterySaver = device.batterySaver,
            streakLen = state.streakLen,
            dayClosed = state.dayClosed,
            dueCards = state.dueCards,
            daysSinceLastOpen = daysSinceLastOpen,
            notifsLast24h = eventDao.countSince(EventTypes.NOTIF_SHOWN, timestamp - DAY_MS),
        )

        eventDao.insert(
            EventEntity(
                ts = timestamp,
                type = type,
                sessionId = sessionId,
                payload = if (payload.isEmpty()) null else json.encodeToString(payload),
                snapshot = json.encodeToString(snapshot.toSerializable()),
            ),
        )

        if (type == EventTypes.APP_OPEN) {
            val profile = profile().recordOpen(timestamp)
            profileDao.upsert(
                ActivityProfileEntity(
                    id = 1,
                    profileJson = json.encodeToString(profile.toSerializable()),
                    updatedAt = timestamp,
                ),
            )
        }
    }

    /** Текущий профиль активности (с затуханием на текущий момент). */
    suspend fun profile(now: Long = System.currentTimeMillis()): ActivityProfile {
        val entity = profileDao.get() ?: return ActivityProfile.empty()
        val stored = runCatching {
            json.decodeFromString<ProfileJson>(entity.profileJson).toDomain()
        }.getOrDefault(ActivityProfile.empty())
        return stored.decayed(now)
    }

    fun count() = eventDao.count()

    suspend fun all(): List<EventEntity> = eventDao.all()

    suspend fun clear() {
        eventDao.clear()
        profileDao.clear()
    }

    /** CSV-выгрузка событий со снапшотами: сырые данные для внешних экспериментов. */
    suspend fun exportCsv(): File {
        val events = eventDao.all().sortedBy { it.ts }
        exportDir.mkdirs()
        val file = File(exportDir, "bebringo-events.csv")
        file.printWriter().use { writer ->
            writer.println(
                "ts,iso_time,type,session_id,hour,weekday,is_wifi,is_charging," +
                    "battery_saver,streak_len,day_closed,due_cards,days_since_last_open," +
                    "notifs_last_24h,payload",
            )
            events.forEach { event ->
                val snap = runCatching {
                    json.decodeFromString<SnapshotJson>(event.snapshot ?: "")
                }.getOrNull()
                val iso = Instant.ofEpochMilli(event.ts).atZone(ZoneId.systemDefault())
                writer.println(
                    listOf(
                        event.ts,
                        iso,
                        event.type,
                        event.sessionId ?: "",
                        snap?.hour ?: "",
                        snap?.weekday ?: "",
                        snap?.isWifi ?: "",
                        snap?.isCharging ?: "",
                        snap?.batterySaver ?: "",
                        snap?.streakLen ?: "",
                        snap?.dayClosed ?: "",
                        snap?.dueCards ?: "",
                        snap?.daysSinceLastOpen ?: "",
                        snap?.notifsLast24h ?: "",
                        (event.payload ?: "").csvEscape(),
                    ).joinToString(","),
                )
            }
        }
        return file
    }

    private fun String.csvEscape(): String {
        if (none { it == ',' || it == '"' || it == '\n' }) return this
        return "\"" + replace("\"", "\"\"") + "\""
    }

    private fun EventSnapshot.toSerializable() = SnapshotJson(
        hour = hour,
        weekday = weekday,
        isWifi = isWifi,
        isCharging = isCharging,
        batterySaver = batterySaver,
        streakLen = streakLen,
        dayClosed = dayClosed,
        dueCards = dueCards,
        daysSinceLastOpen = daysSinceLastOpen,
        notifsLast24h = notifsLast24h,
    )

    private fun SnapshotJson.toDomain() = EventSnapshot(
        hour = hour,
        weekday = weekday,
        isWifi = isWifi,
        isCharging = isCharging,
        batterySaver = batterySaver,
        streakLen = streakLen,
        dayClosed = dayClosed,
        dueCards = dueCards,
        daysSinceLastOpen = daysSinceLastOpen,
        notifsLast24h = notifsLast24h,
    )

    private fun ActivityProfile.toSerializable() = ProfileJson(
        histogram = histogram,
        weekdayObserved = weekdayObserved,
        firstOpenHours = firstOpenHours,
        lastDecayAt = lastDecayAt,
    )

    private fun ProfileJson.toDomain() = ActivityProfile(
        histogram = histogram.ifEmpty { List(ActivityProfile.HISTOGRAM_SIZE) { 0.0 } },
        weekdayObserved = weekdayObserved.ifEmpty { List(ActivityProfile.DAYS_PER_WEEK) { 0.0 } },
        firstOpenHours = firstOpenHours,
        lastDecayAt = lastDecayAt,
    )

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
