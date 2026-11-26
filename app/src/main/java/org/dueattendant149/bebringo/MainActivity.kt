package org.dueattendant149.bebringo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.notify.Reminders
import org.dueattendant149.bebringo.ui.navigation.BebringoNavHost
import org.dueattendant149.bebringo.ui.navigation.BottomBar
import org.dueattendant149.bebringo.ui.navigation.Routes
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.widget.BebringoWidgets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        Reminders.ensureChannel(this)
        lifecycleScope.launch {
            BebringoWidgets.updateAll(this@MainActivity)
        }
        setContent {
            val dynamicColor by ServiceLocator.settings.dynamicColor.collectAsState(initial = false)
            BebringoTheme(dynamicColor = dynamicColor) {
                BebringoRoot()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun BebringoRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onboarded by ServiceLocator.settings.onboarded.collectAsState(initial = null)
    val anchorTime by ServiceLocator.settings.anchorTime.collectAsState(initial = "вечером")

    LaunchedEffect(onboarded, anchorTime) {
        if (onboarded == true) {
            Reminders.schedule(context, anchorTime)
        }
    }

    // Пока флаг не прочитан, ничего не показываем: иначе пользователь увидит
    // мигание онбординга при каждом запуске.
    if (onboarded == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    val startDestination = if (onboarded == true) Routes.HOME else Routes.ONBOARDING_GOAL

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in Routes.bottomBarRoutes) {
                BottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        BebringoNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
