package com.securefilemanager.app.extensions

import android.app.Activity
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedSharedPreferences
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.AuthenticationActivity
import com.securefilemanager.app.activities.DecompressActivity
import com.securefilemanager.app.activities.FavoritesActivity
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.helpers.crypto.PrefCrypto
import com.securefilemanager.app.services.UnlockAppService
import com.securefilemanager.app.services.ZipManagerService
import org.jetbrains.annotations.NotNull
import java.io.File

fun Context.getSharedPrefs(): SharedPreferences =
    getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

fun Context.getEncryptedSharedPrefs() = EncryptedSharedPreferences.create(
    this,
    PrefCrypto.KEY_ALIAS,
    PrefCrypto.getKey(this),
    PrefCrypto.PREF_KEY_ENCRYPTION_SCHEME,
    PrefCrypto.PREF_VALUE_ENCRYPTION_SCHEME
)

val Context.isBiometricPresent: Boolean
    get() = BiometricManager.from(this).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS

val Context.isBiometricNotSpecified: Boolean
    get() = BiometricManager.from(this)
        .canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

fun Context.toastLong(id: Int): Toast? = toast(getString(id), Toast.LENGTH_LONG)

fun Context.toastLong(msg: String): Toast? = toast(msg, Toast.LENGTH_LONG)

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT): Toast? = toast(getString(id), length)

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT): Toast? {
    try {
        if (isOnMainThread()) {
            return doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (e: Exception) {
    }
    return null
}

private fun doToast(context: Context, message: String, length: Int): Toast? {
    val toast = Toast.makeText(context, message, length)
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            toast.show()
            return toast
        }
    } else {
        toast.show()
        return toast
    }
    return null
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

val Context.sdCardPath: String get() = config.sdCardPath
val Context.internalStoragePath: String get() = config.internalStoragePath
val Context.hiddenPath: String get() = config.hiddenPath

// some helper functions were taken from https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
fun Context.getRealPathFromURI(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    if (isDownloadsDocument(uri)) {
        val id = DocumentsContract.getDocumentId(uri)
        if (id.areDigitsOnly()) {
            val newUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"),
                id.toLong()
            )
            val path = getDataColumn(newUri)
            if (path != null) {
                return path
            }
        }
    } else if (isExternalStorageDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val parts = documentId.split(":")
        if (parts[0].equals("primary", true)) {
            return "${this.getExternalFilesDir(null)?.absolutePath}/${parts[1]}"
        }
    } else if (isMediaDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val split = documentId.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()

        val contentUri = when (split[0]) {
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val selection = "_id=?"
        val selectionArgs = arrayOf(split[1])
        val path = getDataColumn(contentUri, selection, selectionArgs)
        if (path != null) {
            return path
        }
    }

    return getDataColumn(uri)
}

fun Context.getDataColumn(
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null
): String? {
    var cursor: Cursor? = null
    try {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            val data = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
            if (data != "null") {
                return data
            }
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}

private fun isMediaDocument(uri: Uri) = uri.authority == "com.android.providers.media.documents"

private fun isDownloadsDocument(uri: Uri) =
    uri.authority == "com.android.providers.downloads.documents"

private fun isExternalStorageDocument(uri: Uri) =
    uri.authority == "com.android.externalstorage.documents"

fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(
    this,
    getPermissionString(permId)
) == PackageManager.PERMISSION_GRANTED

fun Context.getFilePublicUri(file: File, applicationId: String): Uri {
    // for images/videos/gifs try getting a media content uri first, like content://media/external/images/media/438
    // if media content uri is null, get our custom uri like content://com.simplemobiletools.gallery.provider/external_files/emulated/0/DCIM/IMG_20171104_233915.jpg
    var uri = if (file.isMediaFile()) {
        getMediaContentUri(file.absolutePath)
    } else {
        getMediaContent(file.absolutePath, MediaStore.Files.getContentUri("external"))
    }

    if (uri == null) {
        uri = FileProvider.getUriForFile(this, "$applicationId.provider", file)
    }

    return uri!!
}

fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri("external")
    }

    return getMediaContent(path, uri)
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = MediaStore.Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            val id = cursor.getIntValue(MediaStore.Images.Media._ID).toString()
            return Uri.withAppendedPath(uri, id)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}

fun Context.getMimeTypeFromUri(uri: Uri): String {
    var mimetype = uri.path?.getMimeType() ?: ""
    if (mimetype.isEmpty()) {
        try {
            mimetype = contentResolver.getType(uri) ?: ""
        } catch (e: IllegalStateException) {
        }
    }
    return mimetype
}

fun Context.ensurePublicUri(path: String, applicationId: String): Uri? {
    val uri = Uri.parse(path)
    return if (uri.scheme == "content") {
        uri
    } else {
        val newPath = if (uri.toString().startsWith("/")) uri.toString() else uri.path
        val file = File(newPath!!)
        getFilePublicUri(file, applicationId)
    }
}

fun Context.updateSDCardPath() {
    ensureBackgroundThread {
        val oldPath = config.sdCardPath
        config.sdCardPath = getSDCardPath()
        if (oldPath != config.sdCardPath) {
            config.treeUri = ""
        }
    }
}

fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}

fun Context.getTimeFormat() = if (config.use24HourFormat) TIME_FORMAT_24 else TIME_FORMAT_12

fun Context.getResolution(path: String): Point? {
    return if (path.isImageFast() || path.isImageSlow()) {
        path.getImageResolution()
    } else if (path.isVideoFast() || path.isVideoSlow()) {
        getVideoResolution(path)
    } else {
        null
    }
}

fun Context.getVideoResolution(path: String): Point? {
    var point = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        val height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
        Point(width, height)
    } catch (ignored: Exception) {
        null
    }

    if (point == null && path.startsWith("content://", true)) {
        try {
            val fd = contentResolver.openFileDescriptor(Uri.parse(path), "r")?.fileDescriptor
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)
            val width =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            val height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            point = Point(width, height)
        } catch (ignored: Exception) {
        }
    }

    return point
}

fun Context.getFontSizeText() = getString(
    when (config.fontSize) {
        FONT_SIZE_SMALL -> R.string.small
        FONT_SIZE_MEDIUM -> R.string.medium
        FONT_SIZE_LARGE -> R.string.large
        else -> R.string.extra_large
    }
)

fun Context.getTextSize() = when (config.fontSize) {
    FONT_SIZE_SMALL -> resources.getDimension(R.dimen.smaller_text_size)
    FONT_SIZE_MEDIUM -> resources.getDimension(R.dimen.bigger_text_size)
    FONT_SIZE_LARGE -> resources.getDimension(R.dimen.big_text_size)
    else -> resources.getDimension(R.dimen.extra_big_text_size)
}

fun Context.hasDeviceCamera(): Boolean =
    this.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)!!

fun Context.getUriForFile(@NotNull file: File): Uri =
    FileProvider.getUriForFile(
        this,
        AUTHORITY,
        file
    )

fun Context.getNotificationManager(): NotificationManager =
    this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val Context.config: Config get() = Config.getInstance(applicationContext)

fun Context.getDrawableById(id: Int): Drawable? = ContextCompat.getDrawable(this, id)

fun Context.openZip(path: String) =
    Intent(this, DecompressActivity::class.java).apply {
        putExtra(DecompressActivity.EXTRA_PATH, path)
    }.also {
        this.startActivity(it)
    }

fun Context.decompressZip(path: String, destination: String? = null, password: CharArray? = null) =
    Intent(this, ZipManagerService::class.java).apply {
        action = ZipManagerService.ACTION_DECOMPRESSION
        putExtra(ZipManagerService.EXTRA_PATH, path)
        putExtra(ZipManagerService.EXTRA_DESTINATION, destination)
        putExtra(ZipManagerService.EXTRA_PASSWORD, password)
    }.also {
        this.startService(it)
    }

fun Context.getVersion(): String =
    this.packageManager.getPackageInfo(this.packageName, 0).versionName

fun Context.openUri(uri: String) =
    Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(uri)
    }.also {
        this.startActivity(it)
    }

fun Context.openSystemSettings() {
    this.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    )
}

fun Context.openFavorites() {
    this.startActivity(
        Intent(this, FavoritesActivity::class.java)
    )
}

fun Context.isBiometricSet(): Boolean =
    this.config.useBiometricAuthentication && this.isBiometricPresent

fun Context.isAuthenticatorSet(): Boolean =
    this.config.isAppLock && (this.config.isPasswordSet() || this.isBiometricSet())

fun Context.isAuthenticatorNotSet(): Boolean = !this.isAuthenticatorSet()

fun Context.openAuthenticationActivity() {
    this.startActivity(AuthenticationActivity.getIntent(this))
}

fun Context.openSettingsActivity() {
    this.startActivity(Intent(Settings.ACTION_SETTINGS))
}

fun Context.startStopUnlockAppService() {
    if (this.isAuthenticatorSet()) {
        if (this.config.wasAppProtectionHandled) {
            this.startUnlockAppService()
        }
    } else {
        this.stopUnlockAppService()
    }
}

fun Context.startUnlockAppService() {
    this.startService(UnlockAppService.getStartIntent(this))
}

fun Context.stopUnlockAppService() {
    this.startService(UnlockAppService.getStopIntent(this))
}

fun Context.startAuthenticationActivity() {
    if (
        !this.config.isAppForeground ||
        this.config.wasAppProtectionHandled ||
        this.isAuthenticatorNotSet()
    ) {
        return
    }
    this.openAuthenticationActivity()
}

fun Context.appLock() {
    if (this.config.wasAppProtectionHandled) {
        this.stopUnlockAppService()
    }
    this.config.wasAppProtectionHandled = false
}

fun Context.hasProperStoredTreeUri(): Boolean {
    val uri = config.treeUri
    val hasProperUri = contentResolver.persistedUriPermissions.any { it.uri.toString() == uri }
    if (!hasProperUri) {
        config.treeUri = ""
    }
    return hasProperUri
}
