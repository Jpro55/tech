package com.qarin.wear.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.qarin.shared.models.QarinReminder
import com.qarin.wear.ui.QarinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersScreen(
    viewModel: QarinViewModel,
    onVoiceActivated: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
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
                Text("التذكيرات", fontSize = 16.sp, color = MaterialTheme.colors.primary)
            }

            item {
                CompactChip(
                    onClick = { onVoiceActivated() },
                    label = { Text("🎤 إضافة تذكير") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (reminders.isEmpty()) {
                item {
                    Text(
                        "لا توجد تذكيرات\nقل: ذكرني بـ [العنوان]",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            items(reminders.size) { index ->
                ReminderItem(
                    reminder = reminders[index],
                    onDelete = { viewModel.deleteReminder(it) },
                    onSpeak = { viewModel.speak("تذكير: ${it.title} في ${formatTime(it.triggerTime)}") }
                )
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: QarinReminder,
    onDelete: (QarinReminder) -> Unit,
    onSpeak: (QarinReminder) -> Unit
) {
    Chip(
        onClick = { onSpeak(reminder) },
        label = {
            Text(
                text = reminder.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
        },
        secondaryLabel = {
            Text(
                text = formatTime(reminder.triggerTime),
                fontSize = 10.sp,
                color = if (reminder.isActive) Color(0xFF4CAF50) else Color.Gray
            )
        },
        icon = { Text(if (reminder.isActive) "🔔" else "🔕", fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale("ar"))
    return sdf.format(Date(timestamp))
}
