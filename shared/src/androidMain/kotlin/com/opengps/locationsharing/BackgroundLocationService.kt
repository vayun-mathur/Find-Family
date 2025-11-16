package com.opengps.locationsharing

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class BackgroundLocationService : Service() {
    private var serviceJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startUpdatingNotification()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startUpdatingNotification() {
        val locationManager = getSystemService(LocationManager::class.java)
        locationManager.requestLocationUpdates(
            LocationManager.FUSED_PROVIDER,
            SHARE_INTERVAL,
            0F
        ) {}

        serviceJob = SuspendScope {
            while(platformInternal == null)
                platformInternal = AndroidPlatform(this@BackgroundLocationService)
            //TODO: eventually re-enable tor
            //runtime.startDaemonAsync()
            Networking.init()
            while(true) {
                val location = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                if(location != null) {
                    backgroundTask(
                        Coord(
                            location.latitude,
                            location.longitude
                        ),
                        location.speed
                    )
                } else {
                    println("Location unavailable")
                }
                delay(SHARE_INTERVAL)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}