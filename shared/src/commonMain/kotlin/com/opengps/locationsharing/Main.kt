package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun Main() {
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            Networking.init()
            getPlatform().runBackgroundService()
        }
    }
    UI()
}