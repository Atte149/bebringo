package org.dueattendant149.bebringo.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.data.backup.BackupManager
import org.dueattendant149.bebringo.data.content.CourseLoader
import org.dueattendant149.bebringo.data.local.BebringoDatabase
import org.dueattendant149.bebringo.data.prefs.SettingsStore
import org.dueattendant149.bebringo.data.repository.CollectionRepository
import org.dueattendant149.bebringo.data.repository.CourseRepository
import org.dueattendant149.bebringo.data.repository.EventRepository
import org.dueattendant149.bebringo.data.repository.ProgressRepository
import org.dueattendant149.bebringo.data.repository.SkillRepository
import org.dueattendant149.bebringo.data.repository.SnapshotState
import org.dueattendant149.bebringo.data.repository.SrsRepository
import org.dueattendant149.bebringo.data.repository.StreakRepository
import org.dueattendant149.bebringo.data.repository.TelemetryRepository
import org.dueattendant149.bebringo.domain.entry.DeviceState
import org.dueattendant149.bebringo.domain.srs.FsrsScheduler
import org.dueattendant149.bebringo.domain.srs.Scheduler
import java.io.File
import java.time.LocalDate

/**
 * Ручной контейнер зависимостей.
 *
 * Hilt здесь избыточен: приложение на одного пользователя, граф зависимостей плоский.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: BebringoDatabase by lazy { BebringoDatabase.get(appContext) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val scheduler: Scheduler by lazy { FsrsScheduler(desiredRetention = 0.9) }

    val courseRepository: CourseRepository by lazy {
        CourseRepository(CourseLoader(appContext), database.lemmaDao())
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(database.blockProgressDao())
    }

    val srsRepository: SrsRepository by lazy {
        SrsRepository(database.srsDao(), scheduler)
    }

    val streakRepository: StreakRepository by lazy {
        StreakRepository(database.streakDao())
    }

    val telemetryRepository: TelemetryRepository by lazy {
        TelemetryRepository(database.telemetryDao(), File(appContext.filesDir, "exports"))
    }

    val eventRepository: EventRepository by lazy {
        EventRepository(database.eventDao(), database.activityProfileDao(), File(appContext.filesDir, "exports"))
    }

    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Пишет событие слоя рекомендации входа асинхронно и без ошибок для вызывающего:
     * телеметрия не должна ломать учебный сценарий.
     */
    fun logEvent(type: String, sessionId: String? = null, payload: Map<String, String> = emptyMap()) {
        eventScope.launch {
            runCatching {
                val today = LocalDate.now()
                val streak = streakRepository.state(today)
                eventRepository.record(
                    type = type,
                    sessionId = sessionId,
                    payload = payload,
                    device = deviceState(),
                    state = SnapshotState(
                        streakLen = streak.days,
                        dayClosed = streak.blocksToday > 0,
                        dueCards = srsRepository.due(System.currentTimeMillis(), limit = 50).size,
                    ),
                )
            }
        }
    }

    /** Контекст устройства для снапшота события. */
    private fun deviceState(): DeviceState {
        val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val capabilities = connectivity?.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCharging = runCatching {
            appContext.getSystemService(BatteryManager::class.java)?.isCharging
        }.getOrNull()
        val batterySaver = runCatching {
            appContext.getSystemService(PowerManager::class.java)?.isPowerSaveMode
        }.getOrNull()
        return DeviceState(isWifi = isWifi, isCharging = isCharging, batterySaver = batterySaver)
    }

    val collectionRepository: CollectionRepository by lazy {
        CollectionRepository(database.collectionDao())
    }

    val skillRepository: SkillRepository by lazy {
        SkillRepository(database.skillDao())
    }

    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database)
    }
}
