package org.dueattendant149.bebringo.domain.entry

/**
 * Типы событий слоя рекомендации входа.
 *
 * Каждое событие пишется в append-only таблицу `events` вместе со снапшотом
 * контекста. Тишина — тоже событие: решение «молчать» логируется с причиной,
 * иначе модель не сможет отличить «не показали» от «не было данных».
 */
object EventTypes {
    // Жизненный цикл
    const val APP_OPEN = "app_open"
    const val APP_CLOSE = "app_close"
    const val SCREEN_VIEW = "screen_view"

    // Учёба
    const val LESSON_STEP = "lesson_step"
    const val BLOCK_CLOSED = "block_closed"
    const val REVIEW_DONE = "review_done"
    const val READING_OPEN = "reading_open"
    const val CHAT_OPEN = "chat_open"
    const val DAY_CLOSED = "day_closed"

    // Стрик
    const val FREEZE_USED = "freeze_used"
    const val STREAK_REPAIRED = "streak_repaired"
    const val STREAK_BROKEN = "streak_broken"

    // Уведомления
    const val NOTIF_DECIDED = "notif_decided"
    const val NOTIF_SHOWN = "notif_shown"
    const val NOTIF_TAPPED = "notif_tapped"
    const val NOTIF_IGNORED = "notif_ignored"
    const val NOTIF_EXPIRED = "notif_expired"
    const val FOLLOWUP_SENT = "followup_sent"

    // Среда
    const val CONNECTIVITY_CHANGE = "connectivity_change"
    const val CHARGER_CHANGE = "charger_change"
    const val SETTINGS_CHANGE = "settings_change"
}

/** Состояние устройства в момент события (для снапшота). */
data class DeviceState(
    val isWifi: Boolean? = null,
    val isCharging: Boolean? = null,
    val batterySaver: Boolean? = null,
)

/**
 * Снапшот контекста события: ровно те признаки, на которых учится модель
 * «отправить / помолчать».
 */
data class EventSnapshot(
    /** Час суток 0..23. */
    val hour: Int,
    /** День недели 1..7 (ISO: понедельник = 1). */
    val weekday: Int,
    val isWifi: Boolean?,
    val isCharging: Boolean?,
    val batterySaver: Boolean?,
    /** Текущая длина стрика в днях. */
    val streakLen: Int,
    /** Закрыт ли день (хотя бы один блок сегодня). */
    val dayClosed: Boolean,
    /** Сколько карточек FSRS созрело к повторению. */
    val dueCards: Int,
    /** Дней с последнего входа (0 — сегодня уже заходил, -1 — не заходил никогда). */
    val daysSinceLastOpen: Int,
    /** Сколько уведомлений показано за последние 24 часа. */
    val notifsLast24h: Int,
)
