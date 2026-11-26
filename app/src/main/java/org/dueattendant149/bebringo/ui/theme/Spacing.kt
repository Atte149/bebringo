package org.dueattendant149.bebringo.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Шкала отступов. Никаких «на глаз»: всё, что больше 4dp, берётся отсюда.
 */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Поле экрана по бокам. */
    val screen = 20.dp
}

/**
 * Радиусы. Кнопки — пилюля, карточки — large.
 */
object Radius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 28.dp
    val pill = 100.dp
}
