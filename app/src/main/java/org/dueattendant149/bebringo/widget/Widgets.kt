package org.dueattendant149.bebringo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dueattendant149.bebringo.MainActivity
import org.dueattendant149.bebringo.R
import org.dueattendant149.bebringo.data.content.CourseLoader
import org.dueattendant149.bebringo.data.local.BebringoDatabase
import org.dueattendant149.bebringo.domain.streak.StreakCalculator
import org.dueattendant149.bebringo.domain.streak.StreakDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

private const val PREFS_NAME = "bebringo_widget_cache"
const val ACTION_TOGGLE_WORD_REVEAL = "org.dueattendant149.bebringo.action.TOGGLE_WORD_REVEAL"

/** Статус одного дня в 5-дневной полосе виджета (в стиле Duolingo). */
private enum class WidgetDayStatus {
    MISSED,
    DONE,
    FREEZE,
    TODAY_PENDING,
    TODAY_DONE,
}

private data class WidgetDaySlot(
    val labelRu: String,
    val status: WidgetDayStatus,
)

private data class StreakWidgetData(
    val streakDays: Int,
    val freezesLeft: Int,
    val blocksToday: Int,
    val stepCurrent: Int,
    val stepTotal: Int,
    val usedFreezeRecently: Boolean,
    val isEveningAtRisk: Boolean,
    val slots: List<WidgetDaySlot>,
)

private data class WordWidgetData(
    val dayOfYear: Int,
    val wordRo: String,
    val translationRu: String,
    val exampleRo: String,
)

/** Утилита для мгновенного обновления всех виджетов на домашнем экране. */
object BebringoWidgets {
    suspend fun updateAll(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val streakData = loadStreakWidgetDataFromDb(appContext)
                saveStreakCache(appContext, streakData)
                val wordData = loadWordWidgetDataFromDb(appContext)
                saveWordCache(appContext, wordData)

                val manager = AppWidgetManager.getInstance(appContext)
                val streakIds = manager.getAppWidgetIds(ComponentName(appContext, StreakWidgetReceiver::class.java))
                if (streakIds.isNotEmpty()) {
                    val views = buildStreakRemoteViews(appContext, streakData)
                    streakIds.forEach { id -> manager.updateAppWidget(id, views) }
                }

                val wordIds = manager.getAppWidgetIds(ComponentName(appContext, WordOfDayWidgetReceiver::class.java))
                if (wordIds.isNotEmpty()) {
                    val revealed = isWordRevealedToday(appContext, wordData.dayOfYear)
                    val views = buildWordRemoteViews(appContext, wordData, revealed)
                    wordIds.forEach { id -> manager.updateAppWidget(id, views) }
                }
            }
        }
    }
}

private fun shortDayOfWeekRu(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}

private fun formatDaysRu(days: Int): String {
    val n = abs(days) % 100
    val last = n % 10
    val word = when {
        n in 11..19 -> "дней"
        last == 1 -> "день"
        last in 2..4 -> "дня"
        else -> "дней"
    }
    return "$days $word"
}

private fun buildFiveDaySlots(
    today: LocalDate,
    byDate: Map<LocalDate, StreakDay>,
    blocksToday: Int,
): List<WidgetDaySlot> {
    return (4 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val label = shortDayOfWeekRu(date.dayOfWeek)
        val record = byDate[date]
        val status = if (offset == 0) {
            if (blocksToday > 0 || (record?.closedBlocks ?: 0) > 0) {
                WidgetDayStatus.TODAY_DONE
            } else {
                WidgetDayStatus.TODAY_PENDING
            }
        } else {
            when {
                record == null -> WidgetDayStatus.MISSED
                record.closedBlocks > 0 || record.repaired -> WidgetDayStatus.DONE
                record.usedFreeze -> WidgetDayStatus.FREEZE
                else -> WidgetDayStatus.MISSED
            }
        }
        WidgetDaySlot(labelRu = label, status = status)
    }
}

private fun readStreakCache(context: Context): StreakWidgetData {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val today = LocalDate.now()
    val blocksToday = prefs.getInt("streak_blocks_today", 0)
    val hour = LocalTime.now().hour
    val encodedSlots = prefs.getString("streak_slots_v2", null)
    val slots = if (!encodedSlots.isNullOrBlank()) {
        encodedSlots.split("|").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size == 2) {
                val st = runCatching { WidgetDayStatus.valueOf(parts[1]) }.getOrDefault(WidgetDayStatus.MISSED)
                WidgetDaySlot(parts[0], st)
            } else null
        }.takeIf { it.size == 5 }
    } else null

    return StreakWidgetData(
        streakDays = prefs.getInt("streak_days", 0),
        freezesLeft = prefs.getInt("streak_freezes", 2),
        blocksToday = blocksToday,
        stepCurrent = prefs.getInt("streak_step_current", 0),
        stepTotal = prefs.getInt("streak_step_total", 5),
        usedFreezeRecently = prefs.getBoolean("streak_freeze_recent", false),
        isEveningAtRisk = blocksToday == 0 && hour >= 19,
        slots = slots ?: buildFiveDaySlots(today, emptyMap(), blocksToday),
    )
}

private fun saveStreakCache(context: Context, data: StreakWidgetData) {
    val encodedSlots = data.slots.joinToString("|") { "${it.labelRu}:${it.status.name}" }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putInt("streak_days", data.streakDays)
        .putInt("streak_freezes", data.freezesLeft)
        .putInt("streak_blocks_today", data.blocksToday)
        .putInt("streak_step_current", data.stepCurrent)
        .putInt("streak_step_total", data.stepTotal)
        .putBoolean("streak_freeze_recent", data.usedFreezeRecently)
        .putString("streak_slots_v2", encodedSlots)
        .apply()
}

private fun readWordCache(context: Context): WordWidgetData {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val today = LocalDate.now().dayOfYear
    return WordWidgetData(
        dayOfYear = prefs.getInt("word_day_of_year", today),
        wordRo = prefs.getString("word_ro", "bună dimineața") ?: "bună dimineața",
        translationRu = prefs.getString("word_ru", "доброе утро") ?: "доброе утро",
        exampleRo = prefs.getString("word_example_ro", "Bună dimineața!") ?: "Bună dimineața!",
    )
}

private fun saveWordCache(context: Context, data: WordWidgetData) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putInt("word_day_of_year", data.dayOfYear)
        .putString("word_ro", data.wordRo)
        .putString("word_ru", data.translationRu)
        .putString("word_example_ro", data.exampleRo)
        .apply()
}

private fun isWordRevealedToday(context: Context, dayOfYear: Int): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("revealed_day_$dayOfYear", false)
}

private fun toggleWordRevealedToday(context: Context, dayOfYear: Int): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val key = "revealed_day_$dayOfYear"
    val next = !prefs.getBoolean(key, false)
    prefs.edit().putBoolean(key, next).apply()
    return next
}

private suspend fun loadStreakWidgetDataFromDb(context: Context): StreakWidgetData {
    return runCatching {
        val db = BebringoDatabase.get(context)
        val today = LocalDate.now()
        val days = db.streakDao().all().map {
            StreakDay(
                day = LocalDate.parse(it.day),
                closedBlocks = it.closedBlocks,
                usedFreeze = it.usedFreeze,
                repaired = it.repaired,
            )
        }
        val streak = StreakCalculator.current(days, today)
        val byDate = days.associateBy { it.day }
        val unfinished = db.blockProgressDao().unfinished()
        val stepCurrent = unfinished?.stepIndex ?: 0
        val stepTotal = 5
        val hour = LocalTime.now().hour
        val usedFreezeRecently = byDate[today.minusDays(1)]?.usedFreeze == true ||
            byDate[today.minusDays(2)]?.usedFreeze == true

        StreakWidgetData(
            streakDays = streak.days,
            freezesLeft = streak.freezesLeft,
            blocksToday = streak.blocksToday,
            stepCurrent = stepCurrent,
            stepTotal = stepTotal,
            usedFreezeRecently = usedFreezeRecently,
            isEveningAtRisk = streak.blocksToday == 0 && hour >= 19,
            slots = buildFiveDaySlots(today, byDate, streak.blocksToday),
        )
    }.getOrDefault(readStreakCache(context))
}

private suspend fun loadWordWidgetDataFromDb(context: Context): WordWidgetData {
    val today = LocalDate.now().dayOfYear
    return runCatching {
        val db = BebringoDatabase.get(context)
        val dbLemmas = db.lemmaDao().all()
        if (dbLemmas.isNotEmpty()) {
            val chosen = dbLemmas[today % dbLemmas.size]
            WordWidgetData(
                dayOfYear = today,
                wordRo = chosen.lemma,
                translationRu = chosen.translationRu,
                exampleRo = chosen.exampleRo ?: chosen.lemma,
            )
        } else {
            val course = CourseLoader(context).loadAllTracks()
            val lemmas = course.lemmas.values.toList()
            val chosen = lemmas[today % lemmas.size.coerceAtLeast(1)]
            WordWidgetData(
                dayOfYear = today,
                wordRo = chosen.lemma,
                translationRu = chosen.translationRu,
                exampleRo = chosen.exampleRo ?: chosen.lemma,
            )
        }
    }.getOrDefault(readWordCache(context))
}

private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun buildStreakRemoteViews(context: Context, data: StreakWidgetData): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_streak)
    val doneToday = data.blocksToday > 0
    val isCold = !doneToday && (data.streakDays == 0 || data.usedFreezeRecently)

    val bgRes = when {
        doneToday -> R.drawable.widget_bg_lit_gold
        data.isEveningAtRisk -> R.drawable.widget_bg_evening_risk
        isCold -> R.drawable.widget_bg_cold_night
        else -> R.drawable.widget_bg_twilight_wait
    }
    val particlesRes = when {
        isCold -> R.drawable.bg_widget_particles_snow
        else -> R.drawable.bg_widget_particles_sparks
    }
    val flameIconRes = when {
        doneToday -> R.drawable.ic_widget_flame_active
        isCold -> R.drawable.ic_widget_flame_cold
        else -> R.drawable.ic_widget_flame_pending
    }
    val mascotRes = when {
        doneToday && data.streakDays >= 5 -> R.drawable.mascot_star
        doneToday -> R.drawable.mascot_celebrate
        data.isEveningAtRisk -> R.drawable.mascot_lantern
        isCold -> R.drawable.mascot_frost
        data.stepCurrent > 0 -> R.drawable.mascot_flag_ro
        else -> R.drawable.mascot_wave
    }
    val subtitle = when {
        doneToday -> "Огонёк горит ярко!"
        data.isEveningAtRisk -> "Угли гаснут! Спасём серию?"
        isCold -> "Так холодно без урока..."
        data.stepCurrent > 0 -> "Шаг ${data.stepCurrent + 1} из ${data.stepTotal} ждёт тебя!"
        else -> "Зажжём огонёк сегодня?"
    }
    val subtitleColor = when {
        doneToday -> Color.parseColor("#FFE082")
        data.isEveningAtRisk -> Color.parseColor("#FFCCBC")
        isCold -> Color.parseColor("#C7DCF2")
        else -> Color.parseColor("#C8E6C9")
    }

    views.setInt(R.id.widget_streak_root, "setBackgroundResource", bgRes)
    views.setImageViewResource(R.id.widget_particles, particlesRes)
    views.setImageViewResource(R.id.widget_mascot, mascotRes)
    views.setImageViewResource(R.id.widget_flame_icon, flameIconRes)
    views.setTextViewText(R.id.widget_streak_days, formatDaysRu(data.streakDays))
    views.setTextViewText(R.id.widget_subtitle, subtitle)
    views.setTextColor(R.id.widget_subtitle, subtitleColor)

    val labelIds = intArrayOf(
        R.id.widget_day_label_0,
        R.id.widget_day_label_1,
        R.id.widget_day_label_2,
        R.id.widget_day_label_3,
        R.id.widget_day_label_4,
    )
    val iconIds = intArrayOf(
        R.id.widget_day_icon_0,
        R.id.widget_day_icon_1,
        R.id.widget_day_icon_2,
        R.id.widget_day_icon_3,
        R.id.widget_day_icon_4,
    )

    data.slots.take(5).forEachIndexed { idx, slot ->
        views.setTextViewText(labelIds[idx], slot.labelRu)
        val labelColor = if (idx == 4) Color.WHITE else Color.parseColor("#B8D0E8")
        views.setTextColor(labelIds[idx], labelColor)

        val statusDrawable = when (slot.status) {
            WidgetDayStatus.DONE -> R.drawable.ic_widget_day_done
            WidgetDayStatus.FREEZE -> R.drawable.ic_widget_day_freeze
            WidgetDayStatus.TODAY_DONE -> R.drawable.ic_widget_day_today_done
            WidgetDayStatus.TODAY_PENDING -> R.drawable.ic_widget_day_today_pending
            WidgetDayStatus.MISSED -> R.drawable.ic_widget_day_missed
        }
        views.setImageViewResource(iconIds[idx], statusDrawable)
    }

    views.setOnClickPendingIntent(R.id.widget_streak_root, openAppPendingIntent(context, 101))
    return views
}

private fun buildWordRemoteViews(
    context: Context,
    data: WordWidgetData,
    revealed: Boolean,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_word_of_day)

    views.setTextViewText(R.id.widget_word_ro, data.wordRo)
    views.setImageViewResource(R.id.widget_word_mascot, R.drawable.mascot_scholar)
    views.setTextViewText(R.id.widget_word_ru, data.translationRu)
    views.setTextViewText(R.id.widget_word_example, "«${data.exampleRo}»")

    if (revealed) {
        views.setInt(R.id.widget_reveal_box, "setBackgroundResource", R.drawable.widget_reveal_dark_shown)
        views.setViewVisibility(R.id.widget_reveal_hint, View.GONE)
        views.setViewVisibility(R.id.widget_word_ru, View.VISIBLE)
        views.setViewVisibility(R.id.widget_word_example, View.VISIBLE)
    } else {
        views.setInt(R.id.widget_reveal_box, "setBackgroundResource", R.drawable.widget_reveal_dark_hidden)
        views.setViewVisibility(R.id.widget_reveal_hint, View.VISIBLE)
        views.setViewVisibility(R.id.widget_word_ru, View.GONE)
        views.setViewVisibility(R.id.widget_word_example, View.GONE)
    }

    views.setOnClickPendingIntent(R.id.widget_word_header, openAppPendingIntent(context, 202))

    val toggleIntent = Intent(context, WordOfDayWidgetReceiver::class.java).apply {
        action = ACTION_TOGGLE_WORD_REVEAL
    }
    val togglePendingIntent = PendingIntent.getBroadcast(
        context,
        203,
        toggleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_reveal_box, togglePendingIntent)

    return views
}

/**
 * Выразительный виджет стрика в духе Duolingo (нативный RemoteViews без Glance-задержек).
 */
class StreakWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = readStreakCache(context)
        val initialViews = buildStreakRemoteViews(context, cached)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, initialViews) }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fresh = loadStreakWidgetDataFromDb(appContext)
                saveStreakCache(appContext, fresh)
                val views = buildStreakRemoteViews(appContext, fresh)
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Интерактивный виджет «Слово дня» с тапом для проверки перевода (нативный RemoteViews).
 */
class WordOfDayWidgetReceiver : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_WORD_REVEAL) {
            val data = readWordCache(context)
            val revealed = toggleWordRevealedToday(context, data.dayOfYear)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WordOfDayWidgetReceiver::class.java))
            val views = buildWordRemoteViews(context, data, revealed)
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = readWordCache(context)
        val revealedCached = isWordRevealedToday(context, cached.dayOfYear)
        val initialViews = buildWordRemoteViews(context, cached, revealedCached)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, initialViews) }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fresh = loadWordWidgetDataFromDb(appContext)
                saveWordCache(appContext, fresh)
                val revealed = isWordRevealedToday(appContext, fresh.dayOfYear)
                val views = buildWordRemoteViews(appContext, fresh, revealed)
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
