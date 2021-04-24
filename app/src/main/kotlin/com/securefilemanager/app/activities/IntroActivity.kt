package com.securefilemanager.app.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.viewpager.widget.ViewPager
import com.github.appintro.AppIntro2
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.addFlagsSecure
import com.securefilemanager.app.extensions.appLock
import com.securefilemanager.app.extensions.config
import com.securefilemanager.app.extensions.setTheme
import com.securefilemanager.app.fragments.intro.*
import com.securefilemanager.app.observers.AuthenticationObserver
import com.securefilemanager.app.receivers.LockReceiver

class IntroActivity : AppIntro2() {

    private lateinit var mAuthenticationObserver: AuthenticationObserver

    private var lockReceiver: LockReceiver = LockReceiver()
    override val layoutId = R.layout.activity_intro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mAuthenticationObserver = AuthenticationObserver(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this.mAuthenticationObserver)

        this.registerReceiver(this.lockReceiver, LockReceiver.getIntent())

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            this.appLock()
        }

        this.addFlagsSecure()
        this.setTheme()

        val isWizardMode = this.intent.extras?.getBoolean(WIZARD_MODE) ?: WIZARD_MODE_DEFAULT

        this.isWizardMode = isWizardMode
        this.isSystemBackButtonLocked = isWizardMode
        this.isIndicatorEnabled = true

        this.setImmersiveMode()
        this.showStatusBar(true)
        this.setStatusBarColor(Color.TRANSPARENT)
        this.setNavBarColor(Color.TRANSPARENT)
        this.setCustomTransformer(PageTransformerParallax())
        this.setIndicatorColor(
            selectedIndicatorColor = this.getColor(R.color.color_primary_dark),
            unselectedIndicatorColor = this.getColor(R.color.divider_grey)
        )

        this.addSlide(IntroPermissionFragment.newInstance())
        this.addSlide(IntroLockFragment.newInstance())
        this.addSlide(IntroHidingFragment.newInstance())
        this.addSlide(IntroEncryptingFragment.newInstance())
        this.addSlide(IntroOtherFragment.newInstance())
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this.mAuthenticationObserver)
        this.unregisterReceiver(this.lockReceiver)
        super.onDestroy()
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        this.finishActivity()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        this.config.isAppWizardDone = true
        this.finishActivity()
    }

    override fun onResume() {
        super.onResume()
        this.setImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        this.setImmersiveMode()
    }

    private fun finishActivity() {
        this.setResult(RESULT_OK, null)
        this.finish()
    }

    class PageTransformerParallax : ViewPager.PageTransformer {

        override fun transformPage(page: View, position: Float) {
            if (position > -1 && position < 1) {
                try {
                    page.findViewById<View>(R.id.main).translationX = -position
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }

    }

    companion object {

        private const val WIZARD_MODE = "wizard_mode"
        private const val WIZARD_MODE_DEFAULT = true

        fun getIntent(context: Context, wizardMode: Boolean = WIZARD_MODE_DEFAULT) =
            Intent(context, IntroActivity::class.java).apply {
                this.putExtra(WIZARD_MODE, wizardMode)
            }
    }

}
