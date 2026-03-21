package com.qarin.mobile.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.qarin.mobile.data.db.QarinMobileDatabase
import com.qarin.shared.models.DataPaths
import com.qarin.shared.models.SyncPayload
import kotlinx.coroutines.tasks.await

class MobileSyncManager(
    private val context: Context,
    private val db: QarinMobileDatabase
) {
    private val tag = "MobileSyncManager"

    /**
     * Send all phone data to a specific watch node (response to sync request)
     */
    suspend fun sendAllDataToWatch(nodeId: String) {
        try {
            val tasks = db.taskDao().getAll().map { it.toModel() }
            val skills = db.skillDao().getAll().map { it.toModel() }
            val reminders = db.reminderDao().getAll().map { it.toModel() }
            val notes = db.noteDao().getAll().map { it.toModel() }

            val payload = SyncPayload(
                tasks = tasks, skills = skills,
                reminders = reminders, notes = notes
            )

            Wearable.getMessageClient(context).sendMessage(
                nodeId,
                DataPaths.SYNC_RESPONSE_PATH,
                payload.toJson().toByteArray()
            ).await()

            Log.d(tag, "Sent all data to watch: ${tasks.size} tasks, ${skills.size} skills")
        } catch (e: Exception) {
            Log.e(tag, "Failed to send data to watch", e)
        }
    }

    /**
     * Push updated data to all connected watches
     */
    suspend fun pushUpdatesToWatch() {
        try {
            val tasks = db.taskDao().getAll().map { it.toModel() }
            val skills = db.skillDao().getAll().map { it.toModel() }
            val reminders = db.reminderDao().getAll().map { it.toModel() }
            val notes = db.noteDao().getAll().map { it.toModel() }

            val payload = SyncPayload(
                tasks = tasks, skills = skills,
                reminders = reminders, notes = notes
            )

            val request = PutDataMapRequest.create(DataPaths.SYNC_DATA_PATH).apply {
                dataMap.putString("payload", payload.toJson())
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }

            Wearable.getDataClient(context)
                .putDataItem(request.asPutDataRequest().setUrgent())
                .await()

            Log.d(tag, "Pushed updates to watch")
        } catch (e: Exception) {
            Log.e(tag, "Failed to push updates to watch", e)
        }
    }

    suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
