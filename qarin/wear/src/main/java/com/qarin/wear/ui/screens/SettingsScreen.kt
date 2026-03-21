package com.qarin.wear.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.qarin.wear.ui.QarinViewModel

@Composable
fun SettingsScreen(viewModel: QarinViewModel) {
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
            item {
                Text("الإعدادات", fontSize = 16.sp, color = MaterialTheme.colors.primary)
            }

            item {
                ToggleChip(
                    checked = true,
                    onCheckedChange = {},
                    label = { Text("TTS - تحويل نص لصوت") },
                    secondaryLabel = { Text("Google TTS") },
                    modifier = Modifier.fillMaxWidth(),
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.switchIcon(checked = true),
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                ToggleChip(
                    checked = true,
                    onCheckedChange = {},
                    label = { Text("تشفير البيانات") },
                    secondaryLabel = { Text("AES-256 + SQLCipher") },
                    modifier = Modifier.fillMaxWidth(),
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.switchIcon(checked = true),
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = { viewModel.manualSync() },
                    label = { Text("مزامنة يدوية") },
                    secondaryLabel = { Text("Manual Sync") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Chip(
                    onClick = { viewModel.speak("مرحبا، أنا قرين، مساعدك الشخصي على الساعة الذكية") },
                    label = { Text("اختبار TTS") },
                    secondaryLabel = { Text("Test TTS") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
