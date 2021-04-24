package com.securefilemanager.app.helpers

import android.content.Context
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

open class Config(val context: Context) {
    private val prefs = context.getSharedPrefs()
    private val defaultFontSize = context.resources.getInteger(R.integer.default_font_size)
    private val prefsEncrypted = context.getEncryptedSharedPrefs()

    companion object {
        @Volatile
        private var INSTANCE: Config? = null
        fun getInstance(context: Context): Config {
            if (INSTANCE == null) {
                INSTANCE = Config(context)
            }
            return INSTANCE!!
        }
    }

    var isAppForeground: Boolean
        get() = prefs.getBoolean(IS_APP_FOREGROUND, false)
        set(isAppForeground) = prefs.edit().putBoolean(IS_APP_FOREGROUND, isAppForeground).apply()

    var isAppWizardDone: Boolean
        get() = prefs.getBoolean(IS_APP_WIZARD_DONE, false)
        set(isAppWizardDone) = prefs.edit().putBoolean(IS_APP_WIZARD_DONE, isAppWizardDone).apply()

    var isAppTutorialShowed: Boolean
        get() = prefs.getBoolean(IS_APP_TUTORIAL_SHOWED, false)
        set(isAppTutorialDone) = prefs.edit().putBoolean(IS_APP_TUTORIAL_SHOWED, isAppTutorialDone)
            .apply()

    var isHideTutorialShowed: Boolean
        get() = prefs.getBoolean(IS_HIDE_TUTORIAL_SHOWED, false)
        set(isHideTutorialDone) = prefs.edit()
            .putBoolean(IS_HIDE_TUTORIAL_SHOWED, isHideTutorialDone)
            .apply()

    var isAppBetaWarningShowed: Boolean
        get() = prefs.getBoolean(IS_APP_BETA_WARNING_SHOWED, false)
        set(isAppBetaWarningDone) = prefs.edit()
            .putBoolean(IS_APP_BETA_WARNING_SHOWED, isAppBetaWarningDone)
            .apply()

    var treeUri: String
        get() = prefs.getString(TREE_URI, "")!!
        set(uri) = prefs.edit().putString(TREE_URI, uri).apply()

    var sdCardPath: String
        get() = prefs.getString(SD_CARD_PATH, getDefaultSDCardPath())!!
        set(sdCardPath) = prefs.edit().putString(SD_CARD_PATH, sdCardPath).apply()

    private fun getDefaultSDCardPath() =
        if (prefs.contains(SD_CARD_PATH)) "" else context.getSDCardPath()

    var internalStoragePath: String
        get() = prefs.getString(INTERNAL_STORAGE_PATH, getDefaultInternalPath())!!
        set(internalStoragePath) = prefs.edit()
            .putString(INTERNAL_STORAGE_PATH, internalStoragePath).apply()

    private fun getDefaultInternalPath() =
        if (prefs.contains(INTERNAL_STORAGE_PATH)) "" else context.getInternalStoragePath()

    var hiddenPath: String
        get() = prefs.getString(HIDDEN_PATH, getDefaultHiddenPath())!!
        set(hiddenPath) = prefs.edit()
            .putString(HIDDEN_PATH, hiddenPath).apply()

    private fun getDefaultHiddenPath() =
        if (prefs.contains(HIDDEN_PATH)) "" else context.getHiddenPath()

    var keepLastModified: Boolean
        get() = prefs.getBoolean(SETTINGS_KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefs.edit()
            .putBoolean(SETTINGS_KEEP_LAST_MODIFIED, keepLastModified)
            .apply()

    var keepAfterEncryptionOperation: Boolean
        get() = prefs.getBoolean(SETTINGS_KEEP_AFTER_ENCRYPTION_OPERATION, false)
        set(deleteAfterEncryptionOperation) = prefs.edit()
            .putBoolean(SETTINGS_KEEP_AFTER_ENCRYPTION_OPERATION, deleteAfterEncryptionOperation)
            .apply()

    var disableScreenshots: Boolean
        get() = prefs.getBoolean(SETTINGS_DISABLE_SCREENSHOTS, true)
        set(disableScreenshots) = prefs.edit()
            .putBoolean(SETTINGS_DISABLE_SCREENSHOTS, disableScreenshots)
            .apply()

    var showMediaPreview: Boolean
        get() = prefs.getBoolean(SETTINGS_SHOW_THUMBNAIL_PREVIEW, false)
        set(showMediaPreview) = prefs.edit()
            .putBoolean(SETTINGS_SHOW_THUMBNAIL_PREVIEW, showMediaPreview)
            .apply()

    var isAppLock: Boolean
        get() = prefs.getBoolean(SETTINGS_APP_LOCK, false)
        set(appLock) = prefs.edit().putBoolean(SETTINGS_APP_LOCK, appLock).apply()

    var useBiometricAuthentication: Boolean
        get() = prefs.getBoolean(SETTINGS_USE_BIOMETRIC_AUTHENTICATION, false)
        set(useBiometricAuthentication) = prefs.edit()
            .putBoolean(SETTINGS_USE_BIOMETRIC_AUTHENTICATION, useBiometricAuthentication).apply()

    var lastConflictApplyToAll: Boolean
        get() = prefs.getBoolean(LAST_CONFLICT_APPLY_TO_ALL, true)
        set(lastConflictApplyToAll) = prefs.edit()
            .putBoolean(LAST_CONFLICT_APPLY_TO_ALL, lastConflictApplyToAll).apply()

    var lastConflictResolution: Int
        get() = prefs.getInt(LAST_CONFLICT_RESOLUTION, CONFLICT_SKIP)
        set(lastConflictResolution) = prefs.edit()
            .putInt(LAST_CONFLICT_RESOLUTION, lastConflictResolution).apply()

    var sorting: Int
        get() = prefs.getInt(SORT_ORDER, context.resources.getInteger(R.integer.default_sorting))
        set(sorting) = prefs.edit().putInt(SORT_ORDER, sorting).apply()

    fun saveCustomSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            sorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path.toLowerCase(Locale.getDefault()), value)
                .apply()
        }
    }

    fun getFolderSorting(path: String) =
        prefs.getInt(SORT_FOLDER_PREFIX + path.toLowerCase(Locale.getDefault()), sorting)

    fun removeCustomSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path.toLowerCase(Locale.getDefault())).apply()
    }

    fun hasCustomSorting(path: String) =
        prefs.contains(SORT_FOLDER_PREFIX + path.toLowerCase(Locale.getDefault()))

    var use24HourFormat: Boolean
        get() = prefs.getBoolean(
            SETTINGS_CHANGE_HOUR_FORMAT,
            DateFormat.is24HourFormat(context)
        )
        set(use24HourFormat) = prefs.edit()
            .putBoolean(SETTINGS_CHANGE_HOUR_FORMAT, use24HourFormat).apply()

    var dateFormat: String
        get() = prefs.getString(SETTINGS_CHANGE_DATE_TIME_FORMAT, getDefaultDateFormat())!!
        set(dateFormat) = prefs.edit().putString(SETTINGS_CHANGE_DATE_TIME_FORMAT, dateFormat)
            .apply()

    var theme: Int
        get() = prefs.getInt(SETTINGS_THEME, MODE_NIGHT_FOLLOW_SYSTEM)
        set(theme) = prefs.edit().putInt(SETTINGS_THEME, theme).apply()

    private fun getDefaultDateFormat(): String {
        val format = DateFormat.getDateFormat(context)
        val pattern = (format as SimpleDateFormat).toLocalizedPattern()
        return when (pattern.toLowerCase(Locale.getDefault()).replace(" ", "")) {
            "dd/mm/y" -> DATE_FORMAT_TWO
            "mm/dd/y" -> DATE_FORMAT_THREE
            "y-mm-dd" -> DATE_FORMAT_FOUR
            "dmmmmy" -> DATE_FORMAT_FIVE
            "mmmmdy" -> DATE_FORMAT_SIX
            "mm-dd-y" -> DATE_FORMAT_SEVEN
            "dd-mm-y" -> DATE_FORMAT_EIGHT
            else -> DATE_FORMAT_ONE
        }
    }

    var fontSize: Int
        get() = prefs.getInt(SETTINGS_FONT_SIZE, this.defaultFontSize)
        set(size) = prefs.edit().putInt(SETTINGS_FONT_SIZE, size).apply()

    var passwordHash: String
        get() = prefsEncrypted.getString(PASSWORD_HASH, "")!!
        set(password) = prefsEncrypted.edit().putString(PASSWORD_HASH, password)
            .apply()

    fun isPasswordSet(): Boolean {
        return this.passwordHash != ""
    }

    fun passwordRemove() {
        this.passwordHash = ""
    }

    var homeFolder: String
        get(): String {
            var path = prefs.getString(HOME_FOLDER, "")!!
            if (path.isEmpty() || !File(path).isDirectory) {
                path = context.getInternalStoragePath()
                homeFolder = path
            }
            return path
        }
        set(homeFolder) = prefs.edit().putString(HOME_FOLDER, homeFolder).apply()

    fun addFavorite(path: String) {
        val currFavorites = HashSet(favorites)
        currFavorites.add(path)
        favorites = currFavorites
    }

    fun moveFavorite(oldPath: String, newPath: String) {
        if (!favorites.contains(oldPath)) {
            return
        }

        val currFavorites = HashSet(favorites)
        currFavorites.remove(oldPath)
        currFavorites.add(newPath)
        favorites = currFavorites
    }

    fun removeFavorite(path: String) {
        if (!favorites.contains(path)) {
            return
        }

        val currFavorites = HashSet(favorites)
        currFavorites.remove(path)
        favorites = currFavorites
    }

    var favorites: MutableSet<String>
        get() = prefs.getStringSet(FAVORITES, HashSet())!!
        set(favorites) = prefs.edit().remove(FAVORITES).putStringSet(FAVORITES, favorites).apply()

    var wasAppProtectionHandled: Boolean
        get() = prefs.getBoolean(WAS_APP_PROTECTION_HANDLED, false)
        set(wasAppProtectionHandled) = prefs.edit()
            .putBoolean(WAS_APP_PROTECTION_HANDLED, wasAppProtectionHandled).apply()

}
