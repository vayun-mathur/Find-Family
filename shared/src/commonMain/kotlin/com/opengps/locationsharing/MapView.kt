package com.opengps.locationsharing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.sargunv.maplibrecompose.compose.CameraState
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.core.GestureSettings
import dev.whyoleg.cryptography.algorithms.RSA
import io.github.dellisd.spatialk.geojson.Position
import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun TextP(text: String) = @Composable {Text(text)}

@Composable
fun UserPicture(user: User, size: Dp) {
    val modifier = Modifier.clip(CircleShape).size(size).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    if(user.photo != null)
        AsyncImage(user.photo, null, modifier, contentScale = ContentScale.FillWidth)
    else {
        Box(modifier.background(Color.Green)) {
            Text(user.name.first().toString(), Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}

fun DrawScope.Circle(position: Offset, color: Color, borderColor: Color, radius: Float) {
    drawCircle(color, radius, position)
    drawCircle(borderColor, radius, position, style = Stroke(width = radius/20))
}

fun DrawScope.CenteredText(textMeasurer: TextMeasurer, text: String, position: Offset, color: Color = Color.Black) {
    try {
        drawText(textMeasurer, text, position - textMeasurer.measure(text).size / 2, style = TextStyle(color = color))
    } catch(e: Exception) {
        e.printStackTrace()
    }
}

operator fun Offset.minus(intSize: IntSize): Offset {
    return Offset(x - intSize.width, y - intSize.height)
}

@Composable
fun UserCard(user: User, showSupportingContent: Boolean) {
    val lastUpdatedTime = user.lastLocationValue?.timestamp?.let { timestring(it) } ?: "Never"
    val speed = user.lastLocationValue?.speed?.times(10)?.roundToInt()?.div(10F) ?: 0.0
    val sinceTime = user.lastLocationChangeTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeSinceEntry = Clock.System.now() - user.lastLocationChangeTime
    val sinceString = if(user.locationName == "Unnamed Location")
        ""
    else if(timeSinceEntry < 60.seconds)
        "Since just now"
    else if(timeSinceEntry < 15.minutes)
        "Since ${timeSinceEntry.inWholeMinutes} minutes ago"
    else {
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
        "Since $formattedTime $formattedDate"
    }
    Card(if(showSupportingContent) Modifier.clickable(onClick = { selectedObject = user}) else Modifier) {
        ListItem(
            leadingContent = {
                if(user.deleteAt == null)
                    Column(Modifier.width(65.dp)) {
                        UserPicture(user, 65.dp)
                        Spacer(Modifier.height(4.dp))
                        user.lastLocationValue?.battery?.let {
                            BatteryBar(it)
                        }
                    }
            },
            headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) },
            supportingContent = if(showSupportingContent) {
                {
                    if(user.deleteAt == null)
                        Text("Updated $lastUpdatedTime\nAt ${user.locationName}\n$sinceString")
                    else {
                        Button({
                            SuspendScope {
                                platform.copyToClipboard("https://findfamily.cc/view/${user.id}/${user.locationName}")
                            }
                        }) {
                            Text("Copy link")
                        }
                    }
                }
            } else null, trailingContent = if(showSupportingContent && user.deleteAt == null){
                TextP("$speed m/s")
            } else null)
    }
}

@Composable
fun WaypointCard(waypoint: Waypoint, users: List<User>) {
    val usersWithin = users.filter { it.locationName == waypoint.name }
    val usersString = usersWithin.joinToString { it.name } + when(usersWithin.size) {
        0 -> "nobody is"
        1 -> " is"
        else -> " are"
    } + " currently here"
    Card(Modifier.clickable(onClick = { selectedObject = waypoint;
        isEditingWaypoint = false})) {
        ListItem(
            headlineContent = { Text(waypoint.name, fontWeight = FontWeight.Bold) },
            supportingContent = TextP(usersString)
        )
    }
}

var currentWaypointPosition by mutableStateOf(Coord(0.0,0.0))
var currentWaypointRadius by mutableStateOf(0.0)
var selectedObject by mutableStateOf<ObjectParent?>(null)

private lateinit var camera: CameraState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapView() {
    val primaryColor = MaterialTheme.colorScheme.primary

    camera = rememberCameraState()

    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        SuspendScope {
            camera.awaitInitialized()
            initialized = true
        }
    }

    val usersDao = platform.database.usersDao()

    var objects by remember {mutableStateOf(mapOf<ULong, ObjectParent>())}
    val waypoints by remember { derivedStateOf { objects.values.filterIsInstance<Waypoint>() } }
    val users by remember { derivedStateOf { objects.values.filterIsInstance<User>() } }
    val devices by remember { derivedStateOf { objects.values.filterIsInstance<BluetoothDevice>() } }

    var longHeldPoint by remember { mutableStateOf(Coord(0.0,0.0)) }

    val addPersonPopupEnable = BasicDialog { AddPersonPopup() }
    val addTemporaryPersonPopupEnable = BasicDialog { AddPersonPopupTemporary() }
    val addDevicePopupEnable = BasicDialog { AddDevicePopup() }

    LaunchedEffect(Unit) {
        SuspendScope {
            objects = (platform.database.waypointDao().getAll() + usersDao.getAll() + platform.database.bluetoothDeviceDao().getAll()).associateBy { it.id }
            while(Networking.userid == null) { delay(500) }
            if(users.isEmpty()) {
                val newUser = User(Networking.userid!!, "Me", null, "Unnamed Location", true, true, null, null)
                usersDao.upsert(newUser)
                objects = objects + (newUser.id to newUser)
            }

            while(true) {
                objects = (platform.database.waypointDao().getAll() + usersDao.getAll() + platform.database.bluetoothDeviceDao().getAll()).associateBy { it.id }
                selectedObject = objects[selectedObject?.id]
                delay(1000)
            }
        }
    }

    LaunchedEffect(selectedObject) {
        val obj = selectedObject
        if(obj is User && obj.lastLocationValue != null) {
            camera.animateTo(camera.position.copy(target = Position(obj.lastLocationValue!!.coord.lon, obj.lastLocationValue!!.coord.lat), zoom = max(camera.position.zoom, 14.0)))
        }
        if(obj is BluetoothDevice && obj.lastLocationValue != null) {
            camera.animateTo(camera.position.copy(target = Position(obj.lastLocationValue.coord.lon, obj.lastLocationValue.coord.lat), zoom = max(camera.position.zoom, 14.0)))
        }
        if(obj is Waypoint) {
            camera.animateTo(camera.position.copy(target = Position(obj.coord.lon, obj.coord.lat), zoom = max(camera.position.zoom, 14.0)))
        }
    }

    BottomSheetScaffold({
        Column(Modifier.heightIn(max = 400.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SheetContent(selectedObject, users, waypoints, devices)
            }
        }
                        }, topBar = {
        val actions:  @Composable RowScope.() -> Unit = {
            val obj = selectedObject
            if(obj is Waypoint && !isEditingWaypoint) {
                IconButton({
                    currentWaypointPosition = obj.coord
                    currentWaypointRadius = obj.range
                    UISuspendScope {
                        camera.animateTo(camera.position.copy(target = Position(currentWaypointPosition.lon, currentWaypointPosition.lat)))
                    }
                    isEditingWaypoint = true
                }) {
                    Icon(Icons.Default.Edit, null)
                }
            }
            if(obj != null) {
                IconButton({
                    SuspendScope {
                        when (obj) {
                            is User -> usersDao.delete(obj)
                            is Waypoint -> platform.database.waypointDao().delete(obj)
                            is BluetoothDevice -> platform.database.bluetoothDeviceDao().delete(obj)
                        }
                        objects = objects - obj.id
                    }
                    selectedObject = null
                }) {
                    Icon(Icons.Default.Delete, null)
                }
            }
            var expanded by remember { mutableStateOf(false) }
            IconButton({ expanded = true }) {
                Icon(Icons.Default.Add, null)
            }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(TextP("Add Person"),
                    { addPersonPopupEnable(); expanded = false })
                DropdownMenuItem(TextP("Create Shareable Link"),
                    { addTemporaryPersonPopupEnable(); expanded = false })
                DropdownMenuItem(TextP("Add Saved Location"),
                    {
                        val newWaypoint = Waypoint(Random.nextULong(), "New Saved Place", 100.0, Coord(camera.position.target.latitude, camera.position.target.longitude), mutableListOf())
                        objects = objects + (newWaypoint.id to newWaypoint)
                        SuspendScope {
                            platform.database.waypointDao().upsert(newWaypoint)
                        }
                        selectedObject = newWaypoint
                        isEditingWaypoint = true
                        expanded = false })
//                DropdownMenuItem(TextP("Add Bluetooth Device"),
//                    { addDevicePopupEnable(); expanded = false })
            }
        }
        val navIcon = @Composable {
            if (selectedObject != null)
                IconButton({ selectedObject = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
        }
        TopAppBar(TextP(selectedObject?.name ?: "Location Sharing"), Modifier, navIcon, actions)
    }, sheetPeekHeight = 200.dp) { padding ->

        Box(Modifier.padding(padding).fillMaxSize()) {
            val density = LocalDensity.current
            MaplibreMap(styleUri = "https://tiles.openfreemap.org/styles/liberty",
                gestureSettings = GestureSettings(
                    isTiltGesturesEnabled = false,
                    isZoomGesturesEnabled = true,
                    isRotateGesturesEnabled = false,
                    isScrollGesturesEnabled = true,
                ), cameraState = camera,
                onMapClick = { position, offset ->
                    for (user in users) {
                        if(user.lastLocationValue == null) continue
                        val center = camera.screenLocationFromPosition(
                            Position(
                                user.lastLocationValue!!.coord.lon,
                                user.lastLocationValue!!.coord.lat
                            )
                        )
                        if((center - offset).getDistance() * density.density < 80) {
                            selectedObject = user
                            return@MaplibreMap ClickResult.Pass
                        }
                    }
                    for (waypoint in waypoints) {
                        val center = camera.screenLocationFromPosition(
                            Position(
                                waypoint.coord.lon,
                                waypoint.coord.lat
                            )
                        )
                        if((center - offset).getDistance() * density.density < 80) {
                            selectedObject = waypoint
                            return@MaplibreMap ClickResult.Pass
                        }
                    }
                    ClickResult.Pass
                }
            )
            val textMeasurer = rememberTextMeasurer()
            Canvas(Modifier.fillMaxSize()) {
                if(initialized) {
                    for (waypoint in waypoints) {
                        val radiusMeters =
                            if (waypoint.id == selectedObject?.id) currentWaypointRadius else waypoint.range
                        val (lat, lon) =
                            if (waypoint.id == selectedObject?.id) currentWaypointPosition else waypoint.coord

                        val center = camera.screenLocationFromPosition(Position(lon, lat))
                        if(center.x < 0.dp || center.y < 0.dp || center.x > size.toDpSize().width || center.y > size.toDpSize().height) continue
                        val circumferenceAtLatitude = 40_075_000 * cos(radians(waypoint.coord.lat))
                        val radiusInDegrees = 360 * radiusMeters / circumferenceAtLatitude
                        val edgePoint = camera.screenLocationFromPosition(
                            Position(lon + radiusInDegrees, lat))
                        val radiusPx = abs((center.x - edgePoint.x).toPx())
                        println(radiusInDegrees)
                        Circle(
                            Offset(center.x.toPx(), center.y.toPx()),
                            Color(0x80Add8e6),
                            Color(0xffAdd8e6),
                            radiusPx)
                    }
                    for (user in users) {
                        if(user.lastLocationValue == null) continue
                        val center = camera.screenLocationFromPosition(
                            Position(
                                user.lastLocationValue!!.coord.lon,
                                user.lastLocationValue!!.coord.lat
                            )
                        )
                        if(center.x < 0.dp || center.y < 0.dp || center.x > size.toDpSize().width || center.y > size.toDpSize().height) continue

                        Circle(
                            Offset(center.x.toPx(), center.y.toPx()),
                            Color.Green,
                            primaryColor,
                            75f
                        )
                        CenteredText(
                            textMeasurer,
                            "" + user.name[0],
                            Offset(center.x.toPx(), center.y.toPx()),
                            color = primaryColor
                        )

                        // todo: show profile photo
                    }
                }
            }

            val obj = selectedObject
            if(obj is User || obj is BluetoothDevice) {
                locations[obj.id]?.let { locs ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                        Card(Modifier.fillMaxWidth(0.5f)) {
                            var percentage by remember { mutableStateOf(1.0) }
                            Slider(
                                percentage.toFloat(),
                                { percentage = it.toDouble() },
                                Modifier.padding(16.dp)
                            )
                            // interpolate along locations as a percentage based on the timestamp
                            val latest = locs.maxOf { it.timestamp }
                            val oldest = max(locs.minOf { it.timestamp }, latest - 1.days.inWholeMilliseconds)
                            val points = locs.map {
                                it.timestamp to it.coord
                            }
                            val simulatedTimestamp =
                                (percentage * (latest - oldest)).toLong() + oldest
                            val simulatedLocationFirst =
                                points.find { it.first > simulatedTimestamp }
                            val simulatedLocationSecond =
                                points.findLast { it.first < simulatedTimestamp }
                            val simulatedLocation =
                                if (simulatedLocationFirst != null && simulatedLocationSecond != null) {
                                    val ratio =
                                        (simulatedTimestamp - simulatedLocationFirst.first) / (simulatedLocationSecond.first - simulatedLocationFirst.first)
                                    Coord(
                                        simulatedLocationFirst.second.lat * (1 - ratio) + simulatedLocationSecond.second.lat * ratio,
                                        simulatedLocationFirst.second.lon * (1 - ratio) + simulatedLocationSecond.second.lon * ratio
                                    )
                                } else if (simulatedLocationFirst != null) {
                                    simulatedLocationFirst.second
                                } else if (simulatedLocationSecond != null) {
                                    simulatedLocationSecond.second
                                } else {
                                    null
                                }
                            LaunchedEffect(simulatedLocation) {
                                if(simulatedLocation != null)
                                    camera.animateTo(camera.position.copy(target = Position(simulatedLocation.lon, simulatedLocation.lat)))
                            }
                            if (simulatedLocation != null) {
                                val (x, y) = doProjection(simulatedLocation)
                                val tstr = timestring(simulatedTimestamp)
                                ListItem(
                                    TextP("Showing: ${if (tstr == "just now") "Present" else "Past"}"),
                                    supportingContent = TextP(tstr)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DpOffset.getDistance(): Float {
    return sqrt(x.value * x.value + y.value * y.value)

}

@Composable
fun DialogScope.AddDevicePopup() {
    var bluetoothDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    LaunchedEffect(Unit) {
        SuspendScope {
            val already = platform.database.bluetoothDeviceDao().getAll()
            bluetoothDevices = platform.nearBluetoothDevices.filter { it !in already }
        }
    }
    Column {
        Text("Connect a Bluetooth Device", style = MaterialTheme.typography.titleLarge)
        Text("Devices need to be paired to your phone before they appear here", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.padding(8.dp))
        bluetoothDevices.forEach { bluetoothDevice ->
            ListItem(
                TextP(bluetoothDevice.name),
                Modifier.clickable {
                    SuspendScope {
                        platform.database.bluetoothDeviceDao().upsert(bluetoothDevice)
                        close()
                    }
                }
            )
        }
    }
}

interface DialogScope { fun close() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicDialog(dismiss: () -> Unit = {}, content: @Composable DialogScope.() -> Unit): () -> Unit {
    var enable by remember { mutableStateOf(false) }
    val dialogScope = object : DialogScope {
        override fun close() {
            enable = false
            dismiss()
        }
    }
    if(enable) {
        BasicAlertDialog({ enable = false; dismiss() }) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    Arrangement.spacedBy(16.dp),
                    Alignment.CenterHorizontally,
                ) {
                    content(dialogScope)
                }
            }
        }
    }
    return {enable = true}
}

@Composable
fun UserSheetContent(user: User) {
    val usersDao = platform.database.usersDao()

    val requestPickContact1 = platform.requestPickContact { name, photo ->
        SuspendScope {
            usersDao.upsert(user.copy(name = name, photo = photo))
        }
    }
    UserCard(user, true)
    Spacer(Modifier.height(4.dp))
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(user.deleteAt == null) {
            Card {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show on Map")
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            user.receive,
                            { recieve ->
                                SuspendScope {
                                    usersDao.upsert(user.copy(receive = recieve))
                                }
                            })
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Share your location")
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            user.send,
                            { send ->
                                SuspendScope {
                                    usersDao.upsert(user.copy(send = send))
                                }
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
        } else {
            val remainingTime = user.deleteAt!! - Clock.System.now()
            Text("Time remaining: ${remainingTime.inWholeHours} hours, ${remainingTime.inWholeMinutes%60} minutes")
            Spacer(Modifier.height(4.dp))
        }
    }
}

var isEditingWaypoint by mutableStateOf(false)

@Composable
fun WaypointSheetContent(waypoint: Waypoint, users: List<User>) {

    LaunchedEffect(waypoint) {
        currentWaypointPosition = waypoint.coord
        currentWaypointRadius = waypoint.range
    }

    LaunchedEffect(camera.position.target.latitude, camera.position.target.longitude) {
        if(isEditingWaypoint) {
            currentWaypointPosition = Coord(camera.position.target.latitude, camera.position.target.longitude)
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val waypointNewName = SimpleOutlinedTextField("Saved Place Name", readOnly = !isEditingWaypoint, initial = waypoint.name)
        val waypointNewRadius = SimpleOutlinedTextField("Saved Place Range (Radius)", initial = waypoint.range.toString(), suffix = {Text("meters")}, readOnly = !isEditingWaypoint, onChange = {
            if(it.isPositiveNumber()) {
                if(it.toDouble() > 1000) {
                    currentWaypointRadius = 1000.0
                    return@SimpleOutlinedTextField "1000.0"
                } else
                    currentWaypointRadius = it.toDouble()
            }
            return@SimpleOutlinedTextField it
        })

        if(isEditingWaypoint) {
            OutlinedButton(
                {
                    SuspendScope {
                        platform.database.waypointDao().upsert(
                            waypoint.copy(
                                coord = currentWaypointPosition,
                                name = waypointNewName(),
                                range = waypointNewRadius().toDouble()
                            )
                        )
                    }
                    isEditingWaypoint = false
                },
                enabled = waypointNewName().isNotEmpty() && waypointNewRadius().isPositiveNumber()
            ) {
                Text("Save")
            }
        }

        if(!isEditingWaypoint)
            Card() {
                Column(Modifier.fillMaxWidth(0.8f)) {
                    Spacer(Modifier.height(8.dp))
                    Text("Saved Place Entry/Exit Notifications:", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    users.forEach { user ->
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
    }
}

@Composable
fun SheetContent(selectedObject: ObjectParent?, users: List<User>, waypoints: List<Waypoint>, devices: List<BluetoothDevice>) {
    when(selectedObject) {
        is BluetoothDevice -> DeviceSheetContent(selectedObject)
        is User -> UserSheetContent(selectedObject)
        is Waypoint -> WaypointSheetContent(selectedObject, users)
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                users.filter { it.deleteAt == null }.forEach { UserCard(it, true) }
                users.filter { it.deleteAt != null }.forEach { UserCard(it, true) }
                devices.forEach { DeviceCard(it) }
                waypoints.forEach { WaypointCard(it, users) }
            }
        }
    }
    Spacer(Modifier.height(32.dp))
}

@Composable
fun DeviceCard(device: BluetoothDevice) {
    Card(Modifier.clickable(onClick = { selectedObject = device})) {
        ListItem(
            headlineContent = { Text(device.name, fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
fun DeviceSheetContent(device: BluetoothDevice) {
    Card(Modifier.clickable(onClick = { selectedObject = device})) {
        ListItem(
            headlineContent = { Text(device.name, fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
fun DialogScope.AddPersonPopup() {
    var contactName by remember { mutableStateOf("") }
    var contactPhoto by remember { mutableStateOf<String?>(null) }
    val requestPickContact2 = platform.requestPickContact { name, photo ->
        contactName = name
        contactPhoto = photo
    }
    Box(Modifier.clickable { requestPickContact2() }) {
        if (contactName.isNotEmpty())
            UserCard(
                User(
                    Random.nextULong(),
                    contactName,
                    contactPhoto,
                    "",
                    false,
                    false,
                    null,
                    null
                ), false
            )
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
                            "Tap to Select Contact",
                            fontWeight = FontWeight.Bold
                        )
                    })
            }
    }
    val recipientID = SimpleOutlinedTextField("Contact's FindFamily ID")

    Text("Share your FindFamily ID with your contact, then enter their ID here")
    OutlinedButton({
        platform.copyToClipboard(Networking.userid!!.encodeBase26())
    }) {
        Text("Copy Your FindFamily ID")
    }

    OutlinedButton({
        SuspendScope {
            val trueID = recipientID().decodeBase26()
            val newUser = User(
                trueID,
                contactName,
                contactPhoto,
                "Unnamed Location",
                true,
                true,
                null,
                null,
            )
            platform.database.usersDao().upsert(newUser)
            close()
        }
    }, enabled = contactName.isNotEmpty() && recipientID().isNotEmpty()
    ) {
        Text("Start Location Sharing")
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogScope.AddPersonPopupTemporary() {
    var expiryTime by remember { mutableStateOf("15 minutes") }

    Text("Temporary FindFamily Link", style = MaterialTheme.typography.titleLarge)

    Spacer(Modifier.height(4.dp))
    val contactName = SimpleOutlinedTextField("Name this Link")
    Spacer(Modifier.width(16.dp))
    Column {
        var expanded by remember { mutableStateOf(false) }
        OutlinedTextField(expiryTime, {}, Modifier.clickable { expanded = true }, readOnly = true, enabled = false, label = TextP("Link Expiry in"),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = OutlinedTextFieldDefaults.colors().copy(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                //For Icons
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        DropdownMenu(expanded, { expanded = false }) {
            listOf("15 minutes", "30 minutes", "1 hour", "2 hours", "6 hours", "12 hours", "1 day").forEach { selectionOption ->
                DropdownMenuItem(TextP(text = selectionOption), {
                    expiryTime = selectionOption
                    expanded = false
                })
            }
        }
    }

    OutlinedButton({
        SuspendScope {
            val keypair = Networking.generateKeyPair()

            val newUser = User(Random.nextULong(), contactName(), null,
                keypair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.PEM).encodeBase64(),
                false, true, null, null,
                deleteAt = Clock.System.now() + when (expiryTime) {
                        "15 minutes" -> 15.minutes
                        "30 minutes" -> 30.minutes
                        "1 hour" -> 1.hours
                        "2 hours" -> 2.hours
                        "6 hours" -> 6.hours
                        "12 hours" -> 12.hours
                        "1 day" -> 1.days
                        else -> throw IllegalStateException("Invalid expiry time for location sharing")
                    },
                encryptionKey = keypair.publicKey.encodeToByteArray(RSA.PublicKey.Format.PEM).encodeBase64()
            )
            platform.database.usersDao().upsert(newUser)
            close()
        } }, Modifier,contactName().isNotEmpty() && expiryTime.isNotEmpty()) {
        Text("Create Temporary Sharing Link")
    }
}

@Composable
fun SimpleOutlinedTextField(label: String, initial: String = "", suffix: @Composable (() -> Unit)? = null, readOnly: Boolean = false, onChange: (String) -> String? = {null}): ()->String {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(text, { text = it; text = onChange(it)?:text }, label = { Text(label) }, suffix = suffix, readOnly = readOnly)
    return { text }
}

@Composable
fun BatteryBar(percent: Float, width: Dp = 30.dp, height: Dp = 15.dp) {
    val color = when {
        percent > 50 -> Color.Green
        percent > 20 -> Color.Yellow
        else -> Color.Red
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.size(width, height).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxHeight().width((width * (percent / 100f))).background(color, RoundedCornerShape(4.dp)))
        }
        Text("${percent.toInt()}%", fontSize = 12.sp)
    }
}