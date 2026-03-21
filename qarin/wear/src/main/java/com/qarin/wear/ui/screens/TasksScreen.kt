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
import com.qarin.shared.models.QarinTask
import com.qarin.shared.models.TaskPriority
import com.qarin.shared.models.TaskStatus
import com.qarin.wear.ui.QarinViewModel

@Composable
fun TasksScreen(
    viewModel: QarinViewModel,
    onVoiceActivated: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val scalingLazyListState = rememberScalingLazyListState()
    var showAddDialog by remember { mutableStateOf(false) }

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
                Text("المهام", fontSize = 16.sp, color = MaterialTheme.colors.primary)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactChip(
                        onClick = { onVoiceActivated() },
                        label = { Text("🎤 إضافة صوتي") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Text(
                        "لا توجد مهام\nاضغط الميكروفون\nلإضافة مهمة",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            items(tasks.size) { index ->
                TaskItem(
                    task = tasks[index],
                    onToggle = { task ->
                        val newStatus = if (task.status == TaskStatus.DONE)
                            TaskStatus.PENDING else TaskStatus.DONE
                        viewModel.updateTaskStatus(task, newStatus)
                    },
                    onDelete = { viewModel.deleteTask(it) },
                    onSpeak = { viewModel.speak("${it.title}: ${it.status.name}") }
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: QarinTask,
    onToggle: (QarinTask) -> Unit,
    onDelete: (QarinTask) -> Unit,
    onSpeak: (QarinTask) -> Unit
) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFFF5722)
        TaskPriority.MEDIUM -> Color(0xFFFFC107)
        TaskPriority.LOW -> Color(0xFF4CAF50)
    }

    SplitToggleChip(
        modifier = Modifier.fillMaxWidth(),
        checked = task.status == TaskStatus.DONE,
        onCheckedChange = { onToggle(task) },
        label = {
            Text(
                text = task.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = if (task.status == TaskStatus.DONE) Color.Gray else Color.White
            )
        },
        secondaryLabel = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .padding(top = 2.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = priorityColor)
                    }
                }
                Text(
                    text = when (task.priority) {
                        TaskPriority.HIGH -> "عالية"
                        TaskPriority.MEDIUM -> "متوسطة"
                        TaskPriority.LOW -> "منخفضة"
                    },
                    fontSize = 10.sp,
                    color = priorityColor
                )
            }
        },
        onClick = { onSpeak(task) },
        toggleControl = {
            Icon(
                imageVector = if (task.status == TaskStatus.DONE)
                    androidx.compose.material.icons.Icons.Default.CheckCircle
                else
                    androidx.compose.material.icons.Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.status == TaskStatus.DONE) Color(0xFF4CAF50) else Color.Gray
            )
        }
    )
}
