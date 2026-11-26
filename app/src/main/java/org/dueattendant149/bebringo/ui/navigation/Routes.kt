package org.dueattendant149.bebringo.ui.navigation

/** Маршруты приложения. Одна точка правды для навигации. */
object Routes {
    // Онбординг
    const val ONBOARDING_GOAL = "onboarding/goal"
    const val ONBOARDING_ANCHOR = "onboarding/anchor"
    const val ONBOARDING_PLACEMENT = "onboarding/placement"
    const val ONBOARDING_HEAD_START = "onboarding/head_start"
    const val ONBOARDING_READY = "onboarding/ready"

    // Основные вкладки
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    // Учебный поток
    const val LESSON = "lesson/{blockId}"
    const val REVIEW = "review"
    const val REVIEW_SESSION = "review_session"

    fun lesson(blockId: String): String = "lesson/$blockId"

    // Чтение
    const val READING_LIST = "reading"
    const val READER = "reader/{readingId}"

    fun reader(readingId: String): String = "reader/$readingId"

    // Прочее
    const val CHAT_LIST = "chat"
    const val CHAT = "chat/{scenarioId}"

    fun chat(scenarioId: String): String = "chat/$scenarioId"
    const val WORD_OF_DAY = "word_of_day"
    const val LAB = "lab"
    const val LLM_SETTINGS = "settings/llm"
    const val DIACRITICS_SETTINGS = "settings/diacritics"
    const val BACKUP = "backup"
    const val TELEMETRY = "telemetry"

    /** Маршруты, на которых показывается нижняя навигация. */
    val bottomBarRoutes = setOf(HOME, PROGRESS, PROFILE)
}
