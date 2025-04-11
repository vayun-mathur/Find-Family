package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect


@Composable
fun Main() {
    LaunchedEffect(Unit) {
        getPlatform().runBackgroundService()
    }
    UI()
}