package com.securefilemanager.app.dialogs

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.config
import com.securefilemanager.app.extensions.setupDialogStuff

class BetaWarningDialog(activity: Activity) {

    init {
        val view = View.inflate(activity, R.layout.dialog_beta_warning, null)

        AlertDialog.Builder(activity).apply {
            setPositiveButton(R.string.ok) { dialog, _ ->
                activity.config.isAppBetaWarningShowed = true
                dialog.dismiss()
            }
            create().apply {
                activity.setupDialogStuff(view, this)
            }
        }
    }

}
