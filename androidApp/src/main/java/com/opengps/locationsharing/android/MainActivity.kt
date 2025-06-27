package com.opengps.locationsharing.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.opengps.locationsharing.Main
import com.opengps.locationsharing.Networking
import com.opengps.locationsharing.platform
import com.opengps.locationsharing.platformInternal
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init


class MainActivity : ComponentActivity() {
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceChannel = NotificationChannel(
            "WAYPOINT_ENTER_EXIT",
            "Waypoint Enter/Leave",
            NotificationManager.IMPORTANCE_HIGH
        )

        val serviceChannel2 = NotificationChannel(
            "SHARING_REQUEST",
            "Location Sharing Request",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(serviceChannel2)

        if(platformInternal == null)
            platformInternal = com.opengps.locationsharing.AndroidPlatform(this)

        FileKit.init(this);

        setContent {

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                if(it) {
                    platform.runBackgroundService()
                }
            }
            val launcherBT = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                launcherBT.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            }

            MyApplicationTheme {
                Main()
            }
        }
    }
}