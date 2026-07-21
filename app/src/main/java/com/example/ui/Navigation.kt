package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    NavHost(
        navController = navController,
        startDestination = if (onboardingComplete) "home" else "onboarding",
        modifier = modifier
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
        composable("session/{mode}?loopId={loopId}") { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "WALK"
            val loopId = backStackEntry.arguments?.getString("loopId")?.toIntOrNull()
            ActiveSessionScreen(
                mode = mode,
                loopId = loopId,
                onEndSession = {
                    navController.popBackStack("home", inclusive = false)
                    // We pop back to home rather than navigating to history here directly to keep the backstack clean,
                    // but we could navigate to history. Let's just pop to home.
                }
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSessionClick = { sessionId ->
                    navController.navigate("sessionDetail/$sessionId")
                }
            )
        }
        composable("sessionDetail/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toIntOrNull() ?: return@composable
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
                    navController.popBackStack()
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
                    navController.popBackStack()
                }
            )
        }
    }
}
