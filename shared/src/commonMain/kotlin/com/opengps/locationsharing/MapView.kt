package com.opengps.locationsharing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.asSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toLocalDateTime
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centerOnMarker
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.disableFlingZoom
import ovh.plrapps.mapcompose.api.enableZooming
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.hasMarker
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onLongPress
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.cos
import kotlin.math.pow
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val client = HttpClient { install(HttpCache) }
private val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
    client.get("https://tile.openstreetmap.org/$zoomLvl/$col/$row.png").bodyAsChannel().asSource()
}

private const val maxLevel = 18
private val mapSize = 256 * 2.0.pow(maxLevel).toInt()
val state = MapState(maxLevel + 1, mapSize, mapSize, workerCount = 16).apply {
    addLayer(tileStreamProvider)
    enableZooming()
    disableFlingZoom()
}

@Composable
fun UserPicture(user: User, size: Dp) {
    if(user.photo != null) {
        AsyncImage(user.photo, null, Modifier.clip(CircleShape).size(size).border(2.dp, Color.Black, CircleShape), contentScale = ContentScale.FillWidth)
    } else {
        Box(Modifier.clip(CircleShape).size(size).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).background(Color.Green)) {
            Text(user.name.first().toString(), Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}

@Composable
fun Circle(
    modifier: Modifier,
    mapState: MapState,
    position: Offset,
    color: Color,
    borderColor: Color,
    radius: Float,
    isScaling: Boolean
) {
    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        val side = if (isScaling) radius else radius / mapState.scale
        drawCircle(
            color,
            center = position,
            radius = side,
        )
        drawCircle(
            borderColor,
            center = position,
            radius = side,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

fun SuspendScope(block: suspend () -> Unit): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        block()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapView() {
    val platform = getPlatform()
    val usersDao = platform.database.usersDao()
    val users = remember { mutableStateMapOf<ULong, User>() }

    var selectedID by remember { mutableStateOf<ULong?>(null) }
    var addPersonPopupEnabled by remember { mutableStateOf(false) }
    var addWaypointPopupEnable by remember { mutableStateOf(false) }
    var longHeldPoint by remember { mutableStateOf(Coord(0.0,0.0)) }
    var editingWaypoint by remember { mutableStateOf(false) }
    var editingWaypointPosition by remember { mutableStateOf(Pair(0.0,0.0)) }
    var editingWaypointRadius by remember { mutableStateOf(0.0) }
    val waypoints: SnapshotStateMap<ULong, Waypoint> = remember { mutableStateMapOf() }

    fun addUserMarker(user: User) {
        val loc = latestLocations[user.id]?: return
        val res = doProjection(loc.coord)
        state.addMarker(user.id.toString(), res.first, res.second, relativeOffset = Offset(-0.5f, -0.5f)) {
            UserPicture(user, 50.dp)
        }
    }

    fun addWaypointMarker(waypoint: Waypoint) {
        val res = doProjection(waypoint.coord)
        state.addMarker(waypoint.id.toString(), res.first, res.second) {
            if(state.scale > 0.6)
                Icon(Icons.Default.LocationOn, null, tint = Color.Blue)
        }
    }

    LaunchedEffect(Unit) {
        SuspendScope {
            waypoints.putAll(platform.database.waypointDao().getAll().associateBy { it.id })
            users.putAll(usersDao.getAll().associateBy { it.id })
            while(Networking.userid == null) { delay(500) }
            if(users.values.isEmpty()) {
                val newUser = User(
                    Networking.userid!!,
                    "Me",
                    null,
                    "Unnamed Location",
                    true,
                    true
                )
                usersDao.upsert(newUser)
                users[newUser.id] = newUser
            }

            state.removeAllMarkers()
            if(selectedID != null && users[selectedID] != null) {
                addUserMarker(users[selectedID!!]!!)
                state.centerOnMarker(selectedID!!.toString())
            } else {
                users.values.forEach(::addUserMarker)
            }
            waypoints.values.forEach(::addWaypointMarker)
        }
    }

    @Composable
    fun WaypointCard(waypoint: Waypoint) {
        Card(Modifier.clickable {
            selectedID = waypoint.id
            SuspendScope {
                state.centerOnMarker(waypoint.id.toString())
            }
        }) {
            var usersString by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                while(true) {
                    val usersWithin = users.values.filter { it.locationName == waypoint.name }
                    usersString = usersWithin.joinToString { it.name } + when(usersWithin.size) {
                        0 -> "nobody is"
                        1 -> " is"
                        else -> " are"
                    } + " currently here"
                    delay(1000)
                }
            }
            ListItem(
                headlineContent = { Text(waypoint.name, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(usersString) }
            )
        }
    }

    @Composable
    fun UserCard(user: User, showSupportingContent: Boolean) {
        Card(Modifier.clickable {
            selectedID = user.id
            SuspendScope {
                state.centerOnMarker(user.id.toString())
            }
        }) {
            var lastUpdatedTime by remember { mutableStateOf("") }
            var sinceString by remember { mutableStateOf("") }
            var speed by remember { mutableStateOf(0.0F) }
            LaunchedEffect(Unit) {
                while(true) {
                    if(latestLocations.containsKey(user.id)) {
                        speed = latestLocations[user.id]!!.speed
                        lastUpdatedTime = timestring(latestLocations[user.id]!!.timestamp)
                    } else {
                        lastUpdatedTime = "Never"
                    }
                    if(users[user.id] == null) {
                        delay(1000)
                        continue
                    }
                    val sinceTime = users[user.id]!!.lastLocationChangeTime.toLocalDateTime(TimeZone.currentSystemDefault())
                    val timeSinceEntry = Clock.System.now() - users[user.id]!!.lastLocationChangeTime
                    if(users[user.id]!!.locationName == "Unnamed Location") {
                        sinceString = ""
                    } else if(timeSinceEntry < 60.seconds) {
                        sinceString = "Since just now"
                    } else if(timeSinceEntry < 15.minutes) {
                        sinceString = "Since ${timeSinceEntry.inWholeMinutes} minutes ago"
                    } else {
                        val formattedTime = sinceTime.format(LocalDateTime.Format {
                            amPmHour(Padding.NONE)
                            chars(":")
                            minute()
                            chars(" ")
                            amPmMarker("am", "pm")
                        })
                        val formattedDate = when(sinceTime.date.toEpochDays() - Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays()) {
                            0 -> "today"
                            1 -> "yesterday"
                            else -> sinceTime.date.format(LocalDate.Format {
                                monthName(MonthNames.ENGLISH_ABBREVIATED)
                                chars(" ")
                                dayOfMonth()
                            })
                        }
                        sinceString = "Since $formattedTime $formattedDate"
                    }
                    delay(1000)
                }
            }

            ListItem(
                leadingContent = {
                    Column(Modifier.width(65.dp)) {
                        UserPicture(user, 65.dp)
                        Spacer(Modifier.height(4.dp))
                        latestLocations[user.id]?.battery?.let {
                            BatteryBar(it)
                        }
                    }
                                 },
                headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) },
                supportingContent = if(showSupportingContent) {
                    {Text("At ${user.locationName}\nUpdated $lastUpdatedTime\n$sinceString")}
                } else null, trailingContent = if(showSupportingContent){
                    {Text("$speed m/s")}
                } else null)
        }
    }

    LaunchedEffect(Unit) {
        state.onLongPress { x, y ->
            longHeldPoint = doInverseProjection(x, y)
            addWaypointPopupEnable = true
        }
        state.onMarkerClick { id, x, y ->
            selectedID = id.toULong()
            SuspendScope {
                state.centerOnMarker(id)
            }
        }
    }

    LaunchedEffect(state.centroidY, state.centroidX) {
        if(editingWaypoint) {
            state.moveMarker(selectedID!!.toString(), state.centroidX, state.centroidY)
            editingWaypointPosition = Pair(state.centroidX, state.centroidY)
        }
    }

    LaunchedEffect(latestLocations) {
        latestLocations.forEach { (id, location) ->
            if(state.hasMarker(id.toString())) {
                val res = doProjection(location.coord)
                state.moveMarker(id.toString(), res.first, res.second)
            } else {
                if(users[id] != null)
                    addUserMarker(users[id]!!)
            }
            if(users[id] != null) {
                val waypointOverlap = waypoints.values.firstOrNull { waypoint ->
                    havershine(location.coord, waypoint.coord) < waypoint.range
                }
                val newLocationName = waypointOverlap?.name?: ("Unnamed Location")
                if(users[id]!!.locationName != newLocationName) {
                    users[id]!!.locationName = newLocationName
                    users[id]!!.lastLocationChangeTime = Clock.System.now()
                    usersDao.upsert(users[id]!!)
                }
            }
        }
    }
    val requestPickContact1 = platform.requestPickContact { name, photo ->
        SuspendScope {
            val newUser = users[selectedID]!!.copy(name = name, photo = photo)
            users[selectedID!!] = newUser
            usersDao.upsert(newUser)
            state.removeMarker(selectedID!!.toString())
            addUserMarker(users[selectedID!!]!!)
        }
    }
    BasicDialog(addWaypointPopupEnable, { addWaypointPopupEnable = false }) {
        Text("Add Saved Location", style = MaterialTheme.typography.headlineMedium)
        var waypointName by remember { mutableStateOf("") }
        OutlinedTextField(
            waypointName,
            { waypointName = it },
            label = { Text("Saved Place Name") })

        var waypointRadius by remember { mutableStateOf("") }
        OutlinedTextField(
            waypointRadius,
            { waypointRadius = it },
            label = { Text("Saved Place Range (Radius)") }, suffix = { Text("meters") })

        Row {
            OutlinedButton(
                {
                    val id = Random.nextULong()
                    waypoints[id] =
                        Waypoint(id, waypointName, waypointRadius.toDouble(), longHeldPoint, mutableListOf())
                    SuspendScope {
                        platform.database.waypointDao().upsert(waypoints[id]!!)
                    }
                    addWaypointPopupEnable = false
                    addWaypointMarker(waypoints[id]!!)
                },
                enabled = (waypointName.isNotEmpty() && waypointRadius.isPositiveNumber())
            ) {
                Text("Add Location")
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton({ addWaypointPopupEnable = false }) {
                Text("Cancel")
            }
        }
    }
    BasicDialog(addPersonPopupEnabled, { addPersonPopupEnabled = false }) {
        var contactName by remember { mutableStateOf("") }
        var contactPhoto by remember { mutableStateOf<String?>(null) }
        val requestPickContact2 = platform.requestPickContact { name, photo ->
            contactName = name
            contactPhoto = photo
        }

        Box(Modifier.clickable { requestPickContact2() }) {
            if (contactName.isNotEmpty())
                UserCard(User(Random.nextULong(), contactName, contactPhoto, "", false, false), false)
            else
                Card {
                    ListItem(
                        leadingContent = {
                            Box(
                                Modifier.clip(CircleShape).size(50.dp).border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ).background(Color.Green)
                            )
                        },
                        headlineContent = {
                            Text(
                                "Select Contact",
                                fontWeight = FontWeight.Bold
                            )
                        })
                }
        }
        var recipientID by remember { mutableStateOf("") }
        OutlinedTextField(
            recipientID,
            { recipientID = it },
            label = { Text("Contact's User ID") })

        Text("Your contact will also need to enable location sharing within their app by entering ${Networking.userid!!.encodeBase26()}")

        var receive by remember { mutableStateOf(true) }
        var send by remember { mutableStateOf(true) }
        Column() {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show on Map")
                Spacer(Modifier.weight(1f))
                Checkbox(receive, { receive = it })
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Share your location")
                Spacer(Modifier.weight(1f))
                Checkbox(send, { send = it })
            }
        }

        OutlinedButton(
            {
                val trueID = recipientID.decodeBase26()
                addPersonPopupEnabled = false
                val newUser = User(trueID, contactName, contactPhoto, "", receive, send)
                users[trueID] = newUser
                SuspendScope {
                    usersDao.upsert(newUser)
                }
            },
            enabled = (contactName.isNotEmpty() && recipientID.isNotEmpty())
        ) {
            Text("Start Location Sharing")
        }
    }
    BottomSheetScaffold(topBar = {
        val actions:  @Composable RowScope.() -> Unit = {
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.Add, null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Person") },
                        onClick = { addPersonPopupEnabled = true; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Saved Location") },
                        onClick = { addWaypointPopupEnable = true; expanded = false }
                    )
                }
            }
        }
        if (selectedID != null) {
            if (users[selectedID] != null) {
                val user = users[selectedID]!!
                TopAppBar(title = { Text(user.name) }, navigationIcon = {
                    IconButton(onClick = { selectedID = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }, actions = actions)
            } else {
                val waypoint = waypoints[selectedID]!!
                TopAppBar(title = { Text("Saved Place: " + waypoint.name) }, navigationIcon = {
                    IconButton(onClick = { selectedID = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }, actions = actions)
            }
        } else {
            TopAppBar(title = { Text("Location Sharing") }, actions = actions)
        }
    }, sheetContent = {
        if (selectedID == null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                users.values.forEach { UserCard(it, true) }
                waypoints.values.forEach { WaypointCard(it) }
            }
        } else if (users[selectedID] != null) {
            UserCard(users[selectedID]!!, true)
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card() {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show on Map")
                            Spacer(Modifier.weight(1f))
                            Checkbox(
                                users[selectedID]!!.receive,
                                {
                                    val newUser = users[selectedID]!!.copy(receive = it)
                                    users[selectedID!!] = newUser
                                    SuspendScope{usersDao.upsert(newUser)}
                                })
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Share your location")
                            Spacer(Modifier.weight(1f))
                            Checkbox(
                                users[selectedID]!!.send,
                                {
                                    val newUser = users[selectedID]!!.copy(send = it)
                                    users[selectedID!!] = newUser
                                    SuspendScope{usersDao.upsert(newUser)}
                                })
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton({
                    requestPickContact1()
                }) {
                    Text("Change connected contact")
                }
                OutlinedButton({
                    SuspendScope {
                        usersDao.delete(selectedID!!)
                        users.remove(selectedID!!)
                        selectedID = null
                    }
                }) {
                    Text("Disconnect")
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val waypoint = waypoints[selectedID]!!
                var waypointNewName by remember { mutableStateOf(waypoint.name) }
                var waypointNewRadius by remember { mutableStateOf(waypoint.range.toString()) }
                OutlinedTextField(
                    waypointNewName,
                    { waypointNewName = it },
                    label = { Text("Saved Place Name") },
                    readOnly = !editingWaypoint
                )
                OutlinedTextField(waypointNewRadius,
                    {
                        waypointNewRadius = it
                        if (it.isPositiveNumber()) editingWaypointRadius = it.toDouble()
                    },
                    label = { Text("Saved Place Range (Radius)") },
                    suffix = { Text("meters") },
                    readOnly = !editingWaypoint
                )

                OutlinedButton(
                    {
                        if (!editingWaypoint) {
                            editingWaypointPosition = doProjection(waypoint.coord)
                            editingWaypointRadius = waypoint.range
                            SuspendScope {
                                state.centerOnMarker(selectedID!!.toString())
                            }
                            editingWaypoint = true
                        } else {
                            editingWaypoint = false
                            val coord = doInverseProjection(
                                editingWaypointPosition.first,
                                editingWaypointPosition.second
                            )
                            waypoints[selectedID!!] = Waypoint(
                                selectedID!!,
                                waypointNewName,
                                waypointNewRadius.toDouble(),
                                coord,
                                mutableListOf()
                            )
                            SuspendScope {
                                platform.database.waypointDao()
                                    .upsert(waypoints[selectedID]!!)
                            }
                        }
                    },
                    enabled = !editingWaypoint || (waypointNewName.isNotEmpty() && waypointNewRadius.isPositiveNumber())
                ) {
                    Text(if (editingWaypoint) "Save" else "Edit Name/Location")
                }

                if(!editingWaypoint)
                    Card() {
                        Column(Modifier.fillMaxWidth(0.8f)) {
                            Spacer(Modifier.height(8.dp))
                            Text("Saved Place Entry/Exit Notifications:", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            users.values.forEach { user ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(16.dp))
                                    Text(user.name)
                                    Spacer(Modifier.weight(1f))
                                    var checked by remember { mutableStateOf(!waypoint.usersInactive.contains(user.id)) }
                                    Checkbox(checked, {
                                        checked = it
                                        SuspendScope {
                                            if(!checked) {
                                                waypoint.usersInactive += user.id
                                            } else {
                                                waypoint.usersInactive -= user.id
                                            }
                                            platform.database.waypointDao().upsert(waypoint)
                                        }
                                    })
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                OutlinedButton({
                    SuspendScope {
                        platform.database.waypointDao().delete(selectedID!!.toLong())
                        waypoints.remove(selectedID!!)
                        selectedID = null
                    }
                }) {
                    Text("Delete Saved Place")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }) {
        state.onTap { x, y ->
            selectedID = null
        }
        MapUI(Modifier.fillMaxSize(), state = state) {
            waypoints.values.forEach { waypoint ->

                val (origX, origY) =
                    if (editingWaypoint && waypoint.id == selectedID) editingWaypointPosition
                    else doProjection(waypoint.coord)
                val radius =
                    ((if (editingWaypoint && waypoint.id == selectedID) editingWaypointRadius else waypoint.range) / 12_742_000/3) * 1 / cos(
                        radians(waypoint.coord.lat)
                    )
                Circle(
                    modifier = Modifier,
                    mapState = state,
                    position = Offset(
                        state.fullSize.width * origX.toFloat(),
                        state.fullSize.height * origY.toFloat()
                    ),
                    color = Color(0x80Add8e6),
                    borderColor = Color(0xffAdd8e6),
                    radius = state.fullSize.height * radius.toFloat(),
                    isScaling = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicDialog(enable: Boolean, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if(enable) {
        BasicAlertDialog({ dismiss() }) {
            Column() {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryBar(batteryPercentage: Float) {
    // Make sure percentage is between 0 and 100
    val clampedPercentage = batteryPercentage.coerceIn(0.0f, 100f)
    val width = 30.dp
    val height = 15.dp

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Box(
            modifier = Modifier
                .width(width) // Width of the battery bar
                .height(height) // Height of the battery bar
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(4.dp)
                ) // Border to represent the battery outline
        ) {
            // Battery fill based on percentage
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((width * (clampedPercentage / 100f))) // Fill the width based on percentage
                    .background(
                        color = when {
                            clampedPercentage > 50 -> Color.Green // Full/High Battery
                            clampedPercentage > 20 -> Color.Yellow // Medium Battery
                            else -> Color.Red // Low Battery
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Text("${clampedPercentage.toInt()}%", fontSize = 12.sp)
    }
}