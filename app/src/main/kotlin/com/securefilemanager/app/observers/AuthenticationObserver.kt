package com.securefilemanager.app.observers

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.securefilemanager.app.extensions.config
import com.securefilemanager.app.extensions.startAuthenticationActivity
import com.securefilemanager.app.extensions.startStopUnlockAppService

class AuthenticationObserver(activity: Activity) : LifecycleObserver {

    private var mActivity: Activity = activity

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackground() {
        this.mActivity.config.isAppForeground = false
        this.mActivity.startStopUnlockAppService()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        this.mActivity.config.isAppForeground = true
        this.mActivity.startStopUnlockAppService()
        this.mActivity.startAuthenticationActivity()
    }

}
