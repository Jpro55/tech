package com.qarin.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.*
import com.qarin.wear.ui.QarinViewModel
import com.qarin.wear.ui.WearScreen

@Composable
fun QarinWearApp(
    viewModel: QarinViewModel,
    onVoiceActivated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigate = { screen ->
                    when (screen) {
                        WearScreen.TASKS -> navController.navigate("tasks")
                        WearScreen.SKILLS -> navController.navigate("skills")
                        WearScreen.REMINDERS -> navController.navigate("reminders")
                        WearScreen.NOTES -> navController.navigate("notes")
                        WearScreen.SETTINGS -> navController.navigate("settings")
                        else -> {}
                    }
                },
                onVoiceActivated = onVoiceActivated
            )
        }
        composable("tasks") {
            TasksScreen(viewModel = viewModel, onVoiceActivated = onVoiceActivated)
        }
        composable("skills") {
            SkillsScreen(viewModel = viewModel, onVoiceActivated = onVoiceActivated)
        }
        composable("reminders") {
            RemindersScreen(viewModel = viewModel, onVoiceActivated = onVoiceActivated)
        }
        composable("notes") {
            NotesScreen(viewModel = viewModel, onVoiceActivated = onVoiceActivated)
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
