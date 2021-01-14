package com.securefilemanager.app.dialogs

import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.*
import kotlinx.android.synthetic.main.dialog_compress_as.view.*

class CompressAsDialog(
    val activity: BaseAbstractActivity,
    val path: String,
    val callback: (destination: String, password: CharArray?) -> Unit
) : View.OnKeyListener {
    private val view = View.inflate(activity, R.layout.dialog_compress_as, null)
    private lateinit var passwordAgainLayout: TextInputLayout

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        this.setPasswordLayoutWithoutError()
        return false
    }

    init {
        val compressAsDialog: CompressAsDialog = this
        val filename = path.getFilenameFromPath()
        val indexOfDot =
            if (filename.contains('.') && !activity.getIsPathDirectory(path)) filename.lastIndexOf(".") else filename.length
        val baseFilename = filename.substring(0, indexOfDot)
        var realPath = path.getParentPath()

        view.apply {
            password_layout.isEnabled = password_switch.isChecked
            password_again_layout.isEnabled = password_switch.isChecked

            password_switch.setOnCheckedChangeListener { _, isChecked ->
                password_layout.isEnabled = isChecked
                password_again_layout.isEnabled = isChecked
            }

            file_name.setText(baseFilename)
            file_path.text = activity.humanizePath(realPath)
            file_path.setOnClickListener {
                FilePickerDialog(
                    activity,
                    realPath,
                    pickFile = false,
                    showFAB = true
                ) {
                    file_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.compress_as) {
                    passwordAgainLayout = view.password_again_layout
                    view.password_again.setOnKeyListener(compressAsDialog)
                    showKeyboard(view.file_name)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = view.file_name.value
                        val password = view.password
                        val passwordAgain = view.password_again
                        val passwordEnabled = view.password_switch.isChecked
                        when {
                            name.isEmpty() -> activity.toast(R.string.empty_name)
                            password.value != passwordAgain.value ->
                                passwordAgainLayout.setError(activity, R.string.password_not_match)
                            name.isAValidFilename() -> {
                                val newPath = "$realPath/$name.zip"
                                if (activity.getDoesFilePathExist(newPath)) {
                                    activity.toast(R.string.name_taken)
                                    return@OnClickListener
                                }

                                dismiss()
                                callback(
                                    newPath,
                                    if (password.value.isEmpty() || !passwordEnabled) null else password.value.toCharArray()
                                )
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    })
                }
            }
    }

    private fun setPasswordLayoutWithoutError() {
        this.passwordAgainLayout.error = null
    }
}
