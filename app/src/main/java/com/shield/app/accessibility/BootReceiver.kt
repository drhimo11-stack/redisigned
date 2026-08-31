package com.shield.app.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            if (ShieldKeepAliveService.wasActive(context)) {
                ShieldKeepAliveService.start(context)
            }
        }
    }
}
