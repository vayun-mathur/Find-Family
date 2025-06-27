package com.opengps.locationsharing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage


@Composable
fun Main() {
    var isSetup by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Networking.init()
        if(Networking.userid != null) {
            isSetup = true
        }
        platform.runBackgroundService()
    }
    if(isSetup)
        MapView()
    else {
        SetupPage{
            platform.runBackgroundService()
            isSetup = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPage(completeSetup: ()->Unit) {
    // TODO: prevent already registered IDS

    var subtext by remember { mutableStateOf("") }

    fun checkError(s: String): Boolean {
        if(s.length > 13) {
            subtext = "ID cannot be longer than 13 characters"
        } else if(s.length < 5) {
            subtext = "ID cannot be shorter than 5 characters"
        } else if(s.any{!it.isLetter()}) {
            subtext = "ID can only contain letters"
        } else {
            subtext = ""
            return false
        }
        return true;
    }

    var name by remember { mutableStateOf<String?>(null) }
    var photo by remember { mutableStateOf<String?>(null) }

    val contactPicker = platform.requestPickContact { name_, photo_ ->
        name = name_
        photo = photo_
    }

    Scaffold(topBar = {
        TopAppBar({ Text("TopAppBar") })
    }) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val userID = SimpleOutlinedTextField(
                    "User ID",
                    isError = ::checkError,
                    subtext = { subtext })

                OutlinedButton({
                    contactPicker()
                }) {
                    if(name == null) {
                        Text("Choose Self Contact")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserPicture(photo, name!!.first(), 45.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(name!!)
                        }
                    }
                }

                OutlinedButton({
                    SuspendScope {
                        val finalID = userID().decodeBase26()
                        platform.dataStoreUtils.setLong("userid", finalID.toLong())
                        platform.database.usersDao().upsert(User(finalID, name!!, photo, "Unnamed Location", true, RequestStatus.MUTUAL_CONNECTION, null, null))
                        completeSetup()
                    }
                }, enabled = (name != null) && !checkError(userID())) {
                    Text("Complete Setup")
                }
            }
        }
    }


}