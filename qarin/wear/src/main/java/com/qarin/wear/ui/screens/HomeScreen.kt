package com.qarin.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.qarin.wear.ui.QarinViewModel
import com.qarin.wear.ui.WearScreen
import com.qarin.wear.ui.WearUiState

@Composable
fun HomeScreen(
    viewModel: QarinViewModel,
    uiState: WearUiState,
    onNavigate: (WearScreen) -> Unit,
    onVoiceActivated: () -> Unit
) {
    val scalingLazyListState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = scalingLazyListState) }
    ) {
        ScalingLazyColumn(
            state = scalingLazyListState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            // App Title
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "قـرين",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (uiState.isPhoneConnected) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Text(
                            text = if (uiState.isPhoneConnected) "متصل" else "غير متصل",
                            fontSize = 10.sp,
                            color = if (uiState.isPhoneConnected) Color(0xFF4CAF50) else Color(0xFFFF5722)
                        )
                    }
                }
            }

            // Voice Activation Button - "يا قرين"
            item {
                Chip(
                    onClick = onVoiceActivated,
                    label = { Text("يا قرين 🎤", textAlign = TextAlign.Center) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                )
            }

            // Navigation Menu
            item {
                Chip(
                    onClick = { onNavigate(WearScreen.TASKS) },
                    label = { Text("المهام") },
                    secondaryLabel = { Text("Tasks") },
                    icon = { Text("✅") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(WearScreen.SKILLS) },
                    label = { Text("المهارات") },
                    secondaryLabel = { Text("Skills") },
                    icon = { Text("⭐") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(WearScreen.REMINDERS) },
                    label = { Text("التذكيرات") },
                    secondaryLabel = { Text("Reminders") },
                    icon = { Text("🔔") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(WearScreen.NOTES) },
                    label = { Text("الملاحظات") },
                    secondaryLabel = { Text("Notes") },
                    icon = { Text("📝") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Camera Button
            item {
                Chip(
                    onClick = { viewModel.requestCameraCapture() },
                    label = { Text("الكاميرا") },
                    secondaryLabel = { Text("Camera") },
                    icon = { Text("📷") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            // Sync Button
            item {
                CompactChip(
                    onClick = { viewModel.manualSync() },
                    label = { Text(if (uiState.isSyncing) "جارٍ المزامنة..." else "مزامنة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Message display
            uiState.message?.let { msg ->
                item {
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }
}
