package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ActiveSessionScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SessionDetailScreen
import com.example.ui.screens.LoopsScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("looptrack_ui", android.content.Context.MODE_PRIVATE) }
    var onboardingComplete by remember { mutableStateOf(preferences.getBoolean("onboarding_complete", false)) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = setOf("home", "loops", "history", "settings")
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                LoopTrackBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingComplete) "home" else "onboarding",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onContinue = {
                        preferences.edit().putBoolean("onboarding_complete", true).apply()
                        onboardingComplete = true
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onStartSession = { mode, loopId ->
                        navController.navigate("session/$mode?loopId=$loopId")
                    },
                    onNavigateToHistory = {
                        navController.navigate("history")
                    },
                    onNavigateToLoops = {
                        navController.navigate("loops")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            composable(
                route = "session/{mode}?loopId={loopId}",
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("loopId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val mode = entry.arguments?.getString("mode") ?: "WALK"
                val loopId = entry.arguments?.getString("loopId")?.toIntOrNull()
                ActiveSessionScreen(
                    mode = mode,
                    loopId = loopId,
                    onEndSession = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
            composable("history") {
                HistoryScreen(
                    onBack = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onSessionClick = { sessionId ->
                        navController.navigate("sessionDetail/$sessionId")
                    }
                )
            }
            composable("sessionDetail/{sessionId}") { entry ->
                val sessionId = entry.arguments?.getString("sessionId")?.toIntOrNull() ?: return@composable
                SessionDetailScreen(
                    sessionId = sessionId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("loops") {
                LoopsScreen(
                    onBack = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onStartCalibration = {
                        navController.navigate("calibration")
                    }
                )
            }
            composable("calibration") {
                CalibrationScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun LoopTrackBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val destinations = listOf(
        TopLevelDestination("home", "Home", Icons.Filled.Home),
        TopLevelDestination("loops", "Loops", Icons.Filled.Route),
        TopLevelDestination("history", "History", Icons.Filled.History),
        TopLevelDestination("settings", "Settings", Icons.Filled.Settings)
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 0.dp
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(destination.route) },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
