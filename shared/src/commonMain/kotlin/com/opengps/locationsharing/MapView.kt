package com.opengps.locationsharing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

val users: SnapshotStateMap<ULong, User> = mutableStateMapOf()

@Composable
fun UserPicture(user: User) {
    if(user.photo != null) {
        AsyncImage(user.photo, null, Modifier.clip(CircleShape).size(50.dp).border(2.dp, Color.Black, CircleShape), contentScale = ContentScale.FillWidth)
    } else {
        Box(Modifier.clip(CircleShape).size(50.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).background(Color.Green)) {
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

fun SuspendScope(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        block()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapView(navController: NavHostController) {
    val platform = getPlatform()

    var selectedID by remember { mutableStateOf<ULong?>(null) }
    var addPopupEnable by remember { mutableStateOf(false) }
    var addWaypointPopupEnable by remember { mutableStateOf(false) }
    var longHeldPoint by remember { mutableStateOf(Coord(0.0,0.0)) }
    var editingWaypoint by remember { mutableStateOf(false) }
    var editingWaypointPosition by remember { mutableStateOf(Pair(0.0,0.0)) }
    var editingWaypointRadius by remember { mutableStateOf(0.0) }
    val waypoints: SnapshotStateMap<ULong, Waypoint> = remember { mutableStateMapOf() }

    LaunchedEffect(Unit) {
        SuspendScope {
            waypoints.putAll(platform.database.waypointDao().getAll().associateBy { it.id })
            while(Networking.userid == null) { delay(500) }
            users[Networking.userid!!] = User(Networking.userid!!, "Me", null, "Unnamed Location", true, true)
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
            LaunchedEffect(Unit) {
                while(true) {
                    if(latestLocations.containsKey(user.id)) {
                        lastUpdatedTime = if (latestLocations[user.id]?.timestamp != null) {
                            timestring(latestLocations[user.id]!!.timestamp)
                        } else {
                            "Never"
                        }
                    }
                    delay(1000)
                }
            }

            ListItem(
                leadingContent = {UserPicture(user)},
                headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) },
                supportingContent = if(showSupportingContent) {
                    {Text("At ${user.locationName}\nUpdated $lastUpdatedTime")}
                } else null)
        }
    }

    fun addUserMarker(user: User) {
        val loc = latestLocations[user.id]?: return
        val res = doProjection(loc.coord)
        state.addMarker(user.id.toString(), res.first, res.second, relativeOffset = Offset(-0.5f, -0.5f)) {
            UserPicture(user)
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

    LaunchedEffect(selectedID) {
        state.removeAllMarkers()
        if(selectedID != null && users[selectedID] != null) {
            addUserMarker(users[selectedID!!]!!)
            state.centerOnMarker(selectedID!!.toString())
        } else {
            users.values.forEach(::addUserMarker)
        }
        waypoints.values.forEach(::addWaypointMarker)
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
            }
            if(users[id] != null) {
                val waypointOverlap = waypoints.values.firstOrNull { waypoint ->
                    havershine(location.coord, waypoint.coord) < waypoint.range
                }
                users[id]!!.locationName = waypointOverlap?.name?: "Unnamed Location"
            }
        }
    }
    val requestPickContact1 = platform.requestPickContact { name, photo ->
        users[selectedID!!] = User(selectedID!!, name, photo, users[selectedID]!!.locationName, users[selectedID]!!.receive, users[selectedID]!!.send)
        state.removeMarker(selectedID!!.toString())
        addUserMarker(users[selectedID!!]!!)
    }
    BasicDialog(addWaypointPopupEnable, { addWaypointPopupEnable = false }) {
        Text("Add a Waypoint", style = MaterialTheme.typography.headlineMedium)
        var waypointName by remember { mutableStateOf("") }
        OutlinedTextField(
            waypointName,
            { waypointName = it },
            label = { Text("Waypoint Name") })

        var waypointRadius by remember { mutableStateOf("") }
        OutlinedTextField(
            waypointRadius,
            { waypointRadius = it },
            label = { Text("Waypoint Range (Radius)") }, suffix = { Text("meters") })

        Spacer(Modifier.weight(1f))

        Row {
            OutlinedButton(
                {
                    val id = Random.nextULong()
                    waypoints[id] =
                        Waypoint(id, waypointName, waypointRadius.toDouble(), longHeldPoint)
                    SuspendScope {
                        platform.database.waypointDao().upsert(waypoints[id]!!)
                    }
                    addWaypointPopupEnable = false
                    addWaypointMarker(waypoints[id]!!)
                },
                enabled = (waypointName.isNotEmpty() && waypointRadius.isPositiveNumber())
            ) {
                Text("Add Waypoint")
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton({ addWaypointPopupEnable = false }) {
                Text("Cancel")
            }
        }
    }
    BasicDialog(addPopupEnable, { addPopupEnable = false }) {
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
                Text("Receive")
                Spacer(Modifier.weight(1f))
                Checkbox(receive, { receive = it })
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Send")
                Spacer(Modifier.weight(1f))
                Checkbox(send, { send = it })
            }
        }

        OutlinedButton(
            {
                val trueID = recipientID.decodeBase26()
                addPopupEnable = false
                users[trueID] = User(trueID, contactName, contactPhoto, "", receive, send)
            },
            enabled = (contactName.isNotEmpty() && recipientID.isNotEmpty())
        ) {
            Text("Start Location Sharing")
        }
    }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    )
    BottomSheetScaffold(topBar = {
        if (selectedID != null) {
            if (users[selectedID] != null) {
                val user = users[selectedID]!!
                TopAppBar(title = { Text(user.name) }, navigationIcon = {
                    IconButton(onClick = { selectedID = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                })
            } else {
                val waypoint = waypoints[selectedID]!!
                TopAppBar(title = { Text("Waypoint: " + waypoint.name) }, navigationIcon = {
                    IconButton(onClick = { selectedID = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                })
            }
        } else {
            TopAppBar(title = { Text("Location Sharing") }, actions = {
                IconButton(onClick = { addPopupEnable = true }) {
                    Icon(Icons.Default.Add, null)
                }
            })
        }
    }, sheetContent = {
        if (selectedID == null) {
            users.values.forEach { UserCard(it, true) }
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
                            Text("Receive")
                            Spacer(Modifier.weight(1f))
                            Checkbox(
                                users[selectedID]!!.receive,
                                { users[selectedID!!] = users[selectedID]!!.copy(receive = it) })
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Send")
                            Spacer(Modifier.weight(1f))
                            Checkbox(
                                users[selectedID]!!.send,
                                { users[selectedID!!] = users[selectedID]!!.copy(send = it) })
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
                    users.remove(selectedID)
                    selectedID = null
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
                    label = { Text("Waypoint Name") },
                    readOnly = !editingWaypoint
                )
                OutlinedTextField(waypointNewRadius,
                    {
                        waypointNewRadius = it
                        if (it.isPositiveNumber()) editingWaypointRadius = it.toDouble()
                    },
                    label = { Text("Waypoint Range (Radius)") },
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
                                coord
                            )
                            SuspendScope {
                                platform.database.waypointDao()
                                    .upsert(waypoints[selectedID]!!)
                            }
                        }
                    },
                    enabled = !editingWaypoint || (waypointNewName.isNotEmpty() && waypointNewRadius.isPositiveNumber())
                ) {
                    Text(if (editingWaypoint) "Save" else "Edit")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }) {
        MapUI(Modifier.fillMaxSize(), state = state) {
            waypoints.values.forEach { waypoint ->

                val (origX, origY) =
                    if (editingWaypoint && waypoint.id == selectedID) editingWaypointPosition
                    else doProjection(waypoint.coord)
                val radius =
                    ((if (editingWaypoint && waypoint.id == selectedID) editingWaypointRadius else waypoint.range) / 12_742_000) * 1 / cos(
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
                    radius = state.fullSize.width * radius.toFloat(),
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