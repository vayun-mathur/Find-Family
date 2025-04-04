package com.opengps.locationsharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun UI() {
    val navController = rememberNavController()
    NavHost(navController, "map") {
        composable("map") {
            MapView(navController)
        }
        composable("settings") {
            SettingsView(navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(navController: NavHostController) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        })
    }) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(16.dp)) {
            var useTor by remember { mutableStateOf(getPlatform().dataStoreUtils.getBooleanOrDefault("useTor", false)) }
            Column() {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use Tor")
                    Spacer(Modifier.weight(1f))
                    Checkbox(useTor, { checked ->
                        useTor = checked
                        SuspendScope {
                            getPlatform().dataStoreUtils.setBoolean("useTor", useTor)
                        }
                    })
                }
                Text("This option requires running Orbot on your device. It connects to the hidden onion service rather than the clearnet url.", Modifier.fillMaxWidth(0.8f), style = MaterialTheme.typography.labelMedium)
            }

        }
    }
}
