package com.qarin.wear.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qarin.shared.models.*
import com.qarin.wear.QarinApp
import com.qarin.wear.data.db.*
import com.qarin.wear.data.sync.WearSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class QarinViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as QarinApp).database
    private val syncManager = WearSyncManager(app, db)

    // TTS Engine
    private var tts: TextToSpeech? = null
    private val _ttsReady = MutableStateFlow(false)

    // Data flows
    val tasks = db.taskDao().getAllFlow().map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skills = db.skillDao().getAllFlow().map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders = db.reminderDao().getAllFlow().map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes = db.noteDao().getAllFlow().map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State
    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    init {
        initTts(app)
        checkConnection()
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try Arabic first
                val arabicResult = tts?.setLanguage(Locale("ar"))
                if (arabicResult == TextToSpeech.LANG_MISSING_DATA ||
                    arabicResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
                _ttsReady.value = true
                Log.d("TTS", "TTS initialized")
            }
        }
    }

    fun speak(text: String) {
        if (_ttsReady.value) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "qarin_tts")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    // --- Tasks ---
    fun addTask(title: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM) {
        viewModelScope.launch {
            val task = QarinTask(
                title = title, description = description,
                priority = priority, createdOnWatch = true
            )
            db.taskDao().insert(TaskEntity.fromModel(task))
            speak("تمت إضافة المهمة: $title")
            syncIfConnected()
        }
    }

    fun updateTaskStatus(task: QarinTask, status: TaskStatus) {
        viewModelScope.launch {
            val updated = task.copy(status = status, updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_SYNC)
            db.taskDao().update(TaskEntity.fromModel(updated))
            syncIfConnected()
        }
    }

    fun deleteTask(task: QarinTask) {
        viewModelScope.launch {
            db.taskDao().deleteById(task.id)
            syncIfConnected()
        }
    }

    // --- Skills ---
    fun addSkill(name: String, description: String = "", category: String = "") {
        viewModelScope.launch {
            val skill = QarinSkill(
                name = name, description = description,
                category = category, createdOnWatch = true
            )
            db.skillDao().insert(SkillEntity.fromModel(skill))
            speak("تمت إضافة المهارة: $name")
            syncIfConnected()
        }
    }

    fun updateSkillProgress(skill: QarinSkill, progress: Int) {
        viewModelScope.launch {
            val updated = skill.copy(progress = progress,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_SYNC)
            db.skillDao().update(SkillEntity.fromModel(updated))
            syncIfConnected()
        }
    }

    // --- Reminders ---
    fun addReminder(title: String, message: String = "", triggerTime: Long) {
        viewModelScope.launch {
            val reminder = QarinReminder(
                title = title, message = message,
                triggerTime = triggerTime, createdOnWatch = true
            )
            db.reminderDao().insert(ReminderEntity.fromModel(reminder))
            speak("تم ضبط التذكير: $title")
            syncIfConnected()
        }
    }

    fun deleteReminder(reminder: QarinReminder) {
        viewModelScope.launch {
            db.reminderDao().delete(ReminderEntity.fromModel(reminder))
            syncIfConnected()
        }
    }

    // --- Notes ---
    fun addNote(content: String, title: String = "") {
        viewModelScope.launch {
            val note = QarinNote(
                title = title, content = content, createdOnWatch = true
            )
            db.noteDao().insert(NoteEntity.fromModel(note))
            speak("تم حفظ الملاحظة")
            syncIfConnected()
        }
    }

    // --- Camera ---
    fun requestCameraCapture() {
        viewModelScope.launch {
            if (syncManager.isPhoneConnected()) {
                syncManager.requestCameraCapture()
                _uiState.update { it.copy(waitingForPhoto = true) }
                speak("جارٍ فتح الكاميرا")
            } else {
                speak("الهاتف غير متصل")
                _uiState.update { it.copy(message = "الهاتف غير متصل") }
            }
        }
    }

    // --- Sync ---
    private fun syncIfConnected() {
        viewModelScope.launch {
            if (syncManager.isPhoneConnected()) {
                syncManager.pushPendingToPhone()
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            syncManager.pushPendingToPhone()
            syncManager.requestSyncFromPhone()
            _uiState.update { it.copy(isSyncing = false, message = "تمت المزامنة") }
            speak("تمت المزامنة")
        }
    }

    private fun checkConnection() {
        viewModelScope.launch {
            val connected = syncManager.isPhoneConnected()
            _uiState.update { it.copy(isPhoneConnected = connected) }
        }
    }

    // --- Voice Command Handler ---
    fun handleVoiceCommand(command: String) {
        val lower = command.lowercase()
        speak("أمرك يا سيدي")
        when {
            lower.contains("مهمة") || lower.contains("task") -> {
                val title = command.replace(Regex("(?i)(أضف|add|مهمة جديدة|new task|مهمة)"), "").trim()
                if (title.isNotEmpty()) addTask(title)
                else _uiState.update { it.copy(screen = WearScreen.TASKS) }
            }
            lower.contains("تذكير") || lower.contains("reminder") -> {
                _uiState.update { it.copy(screen = WearScreen.REMINDERS) }
            }
            lower.contains("ملاحظة") || lower.contains("note") -> {
                _uiState.update { it.copy(screen = WearScreen.NOTES) }
            }
            lower.contains("مهارة") || lower.contains("skill") -> {
                _uiState.update { it.copy(screen = WearScreen.SKILLS) }
            }
            lower.contains("كاميرا") || lower.contains("camera") || lower.contains("صور") -> {
                requestCameraCapture()
            }
            lower.contains("مزامنة") || lower.contains("sync") -> {
                manualSync()
            }
            else -> {
                speak("لم أفهم الأمر، حاول مرة أخرى")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

data class WearUiState(
    val screen: WearScreen = WearScreen.HOME,
    val isPhoneConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val waitingForPhoto: Boolean = false,
    val isListening: Boolean = false,
    val message: String? = null
)

enum class WearScreen { HOME, TASKS, SKILLS, REMINDERS, NOTES, SETTINGS }
