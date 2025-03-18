package com.opengps.locationsharing.android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.opengps.locationsharing.BackgroundLocationService
import com.opengps.locationsharing.Main
import com.opengps.locationsharing.Networking
import com.opengps.locationsharing.Platform
import com.opengps.locationsharing.UI
import com.opengps.locationsharing.getPlatform
import com.opengps.locationsharing.platformObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(platformObject == null)
            platformObject = com.opengps.locationsharing.AndroidPlatform(this)

        setContent {
            MyApplicationTheme {
                Main()
            }
        }
    }
}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        GreetingView("Hello, Android!")
    }
}
