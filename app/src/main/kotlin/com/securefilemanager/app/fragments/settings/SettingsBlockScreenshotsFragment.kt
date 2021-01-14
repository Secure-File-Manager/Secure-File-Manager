package com.securefilemanager.app.fragments.settings

import android.os.Bundle

class SettingsBlockScreenshotsFragment : SettingsAbstractFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        this.preferenceScreen = this.initScreen(listOf(this.initBlockScreenshots()))
    }
}
