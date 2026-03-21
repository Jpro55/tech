package com.qarin.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qarin.mobile.ui.MobileTab
import com.qarin.mobile.ui.MobileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QarinMobileApp(
    viewModel: MobileViewModel,
    onVoiceRecord: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("قرين", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(top = 2.dp)
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = if (uiState.isWatchConnected) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (uiState.isWatchConnected) "الساعة متصلة" else "الساعة غير متصلة",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isWatchConnected) Color(0xFF4CAF50) else Color(0xFFFF5722)
                        )
                    }
                },
                actions = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.manualSync() }) {
                            Icon(Icons.Default.Sync, contentDescription = "مزامنة")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.currentTab == MobileTab.TASKS,
                    onClick = { viewModel.setCurrentTab(MobileTab.TASKS) },
                    icon = { Icon(Icons.Default.CheckCircle, null) },
                    label = { Text("المهام") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == MobileTab.SKILLS,
                    onClick = { viewModel.setCurrentTab(MobileTab.SKILLS) },
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("المهارات") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == MobileTab.REMINDERS,
                    onClick = { viewModel.setCurrentTab(MobileTab.REMINDERS) },
                    icon = { Icon(Icons.Default.Notifications, null) },
                    label = { Text("التذكيرات") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == MobileTab.NOTES,
                    onClick = { viewModel.setCurrentTab(MobileTab.NOTES) },
                    icon = { Icon(Icons.Default.Note, null) },
                    label = { Text("الملاحظات") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (uiState.currentTab) {
                MobileTab.NOTES -> {
                    Column {
                        FloatingActionButton(
                            onClick = onVoiceRecord,
                            modifier = Modifier.padding(bottom = 8.dp),
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(Icons.Default.Mic, "تسجيل صوتي")
                        }
                        FloatingActionButton(
                            onClick = { /* Add text note */ },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Add, "إضافة ملاحظة")
                        }
                    }
                }
                else -> {
                    FloatingActionButton(
                        onClick = { /* Open add dialog based on tab */ },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "إضافة")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState.currentTab) {
                MobileTab.TASKS -> MobileTasksScreen(viewModel = viewModel)
                MobileTab.SKILLS -> MobileSkillsScreen(viewModel = viewModel)
                MobileTab.REMINDERS -> MobileRemindersScreen(viewModel = viewModel)
                MobileTab.NOTES -> MobileNotesScreen(viewModel = viewModel, onVoiceRecord = onVoiceRecord)
            }
        }
    }
}
