package com.securefilemanager.app.dialogs

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.htmlText
import kotlinx.android.synthetic.main.dialog_delete_app_data.view.*

class DeleteAppDataDialog(activity: Activity) {

    private var previousToast: Toast? = null
    private var appDataDeleteConfirmationCount: Int = 0

    init {
        val view = View.inflate(activity, R.layout.dialog_delete_app_data, null)
        view.message.text = htmlText(activity.getString(R.string.app_data_clear_summary))

        AlertDialog.Builder(activity).apply {
            setPositiveButton(R.string.clear, null)
            setNegativeButton(R.string.cancel, null)
            create().apply {
                activity.setupDialogStuff(
                    view,
                    this,
                    R.string.app_data_clear_title,
                    "",
                    false
                ) {
                    getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        dismiss()
                    }
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        previousToast?.cancel()
                        appDataDeleteConfirmationCount++
                        when (appDataDeleteConfirmationCount) {
                            1 -> previousToast =
                                activity.toastLong(R.string.app_data_clear_confirmation_1)
                            2 -> previousToast =
                                activity.toastLong(R.string.app_data_clear_confirmation_2)
                            3 -> previousToast =
                                activity.toastLong(R.string.app_data_clear_confirmation_3)
                            4 -> previousToast =
                                activity.toastLong(R.string.app_data_clear_confirmation_4)
                            5 -> try {
                                activity.deleteAppData()
                            } catch (e: Exception) {
                                activity.toast(R.string.app_data_error)
                            }
                        }
                    }
                }
            }
        }
    }

}
