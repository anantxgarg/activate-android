package com.activate.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean("start_on_boot", false)
            val isEnabled = prefs.getBoolean(OverlayService.KEY_IS_ENABLED, false)

            if (startOnBoot && isEnabled) {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }

                if (hasPermission) {
                    val serviceIntent = Intent(context, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_START
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
