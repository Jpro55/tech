package com.qarin.mobile.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qarin.mobile.QarinMobileApp
import com.qarin.mobile.data.db.*
import com.qarin.mobile.data.sync.MobileSyncManager
import com.qarin.shared.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class MobileViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as QarinMobileApp).database
    private val syncManager = MobileSyncManager(app, db)

    private var tts: TextToSpeech? = null

    val tasks = db.taskDao().getAllFlow().map { it.map { e -> e.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val skills = db.skillDao().getAllFlow().map { it.map { e -> e.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = db.reminderDao().getAllFlow().map { it.map { e -> e.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = db.noteDao().getAllFlow().map { it.map { e -> e.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(MobileUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
        initTts(app)
        checkWatchConnection()
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ar"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "qarin_mobile_tts")
    }

    // --- Tasks ---
    fun addTask(title: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM) {
        viewModelScope.launch {
            val task = QarinTask(title = title, description = description, priority = priority)
            db.taskDao().insert(MobileTaskEntity.fromModel(task))
            syncToWatch()
        }
    }

    fun updateTask(task: QarinTask) {
        viewModelScope.launch {
            db.taskDao().update(MobileTaskEntity.fromModel(
                task.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING_SYNC)
            ))
            syncToWatch()
        }
    }

    fun deleteTask(task: QarinTask) {
        viewModelScope.launch {
            db.taskDao().deleteById(task.id)
            syncToWatch()
        }
    }

    // --- Skills ---
    fun addSkill(name: String, description: String = "", category: String = "") {
        viewModelScope.launch {
            val skill = QarinSkill(name = name, description = description, category = category)
            db.skillDao().insert(MobileSkillEntity.fromModel(skill))
            syncToWatch()
        }
    }

    fun updateSkill(skill: QarinSkill) {
        viewModelScope.launch {
            db.skillDao().update(MobileSkillEntity.fromModel(
                skill.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING_SYNC)
            ))
            syncToWatch()
        }
    }

    fun deleteSkill(skill: QarinSkill) {
        viewModelScope.launch {
            db.skillDao().delete(MobileSkillEntity.fromModel(skill))
            syncToWatch()
        }
    }

    // --- Reminders ---
    fun addReminder(title: String, message: String = "", triggerTime: Long) {
        viewModelScope.launch {
            val reminder = QarinReminder(title = title, message = message, triggerTime = triggerTime)
            db.reminderDao().insert(MobileReminderEntity.fromModel(reminder))
            syncToWatch()
        }
    }

    fun deleteReminder(reminder: QarinReminder) {
        viewModelScope.launch {
            db.reminderDao().delete(MobileReminderEntity.fromModel(reminder))
            syncToWatch()
        }
    }

    // --- Notes ---
    fun addNote(content: String, title: String = "", isVoice: Boolean = false) {
        viewModelScope.launch {
            val note = QarinNote(title = title, content = content, isVoiceNote = isVoice)
            db.noteDao().insert(MobileNoteEntity.fromModel(note))
            syncToWatch()
        }
    }

    fun deleteNote(note: QarinNote) {
        viewModelScope.launch {
            db.noteDao().delete(MobileNoteEntity.fromModel(note))
            syncToWatch()
        }
    }

    private fun syncToWatch() {
        viewModelScope.launch {
            if (syncManager.isWatchConnected()) {
                syncManager.pushUpdatesToWatch()
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            syncManager.pushUpdatesToWatch()
            _uiState.update { it.copy(isSyncing = false, snackbarMessage = "تمت المزامنة مع الساعة") }
        }
    }

    private fun checkWatchConnection() {
        viewModelScope.launch {
            val connected = syncManager.isWatchConnected()
            _uiState.update { it.copy(isWatchConnected = connected) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun setCurrentTab(tab: MobileTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

data class MobileUiState(
    val currentTab: MobileTab = MobileTab.TASKS,
    val isWatchConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val snackbarMessage: String? = null
)

enum class MobileTab { TASKS, SKILLS, REMINDERS, NOTES }
