package com.securefilemanager.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.IntentFilter
import com.securefilemanager.app.extensions.appLock

class LockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_LOCKED_BOOT_COMPLETED, ACTION_SCREEN_OFF -> {
                context?.appLock()
            }
        }
    }

    companion object {

        fun getIntent() = IntentFilter().apply { addAction(ACTION_SCREEN_OFF) }

    }

}
