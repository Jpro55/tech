package com.qarin.wear.service

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.qarin.shared.models.DataPaths
import com.qarin.shared.models.SyncPayload
import com.qarin.wear.QarinApp
import com.qarin.wear.data.db.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QarinDataLayerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "QarinDataLayerService"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            Log.d(tag, "Data changed: ${event.dataItem.uri.path}")
            when (event.dataItem.uri.path) {
                DataPaths.SYNC_DATA_PATH -> {
                    val data = event.dataItem.data ?: return@forEach
                    handleSyncData(String(data))
                }
                DataPaths.SYNC_RESPONSE_PATH -> {
                    val data = event.dataItem.data ?: return@forEach
                    handleSyncData(String(data))
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(tag, "Message received: ${messageEvent.path}")
        when (messageEvent.path) {
            DataPaths.SYNC_RESPONSE_PATH -> {
                val payload = String(messageEvent.data)
                handleSyncData(payload)
            }
            DataPaths.CAMERA_PHOTO_PATH -> {
                // Photo bytes received from phone - store as note
                scope.launch {
                    Log.d(tag, "Photo received from phone camera")
                }
            }
        }
    }

    private fun handleSyncData(json: String) {
        scope.launch {
            try {
                val payload = SyncPayload.fromJson(json)
                val db = (application as QarinApp).database

                // Merge tasks from phone
                if (payload.tasks.isNotEmpty()) {
                    db.taskDao().insertAll(payload.tasks.map { TaskEntity.fromModel(it) })
                }
                // Merge skills
                if (payload.skills.isNotEmpty()) {
                    db.skillDao().insertAll(payload.skills.map { SkillEntity.fromModel(it) })
                }
                // Merge reminders
                if (payload.reminders.isNotEmpty()) {
                    db.reminderDao().insertAll(payload.reminders.map { ReminderEntity.fromModel(it) })
                }
                // Merge notes
                if (payload.notes.isNotEmpty()) {
                    db.noteDao().insertAll(payload.notes.map { NoteEntity.fromModel(it) })
                }
                Log.d(tag, "Sync complete: ${payload.tasks.size} tasks, ${payload.skills.size} skills")
            } catch (e: Exception) {
                Log.e(tag, "Error handling sync data", e)
            }
        }
    }
}
