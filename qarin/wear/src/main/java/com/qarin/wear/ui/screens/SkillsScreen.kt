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
import com.qarin.shared.models.QarinSkill
import com.qarin.wear.ui.QarinViewModel

@Composable
fun SkillsScreen(
    viewModel: QarinViewModel,
    onVoiceActivated: () -> Unit
) {
    val skills by viewModel.skills.collectAsState()
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
                Text("المهارات", fontSize = 16.sp, color = MaterialTheme.colors.primary)
            }

            item {
                CompactChip(
                    onClick = { onVoiceActivated() },
                    label = { Text("🎤 إضافة مهارة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (skills.isEmpty()) {
                item {
                    Text(
                        "لا توجد مهارات\nقل: أضف مهارة [الاسم]",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            items(skills.size) { index ->
                SkillItem(
                    skill = skills[index],
                    onProgressUp = { skill ->
                        val newProgress = minOf(100, skill.progress + 10)
                        viewModel.updateSkillProgress(skill, newProgress)
                    },
                    onSpeak = { viewModel.speak("${it.name}: مستوى ${it.level}, تقدم ${it.progress} بالمئة") }
                )
            }
        }
    }
}

@Composable
fun SkillItem(
    skill: QarinSkill,
    onProgressUp: (QarinSkill) -> Unit,
    onSpeak: (QarinSkill) -> Unit
) {
    Card(
        onClick = { onSpeak(skill) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Lvl ${skill.level}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.primary
                )
            }
            if (skill.category.isNotEmpty()) {
                Text(
                    text = skill.category,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = skill.progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                indicatorColor = Color(0xFF4CAF50),
                trackColor = Color.DarkGray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${skill.progress}%", fontSize = 9.sp, color = Color.Gray)
                CompactChip(
                    onClick = { onProgressUp(skill) },
                    label = { Text("+10%", fontSize = 9.sp) }
                )
            }
        }
    }
}
