package com.securefilemanager.app.activities

import android.os.Bundle
import com.securefilemanager.app.R
import com.securefilemanager.app.fragments.settings.SettingsManageStorageFragment


class ManageStorageActivity : BaseAbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_storage)
        this.supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, SettingsManageStorageFragment())
            .commit()
    }

}
