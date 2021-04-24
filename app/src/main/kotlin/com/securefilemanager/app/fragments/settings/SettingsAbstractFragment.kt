package com.securefilemanager.app.fragments.settings

import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.preference.*
import com.bumptech.glide.Glide
import com.securefilemanager.app.R
import com.securefilemanager.app.dialogs.DeleteAppDataDialog
import com.securefilemanager.app.dialogs.PasswordSetupDialog
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

abstract class SettingsAbstractFragment : PreferenceFragmentCompat() {

    private var preferenceAppLock: SwitchPreferenceCompat? = null
    private var preferenceAppPassword: Preference? = null
    private var preferenceUseBiometric: SwitchPreferenceCompat? = null
    private var preferenceOpenBiometricSettings: Preference? = null
    private var preferenceAppLockInfo: Preference? = null

    override fun onResume() {
        super.onResume()
        preferenceUseBiometric?.icon = getUseBiometricIcon()
        preferenceUseBiometric?.summaryOn = getUseBiometricSummaryOn()
        preferenceOpenBiometricSettings?.isVisible = canShowOpenBiometricSetting()
        preferenceAppLock?.icon = getAppLockIcon()
        preferenceAppLock?.summaryOn = getAppLockSummaryOn()
    }

    protected fun initScreen(preferences: List<Preference> = listOf()): PreferenceScreen =
        this.preferenceManager
            .createPreferenceScreen(this.preferenceManager.context)
            .apply {
                preferences.forEach {
                    this.addPreference(it)
                }
            }


    private fun formatDateSample(format: String): String =
        DateFormat.format(
            format,
            Calendar.getInstance(Locale.ENGLISH).apply {
                timeInMillis = sampleTS
            }
        ).toString()

    private fun initCategory(
        screen: PreferenceScreen,
        titleId: Int,
        preferences: List<Preference> = listOf()
    ): PreferenceCategory =
        PreferenceCategory(context).apply {
            screen.addPreference(this)
            title = getString(titleId)
            preferences.forEach {
                this.addPreference(it)
            }
        }

    protected fun initCategoryGeneral(
        screen: PreferenceScreen,
        preferences: List<Preference>
    ): PreferenceCategory =
        this.initCategory(screen, R.string.settings_general, preferences)

    protected fun initCategoryFileOperations(
        screen: PreferenceScreen,
        preferences: List<Preference>
    ): PreferenceCategory =
        this.initCategory(screen, R.string.file_operations, preferences)

    protected fun initCategorySecurity(
        screen: PreferenceScreen,
        preferences: List<Preference> = listOf()
    ): PreferenceCategory =
        this.initCategory(screen, R.string.security, preferences)

    protected fun initTheme(): ListPreference =
        ListPreference(context).apply {
            val activity = requireActivity()
            val config = activity.config
            val theme = config.theme
            val themes = getThemes(activity)
            val valueString = theme.toString()
            key = SETTINGS_THEME
            title = getString(R.string.theme_title)
            icon = activity.getDrawableById(R.drawable.ic_pallete_outline_vector)
            summary = "%s"
            entries = themes.values.toTypedArray()
            entryValues = themes.keys.toTypedArray()
            value = valueString
            setDefaultValue(valueString)
            setOnPreferenceChangeListener { _, newValue ->
                config.theme = newValue.toInt()
                activity.setTheme()
                true
            }
        }

    protected fun initFavorites(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            key = SETTINGS_MANAGE_FAVORITES
            title = getString(R.string.manage_favorites)
            icon = activity.getDrawableById(R.drawable.ic_star_off_outline_vector)
            setOnPreferenceClickListener {
                activity.openFavorites()
                true
            }
        }

    protected fun initDateTimeFormat(): ListPreference =
        ListPreference(context).apply {
            val activity = requireActivity()
            val config = activity.config
            val dateFormat = config.dateFormat
            key = SETTINGS_CHANGE_DATE_TIME_FORMAT
            title = getString(R.string.change_date_and_time_format_title)
            icon = activity.getDrawableById(R.drawable.ic_time_vector)
            summary = "%s"
            entries = DATE_FORMATS.map { formatDateSample(it) }.toTypedArray()
            entryValues = DATE_FORMATS
            value = dateFormat
            setDefaultValue(dateFormat)
            setOnPreferenceChangeListener { _, newValue ->
                config.dateFormat = newValue as String
                true
            }
        }

    protected fun initChangeHourFormat(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            key = SETTINGS_CHANGE_HOUR_FORMAT
            title = getString(R.string.use_24_hour_time_format_title)
            icon = activity.getDrawableById(R.drawable.ic_hourglass_empty_vector)
            summaryOn = getString(R.string.use_24_hour_time_format_title_summary_on)
            summaryOff = getString(R.string.use_24_hour_time_format_title_summary_off)
            isChecked = config.use24HourFormat
            setOnPreferenceChangeListener { _, newValue ->
                config.use24HourFormat = newValue as Boolean
                true
            }
        }

    protected fun initFontSize(): ListPreference =
        ListPreference(context).apply {
            val activity = requireActivity()
            val config = activity.config
            val fontSizes = fontSizes(context)
            val fontSizesStrings = fontSizes.keys.toTypedArray()
            val fontSize = context.getFontSizeText()
            key = SETTINGS_FONT_SIZE
            title = getString(R.string.font_size)
            icon = activity.getDrawableById(R.drawable.ic_format_size_vector)
            summary = "%s"
            entries = fontSizesStrings
            entryValues = fontSizesStrings
            value = fontSize
            setDefaultValue(fontSize)
            setOnPreferenceChangeListener { _, newValue ->
                config.fontSize = fontSizes[newValue as String]!!
                true
            }
        }

    protected fun initAppSystemSettings(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            key = SETTINGS_SYSTEM_SETTINGS
            title = getString(R.string.app_system_settings)
            icon = activity.getDrawableById(R.drawable.ic_settings_vector)
            setOnPreferenceClickListener {
                activity.openSystemSettings()
                true
            }
        }

    protected fun initKeepLastModified(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            key = SETTINGS_KEEP_LAST_MODIFIED
            title = getString(R.string.keep_last_modified_title)
            icon = activity.getDrawableById(R.drawable.ic_update_vector)
            summaryOn = getString(R.string.keep_last_modified_summary_on)
            summaryOff = getString(R.string.keep_last_modified_summary_off)
            isChecked = config.keepLastModified
            setOnPreferenceChangeListener { _, newValue ->
                config.keepLastModified = newValue as Boolean
                true
            }
        }

    protected fun initKeepAfterEncryption(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            key = SETTINGS_KEEP_AFTER_ENCRYPTION_OPERATION
            title = getString(R.string.keep_after_encryption_operation_title)
            icon = activity.getDrawableById(R.drawable.ic_file_sync_vector)
            summaryOn = getString(R.string.keep_after_encryption_operation_summary_on)
            summaryOff = getString(R.string.keep_after_encryption_operation_summary_off)
            isChecked = config.keepAfterEncryptionOperation
            setOnPreferenceChangeListener { _, newValue ->
                config.keepAfterEncryptionOperation = newValue as Boolean
                true
            }
        }

    private fun getBlockScreenshotsIcon(): Drawable? {
        val activity = requireActivity()
        val config = activity.config
        return if (config.disableScreenshots) {
            activity.getDrawableById(R.drawable.ic_cellphone_screenshot_green_vector)
        } else {
            activity.getDrawableById(R.drawable.ic_cellphone_screenshot_red_vector)
        }
    }

    protected fun initBlockScreenshots(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            key = SETTINGS_DISABLE_SCREENSHOTS
            title = getString(R.string.disable_screenshots_title)
            icon = getBlockScreenshotsIcon()
            summaryOn = getString(R.string.disable_screenshots_summary_on)
            summaryOff = getString(R.string.disable_screenshots_summary_off)
            isChecked = config.disableScreenshots
            setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue == true
                config.disableScreenshots = isChecked
                icon = getBlockScreenshotsIcon()
                if (isChecked) {
                    activity.window?.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    activity.window?.clearFlags(
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                }
                true
            }
        }

    private fun clearGlideCache() {
        val activity = requireActivity()
        GlobalScope.launch {
            Glide
                .get(activity)
                .clearDiskCache()
        }
        activity.toast(R.string.media_thumbnail_clear_cleared)
    }

    protected fun initShowThumbnailPreview(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            key = SETTINGS_SHOW_THUMBNAIL_PREVIEW
            title = getString(R.string.show_media_thumbnail_title)
            icon = activity.getDrawableById(R.drawable.ic_image_vector)
            summaryOn = getString(R.string.show_media_thumbnail_summary_on)
            summaryOff = getString(R.string.show_media_thumbnail_summary_off)
            isChecked = config.showMediaPreview
            setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue == true
                config.showMediaPreview = isChecked
                if (!isChecked) {
                    clearGlideCache()
                }
                true
            }
        }

    protected fun initMediaThumbnailClear(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            key = SETTINGS_SHOW_THUMBNAIL_PREVIEW_CLEAR
            title = getString(R.string.media_thumbnail_clear_title)
            icon = activity.getDrawableById(R.drawable.ic_delete_outline_vector)
            setOnPreferenceClickListener {
                clearGlideCache()
                true
            }
        }

    protected fun initAppDataClear(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            key = SETTINGS_APP_DATA_CLEAR
            title = getString(R.string.app_data_clear_title)
            icon = activity.getDrawableById(R.drawable.ic_delete_forever_red_vector)
            setOnPreferenceClickListener {
                clearAppData()
                true
            }
        }

    private fun clearAppData() = DeleteAppDataDialog(requireActivity())

    private fun getAppLockIcon(): Drawable? {
        val activity = requireActivity()
        val config = activity.config
        return if (config.isAppLock) {
            if (activity.isAuthenticatorSet()) {
                activity.getDrawableById(R.drawable.ic_cellphone_key_green_vector)
            } else {
                activity.getDrawableById(R.drawable.ic_cellphone_key_orange_vector)
            }
        } else {
            activity.getDrawableById(R.drawable.ic_cellphone_key_red_vector)
        }
    }

    private fun getAppLockSummaryOn(): String {
        val activity = requireActivity()
        val config = activity.config
        val useBiometricAuthentication = config.useBiometricAuthentication
        return if (activity.isAuthenticatorSet()) {
            getString(R.string.app_lock_summary_on)
        } else {
            if (useBiometricAuthentication) {
                getString(R.string.app_lock_summary_on_warning_biometric)
            } else {
                getString(R.string.app_lock_summary_on_warning)
            }
        }
    }

    private fun initAppLock(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            val isAppLock = config.isAppLock
            key = SETTINGS_APP_LOCK
            title = getString(R.string.app_lock_title)
            icon = getAppLockIcon()
            summaryOn = getAppLockSummaryOn()
            summaryOff = getString(R.string.app_lock_summary_off)
            isChecked = isAppLock
            setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue == true
                config.isAppLock = isChecked
                activity.config.wasAppProtectionHandled = isChecked
                activity.startStopUnlockAppService()
                summaryOn = getAppLockSummaryOn()
                icon = getAppLockIcon()
                preferenceAppPassword?.isVisible = isChecked
                preferenceUseBiometric?.isVisible = isChecked
                preferenceOpenBiometricSettings?.isVisible = canShowOpenBiometricSetting()
                preferenceAppLockInfo?.isVisible = canShowAppLockInfo()
                true
            }
        }

    private fun getAppPasswordSummary(): String {
        val activity = requireActivity()
        val config = activity.config
        return if (config.isPasswordSet())
            getString(R.string.set_app_password_summary_on)
        else
            getString(R.string.set_app_password_summary_off)
    }

    private fun getAppPasswordIcon(): Drawable? {
        val activity = requireActivity()
        val config = activity.config
        return if (config.isPasswordSet()) {
            activity.getDrawableById(R.drawable.ic_form_textbox_password_green_vector)
        } else {
            activity.getDrawableById(R.drawable.ic_form_textbox_password_red_vector)
        }
    }

    private fun initAppPassword(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            val config = activity.config
            key = SETTINGS_APP_PASSWORD
            title = getString(R.string.set_app_password_title)
            icon = getAppPasswordIcon()
            isVisible = config.isAppLock
            summary = getAppPasswordSummary()
            setOnPreferenceClickListener {
                PasswordSetupDialog(activity) {
                    activity.startStopUnlockAppService()
                    activity.runOnUiThread {
                        summary = getAppPasswordSummary()
                        icon = getAppPasswordIcon()
                        preferenceAppLock?.summaryOn = getAppLockSummaryOn()
                        preferenceAppLock?.icon = getAppLockIcon()
                    }
                }
                true
            }
        }

    private fun getUseBiometricIcon(): Drawable? {
        val activity = requireActivity()
        val config = activity.config
        return if (config.useBiometricAuthentication) {
            if (activity.isBiometricPresent) {
                activity.getDrawableById(R.drawable.ic_fingerprint_green_vector)
            } else {
                activity.getDrawableById(R.drawable.ic_fingerprint_orange_vector)
            }
        } else {
            activity.getDrawableById(R.drawable.ic_fingerprint_vector)
        }
    }

    private fun getUseBiometricSummaryOn(): String {
        val activity = requireActivity()
        return if (activity.isBiometricPresent) {
            getString(R.string.use_biometric_authentication_summary_on)
        } else {
            getString(R.string.use_biometric_authentication_summary_on_warning)
        }
    }

    private fun initUseBiometric(): SwitchPreferenceCompat =
        SwitchPreferenceCompat(context).apply {
            val activity = requireActivity()
            val config = activity.config
            val useBiometricAuthentication = config.useBiometricAuthentication
            key = SETTINGS_USE_BIOMETRIC_AUTHENTICATION
            title = getString(R.string.use_biometric_authentication_title)
            icon = getUseBiometricIcon()
            summaryOn = getUseBiometricSummaryOn()
            summaryOff = getString(R.string.use_biometric_authentication_summary_off)
            isVisible = config.isAppLock
            isChecked = useBiometricAuthentication
            setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue == true
                config.useBiometricAuthentication = isChecked
                activity.startStopUnlockAppService()
                icon = getUseBiometricIcon()
                summaryOn = getUseBiometricSummaryOn()
                preferenceAppLock?.summaryOn = getAppLockSummaryOn()
                preferenceAppLock?.icon = getAppLockIcon()
                preferenceAppLockInfo?.isVisible = canShowAppLockInfo()
                preferenceOpenBiometricSettings?.isVisible = canShowOpenBiometricSetting()
                true
            }
        }

    private fun canShowOpenBiometricSetting(): Boolean {
        val activity = requireActivity()
        val config = activity.config
        return activity.isBiometricNotSpecified && config.isAppLock && config.useBiometricAuthentication
    }

    private fun initOpenBiometricSettings(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            key = SETTINGS_OPEN_BIOMETRIC_SETTINGS
            icon = activity.getDrawableById(R.drawable.ic_error_outline_vector)
            summary = getString(R.string.app_open_biometric_settings_summary)
            isVisible = canShowOpenBiometricSetting()
            setOnPreferenceClickListener {
                requireActivity().openSettingsActivity()
                true
            }
        }

    private fun canShowAppLockInfo(): Boolean {
        val config = requireActivity().config
        return config.isAppLock && config.useBiometricAuthentication
    }

    private fun initAppLockInfo(): Preference =
        Preference(context).apply {
            val activity = requireActivity()
            key = SETTINGS_APP_LOCK_INFO
            icon = activity.getDrawableById(R.drawable.ic_info_vector)
            summary = getString(R.string.app_lock_info_summary)
            isVisible = canShowAppLockInfo()
        }

    protected fun initAppLockSet(): List<Preference> {
        this.preferenceAppLock = this.initAppLock()
        this.preferenceAppPassword = this.initAppPassword()
        this.preferenceUseBiometric = this.initUseBiometric()
        this.preferenceOpenBiometricSettings = initOpenBiometricSettings()
        this.preferenceAppLockInfo = this.initAppLockInfo()
        return listOf(
            preferenceAppLock!!,
            preferenceAppPassword!!,
            preferenceUseBiometric!!,
            preferenceOpenBiometricSettings!!,
            preferenceAppLockInfo!!,
        )
    }

    companion object {
        private const val sampleTS = 1557964800000    // May 16, 2019

    }

}
