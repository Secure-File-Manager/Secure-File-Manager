package com.securefilemanager.app.dialogs

import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.HideAction
import com.securefilemanager.app.helpers.isUnhide
import kotlinx.android.synthetic.main.dialog_radio_group.view.*

/**
 * A dialog for choosing between internal, root, SD card (optional) storage
 *
 * @param activity has to be activity to avoid some Theme.AppCompat issues
 * @param currPath current path to decide which storage should be preselected
 * @param callback an anonymous function
 *
 */
class StoragePickerDialog(
    val activity: BaseAbstractActivity,
    currPath: String,
    isMovingOperation: Boolean = false,
    hideAction: HideAction = HideAction.NONE,
    val callback: (pickedPath: String) -> Unit
) {
    private var mDialog: AlertDialog
    private var radioGroup: RadioGroup
    private var defaultSelectedId = 0

    init {
        val resources = activity.resources
        val layoutParams = RadioGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val view = View.inflate(activity, R.layout.dialog_radio_group, null)
        radioGroup = view.dialog_radio_group
        val basePath = currPath.getBasePath(activity)

        if (!isUnhide(hideAction) && !(isMovingOperation && !activity.isPathOnHidden(currPath))) {
            val hide = View.inflate(activity, R.layout.radio_button, null) as RadioButton
            hide.apply {
                id = ID_HIDE
                text = resources.getString(R.string.hidden)
                isChecked = basePath == context.hiddenPath
                setOnClickListener { hiddenPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(hide, layoutParams)
        }

        val internalButton = View.inflate(activity, R.layout.radio_button, null) as RadioButton
        internalButton.apply {
            id = ID_INTERNAL
            text = resources.getString(R.string.internal)
            isChecked = basePath == context.internalStoragePath
            setOnClickListener { internalPicked() }
            if (isChecked) {
                defaultSelectedId = id
            }
        }
        radioGroup.addView(internalButton, layoutParams)

        if (activity.hasExternalSDCard()) {
            val sdButton = View.inflate(activity, R.layout.radio_button, null) as RadioButton
            sdButton.apply {
                id = ID_SD
                text = resources.getString(R.string.sd_card)
                isChecked = basePath == context.sdCardPath
                setOnClickListener { sdPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(sdButton, layoutParams)
        }

        mDialog = AlertDialog.Builder(activity)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.select_storage)
            }
    }

    private fun internalPicked() {
        mDialog.dismiss()
        callback(activity.internalStoragePath)
    }

    private fun hiddenPicked() {
        mDialog.dismiss()
        callback(activity.hiddenPath)
    }

    private fun sdPicked() {
        mDialog.dismiss()
        callback(activity.sdCardPath)
    }

    companion object {
        private const val ID_INTERNAL = 1
        private const val ID_SD = 2
        private const val ID_HIDE = 4
    }

}
