package com.securefilemanager.app.extensions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import androidx.security.crypto.EncryptedFile
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.helpers.EncryptionAction
import com.securefilemanager.app.helpers.crypto.FileCrypto
import com.securefilemanager.app.helpers.ensureBackgroundThread
import com.securefilemanager.app.helpers.isDecryption
import com.securefilemanager.app.helpers.isEncryption
import com.securefilemanager.app.models.FileDirItem
import java.io.*
import java.util.*
import java.util.regex.Pattern

// http://stackoverflow.com/a/40582634/1967672
fun Context.getSDCardPath(): String {
    val directories = getStorageDirectories().filter {
        it != getInternalStoragePath() && !it.equals(
            "/storage/emulated/0",
            true
        )
    }

    var sdCardPath =
        directories.firstOrNull { !physicalPaths.contains(it.toLowerCase(Locale.getDefault())) }
            ?: ""

    // on some devices no method retrieved any SD card path, so test if its not sdcard1 by any chance. It happened on an Android 5.1
    if (sdCardPath.trimEnd('/').isEmpty()) {
        val file = File("/storage/sdcard1")
        if (file.exists()) {
            return file.absolutePath
        }

        sdCardPath = directories.firstOrNull() ?: ""
    }

    val finalPath = sdCardPath.trimEnd('/')
    config.sdCardPath = finalPath
    return finalPath
}

fun Context.hasExternalSDCard() = sdCardPath.isNotEmpty()

fun Context.getStorageDirectories(): Array<String> {
    val paths = HashSet<String>()
    val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
    val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")
    if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
        getExternalFilesDirs(null).filterNotNull().map { it.absolutePath }
            .mapTo(paths) { it.substring(0, it.indexOf("Android/data")) }
    } else {
        val path = this.getExternalFilesDir(null)!!.absolutePath
        val folders = Pattern.compile("/").split(path)
        val lastFolder = folders[folders.size - 1]
        var isDigit = false
        try {
            Integer.valueOf(lastFolder)
            isDigit = true
        } catch (ignored: NumberFormatException) {
        }

        val rawUserId = if (isDigit) lastFolder else ""
        if (TextUtils.isEmpty(rawUserId)) {
            paths.add(rawEmulatedStorageTarget!!)
        } else {
            paths.add(rawEmulatedStorageTarget + File.separator + rawUserId)
        }
    }

    if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
        val rawSecondaryStorages = rawSecondaryStoragesStr!!.split(File.pathSeparator.toRegex())
            .dropLastWhile(String::isEmpty).toTypedArray()
        Collections.addAll(paths, *rawSecondaryStorages)
    }
    return paths.map { it.trimEnd('/') }.toTypedArray()
}

fun Context.getHumanReadablePath(path: String): String =
    getString(
        when (path) {
            internalStoragePath -> R.string.internal
            hiddenPath -> R.string.hidden
            else -> R.string.sd_card
        }
    )

fun Context.humanizePath(path: String): String {
    val trimmedPath = path.trimEnd('/')
    return when (val basePath = path.getBasePath(this)) {
        "/" -> "${getHumanReadablePath(basePath)}$trimmedPath"
        else -> trimmedPath.replaceFirst(basePath, getHumanReadablePath(basePath))
    }
}

fun Context.getInternalStoragePath() =
    if (File("/storage/emulated/0").exists()) "/storage/emulated/0" else this.getExternalFilesDir(
        null
    )!!.absolutePath.trimEnd(
        '/'
    )

fun Context.getHiddenPath(): String = "${this.filesDir.absolutePath}/.hidden"

fun Context.isPathOnSD(path: String) = sdCardPath.isNotEmpty() && path.startsWith(sdCardPath)

fun Context.isPathOnHidden(path: String) = hiddenPath.isNotEmpty() && path.startsWith(hiddenPath)

// no need to use DocumentFile if an SD card is set as the default storage
fun Context.needsStupidWritePermissions(path: String) =
    isPathOnSD(path) && !isSDCardSetAsDefaultStorage()

fun Context.isSDCardSetAsDefaultStorage() =
    sdCardPath.isNotEmpty() && this.getExternalFilesDir(null)!!.absolutePath.equals(
        sdCardPath,
        true
    )

fun Context.tryFastDocumentDelete(path: String, allowDeleteFolder: Boolean): Boolean {
    val document = getFastDocumentFile(path)
    return if (document?.isFile == true || allowDeleteFolder) {
        try {
            DocumentsContract.deleteDocument(contentResolver, document?.uri!!)
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}

fun Context.getFastDocumentFile(path: String): DocumentFile? {
    if (config.sdCardPath.isEmpty()) {
        return null
    }

    val relativePath = Uri.encode(path.substring(config.sdCardPath.length).trim('/'))
    val externalPathPart =
        config.sdCardPath.split("/").lastOrNull(String::isNotEmpty)?.trim('/') ?: return null
    val fullUri = "${config.treeUri}/document/$externalPathPart%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}

fun Context.getDocumentFile(path: String): DocumentFile? {
    var relativePath = path.substring(sdCardPath.length)
    if (relativePath.startsWith(File.separator)) {
        relativePath = relativePath.substring(1)
    }

    return try {
        val treeUri = Uri.parse(config.treeUri)
        var document = DocumentFile.fromTreeUri(applicationContext, treeUri)
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        for (part in parts) {
            document = document?.findFile(part)
        }
        document
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getSomeDocumentFile(path: String) = getFastDocumentFile(path) ?: getDocumentFile(path)

fun Context.scanPathRecursively(path: String, callback: (() -> Unit)? = null) =
    scanPathsRecursively(arrayListOf(path), callback)

fun Context.scanPathsRecursively(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    val allPaths = ArrayList<String>()
    for (path in paths) {
        allPaths.addAll(getPaths(File(path)))
    }
    rescanPaths(allPaths, callback)
}

// avoid calling this multiple times in row, it can delete whole folder contents
fun Context.rescanPaths(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    if (paths.isEmpty()) {
        callback?.invoke()
        return
    }

    for (path in paths) {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = Uri.fromFile(File(path))
            sendBroadcast(this)
        }
    }

    var cnt = paths.size
    MediaScannerConnection.scanFile(applicationContext, paths.toTypedArray(), null) { _, _ ->
        if (--cnt == 0) {
            callback?.invoke()
        }
    }
}

fun Context.rescanPath(path: String, callback: (() -> Unit)? = null) {
    rescanPaths(arrayListOf(path), callback)
}

fun Context.getPaths(file: File): ArrayList<String> {
    val paths = arrayListOf<String>(file.absolutePath)
    if (file.isDirectory) {
        val files = file.listFiles() ?: return paths
        for (curFile in files) {
            paths.addAll(getPaths(curFile))
        }
    }
    return paths
}

fun getFileUri(path: String): Uri = when {
    path.isImageSlow() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    path.isVideoSlow() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    else -> MediaStore.Files.getContentUri("external")
}

// these functions update the mediastore instantly, MediaScannerConnection.scanFileRecursively takes some time to really get applied
fun Context.deleteFromMediaStore(path: String) {
    if (getIsPathDirectory(path)) {
        return
    }

    ensureBackgroundThread {
        try {
            val where = "${MediaStore.MediaColumns.DATA} = ?"
            val args = arrayOf(path)
            contentResolver.delete(getFileUri(path), where, args)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.updateInMediaStore(oldPath: String, newPath: String) {
    ensureBackgroundThread {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, newPath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, newPath.getFilenameFromPath())
            put(MediaStore.MediaColumns.TITLE, newPath.getFilenameFromPath())
        }
        val uri = getFileUri(oldPath)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(oldPath)

        try {
            contentResolver.update(uri, values, selection, selectionArgs)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.updateLastModified(path: String, lastModified: Long) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DATE_MODIFIED, lastModified / 1000)
    }
    File(path).setLastModified(lastModified)
    val uri = getFileUri(path)
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    val selectionArgs = arrayOf(path)

    try {
        contentResolver.update(uri, values, selection, selectionArgs)
    } catch (ignored: Exception) {
    }
}

fun Context.trySAFFileDelete(
    fileDirItem: FileDirItem,
    allowDeleteFolder: Boolean = false,
    callback: ((wasSuccess: Boolean) -> Unit)? = null
) {
    var fileDeleted = tryFastDocumentDelete(fileDirItem.path, allowDeleteFolder)
    if (!fileDeleted) {
        val document = getDocumentFile(fileDirItem.path)
        if (document != null && (fileDirItem.isDirectory == document.isDirectory)) {
            try {
                fileDeleted =
                    (document.isFile || allowDeleteFolder) && DocumentsContract.deleteDocument(
                        applicationContext.contentResolver,
                        document.uri
                    )
            } catch (ignored: Exception) {
                config.treeUri = ""
                config.sdCardPath = ""
            }
        }
    }

    if (fileDeleted) {
        deleteFromMediaStore(fileDirItem.path)
        callback?.invoke(true)
    }
}

fun Context.getFileInputStreamSync(
    path: String,
    encryptionAction: EncryptionAction = EncryptionAction.NONE
): InputStream? =
    if (isDecryption(encryptionAction)) {
        getFileInputEncryptedStreamSync(path)
    } else {
        FileInputStream(File(path))
    }

fun BaseAbstractActivity.getFileOutputStreamSync(
    path: String,
    mimeType: String,
    parentDocumentFile: DocumentFile? = null,
    encryptionAction: EncryptionAction = EncryptionAction.NONE
): OutputStream? {
    val targetFile = File(path)

    return if (needsStupidWritePermissions(path)) {
        var documentFile = parentDocumentFile
        if (documentFile == null) {
            if (getDoesFilePathExist(targetFile.parentFile!!.absolutePath)) {
                documentFile = getDocumentFile(targetFile.parent!!)
            } else {
                documentFile = getDocumentFile(targetFile.parentFile!!.parent!!)
                documentFile = documentFile!!.createDirectory(targetFile.parentFile!!.name)
            }
        }

        if (documentFile == null) {
            showFileCreateError(targetFile.parent!!)
            return null
        }

        try {
            val newDocument = documentFile.createFile(mimeType, path.getFilenameFromPath())
            applicationContext.contentResolver.openOutputStream(newDocument!!.uri)
        } catch (e: Exception) {
            showErrorToast(e)
            null
        }
    } else {
        if (targetFile.parentFile?.exists() == false) {
            targetFile.parentFile!!.mkdirs()
        }

        try {
            if (isEncryption(encryptionAction)) {
                getFileOutputEncryptedStreamSync(targetFile)
            } else {
                FileOutputStream(targetFile)
            }
        } catch (e: Exception) {
            showErrorToast(e)
            null
        }
    }
}

fun Context.getEncryptedFile(file: File): EncryptedFile =
    EncryptedFile.Builder(
        this,
        file,
        FileCrypto.getKey(this),
        FileCrypto.ENCRYPTION_SCHEME
    ).build()

fun Context.getFileInputEncryptedStreamSync(path: String): FileInputStream =
    getEncryptedFile(File(path)).openFileInput()


fun Context.getFileOutputEncryptedStreamSync(file: File): FileOutputStream =
    getEncryptedFile(file).openFileOutput()

fun Context.getDoesFilePathExist(path: String): Boolean =
    File(path).exists()

fun Context.getIsPathDirectory(path: String): Boolean =
    File(path).isDirectory

// avoid these being set as SD card paths
private val physicalPaths = arrayListOf(
    "/storage/sdcard1", // Motorola Xoom
    "/storage/extsdcard", // Samsung SGS3
    "/storage/sdcard0/external_sdcard", // User request
    "/mnt/extsdcard", "/mnt/sdcard/external_sd", // Samsung galaxy family
    "/mnt/external_sd", "/mnt/media_rw/sdcard1", // 4.4.2 on CyanogenMod S3
    "/removable/microsd", // Asus transformer prime
    "/mnt/emmc", "/storage/external_SD", // LG
    "/storage/ext_sd", // HTC One Max
    "/storage/removable/sdcard1", // Sony Xperia Z1
    "/data/sdext", "/data/sdext2", "/data/sdext3", "/data/sdext4", "/sdcard1", // Sony Xperia Z
    "/sdcard2", // HTC One M8s
    "/storage/usbdisk0",
    "/storage/usbdisk1",
    "/storage/usbdisk2"
)

fun Context.standardizePath(path: String): String =
    if (isPathOnHidden(path)) path.replace(hiddenPath, getString(R.string.hidden)) else path

fun Context.getFolderLastModifieds(folder: String): HashMap<String, Long> {
    val lastModifieds = HashMap<String, Long>()
    val projection = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_MODIFIED
    )

    val uri = MediaStore.Files.getContentUri("external")
    val selection =
        "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ?"
    val selectionArgs = arrayOf("$folder/%", "$folder/%/%")

    val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
    cursor?.use {
        if (cursor.moveToFirst()) {
            do {
                try {
                    val lastModified =
                        cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000
                    if (lastModified != 0L) {
                        val name = cursor.getStringValue(MediaStore.Images.Media.DISPLAY_NAME)
                        lastModifieds["$folder/$name"] = lastModified
                    }
                } catch (e: Exception) {
                }
            } while (cursor.moveToNext())
        }
    }

    return lastModifieds
}
