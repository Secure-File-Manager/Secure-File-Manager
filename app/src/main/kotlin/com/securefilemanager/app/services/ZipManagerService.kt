package com.securefilemanager.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.APP_CHANNEL_ID
import com.securefilemanager.app.helpers.getNotificationId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

class ZipManagerService : Service() {

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mNotification: NotificationCompat.Builder
    private lateinit var mProgressMonitor: ProgressMonitor

    private val mCompleteIntent: Intent = Intent(ACTION_LOCAL_COMPLETE)
    private val mNotificationId = getNotificationId()
    private var mRunning = false

    override fun onCreate() {
        super.onCreate()

        this.mNotificationManager = this.getNotificationManager()
        this.mNotificationManager.createNotificationChannel(this.getNotificationChannel())
        this.mNotification = this.getNotificationBuilder()

        this.startForeground(this.mNotificationId, this.mNotification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_COMPRESSION -> {
                if (this.setRunning()) {
                    this.compress(
                        intent.extras?.getStringArrayList(EXTRA_PATH)!!,
                        intent.extras?.getString(EXTRA_DESTINATION)!!,
                        intent.extras?.getCharArray(EXTRA_PASSWORD)
                    )
                }
            }
            ACTION_DECOMPRESSION -> {
                if (this.setRunning()) {
                    this.decompress(
                        intent.extras?.getString(EXTRA_PATH)!!,
                        intent.extras?.getString(EXTRA_DESTINATION),
                        intent.extras?.getCharArray(EXTRA_PASSWORD)
                    )
                }
            }
            ACTION_STOP -> {
                this.stopAction()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        this.mNotificationManager.cancel(this.mNotificationId)
        // TODO: Migrate to another observable pattern
        LocalBroadcastManager
            .getInstance(this.applicationContext)
            .sendBroadcast(this.mCompleteIntent)
        super.onDestroy()
    }

    private fun setRunning(): Boolean {
        if (this.mRunning) {
            this.toast(R.string.compression_action_collision)
            return false
        }
        this.mRunning = true
        return true
    }

    private fun compress(
        sourcePaths: List<String>,
        targetPath: String,
        password: CharArray?
    ) {
        try {
            this.toast(R.string.compressing)

            val parameters = ZipParameters()

            if (password != null && password.isNotEmpty()) {
                this.setEncryptionParameters(parameters)
            }

            this.createZipFile(targetPath, password).also { zipFile: ZipFile ->
                zipFile.insertAll(this, sourcePaths, parameters)
                this.handleNotification(ACTION_COMPRESSION, targetPath)
            }
        } catch (exception: Exception) {
            this.showErrorToast(exception)
            this.stopSelf()
        }
    }

    private fun decompress(path: String, destination: String?, password: CharArray?) {
        try {
            createZipFile(path, password).also { zipFile: ZipFile ->
                this.toast(R.string.decompressing)
                val destinationPath = destination ?: zipFile.file.absolutePath.getParentPath()
                zipFile.extractAll(destinationPath)
                this.handleNotification(ACTION_DECOMPRESSION)
            }
        } catch (e: Exception) {
            this.toast(R.string.decompressing_failed)
            val message = e.message
            if (e is ZipException && message != null) {
                this.toast(message)
            } else {
                this.showErrorToast(e)
            }
        }
    }

    private fun stopAction() {
        this.mNotification.apply {
            setContentTitle(getString(R.string.canceling))
            setContentText(null)
            mNotificationManager.notify(mNotificationId, build())
        }
        this.mProgressMonitor.isCancelAllTasks = true
    }

    private fun createZipFile(path: String, password: CharArray?): ZipFile {
        val zipFile: ZipFile =
            if (password == null || password.isEmpty()) ZipFile(path)
            else ZipFile(path, password)

        zipFile.isRunInThread = true
        this.mProgressMonitor = zipFile.progressMonitor

        return zipFile
    }

    private fun setEncryptionParameters(parameters: ZipParameters): ZipParameters {
        parameters.isEncryptFiles = true
        parameters.encryptionMethod = ENCRYPTION_METHOD
        parameters.aesKeyStrength = AES_KEY_STRENGTH
        return parameters
    }

    private fun handleNotification(
        action: String,
        targetPath: String? = null
    ) {
        GlobalScope.launch {
            while (mProgressMonitor.state != ProgressMonitor.State.READY) {
                updateNotification(action)
                delay(PROGRESS_RECHECK_INTERVAL)
            }

            onComplete(action, targetPath)
        }
    }

    private fun updateNotification(action: String) {
        if (this.mProgressMonitor.fileName != null) {
            this.mNotification.apply {
                setContentTitle(
                    when (action) {
                        ACTION_COMPRESSION -> getString(R.string.compressing)
                        ACTION_DECOMPRESSION -> getString(R.string.decompressing)
                        else -> null
                    }
                )
                setContentText(mProgressMonitor.fileName.getFilenameFromPath())
                setSubText("${mProgressMonitor.percentDone}%")
                setProgress(100, mProgressMonitor.percentDone, false)
                mNotificationManager.notify(mNotificationId, build())
            }
        }
    }

    private fun onComplete(action: String, targetPath: String?) {
        when (this.mProgressMonitor.result) {
            ProgressMonitor.Result.CANCELLED,
            ProgressMonitor.Result.ERROR -> {
                if (targetPath != null) {
                    File(targetPath).delete()
                }
            }
            else -> {
                // nothing
            }
        }

        val text = getCompleteText(action)
        if (text != null) {
            this.toast(text)
        }
        this.stopSelf()
    }

    private fun getCompleteText(action: String): String? {
        return if (mProgressMonitor.result == ProgressMonitor.Result.SUCCESS) {
            when (action) {
                ACTION_COMPRESSION -> getString(R.string.compression_successful)
                ACTION_DECOMPRESSION -> getString(R.string.decompression_successful)
                else -> null
            }
        } else {
            when (action) {
                ACTION_COMPRESSION -> getString(R.string.compressing_failed)
                ACTION_DECOMPRESSION -> getString(R.string.decompressing_failed)
                else -> null
            }
        }
    }

    private fun getNotificationChannel(): NotificationChannel =
        NotificationChannel(
            APP_CHANNEL_ID,
            APP_CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW
        )

    private fun getNotificationBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, APP_CHANNEL_ID)
            .setContentTitle(getString(R.string.starting))
            .setSmallIcon(R.drawable.ic_shield_lock_vector)
            .setShowWhen(false)
            .setSound(null)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(this.getStopAction())

    private fun getStopAction(): NotificationCompat.Action? {
        val broadcastIntent: Intent =
            Intent(this, this::class.java).apply {
                action = ACTION_STOP
            }

        val actionIntent: PendingIntent =
            PendingIntent.getService(this, STOP_REQUEST_CODE, broadcastIntent, 0)

        return NotificationCompat.Action.Builder(
            R.drawable.ic_stop_vector,
            this.getString(R.string.stop),
            actionIntent
        ).build()
    }

    companion object {
        // Action
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_LOCAL_COMPLETE = "ACTION_LOCAL_COMPLETE"
        const val ACTION_COMPRESSION = "ACTION_COMPRESSION"
        const val ACTION_DECOMPRESSION = "ACTION_DECOMPRESSION"

        // Extra
        const val EXTRA_PATH = "EXTRA_PATH"
        const val EXTRA_DESTINATION = "EXTRA_DESTINATION"
        const val EXTRA_PASSWORD = "EXTRA_PASSWORD"

        // Zip parameters
        private val ENCRYPTION_METHOD = EncryptionMethod.AES
        private val AES_KEY_STRENGTH = AesKeyStrength.KEY_STRENGTH_256

        // Other
        private const val PROGRESS_RECHECK_INTERVAL = 500L
        private const val STOP_REQUEST_CODE = 12
    }

}
