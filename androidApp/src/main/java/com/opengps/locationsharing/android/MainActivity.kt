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
import com.opengps.locationsharing.getPlatform
import com.opengps.locationsharing.platformObject

class MainActivity : ComponentActivity() {
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceChannel = NotificationChannel(
            "WAYPOINT_ENTER_EXIT",
            "Waypoint Enter/Leave",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

        if(platformObject == null)
            platformObject = com.opengps.locationsharing.AndroidPlatform(this)

        setContent {

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                if(it) {
                    getPlatform().runBackgroundService()
                }
            }
            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            MyApplicationTheme {
                Main()
            }
        }
    }
}