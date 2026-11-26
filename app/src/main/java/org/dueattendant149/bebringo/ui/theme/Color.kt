package org.dueattendant149.bebringo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра Bebringo (Material Design 3 / Material You).
 *
 * Тёплая лесная и кремовая тема с полноценной тональной иерархией поверхностей M3
 * (`surfaceContainerLowest` .. `surfaceContainerHighest`), контейнерами акцентов
 * (`primaryContainer`, `secondaryContainer`, `tertiaryContainer`) и семантическими
 * состояниями (`success`, `warning`, `error`).
 */
data class BebringoColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val onBg: Color,
    val muted: Color,
    val outline: Color,
    val outlineVariant: Color,
    val errorSurface: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
)

val LightColors = BebringoColors(
    bg = Color(0xFFFDF6EC),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFF4E9D8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF1E4),
    surfaceContainer = Color(0xFFF5EBDC),
    surfaceContainerHigh = Color(0xFFEEE2D0),
    surfaceContainerHighest = Color(0xFFE6D8C3),
    primary = Color(0xFF2F5D3A),
    onPrimary = Color(0xFFFFFBF5),
    primaryContainer = Color(0xFFD6EBD8),
    onPrimaryContainer = Color(0xFF12331B),
    secondary = Color(0xFF6B5842),
    onSecondary = Color(0xFFFFFBF5),
    secondaryContainer = Color(0xFFF3DFC6),
    onSecondaryContainer = Color(0xFF281D0F),
    tertiary = Color(0xFF9C5D27),
    onTertiary = Color(0xFFFFFBF5),
    tertiaryContainer = Color(0xFFFCE1C5),
    onTertiaryContainer = Color(0xFF361B03),
    onBg = Color(0xFF1C1B18),
    muted = Color(0xFF6E685F),
    outline = Color(0xFFD8CCB8),
    outlineVariant = Color(0xFFE8DEC9),
    errorSurface = Color(0xFFFCE8E1),
    error = Color(0xFFB4462C),
    onError = Color(0xFFFFFBF5),
    success = Color(0xFF2F6F42),
    successContainer = Color(0xFFD9F0DE),
    warning = Color(0xFFB86B19),
    warningContainer = Color(0xFFFDE7C8),
)

val DarkColors = BebringoColors(
    bg = Color(0xFF121411),
    surface = Color(0xFF191C18),
    surfaceVariant = Color(0xFF272B25),
    surfaceContainerLowest = Color(0xFF0D0F0C),
    surfaceContainerLow = Color(0xFF171A16),
    surfaceContainer = Color(0xFF1E221D),
    surfaceContainerHigh = Color(0xFF262B25),
    surfaceContainerHighest = Color(0xFF31372F),
    primary = Color(0xFFA8D4B2),
    onPrimary = Color(0xFF11331B),
    primaryContainer = Color(0xFF23482C),
    onPrimaryContainer = Color(0xFFC9EDD0),
    secondary = Color(0xFFD8C3A8),
    onSecondary = Color(0xFF3A2D1C),
    secondaryContainer = Color(0xFF514330),
    onSecondaryContainer = Color(0xFFF5DFC3),
    tertiary = Color(0xFFF2B986),
    onTertiary = Color(0xFF4A2808),
    tertiaryContainer = Color(0xFF693C13),
    onTertiaryContainer = Color(0xFFFFDCC0),
    onBg = Color(0xFFECE6DC),
    muted = Color(0xFFA39D93),
    outline = Color(0xFF3D423B),
    outlineVariant = Color(0xFF2E332C),
    errorSurface = Color(0xFF3B2019),
    error = Color(0xFFF29C85),
    onError = Color(0xFF4D180A),
    success = Color(0xFF8FD1A0),
    successContainer = Color(0xFF1E4429),
    warning = Color(0xFFF5BD76),
    warningContainer = Color(0xFF4A3210),
)

/** Полупрозрачные варианты для второстепенных элементов. */
fun BebringoColors.subtlePrimary(alpha: Float = 0.14f): Color = primary.copy(alpha = alpha)
fun BebringoColors.subtleOutline(alpha: Float = 0.6f): Color = outline.copy(alpha = alpha)
