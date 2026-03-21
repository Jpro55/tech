package com.qarin.mobile.service

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.*
import com.qarin.mobile.QarinMobileApp
import com.qarin.mobile.data.db.*
import com.qarin.mobile.data.sync.MobileSyncManager
import com.qarin.shared.models.DataPaths
import com.qarin.shared.models.SyncPayload
import kotlinx.coroutines.*

class MobileDataLayerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "MobileDataLayerService"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            Log.d(tag, "Data changed: ${event.dataItem.uri.path}")
            when (event.dataItem.uri.path) {
                DataPaths.SYNC_DATA_PATH -> {
                    // Data from watch - merge into phone DB
                    val data = event.dataItem.data ?: return@forEach
                    val mapRequest = DataMapItem.fromDataItem(event.dataItem)
                    val payload = mapRequest.dataMap.getString("payload") ?: return@forEach
                    handleWatchData(payload)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(tag, "Message: ${messageEvent.path} from ${messageEvent.sourceNodeId}")
        when (messageEvent.path) {
            DataPaths.SYNC_REQUEST_PATH -> {
                // Watch requests full sync - send phone data back
                scope.launch {
                    val db = (application as QarinMobileApp).database
                    val syncManager = MobileSyncManager(applicationContext, db)
                    syncManager.sendAllDataToWatch(messageEvent.sourceNodeId)
                }
            }
            DataPaths.CAMERA_REQUEST_PATH -> {
                // Watch wants to take a photo
                Log.d(tag, "Camera request from watch")
                val intent = Intent(this, RemoteCameraService::class.java).apply {
                    putExtra("node_id", messageEvent.sourceNodeId)
                    putExtra("action", "capture")
                }
                startForegroundService(intent)
            }
        }
    }

    private fun handleWatchData(json: String) {
        scope.launch {
            try {
                val payload = SyncPayload.fromJson(json)
                val db = (application as QarinMobileApp).database

                if (payload.tasks.isNotEmpty()) {
                    db.taskDao().insertAll(payload.tasks.map { MobileTaskEntity.fromModel(it) })
                }
                if (payload.skills.isNotEmpty()) {
                    db.skillDao().insertAll(payload.skills.map { MobileSkillEntity.fromModel(it) })
                }
                if (payload.reminders.isNotEmpty()) {
                    db.reminderDao().insertAll(payload.reminders.map { MobileReminderEntity.fromModel(it) })
                }
                if (payload.notes.isNotEmpty()) {
                    db.noteDao().insertAll(payload.notes.map { MobileNoteEntity.fromModel(it) })
                }
                Log.d(tag, "Watch data merged: ${payload.tasks.size} tasks")
            } catch (e: Exception) {
                Log.e(tag, "Error merging watch data", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
