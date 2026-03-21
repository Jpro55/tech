package com.qarin.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.qarin.mobile.ui.MobileViewModel
import com.qarin.shared.models.*
import java.text.SimpleDateFormat
import java.util.*

// ============ TASKS SCREEN ============
@Composable
fun MobileTasksScreen(viewModel: MobileViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, priority ->
                viewModel.addTask(title, desc, priority)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المهام (${tasks.size})", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة")
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد مهام بعد", color = Color.Gray)
                }
            }
        }

        items(tasks, key = { it.id }) { task ->
            MobileTaskCard(
                task = task,
                onToggle = {
                    val newStatus = if (task.status == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
                    viewModel.updateTask(task.copy(status = newStatus))
                },
                onDelete = { viewModel.deleteTask(task) },
                onSpeak = { viewModel.speak(task.title) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مهمة جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("عنوان المهمة *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("الوصف (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("الأولوية:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = {
                                Text(when (p) {
                                    TaskPriority.HIGH -> "عالية"
                                    TaskPriority.MEDIUM -> "متوسطة"
                                    TaskPriority.LOW -> "منخفضة"
                                })
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onAdd(title, description, priority) },
                enabled = title.isNotBlank()
            ) { Text("إضافة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun MobileTaskCard(
    task: QarinTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: () -> Unit
) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFFF5722)
        TaskPriority.MEDIUM -> Color(0xFFFFC107)
        TaskPriority.LOW -> Color(0xFF4CAF50)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.status == TaskStatus.DONE, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null,
                    color = if (task.status == TaskStatus.DONE) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotEmpty()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).padding(1.dp)
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = priorityColor)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when (task.priority) {
                            TaskPriority.HIGH -> "أولوية عالية"
                            TaskPriority.MEDIUM -> "أولوية متوسطة"
                            TaskPriority.LOW -> "أولوية منخفضة"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor
                    )
                    if (task.createdOnWatch) {
                        Spacer(Modifier.width(8.dp))
                        Text("⌚", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Color.Red)
            }
        }
    }
}

// ============ SKILLS SCREEN ============
@Composable
fun MobileSkillsScreen(viewModel: MobileViewModel) {
    val skills by viewModel.skills.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddSkillDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, desc, category ->
                viewModel.addSkill(name, desc, category)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("المهارات (${skills.size})", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة")
                }
            }
        }

        if (skills.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد مهارات بعد", color = Color.Gray)
                }
            }
        }

        items(skills, key = { it.id }) { skill ->
            MobileSkillCard(
                skill = skill,
                onProgressChange = { newProgress ->
                    viewModel.updateSkill(skill.copy(progress = newProgress))
                },
                onDelete = { viewModel.deleteSkill(skill) }
            )
        }
    }
}

@Composable
fun AddSkillDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مهارة جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("اسم المهارة *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it },
                    label = { Text("التصنيف") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onAdd(name, description, category) },
                enabled = name.isNotBlank()) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun MobileSkillCard(skill: QarinSkill, onProgressChange: (Int) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(skill.name, style = MaterialTheme.typography.bodyLarge)
                    if (skill.category.isNotEmpty()) {
                        Text(skill.category, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                Text("Lvl ${skill.level}", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Color.Red)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { skill.progress / 100f },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.LightGray
                )
                Spacer(Modifier.width(8.dp))
                Text("${skill.progress}%", style = MaterialTheme.typography.labelSmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onProgressChange(maxOf(0, skill.progress - 10)) }) { Text("-10%") }
                TextButton(onClick = { onProgressChange(minOf(100, skill.progress + 10)) }) { Text("+10%") }
            }
        }
    }
}

// ============ REMINDERS SCREEN ============
@Composable
fun MobileRemindersScreen(viewModel: MobileViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, msg, time ->
                viewModel.addReminder(title, msg, time)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("التذكيرات (${reminders.size})", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة")
                }
            }
        }

        if (reminders.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد تذكيرات بعد", color = Color.Gray)
                }
            }
        }

        items(reminders, key = { it.id }) { reminder ->
            MobileReminderCard(
                reminder = reminder,
                onDelete = { viewModel.deleteReminder(reminder) }
            )
        }
    }
}

@Composable
fun AddReminderDialog(onDismiss: () -> Unit, onAdd: (String, String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    // For simplicity, use +1 hour from now
    val defaultTime = System.currentTimeMillis() + 3600_000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة تذكير") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("عنوان التذكير *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { message = it },
                    label = { Text("الرسالة") }, modifier = Modifier.fillMaxWidth())
                Text("وقت التذكير: بعد ساعة من الآن", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onAdd(title, message, defaultTime) },
                enabled = title.isNotBlank()) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun MobileReminderCard(reminder: QarinReminder, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (reminder.isActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                null, tint = if (reminder.isActive) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.bodyLarge)
                if (reminder.message.isNotEmpty()) {
                    Text(reminder.message, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(sdf.format(Date(reminder.triggerTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                if (reminder.createdOnWatch) {
                    Text("⌚ من الساعة", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Color.Red)
            }
        }
    }
}

// ============ NOTES SCREEN ============
@Composable
fun MobileNotesScreen(viewModel: MobileViewModel, onVoiceRecord: () -> Unit) {
    val notes by viewModel.notes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, content ->
                viewModel.addNote(content, title)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("الملاحظات (${notes.size})", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = onVoiceRecord) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("صوتي")
                    }
                    FilledTonalButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("نص")
                    }
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد ملاحظات بعد", color = Color.Gray)
                }
            }
        }

        items(notes, key = { it.id }) { note ->
            MobileNoteCard(
                note = note,
                onSpeak = { viewModel.speak(note.content) },
                onDelete = { viewModel.deleteNote(note) }
            )
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة ملاحظة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("العنوان (اختياري)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it },
                    label = { Text("محتوى الملاحظة *") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5)
            }
        },
        confirmButton = {
            Button(onClick = { if (content.isNotBlank()) onAdd(title, content) },
                enabled = content.isNotBlank()) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun MobileNoteCard(note: QarinNote, onSpeak: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale("ar"))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (note.isVoiceNote) "🎤 " else if (note.photoPath != null) "📷 " else "📝 ",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            note.title.ifEmpty { if (note.isVoiceNote) "ملاحظة صوتية" else "ملاحظة نصية" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (note.createdOnWatch) {
                            Spacer(Modifier.width(4.dp))
                            Text("⌚", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (note.content.isNotEmpty()) {
                        Text(note.content, style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray, maxLines = 3)
                    }
                    Text(sdf.format(Date(note.createdAt)),
                        style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
                Row {
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Color.Red)
                    }
                }
            }
        }
    }
}
