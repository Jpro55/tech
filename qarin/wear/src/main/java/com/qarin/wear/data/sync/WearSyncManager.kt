package com.qarin.wear.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.qarin.shared.models.DataPaths
import com.qarin.shared.models.SyncPayload
import com.qarin.wear.data.db.*
import kotlinx.coroutines.tasks.await

class WearSyncManager(private val context: Context, private val db: QarinWearDatabase) {

    private val tag = "WearSyncManager"

    /**
     * Push all pending (unsynced) data to the phone
     */
    suspend fun pushPendingToPhone() {
        try {
            val pendingTasks = db.taskDao().getPendingSync().map { it.toModel() }
            val pendingSkills = db.skillDao().getPendingSync().map { it.toModel() }
            val pendingReminders = db.reminderDao().getPendingSync().map { it.toModel() }
            val pendingNotes = db.noteDao().getPendingSync().map { it.toModel() }

            if (pendingTasks.isEmpty() && pendingSkills.isEmpty() &&
                pendingReminders.isEmpty() && pendingNotes.isEmpty()) {
                Log.d(tag, "Nothing to sync")
                return
            }

            val payload = SyncPayload(
                tasks = pendingTasks,
                skills = pendingSkills,
                reminders = pendingReminders,
                notes = pendingNotes
            )

            val request = PutDataMapRequest.create(DataPaths.SYNC_DATA_PATH).apply {
                dataMap.putString("payload", payload.toJson())
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }

            Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent())
                .await()

            // Mark all as synced
            pendingTasks.forEach { db.taskDao().markSynced(it.id) }
            pendingSkills.forEach { db.skillDao().markSynced(it.id) }
            pendingReminders.forEach { db.reminderDao().markSynced(it.id) }
            pendingNotes.forEach { db.noteDao().markSynced(it.id) }

            Log.d(tag, "Pushed ${pendingTasks.size} tasks to phone")
        } catch (e: Exception) {
            Log.e(tag, "Failed to push data to phone", e)
        }
    }

    /**
     * Request a full sync from the phone
     */
    suspend fun requestSyncFromPhone() {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(
                    node.id,
                    DataPaths.SYNC_REQUEST_PATH,
                    ByteArray(0)
                ).await()
                Log.d(tag, "Sync requested from node: ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to request sync from phone", e)
        }
    }

    /**
     * Send camera capture request to phone
     */
    suspend fun requestCameraCapture() {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(
                    node.id,
                    DataPaths.CAMERA_REQUEST_PATH,
                    "capture".toByteArray()
                ).await()
                Log.d(tag, "Camera request sent to: ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send camera request", e)
        }
    }

    suspend fun isPhoneConnected(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
