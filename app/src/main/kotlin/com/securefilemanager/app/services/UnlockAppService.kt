package com.securefilemanager.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.appLock
import com.securefilemanager.app.extensions.getNotificationManager
import com.securefilemanager.app.extensions.isAuthenticatorNotSet
import com.securefilemanager.app.helpers.APP_CHANNEL_ID
import com.securefilemanager.app.helpers.getNotificationId

class UnlockAppService : Service() {

    private val mNotificationId = getNotificationId()

    override fun onCreate() {
        super.onCreate()

        if(isAuthenticatorNotSet()) {
            return
        }

        val notificationManager: NotificationManager = this.getNotificationManager()
        notificationManager.createNotificationChannel(this.getNotificationChannel())
        val notification: NotificationCompat.Builder = this.getNotificationBuilder()

        this.startForeground(this.mNotificationId, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_LOCK -> {
                this.appLock()
                this.stopSelf()
            }
            ACTION_STOP -> this.stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getNotificationChannel(): NotificationChannel =
        NotificationChannel(
            APP_CHANNEL_ID,
            APP_CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW
        )

    private fun getStopIntent(): PendingIntent? =
        PendingIntent.getService(
            this,
            LOCK_REQUEST_CODE,
            Intent(this, this::class.java).apply {
                action = ACTION_LOCK
            },
            0
        )

    private fun getStopAction(): NotificationCompat.Action? =
        NotificationCompat.Action.Builder(
            R.drawable.ic_lock_vector,
            this.getString(R.string.lock_app),
            this.getStopIntent()
        ).build()

    private fun getNotificationBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, APP_CHANNEL_ID)
            .setContentText(getString(R.string.app_unlocked_notification))
            .setSmallIcon(R.drawable.ic_shield_lock_vector)
            .setShowWhen(false)
            .setSound(null)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(this.getStopIntent())
            .addAction(this.getStopAction())

    companion object {
        // Action
        private const val ACTION_LOCK = "ACTION_LOCK"
        private const val ACTION_STOP = "ACTION_STOP"

        // Other
        private const val LOCK_REQUEST_CODE = 13

        fun getStartIntent(context: Context) = Intent(context, UnlockAppService::class.java)

        fun getStopIntent(context: Context) = Intent(context, UnlockAppService::class.java).apply {
            action = ACTION_STOP
        }
    }

}
