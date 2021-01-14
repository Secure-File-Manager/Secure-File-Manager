package com.securefilemanager.app.dialogs

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.crypto.Password
import kotlinx.android.synthetic.main.dialog_password_authentication.*
import kotlinx.android.synthetic.main.dialog_password_setup.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PasswordSetupDialog(
    val activity: Activity,
    val callback: () -> Unit = {}
) : View.OnKeyListener {
    private val view = View.inflate(activity, R.layout.dialog_password_setup, null)
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordAgainLayout: TextInputLayout

    /**
     * ^.*              : Start
     * (?=.{8,})        : Length
     * (?=.*[a-zA-Z])   : Letters
     * (?=.*\d)         : Digits
     * .*$              : End
     */
    private val passwordPattern =
        "^.*(?=.{8,})(?=.*[a-zA-Z])(?=.*\\d).*$".toRegex()

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        this.setPasswordLayoutWithoutError()
        return false
    }

    init {
        val passwordSetupDialog: PasswordSetupDialog = this

        AlertDialog.Builder(activity).apply {
            setPositiveButton(R.string.ok, null)
            setNegativeButton(R.string.cancel, null)
            setNeutralButton(R.string.password_remove, null)
            create().apply {
                activity.setupDialogStuff(view, this, R.string.password_setup_title, "", false) {
                    passwordLayout = view.password_layout
                    passwordAgainLayout = view.password_again_layout
                    val password: EditText = view.password
                    val passwordAgain: EditText = view.password_again

                    password.setOnKeyListener(passwordSetupDialog)
                    passwordAgain.setOnKeyListener(passwordSetupDialog)
                    showKeyboard(password)

                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        activity.config.passwordRemove()
                        dismiss()
                        callback.invoke()
                    }

                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        when {
                            !passwordPattern.matches(password.value) ->
                                passwordLayout.setError(activity, R.string.invalid_password_pattern)
                            password.value != passwordAgain.value ->
                                passwordAgainLayout.setError(activity, R.string.password_not_match)
                            else -> {
                                setPasswordLayoutWithoutError()
                                progressbar.visibility = View.VISIBLE

                                GlobalScope.launch {
                                    activity.config.passwordHash =
                                        Password().hash(password.value.toByteArray())
                                    dismiss()
                                    callback.invoke()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setPasswordLayoutWithoutError() {
        this.passwordLayout.error = null
        this.passwordAgainLayout.error = null
    }

}
