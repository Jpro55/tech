package com.qarin.mobile.service

import android.app.*
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Wearable
import com.qarin.mobile.R
import com.qarin.mobile.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * RemoteCameraService - Opens camera on phone when requested from watch
 * Captures a photo and sends it back to watch via DataLayer
 */
class RemoteCameraService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "RemoteCameraService"

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nodeId = intent?.getStringExtra("node_id") ?: ""

        // Show foreground notification (required for camera access)
        showForegroundNotification()

        scope.launch {
            try {
                capturePhoto(nodeId)
            } catch (e: Exception) {
                Log.e(tag, "Camera capture failed", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun showForegroundNotification() {
        val channelId = "qarin_camera"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "قرين - الكاميرا", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_camera)
            .setContentTitle("قرين - التقاط صورة")
            .setContentText("جارٍ التقاط صورة من الساعة...")
            .setContentIntent(openIntent)
            .build()

        startForeground(42, notification)
    }

    private suspend fun capturePhoto(targetNodeId: String) {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return

        imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 1)

        val deferred = CompletableDeferred<ByteArray>()

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            deferred.complete(bytes)
        }, null)

        // Open camera
        val openDeferred = CompletableDeferred<CameraDevice>()
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                openDeferred.complete(camera)
            }
            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                openDeferred.completeExceptionally(Exception("Camera disconnected"))
            }
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                openDeferred.completeExceptionally(Exception("Camera error: $error"))
            }
        }, null)

        cameraDevice = openDeferred.await()

        // Create capture session
        val surfaces = listOf(imageReader!!.surface)
        val sessionDeferred = CompletableDeferred<CameraCaptureSession>()
        cameraDevice!!.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                sessionDeferred.complete(session)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                sessionDeferred.completeExceptionally(Exception("Session config failed"))
            }
        }, null)

        captureSession = sessionDeferred.await()

        // Capture request
        val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(imageReader!!.surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        }.build()

        captureSession!!.capture(captureRequest, null, null)

        // Wait for photo
        val photoBytes = withTimeoutOrNull(10_000) { deferred.await() }

        // Cleanup camera
        captureSession?.close()
        cameraDevice?.close()

        if (photoBytes != null) {
            sendPhotoToWatch(photoBytes, targetNodeId)
            Log.d(tag, "Photo captured and sent (${photoBytes.size} bytes)")
        }
    }

    private suspend fun sendPhotoToWatch(photoBytes: ByteArray, nodeId: String) {
        try {
            // If too large, compress
            val dataToSend = if (photoBytes.size > 100_000) {
                compressImage(photoBytes, 50)
            } else {
                photoBytes
            }

            if (nodeId.isNotEmpty()) {
                Wearable.getMessageClient(this).sendMessage(
                    nodeId,
                    com.qarin.shared.models.DataPaths.CAMERA_PHOTO_PATH,
                    dataToSend
                ).await()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send photo to watch", e)
        }
    }

    private fun compressImage(bytes: ByteArray, quality: Int): ByteArray {
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val out = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        scope.cancel()
    }
}

class ReminderAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        val title = intent.getStringExtra("title") ?: "تذكير"
        val message = intent.getStringExtra("message") ?: ""
        Log.d("ReminderAlarm", "Reminder triggered: $title")
        // Show notification
        showReminderNotification(context, title, message)
    }

    private fun showReminderNotification(context: android.content.Context, title: String, message: String) {
        val channelId = "qarin_reminders"
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "تذكيرات قرين", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class MobileBootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            Log.d("MobileBootReceiver", "Boot completed - re-scheduling reminders")
        }
    }
}
