package com.opengps.locationsharing.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.opengps.locationsharing.AndroidPlatform
import com.opengps.locationsharing.platform
import com.opengps.locationsharing.platformInternal

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
                    || intent.action == Intent.ACTION_REBOOT)
            &&
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            if(platformInternal == null)
                platformInternal = AndroidPlatform(context)
            platform.runBackgroundService()
        }
    }
}