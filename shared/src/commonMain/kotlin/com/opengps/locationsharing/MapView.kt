package com.opengps.locationsharing

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.whyoleg.cryptography.algorithms.RSA
import io.github.dellisd.spatialk.geojson.Position
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import location_sharing.shared.generated.resources.Res
import location_sharing.shared.generated.resources.accept_start_sharing
import location_sharing.shared.generated.resources.change_connected_contact
import location_sharing.shared.generated.resources.connect_bluetooth_device
import location_sharing.shared.generated.resources.connect_bluetooth_device_description
import location_sharing.shared.generated.resources.contact_findfamily_id
import location_sharing.shared.generated.resources.contact_findfamily_id_desc
import location_sharing.shared.generated.resources.copy_findfamily_id
import location_sharing.shared.generated.resources.hour
import location_sharing.shared.generated.resources.hours
import location_sharing.shared.generated.resources.minutes
import location_sharing.shared.generated.resources.request_start_sharing
import location_sharing.shared.generated.resources.saved_place_name
import location_sharing.shared.generated.resources.saved_place_notification
import location_sharing.shared.generated.resources.saved_place_range
import location_sharing.shared.generated.resources.share_your_location
import location_sharing.shared.generated.resources.tap_pick_contact
import location_sharing.shared.generated.resources.temporary_link_name
import location_sharing.shared.generated.resources.temporary_link_submit
import location_sharing.shared.generated.resources.temporary_link_title
import location_sharing.shared.generated.resources.time_remaining
import location_sharing.shared.generated.resources.unnamed_location
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

fun TextP(text: String) = @Composable {Text(text)}

@Composable
fun UserPicture(user: User, size: Dp, grayscale: Boolean = false) {
    UserPicture(user.photo, user.name.first(), size, grayscale)
}

@Composable
fun GreenCircle(size: Dp, char: Char? = null, grayscale: Boolean = false) {
    Box(Modifier.clip(CircleShape).size(size).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).background(if(grayscale)Color.Gray else Color.Green )) {
        char?.let {
            Text(char.toString(), Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
        }
    }
}

val ColorFilter.Companion.GrayScale: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun UserPicture(userPhoto: String?, firstChar: Char, size: Dp, grayscale: Boolean) {
    val modifier = Modifier.clip(CircleShape).size(size).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    if(userPhoto != null)
        AsyncImage(userPhoto, null, modifier, contentScale = ContentScale.FillWidth, colorFilter = if(grayscale) ColorFilter.GrayScale else null)
    else {
        GreenCircle(size, firstChar, grayscale)
    }
}

fun DrawScope.Circle(position: Offset, color: Color, borderColor: Color, radius: Float) {
    drawCircle(color, radius, position)
    drawCircle(borderColor, radius, position, style = Stroke(width = radius/20))
}

@OptIn(ExperimentalTime::class)
@Composable
fun UserCard(user: User, showSupportingContent: Boolean) {
    val lastUpdatedTime = user.lastLocationValue?.let { if(it.sleep) "Just now" else timestring(it.timestamp) } ?: "Never"
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
            0L -> "today"
            1L -> "yesterday"
            else -> sinceTime.date.format(DateFormats.MONTH_DAY)
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
                                platform.copyToClipboard("https://findfamily.cc/view/${user.id}#key=${user.locationName}")
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
        0 -> "nobody is currently here"
        1 -> " is currently here"
        else -> " are currently here"
    }
    Card(Modifier.clickable(onClick = { selectedObject = waypoint
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

private fun DpOffset.toOffset(density: Density): Offset {
    with(density) {
        return Offset(x.toPx(), y.toPx())
    }
}

var addPersonPopupEnable: () -> Unit = {}

@Composable
fun ExpandingFloatingActionButton(
    onAddPersonClick: () -> Unit,
    onAddLinkClick: () -> Unit,
    onAddLocationClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // The three buttons that appear when the main FAB is expanded
        if (isExpanded) {
            SmallFloatingActionButton(
                onClick = {
                    onAddPersonClick()
                    isExpanded = false
                },
                content = { Icon(Icons.Default.Person, contentDescription = "Add Person") }
            )
            SmallFloatingActionButton(
                onClick = {
                    onAddLinkClick()
                    isExpanded = false
                },
                content = { Icon(Icons.Default.Link, contentDescription = "Add Link") }
            )
            SmallFloatingActionButton(
                onClick = {
                    onAddLocationClick()
                    isExpanded = false
                },
                content = { Icon(Icons.Default.LocationOn, contentDescription = "Add Location") }
            )
        }

        FloatingActionButton(
            onClick = { isExpanded = !isExpanded }
        ) {
            val rotation by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f)
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

val editingWaypointName = mutableStateOf("")
val editingWaypointRadius = mutableStateOf("")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalTime::class)
@Composable
fun MapView() {
    BackHandler(enabled = selectedObject != null) {
        selectedObject = null
    }

    camera = rememberCameraState()

    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        SuspendScope {
            camera.awaitProjection()
            initialized = true
        }
    }

    val waypointDao = platform.database.waypointDao()
    val bluetoothDao = platform.database.bluetoothDeviceDao()

    var objects by remember {mutableStateOf(mapOf<ULong, ObjectParent>())}
    val waypoints by remember { derivedStateOf { objects.values.filterIsInstance<Waypoint>() } }
    val usersAll by remember { derivedStateOf { objects.values.filterIsInstance<User>() } }
    val users by remember { derivedStateOf { usersAll.filter { it.requestStatus == RequestStatus.MUTUAL_CONNECTION } } }
    val devices by remember { derivedStateOf { objects.values.filterIsInstance<BluetoothDevice>() } }

    addPersonPopupEnable = BasicDialog { AddPersonPopup() }
    val addTemporaryPersonPopupEnable = BasicDialog { AddPersonPopupTemporary() }
    val addDevicePopupEnable = BasicDialog { AddDevicePopup() }

    LaunchedEffect(Unit) {
        SuspendScope {
            UsersCached.init()
            objects = (waypointDao.getAll() + UsersCached.getAll() + bluetoothDao.getAll()).associateBy { it.id }
            while(Networking.userid == null) { delay(500) }
            if(users.isEmpty()) {
                val newUser = User(Networking.userid!!, "Me", null, "Unnamed Location", true, RequestStatus.MUTUAL_CONNECTION, null, null)
                UsersCached.upsert(newUser)
                objects = objects + (newUser.id to newUser)
            }

            while(true) {
                objects = (waypointDao.getAll() + UsersCached.getAll() + bluetoothDao.getAll()).associateBy { it.id }
                selectedObject = objects[selectedObject?.id]
                delay(1000)
            }
        }
    }

    LaunchedEffect(selectedObject?.id) {
        val obj = selectedObject
        val newZoom = max(camera.position.zoom, 14.0)
        obj?.currentPosition()?.let {
            camera.animateTo(camera.position.copy(target = it.toPosition(), zoom = newZoom))
        }
    }

    Scaffold(Modifier.imePadding(),
        floatingActionButton = {
            val obj = selectedObject
            if(obj == null) {
                ExpandingFloatingActionButton(
                    onAddPersonClick = {
                        AddPersonPopupInitial = null
                        addPersonPopupEnable()
                    },
                    onAddLinkClick = {
                        addTemporaryPersonPopupEnable()
                    },
                    onAddLocationClick = {
                        val newWaypoint = Waypoint(
                            Random.nextULong(),
                            "New Saved Place",
                            100.0,
                            Coord(
                                camera.position.target.latitude,
                                camera.position.target.longitude
                            ),
                            mutableListOf()
                        )
                        objects = objects + (newWaypoint.id to newWaypoint)
                        SuspendScope {
                            platform.database.waypointDao().upsert(newWaypoint)
                        }
                        selectedObject = newWaypoint
                        isEditingWaypoint = true
                    }
                )
            } else if(obj is Waypoint) {
                if(!isEditingWaypoint) {
                    FloatingActionButton({
                        currentWaypointPosition = obj.coord
                        currentWaypointRadius = obj.range
                        UISuspendScope {
                            camera.animateTo(camera.position.copy(target = currentWaypointPosition.toPosition()))
                        }
                        isEditingWaypoint = true
                    }) {
                        Icon(Icons.Default.Edit, null)
                    }
                } else if(editingWaypointRadius.value.isPositiveNumber() && editingWaypointRadius.value.toDouble() <= 1000) {
                    FloatingActionButton({
                        SuspendScope {
                            platform.database.waypointDao().upsert(
                                obj.copy(
                                    coord = currentWaypointPosition,
                                    name = editingWaypointName.value,
                                    range = editingWaypointRadius.value.toDouble()
                                )
                            )
                        }
                        isEditingWaypoint = false
                    }) {
                        Icon(Icons.Default.Save, null)
                    }
                }
            }
        },
        topBar = {
        val actions: @Composable RowScope.() -> Unit = {
            val obj = selectedObject
            if(obj != null && obj.id != Networking.userid) {
                IconButton({
                    SuspendScope {
                        when (obj) {
                            is User -> UsersCached.delete(obj)
                            is Waypoint -> waypointDao.delete(obj)
                            is BluetoothDevice -> bluetoothDao.delete(obj)
                        }
                        objects = objects - obj.id
                    }
                    selectedObject = null
                }) {
                    Icon(Icons.Default.Delete, null)
                }
            }
            val save = rememberFileSaverLauncher { file ->
                if (file != null) {
                    SuspendScope {
                        Backup.downloadBackupFile(file)
                    }
                }
            }
            IconButton({
                SuspendScope {
                    save.launch("findfamily_backup", "db")
                }
            }) {
                Icon(Icons.Default.Download, null)
            }
            val restore = rememberFilePickerLauncher { file ->
                if(file != null) {
                    SuspendScope {
                        Backup.restoreBackupFile(file)
                    }
                }
            }
            IconButton({
                restore.launch()
            }) {
                Icon(Icons.Default.Upload, null)
            }
        }
        val navIcon = @Composable {
            if (selectedObject != null)
                IconButton({ selectedObject = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
        }
        TopAppBar(TextP(selectedObject?.name ?: "Find Family"), Modifier, navIcon, actions)
    }, bottomBar = {
        val height by remember {derivedStateOf { when(selectedObject) {
            is User -> 300.dp
            is Waypoint -> 400.dp
            is BluetoothDevice -> 300.dp
            else -> 400.dp
        }}}
        BottomAppBar(Modifier.height(height)) {
            Column(Modifier.height(height).verticalScroll(rememberScrollState())) {
                SheetContent(selectedObject, usersAll, waypoints, devices)
            }
        }
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val density = LocalDensity.current
            var selectedObjectPosition: Position? by remember { mutableStateOf(null) }
            MaplibreMap(Modifier, BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"), camera, 0f..20f,
                options = platform.mapOptions,
                onMapClick = { _, offset ->
                    val coords = (users + waypoints).filter { it.currentPosition() != null }.associateBy { it.currentPosition()!! }
                    val obj = coords.firstNotNullOfOrNull { (coord, obj) ->
                        val center = camera.projection!!.screenLocationFromPosition(coord.toPosition())
                        if((center - offset).getDistance() * density.density < 80) {
                            obj
                        } else null
                    }
                    if(obj != null)
                        selectedObject = obj
                    ClickResult.Pass
                }
            )
            val obj = selectedObject
            if(initialized) {
                key(camera.position) {
                    Canvas(Modifier.fillMaxSize()) {
                        for (waypoint in waypoints) {
                            val radiusMeters =
                                if (waypoint.id == selectedObject?.id) currentWaypointRadius else waypoint.range
                            val coord =
                                if (waypoint.id == selectedObject?.id) currentWaypointPosition else waypoint.coord

                            val center = camera.projection!!.screenLocationFromPosition(coord.toPosition())
                            if(center !in size.toDpSize()) continue
                            val circumferenceAtLatitude =
                                40_075_000 * cos(radians(waypoint.coord.lat))
                            val radiusInDegrees = 360 * radiusMeters / circumferenceAtLatitude
                            val edgePoint = camera.projection!!.screenLocationFromPosition(
                                Position(coord.lon + radiusInDegrees, coord.lat)
                            )
                            val radiusPx = abs((center.x - edgePoint.x).toPx())
                            Circle(
                                center.toOffset(this),
                                Color(0x80Add8e6),
                                Color(0xffAdd8e6),
                                radiusPx
                            )
                        }
                    }
                    for (user in users) {
                        if (user.currentPosition() == null) continue
                        val center =
                            camera.projection!!.screenLocationFromPosition(user.currentPosition()!!.toPosition()) - DpOffset(35.dp, 35.dp)

                        Box(Modifier.offset(center.x, center.y)) {
                            UserPicture(user, 70.dp)
                        }
                    }
                    if(selectedObjectPosition != null && obj is User) {
                        val center =
                            camera.projection!!.screenLocationFromPosition(selectedObjectPosition!!) - DpOffset(35.dp, 35.dp)

                        Box(Modifier.offset(center.x, center.y)) {
                            UserPicture(obj, 70.dp, true)
                        }
                    }
                }
            }

            if(obj is User || obj is BluetoothDevice) {
                var isShowingPresent by remember { mutableStateOf(true) }
                Card(Modifier.width(100.dp).align(Alignment.BottomEnd)) {
                    val colmod = if(isShowingPresent) Modifier else Modifier.fillMaxHeight(1f)
                    Column(colmod.padding(4.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isShowingPresent) {
                            val currentDate = Clock.System.now().toLocalDateTime(
                                TimeZone.currentSystemDefault()
                            ).date
                            val currentTime = Clock.System.now().toLocalDateTime(
                                TimeZone.currentSystemDefault()
                            ).time
                            var pickedLocalDate by remember {
                                mutableStateOf(
                                    Clock.System.now().toLocalDateTime(
                                        TimeZone.currentSystemDefault()
                                    ).date
                                )
                            }
                            var timeOfDay by remember { mutableStateOf(Clock.System.now().toLocalDateTime(
                                TimeZone.currentSystemDefault()
                            ).time.toSecondOfDay()) }
                            val pickedLocalTime by remember { derivedStateOf {
                                LocalTime.fromSecondOfDay(timeOfDay)
                            } }
                            Box(Modifier.weight(1f)) {
                                VerticalSlider(
                                    timeOfDay.toFloat(),
                                    { timeOfDay = it.toInt() },
                                    valueRange = 0.0f..24f*60f*60f,
                                    maximum = if(currentDate == pickedLocalDate) currentTime.toSecondOfDay().toFloat() else null
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                IconButton({
                                    timeOfDay -= 5*60
                                }) {
                                    Text("<<<")
                                }
                                IconButton({
                                    timeOfDay += 5*60
                                }) {
                                    Text(">>>")
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                IconButton({
                                    timeOfDay -= 60
                                }) {
                                    Text("<<")
                                }
                                IconButton({
                                    timeOfDay += 60
                                }) {
                                    Text(">>")
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                IconButton({
                                    timeOfDay -= 10
                                }) {
                                    Text("<")
                                }
                                IconButton({
                                    timeOfDay += 10
                                }) {
                                    Text(">")
                                }
                            }
                            Text(pickedLocalTime.format(DateFormats.TIME_SECOND_AM_PM), fontSize = 11.sp)
                            DatePickerHelper(pickedLocalDate, currentDate) { pickedLocalDate = it }
                            val simulatedTimestamp = pickedLocalDate.atTime(pickedLocalTime)
                                .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

                            var locs by remember { mutableStateOf(locations[obj.id] ?: listOf()) }

                            LaunchedEffect(Unit) {
                                locs = platform.database.locationValueDao().getForID(obj.id)
                            }
                            if (locs.isNotEmpty()) {
                                val points = locs.map { it.timestamp to it.coord }
                                val closest =
                                    points.minBy { abs(it.first - simulatedTimestamp) }
                                selectedObjectPosition = closest.second.toPosition()
                                LaunchedEffect(closest.first) {
                                    val newZoom = max(camera.position.zoom, 14.0)
                                    camera.animateTo(
                                        camera.position.copy(
                                            target = closest.second.toPosition(),
                                            zoom = newZoom
                                        )
                                    )
                                }
                            }
                        }
                        OutlinedButton({
                            isShowingPresent = !isShowingPresent
                        }, Modifier.fillMaxWidth(1f)) {
                            Text(if (isShowingPresent) "History" else "Hide", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private operator fun DpSize.contains(offset: DpOffset): Boolean {
    return offset.x in 0.dp..width && offset.y in 0.dp..height
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
        Text(stringResource(Res.string.connect_bluetooth_device), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.connect_bluetooth_device_description), style = MaterialTheme.typography.bodyMedium)
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

@OptIn(ExperimentalTime::class)
@Composable
fun UserSheetContent(user: User) {

    val requestPickContact1 = platform.requestPickContact { name, photo ->
        SuspendScope {
            UsersCached.upsert(user.copy(name = name, photo = photo))
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
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.share_your_location))
                    Spacer(Modifier.weight(1f))
                    Checkbox(
                        user.send,
                        { send ->
                            SuspendScope {
                                UsersCached.upsert(user.copy(send = send))
                            }
                        })
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton({
                requestPickContact1()
            }) {
                Text(stringResource(Res.string.change_connected_contact))
            }
        } else {
            val remainingTime = user.deleteAt!! - Clock.System.now()
            Text("${stringResource(Res.string.time_remaining)} ${remainingTime.inWholeHours} ${stringResource(Res.string.hours)}, ${remainingTime.inWholeMinutes%60}  ${stringResource(Res.string.minutes)}")
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
        editingWaypointName.value = waypoint.name
        editingWaypointRadius.value = waypoint.range.toString()
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
        OutlinedTextField(editingWaypointName.value, { editingWaypointName.value = it }, Modifier.fillMaxWidth(), readOnly = !isEditingWaypoint, label = { Text(stringResource(Res.string.saved_place_name)) })
        OutlinedTextField(
            editingWaypointRadius.value,
            {
                if(it.isPositiveNumber()) {
                    if(it.toDouble() > 1000) {
                        currentWaypointRadius = 1000.0
                        editingWaypointRadius.value = "1000.0"
                    } else {
                        currentWaypointRadius = it.toDouble()
                    }
                }
                editingWaypointRadius.value = it
            },
            Modifier.fillMaxWidth(),
            readOnly = !isEditingWaypoint,
            label = { Text(stringResource(Res.string.saved_place_range)) },
            suffix = { Text("meters") },
            isError = !(editingWaypointRadius.value.isPositiveNumber() && editingWaypointRadius.value.toDouble() <= 1000),
            supportingText = if(!editingWaypointRadius.value.isPositiveNumber()) {
                { Text("Range must be a positive number") }
            } else if(editingWaypointRadius.value.toDouble() > 1000) {
                { Text("Range must be less than 1000 meters") }
            } else null
        )

        if(!isEditingWaypoint)
            Card() {
                Column(Modifier.fillMaxWidth(0.8f)) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.saved_place_notification), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
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
fun UserAwaitingRequest(user: User) {
    Card {
        ListItem(headlineContent = TextP(user.name), trailingContent = {
            Row {
                OutlinedButton({
                    AddPersonPopupInitial = user.id
                    addPersonPopupEnable()
                }) { TextP("Accept")() }
                OutlinedButton({
                    SuspendScope {
                        UsersCached.delete(user.id)
                    }
                }) { TextP("Deny")() }
            }
        })
    }
}


@Composable
fun UserAwaitingResponse(user: User) {
    Card {
        ListItem(
            leadingContent = {
                UserPicture(user, 65.dp)
            },
            headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) }, trailingContent = {
                IconButton({
                    SuspendScope {
                        UsersCached.delete(user.id)
                    }
                }) {
                    Icon(Icons.Default.Delete, null)
                }
            })
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun SheetContent(selectedObject: ObjectParent?, usersAll: List<User>, waypoints: List<Waypoint>, devices: List<BluetoothDevice>) {
    val users = usersAll.filter { it.requestStatus == RequestStatus.MUTUAL_CONNECTION }
    when(selectedObject) {
        is BluetoothDevice -> DeviceSheetContent(selectedObject)
        is User -> UserSheetContent(selectedObject)
        is Waypoint -> WaypointSheetContent(selectedObject, users)
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                users.filter { it.deleteAt == null }.forEach { UserCard(it, true) }
                if(users.any { it.deleteAt != null }) {
                    TextP("Temporary Links")()
                }
                users.filter { it.deleteAt != null }.forEach { UserCard(it, true) }
                if(usersAll.any { it.requestStatus == RequestStatus.AWAITING_REQUEST }) {
                    TextP("Requests for Sharing")()
                }
                usersAll.filter { it.requestStatus == RequestStatus.AWAITING_REQUEST }.forEach { UserAwaitingRequest(it) }

                if(usersAll.any { it.requestStatus == RequestStatus.AWAITING_RESPONSE }) {
                    TextP("Awaiting Response")()
                }
                usersAll.filter { it.requestStatus == RequestStatus.AWAITING_RESPONSE }.forEach { UserAwaitingResponse(it) }
                devices.forEach { DeviceCard(it) }
                if(waypoints.isNotEmpty()) {
                    TextP("Saved Places")()
                }
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

var AddPersonPopupInitial: ULong? = null

@OptIn(ExperimentalTime::class)
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
                    RequestStatus.AWAITING_RESPONSE,
                    null,
                    null
                ), false
            )
        else
            Card {
                ListItem(
                    leadingContent = { GreenCircle(50.dp) },
                    headlineContent = {
                        Text(stringResource(Res.string.tap_pick_contact),
                            fontWeight = FontWeight.Bold
                        )
                    })
            }
    }
    val recipientID = SimpleOutlinedTextField(stringResource(Res.string.contact_findfamily_id), initial = AddPersonPopupInitial?.encodeBase26()?:"", isError = { UsersCached.getByID(it.decodeBase26())?.requestStatus in listOf(
        RequestStatus.AWAITING_RESPONSE, RequestStatus.MUTUAL_CONNECTION) }, subtext = {
        if(it.decodeBase26() == Networking.userid) "Cannot share your location with yourself"
        else when(UsersCached.getByID(it.decodeBase26())?.requestStatus) {
            RequestStatus.MUTUAL_CONNECTION -> "Already sharing with this person"
            RequestStatus.AWAITING_RESPONSE -> "Already requested to share with this person"
            RequestStatus.AWAITING_REQUEST -> "This person has requested your location"
            else -> null
        }
    }, readOnly = AddPersonPopupInitial != null)

    Text(stringResource(Res.string.contact_findfamily_id_desc))
    OutlinedButton({
        platform.copyToClipboard(Networking.userid!!.encodeBase26())
    }) {
        Text(stringResource(Res.string.copy_findfamily_id))
    }

    val unnamed_str = stringResource(Res.string.unnamed_location)

    OutlinedButton({
        SuspendScope {
            val trueID = recipientID().decodeBase26()
            checkSharingRequests()
            val requestStatus = if(UsersCached.getByID(trueID)?.requestStatus == RequestStatus.AWAITING_REQUEST) {
                checkSharingRequests()
                RequestStatus.MUTUAL_CONNECTION
            } else {
                Networking.sendLocationRequest(trueID)
                RequestStatus.AWAITING_RESPONSE
            }
            val newUser = User(
                trueID,
                contactName,
                contactPhoto,
                unnamed_str,
                true,
                requestStatus,
                null,
                null,
            )
            UsersCached.upsert(newUser)
            close()
        }
    }, enabled = contactName.isNotEmpty() && recipientID().isNotEmpty() && UsersCached.getByID(recipientID().decodeBase26())?.requestStatus !in listOf(
        RequestStatus.AWAITING_RESPONSE, RequestStatus.MUTUAL_CONNECTION)
    ) {
        if(UsersCached.getByID(recipientID().decodeBase26())?.requestStatus == RequestStatus.AWAITING_RESPONSE) {
            Text(stringResource(Res.string.accept_start_sharing))
        } else {
            Text(stringResource(Res.string.request_start_sharing))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DialogScope.AddPersonPopupTemporary() {
    val minutes_str = stringResource(Res.string.minutes)
    val hour_str = stringResource(Res.string.hour)
    val hours_str = stringResource(Res.string.hours)
    var expiryTime by remember { mutableStateOf("15 $minutes_str") }

    Text(stringResource(Res.string.temporary_link_title), style = MaterialTheme.typography.titleLarge)

    Spacer(Modifier.height(4.dp))
    val contactName = SimpleOutlinedTextField(stringResource(Res.string.temporary_link_name))
    Spacer(Modifier.width(16.dp))
    val options = mapOf(
        "15 $minutes_str" to 15.minutes,
        "30 $minutes_str" to 30.minutes,
        "1 $hour_str" to 1.hours,
        "2 $hours_str" to 2.hours,
        "6 $hours_str" to 6.hours,
        "12 $hours_str" to 12.hours,
        "24  $hours_str" to 1.days
    )
    DropdownField(expiryTime, { expiryTime = it }, options.keys)

    OutlinedButton({
        SuspendScope {
            val keypair = Networking.generateKeyPair()

            val newUser = User(Random.nextULong(), contactName(), null,
                keypair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.PEM).encodeBase64(),
                true, RequestStatus.MUTUAL_CONNECTION, null, null,
                deleteAt = Clock.System.now() + options[expiryTime]!!,
                encryptionKey = keypair.publicKey.encodeToByteArray(RSA.PublicKey.Format.PEM).encodeBase64()
            )
            UsersCached.upsert(newUser)
            close()
        } }, Modifier,contactName().isNotEmpty() && expiryTime.isNotEmpty()) {
        Text(stringResource(Res.string.temporary_link_submit))
    }
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