package com.securefilemanager.app.helpers

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Looper
import android.text.Spanned
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.text.HtmlCompat
import com.securefilemanager.app.BuildConfig
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.getDrawableById
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val REAL_FILE_PATH = "real_file_path_2"
const val GENERIC_PERM_HANDLER: Int = 1580
const val IMAGE_REQUEST_CODE: Int = 1581
const val VIDEO_REQUEST_CODE: Int = 1582
const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
const val APP_CHANNEL_ID = "FILE_SECURITY"


// shared preferences
const val PREFS_KEY = "Prefs"
const val IS_APP_FOREGROUND = "is_app_foreground"
const val IS_APP_WIZARD_DONE = "is_app_wizard_done"
const val IS_APP_TUTORIAL_SHOWED = "is_app_tutorial_showed"
const val IS_HIDE_TUTORIAL_SHOWED = "is_hide_tutorial_showed"
const val IS_APP_BETA_WARNING_SHOWED = "is_app_beta_warning_showed"
const val TREE_URI = "tree_uri_2"
const val SD_CARD_PATH = "sd_card_path_2"
const val INTERNAL_STORAGE_PATH = "internal_storage_path"
const val HIDDEN_PATH = "hidden_path"
const val LAST_CONFLICT_RESOLUTION = "last_conflict_resolution"
const val LAST_CONFLICT_APPLY_TO_ALL = "last_conflict_apply_to_all"
const val PASSWORD_HASH = "password_hash"
const val WAS_APP_PROTECTION_HANDLED = "was_app_protection_handled"

// Settings
const val SETTINGS_MANAGE_FAVORITES = "settings_manage_favorites"
const val SETTINGS_THEME = "settings_theme"
const val SETTINGS_CHANGE_DATE_TIME_FORMAT = "settings_change_date_time_format"
const val SETTINGS_CHANGE_HOUR_FORMAT = "settings_change_hour_format"
const val SETTINGS_FONT_SIZE = "settings_font_size"
const val SETTINGS_SYSTEM_SETTINGS = "settings_system_settings"
const val SETTINGS_KEEP_LAST_MODIFIED = "settings_keep_last_modified"
const val SETTINGS_KEEP_AFTER_ENCRYPTION_OPERATION = "settings_keep_after_encryption_operation"
const val SETTINGS_DISABLE_SCREENSHOTS = "settings_disable_screenshots"
const val SETTINGS_SHOW_THUMBNAIL_PREVIEW = "settings_show_thumbnail_preview"
const val SETTINGS_SHOW_THUMBNAIL_PREVIEW_CLEAR = "settings_show_thumbnail_preview_clear"
const val SETTINGS_APP_DATA_CLEAR = "settings_app_data_clear"
const val SETTINGS_APP_LOCK = "settings_app_lock"
const val SETTINGS_APP_PASSWORD = "settings_app_password"
const val SETTINGS_USE_BIOMETRIC_AUTHENTICATION = "settings_use_biometric_authentication"
const val SETTINGS_OPEN_BIOMETRIC_SETTINGS = "settings_open_biometric_settings"
const val SETTINGS_APP_LOCK_INFO = "settings_app_lock_info"

// global intents
const val OPEN_DOCUMENT_TREE = 1000
const val REQUEST_SET_AS = 1002

// sorting
const val SORT_ORDER = "sort_order"
const val SORT_FOLDER_PREFIX = "sort_folder_"
const val SORT_BY_NAME = 1
const val SORT_BY_DATE_MODIFIED = 2
const val SORT_BY_SIZE = 4
const val SORT_BY_EXTENSION = 16
const val SORT_DESCENDING = 1024

// permissions
const val PERMISSION_READ_STORAGE = 1
const val PERMISSION_WRITE_STORAGE = 2

// conflict resolving
const val CONFLICT_SKIP = 1
const val CONFLICT_OVERWRITE = 2
const val CONFLICT_MERGE = 3
const val CONFLICT_KEEP_BOTH = 4

// font sizes
const val FONT_SIZE_SMALL = 0
const val FONT_SIZE_MEDIUM = 1
const val FONT_SIZE_LARGE = 2
const val FONT_SIZE_EXTRA_LARGE = 3

fun fontSizes(context: Context) = hashMapOf<String, Int>().apply {
    put(context.getString(R.string.small), FONT_SIZE_SMALL)
    put(context.getString(R.string.medium), FONT_SIZE_MEDIUM)
    put(context.getString(R.string.large), FONT_SIZE_LARGE)
    put(context.getString(R.string.extra_large), FONT_SIZE_EXTRA_LARGE)
}

const val encryptionExtension: String = "aes"
const val encryptionExtensionDotted: String = ".${encryptionExtension}"

val photoExtensions: Array<String>
    get() = arrayOf(
        ".jpg",
        ".png",
        ".jpeg",
        ".bmp",
        ".webp",
        ".heic",
        ".heif"
    )
val videoExtensions: Array<String>
    get() = arrayOf(
        ".mp4",
        ".mkv",
        ".webm",
        ".avi",
        ".3gp",
        ".mov",
        ".m4v",
        ".3gpp"
    )
val audioExtensions: Array<String>
    get() = arrayOf(
        ".mp3",
        ".wav",
        ".wma",
        ".ogg",
        ".m4a",
        ".opus",
        ".flac",
        ".aac"
    )
val rawExtensions: Array<String>
    get() = arrayOf(
        ".dng",
        ".orf",
        ".nef",
        ".arw",
        ".rw2",
        ".cr2",
        ".cr3"
    )

const val DATE_FORMAT_ONE = "dd.MM.yyyy"
const val DATE_FORMAT_TWO = "dd/MM/yyyy"
const val DATE_FORMAT_THREE = "MM/dd/yyyy"
const val DATE_FORMAT_FOUR = "yyyy-MM-dd"
const val DATE_FORMAT_FIVE = "d MMMM yyyy"
const val DATE_FORMAT_SIX = "MMMM d yyyy"
const val DATE_FORMAT_SEVEN = "MM-dd-yyyy"
const val DATE_FORMAT_EIGHT = "dd-MM-yyyy"

val DATE_FORMATS = arrayOf(
    DATE_FORMAT_ONE,
    DATE_FORMAT_TWO,
    DATE_FORMAT_THREE,
    DATE_FORMAT_FOUR,
    DATE_FORMAT_FIVE,
    DATE_FORMAT_SIX,
    DATE_FORMAT_SEVEN,
    DATE_FORMAT_EIGHT
)

const val TIME_FORMAT_12 = "hh:mm a"
const val TIME_FORMAT_24 = "HH:mm"

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}

fun getConflictResolution(resolutions: LinkedHashMap<String, Int>, path: String): Int {
    return if (resolutions.size == 1 && resolutions.containsKey("")) {
        resolutions[""]!!
    } else if (resolutions.containsKey(path)) {
        resolutions[path]!!
    } else {
        CONFLICT_SKIP
    }
}

const val PATH = "path"

// shared preferences
const val HOME_FOLDER = "home_folder"
const val FAVORITES = "favorites"

// open as
const val OPEN_AS_DEFAULT = 0
const val OPEN_AS_TEXT = 1
const val OPEN_AS_IMAGE = 2
const val OPEN_AS_AUDIO = 3
const val OPEN_AS_VIDEO = 4
const val OPEN_AS_OTHER = 5

// message digest algorithms
val MD5 = "MD5"
val SHA1 = "SHA-1"
val SHA256 = "SHA-256"
val SHA512 = "SHA-512"

enum class EncryptionAction {
    NONE, ENCRYPT, DECRYPT
}

enum class HideAction {
    NONE, HIDE, UNHIDE
}

fun isEncryption(encryptionAction: EncryptionAction): Boolean =
    encryptionAction == EncryptionAction.ENCRYPT

fun isDecryption(encryptionAction: EncryptionAction): Boolean =
    encryptionAction == EncryptionAction.DECRYPT

fun isNotEncryption(encryptionAction: EncryptionAction): Boolean =
    encryptionAction == EncryptionAction.NONE

fun isHide(hideAction: HideAction): Boolean =
    hideAction === HideAction.HIDE

fun isUnhide(hideAction: HideAction): Boolean =
    hideAction === HideAction.UNHIDE

fun isNotHide(hideAction: HideAction): Boolean =
    hideAction === HideAction.NONE

fun getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    else -> ""
}

fun createMediaFile(path: String, extension: String): File =
    File.createTempFile(
        SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault()).format(Date()),
        extension,
        File(path)
    )

fun getNotificationId(): Int = (System.currentTimeMillis() / 1000).toInt()

fun getFilePlaceholderDrawables(context: Context): HashMap<String, Drawable> {
    val fileDrawables = HashMap<String, Drawable>()
    hashMapOf<String, Int>().apply {
        put("aep", R.drawable.ic_file_aep)
        put("aes", R.drawable.ic_file_aes)
        put("ai", R.drawable.ic_file_ai)
        put("avi", R.drawable.ic_file_avi)
        put("css", R.drawable.ic_file_css)
        put("csv", R.drawable.ic_file_csv)
        put("dbf", R.drawable.ic_file_dbf)
        put("doc", R.drawable.ic_file_doc)
        put("docx", R.drawable.ic_file_doc)
        put("dwg", R.drawable.ic_file_dwg)
        put("exe", R.drawable.ic_file_exe)
        put("fla", R.drawable.ic_file_fla)
        put("flv", R.drawable.ic_file_flv)
        put("htm", R.drawable.ic_file_html)
        put("html", R.drawable.ic_file_html)
        put("ics", R.drawable.ic_file_ics)
        put("indd", R.drawable.ic_file_indd)
        put("iso", R.drawable.ic_file_iso)
        put("jpg", R.drawable.ic_file_jpg)
        put("jpeg", R.drawable.ic_file_jpg)
        put("js", R.drawable.ic_file_js)
        put("json", R.drawable.ic_file_json)
        put("m4a", R.drawable.ic_file_m4a)
        put("mp3", R.drawable.ic_file_mp3)
        put("mp4", R.drawable.ic_file_mp4)
        put("ogg", R.drawable.ic_file_ogg)
        put("pdf", R.drawable.ic_file_pdf)
        put("plproj", R.drawable.ic_file_plproj)
        put("prproj", R.drawable.ic_file_prproj)
        put("psd", R.drawable.ic_file_psd)
        put("rtf", R.drawable.ic_file_rtf)
        put("sesx", R.drawable.ic_file_sesx)
        put("svg", R.drawable.ic_file_svg)
        put("txt", R.drawable.ic_file_txt)
        put("vcf", R.drawable.ic_file_vcf)
        put("wav", R.drawable.ic_file_wav)
        put("wmv", R.drawable.ic_file_wmv)
        put("xls", R.drawable.ic_file_xls)
        put("xml", R.drawable.ic_file_xml)
        put("zip", R.drawable.ic_file_zip)
    }.forEach { (key, value) ->
        fileDrawables[key] = context.getDrawableById(value) as Drawable
    }
    return fileDrawables
}

fun htmlText(text: String): Spanned =
    HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)

val normalizeRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()


fun getThemes(context: Context): HashMap<String, String> {
    val themes = HashMap<String, String>()
    hashMapOf<Int, Int>().apply {
        put(MODE_NIGHT_NO, R.string.theme_light)
        put(MODE_NIGHT_YES, R.string.theme_dark)
        put(MODE_NIGHT_FOLLOW_SYSTEM, R.string.theme_system)
    }.forEach { (key, value) ->
        themes[key.toString()] = context.getString(value)
    }
    return themes
}
