package org.dueattendant149.bebringo.ui.screens.onboarding

/**
 * Черновик ответов онбординга.
 *
 * Экран «Готово» сохраняет их одним пакетом, поэтому промежуточные шаги
 * не пишут в DataStore по одному полю.
 */
object OnboardingDraft {
    var goal: String = "life"
    var anchorTime: String = "вечером"
    var anchorAfter: String = "конца работы"

    fun reset() {
        goal = "life"
        anchorTime = "вечером"
        anchorAfter = "конца работы"
    }
}
