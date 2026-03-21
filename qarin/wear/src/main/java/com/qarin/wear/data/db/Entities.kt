package com.qarin.wear.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.qarin.shared.models.*

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val createdOnWatch: Boolean
) {
    fun toModel() = QarinTask(
        id = id, title = title, description = description,
        priority = TaskPriority.valueOf(priority),
        status = TaskStatus.valueOf(status),
        dueDate = dueDate, createdAt = createdAt, updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus),
        createdOnWatch = createdOnWatch
    )

    companion object {
        fun fromModel(m: QarinTask) = TaskEntity(
            id = m.id, title = m.title, description = m.description,
            priority = m.priority.name, status = m.status.name,
            dueDate = m.dueDate, createdAt = m.createdAt, updatedAt = m.updatedAt,
            syncStatus = m.syncStatus.name, createdOnWatch = m.createdOnWatch
        )
    }
}

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val level: Int,
    val progress: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val createdOnWatch: Boolean
) {
    fun toModel() = QarinSkill(
        id = id, name = name, description = description,
        category = category, level = level, progress = progress,
        createdAt = createdAt, updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus),
        createdOnWatch = createdOnWatch
    )

    companion object {
        fun fromModel(m: QarinSkill) = SkillEntity(
            id = m.id, name = m.name, description = m.description,
            category = m.category, level = m.level, progress = m.progress,
            createdAt = m.createdAt, updatedAt = m.updatedAt,
            syncStatus = m.syncStatus.name, createdOnWatch = m.createdOnWatch
        )
    }
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val triggerTime: Long,
    val isRepeat: Boolean,
    val repeatIntervalMinutes: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val createdOnWatch: Boolean
) {
    fun toModel() = QarinReminder(
        id = id, title = title, message = message, triggerTime = triggerTime,
        isRepeat = isRepeat, repeatIntervalMinutes = repeatIntervalMinutes,
        isActive = isActive, createdAt = createdAt, updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus), createdOnWatch = createdOnWatch
    )

    companion object {
        fun fromModel(m: QarinReminder) = ReminderEntity(
            id = m.id, title = m.title, message = m.message, triggerTime = m.triggerTime,
            isRepeat = m.isRepeat, repeatIntervalMinutes = m.repeatIntervalMinutes,
            isActive = m.isActive, createdAt = m.createdAt, updatedAt = m.updatedAt,
            syncStatus = m.syncStatus.name, createdOnWatch = m.createdOnWatch
        )
    }
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val isVoiceNote: Boolean,
    val audioFilePath: String?,
    val photoPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val createdOnWatch: Boolean
) {
    fun toModel() = QarinNote(
        id = id, title = title, content = content, isVoiceNote = isVoiceNote,
        audioFilePath = audioFilePath, photoPath = photoPath,
        createdAt = createdAt, updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus), createdOnWatch = createdOnWatch
    )

    companion object {
        fun fromModel(m: QarinNote) = NoteEntity(
            id = m.id, title = m.title, content = m.content, isVoiceNote = m.isVoiceNote,
            audioFilePath = m.audioFilePath, photoPath = m.photoPath,
            createdAt = m.createdAt, updatedAt = m.updatedAt,
            syncStatus = m.syncStatus.name, createdOnWatch = m.createdOnWatch
        )
    }
}
