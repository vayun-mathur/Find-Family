package com.opengps.locationsharing

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.PermissionChecker
import androidx.core.database.getStringOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.matthewnelson.kmp.tor.resource.noexec.tor.ResourceLoaderTorNoExec
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.service.TorServiceConfig
import kotlin.random.Random
import kotlin.random.nextULong


private val ServiceConfig = TorServiceConfig.Builder {
    // configure...
}

class AndroidPlatform(private val context: Context): Platform() {

    override val runtimeEnvironment: TorRuntime.Environment by lazy {
        ServiceConfig.newEnvironment(ResourceLoaderTorNoExec::getOrCreate)
    }

    override val dataStore: DataStore<Preferences> = createDataStore(context)

    @SuppressLint("Range")
    @Composable
    override fun requestPickContact(callback: (String, String?)->Unit): ()->Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if(uri == null) return@rememberLauncherForActivityResult
            val cur = context.contentResolver.query(uri, null, null, null)!!
            if (cur.moveToFirst()) {
                val name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val photo = cur.getStringOrNull(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                callback(name, photo)
            }
            cur.close()
        }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if(it) {
                launcher.launch()
            }
        }
        return {
            if(PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PermissionChecker.PERMISSION_GRANTED) {
                launcher.launch()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    override val database = Room.databaseBuilder(context, AppDatabase::class.java, "database.db")
        .setDriver(AndroidSQLiteDriver()).addMigrations(*migrations).build()

    override fun runBackgroundService() {

        if(PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED) {
            context.startForegroundService(Intent(context, BackgroundLocationService::class.java))
        }
    }

    override fun createNotification(s: String, channelId: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Location Sharing")
            .setContentText(s)
            .setSmallIcon(R.drawable.baseline_notifications_24) // Replace with your icon
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
        val notificationManager = getSystemService(context, NotificationManager::class.java)!!
        notificationManager.notify(Random.nextInt(), notification)
    }

    override fun copyToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = android.content.ClipData.newPlainText("text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun startScanBluetoothDevices(setRSSI: (String, Int) -> Unit): ()->Unit {
        if(PermissionChecker.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PermissionChecker.PERMISSION_GRANTED
            && PermissionChecker.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PermissionChecker.PERMISSION_GRANTED) {
            val blm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    if (result == null) return
                    if (result.device.name == null) return
                    if (!nearBluetoothDevices.any { it.address == result.device.address })
                        nearBluetoothDevices.add(
                            BluetoothDevice(
                                Random.nextULong(),
                                result.device.name,
                                result.device.address
                            )
                        )
                    setRSSI(result.device.address, result.rssi)
                }
            }
            if(blm.adapter.bluetoothLeScanner != null) {
                blm.adapter.bluetoothLeScanner
                blm.adapter.bluetoothLeScanner.startScan(callback)
                return { blm.adapter.bluetoothLeScanner.stopScan(callback) }
            } else {
                return {}
            }
        }
        return {}
    }

    override val batteryLevel: Float
        get() {
            val batteryStatus: Intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
            val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return level * 100f / scale
        }

    override val name: String = "Android"
}

fun createDataStore(context: Context): DataStore<Preferences> =
    createDataStore(
        producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
    )

@SuppressLint("StaticFieldLeak")
actual var platformInternal: Platform? = null