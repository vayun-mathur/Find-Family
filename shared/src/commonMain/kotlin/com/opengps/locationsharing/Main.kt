package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


@Composable
fun Main() {
    LaunchedEffect(Unit) {
        SuspendScope {
            Networking.init()
        }
        getPlatform().runBackgroundService()
    }
    UI()
}