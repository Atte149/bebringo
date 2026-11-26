package org.dueattendant149.bebringo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.dueattendant149.bebringo.ui.theme.BebringoTheme

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Главная", Icons.Rounded.Home),
    TabItem(Routes.PROGRESS, "Прогресс", Icons.AutoMirrored.Rounded.ShowChart),
    TabItem(Routes.PROFILE, "Профиль", Icons.Rounded.Person),
)

/** Нижняя навигация Material 3: три вкладки с тональным индикатором `primaryContainer`. */
@Composable
fun BottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    NavigationBar(
        modifier = modifier,
        containerColor = colors.surfaceContainer,
        contentColor = colors.onBg,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { if (currentRoute != tab.route) onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimaryContainer,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.muted,
                    unselectedTextColor = colors.muted,
                    indicatorColor = colors.primaryContainer,
                ),
            )
        }
    }
}
