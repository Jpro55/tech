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
import com.qarin.shared.models.QarinNote
import com.qarin.wear.ui.QarinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    viewModel: QarinViewModel,
    onVoiceActivated: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
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
                Text("الملاحظات", fontSize = 16.sp, color = MaterialTheme.colors.primary)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactChip(
                        onClick = { onVoiceActivated() },
                        label = { Text("🎤 صوتي") },
                        modifier = Modifier.weight(1f)
                    )
                    CompactChip(
                        onClick = { viewModel.requestCameraCapture() },
                        label = { Text("📷 صورة") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (notes.isEmpty()) {
                item {
                    Text(
                        "لا توجد ملاحظات\nقل: سجل ملاحظة [النص]",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            items(notes.size) { index ->
                NoteItem(
                    note = notes[index],
                    onSpeak = { viewModel.speak(it.content) }
                )
            }
        }
    }
}

@Composable
fun NoteItem(
    note: QarinNote,
    onSpeak: (QarinNote) -> Unit
) {
    Card(
        onClick = { onSpeak(note) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (note.isVoiceNote) "🎤 ملاحظة صوتية"
                    else if (note.photoPath != null) "📷 صورة"
                    else note.title.ifEmpty { "ملاحظة" },
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale("ar")).format(Date(note.createdAt)),
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
            if (note.content.isNotEmpty()) {
                Text(
                    text = note.content,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
