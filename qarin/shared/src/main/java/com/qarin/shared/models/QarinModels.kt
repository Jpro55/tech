package com.qarin.shared.models

import com.google.gson.Gson
import java.util.UUID

enum class DataType { TASK, SKILL, REMINDER, NOTE }
enum class TaskPriority { LOW, MEDIUM, HIGH }
enum class TaskStatus { PENDING, IN_PROGRESS, DONE }
enum class SyncStatus { SYNCED, PENDING_SYNC, LOCAL_ONLY }

data class QarinTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val createdOnWatch: Boolean = false
)

data class QarinSkill(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: String = "",
    val level: Int = 1, // 1-5
    val progress: Int = 0, // 0-100
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val createdOnWatch: Boolean = false
)

data class QarinReminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String = "",
    val triggerTime: Long,
    val isRepeat: Boolean = false,
    val repeatIntervalMinutes: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val createdOnWatch: Boolean = false
)

data class QarinNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String,
    val isVoiceNote: Boolean = false,
    val audioFilePath: String? = null,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val createdOnWatch: Boolean = false
)

data class SyncPayload(
    val tasks: List<QarinTask> = emptyList(),
    val skills: List<QarinSkill> = emptyList(),
    val reminders: List<QarinReminder> = emptyList(),
    val notes: List<QarinNote> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): SyncPayload = Gson().fromJson(json, SyncPayload::class.java)
    }
}

object DataPaths {
    const val SYNC_DATA_PATH = "/qarin/sync"
    const val CAMERA_REQUEST_PATH = "/qarin/camera/request"
    const val CAMERA_PHOTO_PATH = "/qarin/camera/photo"
    const val VOICE_COMMAND_PATH = "/qarin/voice/command"
    const val COMMAND_RESULT_PATH = "/qarin/command/result"
    const val SYNC_REQUEST_PATH = "/qarin/sync/request"
    const val SYNC_RESPONSE_PATH = "/qarin/sync/response"
}
