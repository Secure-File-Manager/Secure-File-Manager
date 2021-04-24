package com.securefilemanager.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.securefilemanager.app.R
import com.securefilemanager.app.dialogs.PasswordAuthenticationDialog
import com.securefilemanager.app.extensions.*
import kotlinx.android.synthetic.main.activity_authenticate.*
import kotlin.properties.Delegates


class AuthenticationActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mAuthenticationSuccessCallback: (success: Boolean) -> Unit
    private lateinit var mBiometricPrompt: BiometricPrompt
    private lateinit var mPromptInfo: BiometricPrompt.PromptInfo
    private var mPasswordIsSet by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_authenticate)

        authenticate_coordinator_layout.setOnClickListener(this)
        authenticate_button.setOnClickListener(this)

        this.mPasswordIsSet = this.config.isPasswordSet()
        this.mAuthenticationSuccessCallback = {
            if (it) {
                this.config.wasAppProtectionHandled = true
                this.startUnlockAppService()
                this.finish()
            }
        }

        this.mBiometricPrompt = createBiometricPrompt(this.mAuthenticationSuccessCallback)
        this.mPromptInfo = createPromptInfo()

        this.authenticate()
        this.addFlagsSecure()
        this.setTheme()
    }

    override fun onResume() {
        super.onResume()
        if (this.config.wasAppProtectionHandled) {
            this.finish()
        } else {
            this.authenticate()
        }
    }

    override fun onClick(v: View?) {
        this.authenticate()
    }

    override fun onBackPressed() {
        this.quitApp(false)
    }

    private fun authenticate() {
        if (this.config.isAppLock) {
            when {
                this.isBiometricSet() -> {
                    this.mBiometricPrompt.authenticate(createPromptInfo())
                }
                this.mPasswordIsSet -> {
                    PasswordAuthenticationDialog(this, this.mAuthenticationSuccessCallback) {}
                }
                else -> {
                    this.mAuthenticationSuccessCallback.invoke(true)
                }
            }
        } else {
            this.mAuthenticationSuccessCallback.invoke(true)
        }
    }

    private fun createBiometricPrompt(callback: (success: Boolean) -> Unit): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)
        val activity = this
        val callbackAuthentication = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && mPasswordIsSet) {
                    PasswordAuthenticationDialog(activity, callback) { authenticate() }
                    return
                }
                callback(false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                callback(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                callback(true)
            }
        }

        return BiometricPrompt(this, executor, callbackAuthentication)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .apply {
                setTitle(getString(R.string.prompt_info_title))
                setConfirmationRequired(false)
                if (mPasswordIsSet) {
                    setNegativeButtonText(getString(R.string.prompt_info_use_app_password))
                } else {
                    setNegativeButtonText(getString(R.string.cancel))
                }
            }.build()
    }

    companion object {
        fun getIntent(context: Context) =
            Intent(context, AuthenticationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }

}
