package com.securefilemanager.app.extensions

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import com.securefilemanager.app.BuildConfig
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.dialogs.WritePermissionDialog
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.models.FileDirItem
import kotlinx.android.synthetic.main.dialog_title.view.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.HashMap

fun Activity.sharePathIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                if (resolveActivity(packageManager) != null) {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } else {
                    toast(R.string.no_app_found)
                }
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            }
        }
    }
}

fun Activity.sharePathsIntent(paths: ArrayList<String>, applicationId: String) {
    ensureBackgroundThread {
        if (paths.size == 1) {
            sharePathIntent(paths.first(), applicationId)
        } else {
            val uriPaths = ArrayList<String>()
            val newUris = paths.map {
                val uri = getFinalUriFromPath(it, applicationId) ?: return@ensureBackgroundThread
                uriPaths.add(uri.path!!)
                uri
            } as ArrayList<Uri>

            var mimeType = uriPaths.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = paths.getMimeType()
            }

            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, newUris)

                try {
                    if (resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                    } else {
                        toast(R.string.no_app_found)
                    }
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        showErrorToast(e)
                    }
                }
            }
        }
    }
}

fun Activity.setAsIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_ATTACH_DATA
            setDataAndType(newUri, getUriMimeType(path, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(this, getString(R.string.set_as))

            if (resolveActivity(packageManager) != null) {
                startActivityForResult(chooser, REQUEST_SET_AS)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun Activity.openPathIntent(
    path: String,
    forceChooser: Boolean,
    applicationId: String,
    forceMimeType: String = "",
    extras: HashMap<String, Boolean> = HashMap()
) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        val mimeType =
            if (forceMimeType.isNotEmpty()) forceMimeType else getUriMimeType(path, newUri)
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(newUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            for ((key, value) in extras) {
                putExtra(key, value)
            }

            putExtra(REAL_FILE_PATH, path)

            if (resolveActivity(packageManager) != null) {
                val chooser = Intent.createChooser(this, getString(R.string.open_with))
                try {
                    startActivity(if (forceChooser) chooser else this)
                } catch (e: NullPointerException) {
                    showErrorToast(e)
                }
            } else {
                if (!tryGenericMimeType(this, mimeType, newUri)) {
                    toast(R.string.no_app_found)
                }
            }
        }
    }
}

fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}

fun Activity.tryGenericMimeType(intent: Intent, mimeType: String, uri: Uri): Boolean {
    var genericMimeType = mimeType.getGenericMimeType()
    if (genericMimeType.isEmpty()) {
        genericMimeType = "*/*"
    }

    intent.setDataAndType(uri, genericMimeType)
    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        false
    }
}

fun BaseAbstractActivity.deleteFolderBg(
    fileDirItem: FileDirItem,
    deleteMediaOnly: Boolean = true,
    callback: ((wasSuccess: Boolean) -> Unit)? = null
) {
    val folder = File(fileDirItem.path)
    if (folder.exists()) {
        val filesArr = folder.listFiles()
        if (filesArr == null) {
            runOnUiThread {
                callback?.invoke(true)
            }
            return
        }

        val files = filesArr.toMutableList().filter { !deleteMediaOnly || it.isMediaFile() }
        for (file in files) {
            deleteFileBg(file.toFileDirItem(applicationContext), false) { }
        }

        if (folder.listFiles()?.isEmpty() == true) {
            deleteFileBg(fileDirItem, true) { }
        }
    }
    runOnUiThread {
        callback?.invoke(true)
    }
}

fun BaseAbstractActivity.deleteFiles(
    files: ArrayList<FileDirItem>,
    allowDeleteFolder: Boolean = false,
    callback: ((wasSuccess: Boolean) -> Unit)? = null
) {
    ensureBackgroundThread {
        deleteFilesBg(files, allowDeleteFolder, callback)
    }
}

fun BaseAbstractActivity.deleteFilesBg(
    files: ArrayList<FileDirItem>,
    allowDeleteFolder: Boolean = false,
    callback: ((wasSuccess: Boolean) -> Unit)? = null
) {
    if (files.isEmpty()) {
        runOnUiThread {
            callback?.invoke(true)
        }
        return
    }

    var wasSuccess = false

    handleSAFDialog(files[0].path) {
        if (!it) {
            return@handleSAFDialog
        }

        files.forEachIndexed { index, file ->
            deleteFileBg(file, allowDeleteFolder) {
                if (it) {
                    wasSuccess = true
                }

                if (index == files.size - 1) {
                    runOnUiThread {
                        callback?.invoke(wasSuccess)
                    }
                }
            }
        }
    }
}

fun BaseAbstractActivity.deleteFileBg(
    fileDirItem: FileDirItem,
    allowDeleteFolder: Boolean = false,
    callback: ((wasSuccess: Boolean) -> Unit)? = null
) {
    val path = fileDirItem.path
    val file = File(path)
    if (file.absolutePath.startsWith(internalStoragePath) && !file.canWrite()) {
        callback?.invoke(false)
        return
    }

    var fileDeleted = (!file.exists() && file.length() == 0L) || file.delete()
    if (fileDeleted) {
        deleteFromMediaStore(path)
        runOnUiThread {
            callback?.invoke(true)
        }
    } else {
        if (getIsPathDirectory(file.absolutePath) && allowDeleteFolder) {
            fileDeleted = deleteRecursively(file)
        }

        if (!fileDeleted) {
            if (needsStupidWritePermissions(path)) {
                handleSAFDialog(path) {
                    if (it) {
                        trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                    }
                }
            }
        }
    }
}

private fun deleteRecursively(file: File): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child)
        }
    }

    return file.delete()
}

fun Activity.scanPathRecursively(path: String, callback: (() -> Unit)? = null) {
    applicationContext.scanPathRecursively(path, callback)
}

fun Activity.scanPathsRecursively(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    applicationContext.scanPathsRecursively(paths, callback)
}

fun Activity.rescanPaths(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    applicationContext.rescanPaths(paths, callback)
}

fun Activity.rescanPath(path: String, callback: (() -> Unit)? = null) {
    applicationContext.rescanPath(path, callback)
}

fun BaseAbstractActivity.renameFile(
    oldPath: String,
    newPath: String,
    callback: ((success: Boolean) -> Unit)? = null
) {
    if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            if (!it) {
                return@handleSAFDialog
            }

            val document = getSomeDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                runOnUiThread {
                    callback?.invoke(false)
                }
                return@handleSAFDialog
            }

            try {
                ensureBackgroundThread {
                    try {
                        DocumentsContract.renameDocument(
                            applicationContext.contentResolver,
                            document.uri,
                            newPath.getFilenameFromPath()
                        )
                    } catch (ignored: FileNotFoundException) {
                        // FileNotFoundException is thrown in some weird cases, but renaming works just fine
                    }
                    updateInMediaStore(oldPath, newPath)
                    rescanPaths(arrayListOf(oldPath, newPath)) {
                        if (!config.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        deleteFromMediaStore(oldPath)
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false)
                }
            }
        }
    } else if (File(oldPath).renameTo(File(newPath))) {
        if (File(newPath).isDirectory) {
            deleteFromMediaStore(oldPath)
            rescanPaths(arrayListOf(newPath)) {
                runOnUiThread {
                    callback?.invoke(true)
                }
                scanPathRecursively(newPath)
            }
        } else {
            if (!config.keepLastModified) {
                File(newPath).setLastModified(System.currentTimeMillis())
            }
            deleteFromMediaStore(oldPath)
            scanPathsRecursively(arrayListOf(newPath)) {
                runOnUiThread {
                    callback?.invoke(true)
                }
            }
        }
    } else {
        runOnUiThread {
            callback?.invoke(false)
        }
    }
}

fun BaseAbstractActivity.showFileCreateError(path: String) {
    val error = String.format(getString(R.string.could_not_create_file), path)
    config.treeUri = ""
    showErrorToast(error)
}

fun BaseAbstractActivity.createDirectorySync(directory: String): Boolean {
    if (getDoesFilePathExist(directory)) {
        return true
    }

    if (needsStupidWritePermissions(directory)) {
        val documentFile = getDocumentFile(directory.getParentPath()) ?: return false
        val newDir = documentFile.createDirectory(directory.getFilenameFromPath())
        return newDir != null
    }

    return File(directory).mkdirs()
}

fun Activity.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.package_name), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    toast(R.string.value_copied_to_clipboard)
}

fun Activity.setupDialogStuff(
    view: View,
    dialog: AlertDialog,
    titleId: Int = 0,
    titleText: String = "",
    canceledOnTouchOutside: Boolean = true,
    callback: (() -> Unit)? = null
) {
    if (isDestroyed || isFinishing) {
        return
    }

    var title: TextView? = null
    if (titleId != 0 || titleText.isNotEmpty()) {
        title = View.inflate(this, R.layout.dialog_title, null) as TextView
        title.dialog_title_textview.apply {
            if (titleText.isNotEmpty()) {
                text = titleText
            } else {
                setText(titleId)
            }
        }
    }

    dialog.apply {
        setView(view)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCustomTitle(title)
        setCanceledOnTouchOutside(canceledOnTouchOutside)
        show()
    }
    callback?.invoke()
}


fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.tryOpenPathIntent(
    path: String,
    forceChooser: Boolean,
    openAsType: Int = OPEN_AS_DEFAULT
) {
    if (path.isZipFile()) {
        this.openZip(path)
    } else {
        this.openPath(path, forceChooser, openAsType)
    }
}

fun Activity.openPath(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT) {
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID, getMimeType(openAsType))
}

private fun getMimeType(type: Int) = when (type) {
    OPEN_AS_DEFAULT -> ""
    OPEN_AS_TEXT -> "text/*"
    OPEN_AS_IMAGE -> "image/*"
    OPEN_AS_AUDIO -> "audio/*"
    OPEN_AS_VIDEO -> "video/*"
    else -> "*/*"
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}

fun Activity.quitApp(canLock: Boolean = true) {
    if (canLock) {
        this.appLock()
    }
    this.finishAffinity()
}

fun Activity.addFlagsSecure() {
    if (this.config.disableScreenshots) {
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}

fun Activity.deleteAppData() {
    val packageName = this.applicationContext.packageName
    Runtime.getRuntime().exec("pm clear $packageName")
    this.quitApp()
}

fun BaseAbstractActivity.isShowingSAFDialog(path: String): Boolean {
    return if (isPathOnSD(path) && !isSDCardSetAsDefaultStorage() && (config.treeUri.isEmpty() || !hasProperStoredTreeUri())) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        if (resolveActivity(packageManager) == null) {
                            type = "*/*"
                        }

                        if (resolveActivity(packageManager) != null) {
                            checkedDocumentPath = path
                            startActivityForResult(this, OPEN_DOCUMENT_TREE)
                        } else {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun Activity.setTheme() = setDefaultNightMode(this.config.theme)
