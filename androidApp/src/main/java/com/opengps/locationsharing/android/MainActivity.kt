package com.opengps.locationsharing.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.opengps.locationsharing.Main
import com.opengps.locationsharing.getPlatform
import com.opengps.locationsharing.platformObject
import org.torproject.jni.TorService


class MainActivity : ComponentActivity() {
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
            }
        }, IntentFilter(TorService.ACTION_STATUS), Context.RECEIVER_NOT_EXPORTED)


        bindService(Intent(this, TorService::class.java), object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                //moved torService to a local variable, since we only need it once

                val torService: TorService = (service as TorService.LocalBinder).service

                while (torService.torControlConnection == null) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

                Toast.makeText(this@MainActivity, "Established connection", Toast.LENGTH_LONG)
                    .show()
            }

            override fun onServiceDisconnected(name: ComponentName) {
            }
        }, Context.BIND_AUTO_CREATE)

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