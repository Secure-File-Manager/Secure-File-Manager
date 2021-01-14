package com.securefilemanager.app.asynctasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.os.AsyncTask
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.util.Pair
import androidx.documentfile.provider.DocumentFile
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.interfaces.CopyMoveListener
import com.securefilemanager.app.models.FileDirItem
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

class CopyMoveTask(
    val activity: BaseAbstractActivity,
    private val copyOnly: Boolean,
    private val copyMediaOnly: Boolean,
    private val conflictResolutions: LinkedHashMap<String, Int>,
    listener: CopyMoveListener,
    private val encryptionAction: EncryptionAction,
    private val hideAction: HideAction
) : AsyncTask<Pair<ArrayList<FileDirItem>, String>, Void, Boolean>() {
    private var mListener: WeakReference<CopyMoveListener>? = null
    private var mTransferredFiles = ArrayList<FileDirItem>()
    private var mDocuments = LinkedHashMap<String, DocumentFile?>()
    private var mFiles = ArrayList<FileDirItem>()
    private var mFileCountToCopy = 0
    private var mDestinationPath = ""

    // progress indication
    private var mNotificationManager: NotificationManager
    private var mNotificationBuilder: NotificationCompat.Builder
    private var mCurrFilename = ""
    private var mCurrentProgress = 0L
    private var mMaxSize = 0
    private var mNotifId = 0
    private var mIsTaskOver = false
    private var mProgressHandler = Handler()

    init {
        mListener = WeakReference(listener)
        mNotificationManager = activity.getNotificationManager()
        mNotificationBuilder = NotificationCompat.Builder(activity, APP_CHANNEL_ID)
    }

    override fun doInBackground(vararg params: Pair<ArrayList<FileDirItem>, String>): Boolean? {
        if (params.isEmpty()) {
            return false
        }

        val pair = params[0]
        mFiles = pair.first!!
        mDestinationPath = pair.second!!
        mFileCountToCopy = mFiles.size
        mNotifId = getNotificationId()
        mMaxSize = 0
        for (file in mFiles) {
            if (file.size == 0L) {
                file.size = file.getProperSize()
            }
            val newPath = "$mDestinationPath/${file.name}"
            val fileExists = activity.getDoesFilePathExist(newPath)
            if (getConflictResolution(
                    conflictResolutions,
                    newPath
                ) != CONFLICT_SKIP || !fileExists ||
                isEncryption(encryptionAction) || isDecryption(encryptionAction)
            ) {
                mMaxSize += (file.size / 1000).toInt()
            }
        }

        mProgressHandler.postDelayed({
            initProgressNotification()
            updateProgress()
        }, INITIAL_PROGRESS_DELAY)

        for (file in mFiles) {
            try {
                if (!isNotEncryption(encryptionAction)) {
                    val newFileDirItem = if (file.isDirectory) {
                        FileDirItem(
                            file.path,
                            file.name,
                            file.isDirectory
                        )
                    } else if (isEncryption(encryptionAction)) {
                        val newFile = activity.getEncryptedFile(File(file.path))
                        if (file.isEncrypted() || newFile.exists()) {
                            mFileCountToCopy--
                            continue
                        }
                        FileDirItem(newFile.path, newFile.name, newFile.isDirectory)
                    } else {
                        val newFile = activity.getDecryptedFile(File(file.path))
                        if (!file.isEncrypted() || newFile.exists()) {
                            mFileCountToCopy--
                            continue
                        }
                        FileDirItem(newFile.path, newFile.name, newFile.isDirectory)
                    }
                    copy(file, newFileDirItem, encryptionAction)
                    continue
                }
                val newPath = "$mDestinationPath/${file.name}"
                var newFileDirItem =
                    FileDirItem(newPath, newPath.getFilenameFromPath(), file.isDirectory)
                if (activity.getDoesFilePathExist(newPath)) {
                    val resolution = getConflictResolution(conflictResolutions, newPath)
                    if (resolution == CONFLICT_SKIP) {
                        mFileCountToCopy--
                        continue
                    } else if (resolution == CONFLICT_OVERWRITE) {
                        newFileDirItem.isDirectory =
                            if (activity.getDoesFilePathExist(newPath)) File(newPath).isDirectory else activity.getSomeDocumentFile(
                                newPath
                            )!!.isDirectory
                        activity.deleteFileBg(newFileDirItem, true)
                        if (!newFileDirItem.isDirectory) {
                            activity.deleteFromMediaStore(newFileDirItem.path)
                        }
                    } else if (resolution == CONFLICT_KEEP_BOTH) {
                        val newFile = activity.getAlternativeFile(File(newFileDirItem.path))
                        newFileDirItem =
                            FileDirItem(newFile.path, newFile.name, newFile.isDirectory)
                    }
                }

                copy(file, newFileDirItem, encryptionAction)
            } catch (e: Exception) {
                activity.showErrorToast(e)
                return false
            }
        }

        return true
    }

    override fun onPostExecute(success: Boolean) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        mProgressHandler.removeCallbacksAndMessages(null)
        mNotificationManager.cancel(mNotifId)
        val listener = mListener?.get() ?: return

        if (success) {
            listener.copySucceeded(
                copyOnly,
                mTransferredFiles.size >= mFileCountToCopy,
                mDestinationPath,
                encryptionAction,
                hideAction
            )
        } else {
            listener.copyFailed(encryptionAction)
        }
    }

    private fun initProgressNotification() {
        val channelId = "Copy/Move"
        val title = activity.getString(
            when {
                isEncryption(encryptionAction) -> {
                    R.string.encrypting
                }
                isDecryption(encryptionAction) -> {
                    R.string.decrypting
                }
                isHide(hideAction) -> {
                    R.string.hiding
                }
                isUnhide(hideAction) -> {
                    R.string.unhiding
                }
                copyOnly -> {
                    R.string.copying
                }
                else -> {
                    R.string.moving
                }
            }
        )
        val importance = NotificationManager.IMPORTANCE_LOW
        NotificationChannel(channelId, title, importance).apply {
            enableLights(false)
            enableVibration(false)
            mNotificationManager.createNotificationChannel(this)
        }

        mNotificationBuilder.setContentTitle(title)
            .setSmallIcon(R.drawable.ic_shield_lock_vector)
            .setChannelId(channelId)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    private fun updateProgress() {
        if (mIsTaskOver) {
            mNotificationManager.cancel(mNotifId)
            cancel(true)
            return
        }

        mNotificationBuilder.apply {
            setContentText(mCurrFilename)
            setProgress(mMaxSize, (mCurrentProgress / 1000).toInt(), false)
            mNotificationManager.notify(mNotifId, build())
        }

        mProgressHandler.removeCallbacksAndMessages(null)
        mProgressHandler.postDelayed({
            updateProgress()

            if (mCurrentProgress / 1000 > mMaxSize) {
                mIsTaskOver = true
            }
        }, PROGRESS_RECHECK_INTERVAL)
    }

    private fun copy(
        source: FileDirItem,
        destination: FileDirItem,
        encryptionAction: EncryptionAction
    ) {
        if (source.isDirectory) {
            copyDirectory(source, destination.path, encryptionAction)
        } else {
            copyFile(source, destination, encryptionAction)
        }
    }

    private fun copyDirectory(
        source: FileDirItem,
        destinationPath: String,
        encryptionAction: EncryptionAction
    ) {
        if (!activity.createDirectorySync(destinationPath)) {
            val error =
                String.format(activity.getString(R.string.could_not_create_folder), destinationPath)
            activity.showErrorToast(error)
            return
        }

            val children = File(source.path).list()
            if (children != null) {
                for (child in children) {
                    val newPath = "$destinationPath/$child"
                    if (!isNotEncryption(encryptionAction)) {
                        val file = File(newPath)
                        val newFileDirItem = if (file.isDirectory) {
                            FileDirItem(file.path, file.name, file.isDirectory)
                        } else if (isEncryption(encryptionAction)) {
                            val newFile = activity.getEncryptedFile(file)
                            if (file.isEncrypted() || newFile.exists()) {
                                continue
                            }
                            FileDirItem(newFile.path, newFile.name, newFile.isDirectory)
                        } else {
                            val newFile = activity.getDecryptedFile(file)
                            if (!file.isEncrypted() || newFile.exists()) {
                                continue
                            }
                            FileDirItem(newFile.path, newFile.name, newFile.isDirectory)
                        }
                        val oldFileDirItem = file.toFileDirItem(activity)
                        copy(oldFileDirItem, newFileDirItem, encryptionAction)
                    } else {
                        if (activity.getDoesFilePathExist(newPath)) {
                            continue
                        }

                        val oldFile = File(source.path, child)
                        val oldFileDirItem = oldFile.toFileDirItem(activity)
                        val newFileDirItem =
                            FileDirItem(newPath, newPath.getFilenameFromPath(), oldFile.isDirectory)
                        copy(oldFileDirItem, newFileDirItem, encryptionAction)
                    }
                }
            }
            mTransferredFiles.add(source)
    }

    private fun copyFile(
        source: FileDirItem,
        destination: FileDirItem,
        encryptionAction: EncryptionAction
    ) {
        if (copyMediaOnly && !source.path.isMediaFile()) {
            mCurrentProgress += source.size
            return
        }

        val directory = destination.getParentPath()
        if (!activity.createDirectorySync(directory)) {
            val error =
                String.format(activity.getString(R.string.could_not_create_folder), directory)
            activity.showErrorToast(error)
            mCurrentProgress += source.size
            return
        }

        mCurrFilename = source.name
        var inputStream: InputStream? = null
        var out: OutputStream? = null
        try {
            if (!mDocuments.containsKey(directory) && activity.needsStupidWritePermissions(
                    destination.path
                )
            ) {
                mDocuments[directory] = activity.getDocumentFile(directory)
            }
            out = activity.getFileOutputStreamSync(
                destination.path,
                source.path.getMimeType(),
                mDocuments[directory],
                encryptionAction
            )
            inputStream = activity.getFileInputStreamSync(source.path, encryptionAction)!!

            var copiedSize = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0) {
                out!!.write(buffer, 0, bytes)
                copiedSize += bytes
                mCurrentProgress += bytes
                bytes = inputStream.read(buffer)
            }

            out?.flush()

            if (activity.getDoesFilePathExist(destination.path) &&
                (
                    source.size == copiedSize ||
                        (isEncryption(encryptionAction) && source.size <= copiedSize) ||
                        (isDecryption(encryptionAction) && source.size >= copiedSize)
                    )
            ) {
                mTransferredFiles.add(source)
                if (copyOnly && destination.path.isAudioFast()) {
                    activity.rescanPath(destination.path) {
                        if (activity.config.keepLastModified) {
                            copyOldLastModified(source.path, destination.path)
                            File(destination.path).setLastModified(File(source.path).lastModified())
                        }
                    }
                } else if (activity.config.keepLastModified) {
                    copyOldLastModified(source.path, destination.path)
                    File(destination.path).setLastModified(File(source.path).lastModified())
                }

                if (!isNotEncryption(encryptionAction) && !activity.config.keepAfterEncryptionOperation) {
                    activity.deleteFileBg(source, false)
                }

                if (!copyOnly) {
                    activity.deleteFileBg(source)
                    activity.deleteFromMediaStore(source.path)
                } else if (isHide(hideAction)) {
                    activity.deleteFromMediaStore(source.path)
                }
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            try {
                inputStream?.close()
                out?.close()
            } catch (e: Exception) {
                Log.e("CopyMoveTask", e.toString())
            }
        }
    }

    private fun copyOldLastModified(sourcePath: String, destinationPath: String) {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        var selectionArgs = arrayOf(sourcePath)
        val cursor = activity.applicationContext.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (cursor.moveToFirst()) {
                val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
                val dateModified = cursor.getIntValue(MediaStore.Images.Media.DATE_MODIFIED)

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
                    put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
                }

                selectionArgs = arrayOf(destinationPath)
                activity.applicationContext.contentResolver.update(
                    uri,
                    values,
                    selection,
                    selectionArgs
                )
            }
        }
    }

    companion object {
        private const val INITIAL_PROGRESS_DELAY = 3000L
        private const val PROGRESS_RECHECK_INTERVAL = 500L
    }
}
