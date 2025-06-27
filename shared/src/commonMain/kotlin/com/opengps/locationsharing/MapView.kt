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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.sargunv.maplibrecompose.compose.CameraState
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.core.GestureSettings
import dev.sargunv.maplibrecompose.core.OrnamentSettings
import dev.whyoleg.cryptography.algorithms.RSA
import io.github.dellisd.spatialk.geojson.Position
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import location_sharing.shared.generated.resources.Res
import location_sharing.shared.generated.resources.accept_start_sharing
import location_sharing.shared.generated.resources.add_person
import location_sharing.shared.generated.resources.add_saved_location
import location_sharing.shared.generated.resources.change_connected_contact
import location_sharing.shared.generated.resources.connect_bluetooth_device
import location_sharing.shared.generated.resources.connect_bluetooth_device_description
import location_sharing.shared.generated.resources.contact_findfamily_id
import location_sharing.shared.generated.resources.contact_findfamily_id_desc
import location_sharing.shared.generated.resources.copy_findfamily_id
import location_sharing.shared.generated.resources.create_sharable_link
import location_sharing.shared.generated.resources.hour
import location_sharing.shared.generated.resources.hours
import location_sharing.shared.generated.resources.minutes
import location_sharing.shared.generated.resources.new_saved_place
import location_sharing.shared.generated.resources.past
import location_sharing.shared.generated.resources.present
import location_sharing.shared.generated.resources.request_start_sharing
import location_sharing.shared.generated.resources.save
import location_sharing.shared.generated.resources.saved_place_name
import location_sharing.shared.generated.resources.saved_place_notification
import location_sharing.shared.generated.resources.saved_place_range
import location_sharing.shared.generated.resources.share_your_location
import location_sharing.shared.generated.resources.showing
import location_sharing.shared.generated.resources.tap_pick_contact
import location_sharing.shared.generated.resources.temporary_link_expiry
import location_sharing.shared.generated.resources.temporary_link_name
import location_sharing.shared.generated.resources.temporary_link_submit
import location_sharing.shared.generated.resources.temporary_link_title
import location_sharing.shared.generated.resources.time_remaining
import location_sharing.shared.generated.resources.unnamed_location
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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
    UserPicture(user.photo, user.name.first(), size);
}

@Composable
fun UserPicture(userPhoto: String?, firstChar: Char, size: Dp) {
    val modifier = Modifier.clip(CircleShape).size(size).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    if(userPhoto != null)
        AsyncImage(userPhoto, null, modifier, contentScale = ContentScale.FillWidth)
    else {
        Box(modifier.background(Color.Green)) {
            Text(firstChar.toString(), Modifier.align(Alignment.Center), color = Color.White)
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

var addPersonPopupEnable: () -> Unit = {};

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
    val waypointDao = platform.database.waypointDao()
    val bluetoothDao = platform.database.bluetoothDeviceDao()

    var objects by remember {mutableStateOf(mapOf<ULong, ObjectParent>())}
    val waypoints by remember { derivedStateOf { objects.values.filterIsInstance<Waypoint>() } }
    val usersAll by remember { derivedStateOf { objects.values.filterIsInstance<User>() } }
    val users by remember { derivedStateOf { usersAll.filter { it.requestStatus == RequestStatus.MUTUAL_CONNECTION } } }
    val devices by remember { derivedStateOf { objects.values.filterIsInstance<BluetoothDevice>() } }

    addPersonPopupEnable = BasicDialog { AddPersonPopup(usersAll) }
    val addTemporaryPersonPopupEnable = BasicDialog { AddPersonPopupTemporary() }
    val addDevicePopupEnable = BasicDialog { AddDevicePopup() }

    LaunchedEffect(Unit) {
        SuspendScope {
            objects = (waypointDao.getAll() + usersDao.getAll() + bluetoothDao.getAll()).associateBy { it.id }
            while(Networking.userid == null) { delay(500) }
            if(users.isEmpty()) {
                val newUser = User(Networking.userid!!, "Me", null, "Unnamed Location", true, RequestStatus.MUTUAL_CONNECTION, null, null)
                usersDao.upsert(newUser)
                objects = objects + (newUser.id to newUser)
            }

            while(true) {
                objects = (waypointDao.getAll() + usersDao.getAll() + bluetoothDao.getAll()).associateBy { it.id }
                selectedObject = objects[selectedObject?.id]
                delay(1000)
            }
        }
    }

    LaunchedEffect(selectedObject) {
        val obj = selectedObject
        val newZoom = max(camera.position.zoom, 14.0)
        if(obj is User && obj.lastLocationValue != null)
            camera.animateTo(camera.position.copy(target = obj.lastLocationValue!!.coord.toPosition(), zoom = newZoom))
        if(obj is BluetoothDevice && obj.lastLocationValue != null)
            camera.animateTo(camera.position.copy(target = obj.lastLocationValue.coord.toPosition(), zoom = newZoom))
        if(obj is Waypoint)
            camera.animateTo(camera.position.copy(target = obj.coord.toPosition(), zoom = newZoom))
    }

    BottomSheetScaffold({
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                SheetContent(selectedObject, usersAll, waypoints, devices)
        } }, topBar = {
        val actions: @Composable RowScope.() -> Unit = {
            val obj = selectedObject
            if(obj is Waypoint && !isEditingWaypoint) {
                IconButton({
                    currentWaypointPosition = obj.coord
                    currentWaypointRadius = obj.range
                    UISuspendScope {
                        camera.animateTo(camera.position.copy(target = currentWaypointPosition.toPosition()))
                    }
                    isEditingWaypoint = true
                }) {
                    Icon(Icons.Default.Edit, null)
                }
            }
            if(obj != null && obj.id != Networking.userid) {
                IconButton({
                    SuspendScope {
                        when (obj) {
                            is User -> usersDao.delete(obj)
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
            var expanded by remember { mutableStateOf(false) }
            IconButton({ expanded = true }) {
                Icon(Icons.Default.Add, null)
            }
            val new_saved_place_string = stringResource(Res.string.new_saved_place)
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(TextP(stringResource(Res.string.add_person)),
                    { AddPersonPopupInitial = null; addPersonPopupEnable(); expanded = false })
                DropdownMenuItem(TextP(stringResource(Res.string.create_sharable_link)),
                    { addTemporaryPersonPopupEnable(); expanded = false })
                DropdownMenuItem(TextP(stringResource(Res.string.add_saved_location)),
                    {
                        val newWaypoint = Waypoint(Random.nextULong(), new_saved_place_string, 100.0, Coord(camera.position.target.latitude, camera.position.target.longitude), mutableListOf())
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
            IconButton({
                SuspendScope {
                    val file = FileKit.openFileSaver(suggestedName = "findfamily_backup", extension = "db")
                    file?.let {
                        Backup.downloadBackupFile(it)
                    }
                }
            }) {
                Icon(Icons.Default.Download, null);
            }
            IconButton({
                SuspendScope {
                    val file = FileKit.openFilePicker()
                    file?.let {
                        Backup.restoreBackupFile(it)
                    }
                }
            }) {
                Icon(Icons.Default.Upload, null);
            }
        }
        val navIcon = @Composable {
            if (selectedObject != null)
                IconButton({ selectedObject = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
        }
        TopAppBar(TextP(selectedObject?.name ?: "Find Family"), Modifier, navIcon, actions)
    }, sheetPeekHeight = 200.dp) { padding ->

        Box(Modifier.padding(padding).fillMaxSize()) {
            val density = LocalDensity.current
            MaplibreMap(Modifier, "https://tiles.openfreemap.org/styles/liberty", 0f..20f,
                0f..60f,
                GestureSettings(false,true,false,true),
                OrnamentSettings.AllDisabled, camera,
                onMapClick = { _, offset ->
                    for (user in users) {
                        if(user.lastLocationValue == null) continue
                        val center = camera.screenLocationFromPosition(user.lastLocationValue!!.coord.toPosition())
                        if((center - offset).getDistance() * density.density < 80) {
                            selectedObject = user
                            return@MaplibreMap ClickResult.Pass
                        }
                    }
                    for (waypoint in waypoints) {
                        val center = camera.screenLocationFromPosition(waypoint.coord.toPosition())
                        if((center - offset).getDistance() * density.density < 80) {
                            selectedObject = waypoint
                            return@MaplibreMap ClickResult.Pass
                        }
                    }
                    ClickResult.Pass
                }
            )
            val textMeasurer = rememberTextMeasurer()
            key(camera.position) {
                Canvas(Modifier.fillMaxSize()) {
                    if (initialized) {
                        for (waypoint in waypoints) {
                            val radiusMeters =
                                if (waypoint.id == selectedObject?.id) currentWaypointRadius else waypoint.range
                            val coord =
                                if (waypoint.id == selectedObject?.id) currentWaypointPosition else waypoint.coord

                            val center = camera.screenLocationFromPosition(coord.toPosition())
                            if (center.x < 0.dp || center.y < 0.dp || center.x > size.toDpSize().width || center.y > size.toDpSize().height) continue
                            val circumferenceAtLatitude =
                                40_075_000 * cos(radians(waypoint.coord.lat))
                            val radiusInDegrees = 360 * radiusMeters / circumferenceAtLatitude
                            val edgePoint = camera.screenLocationFromPosition(
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
                        for (user in users) {
                            if (user.lastLocationValue == null) continue
                            val center =
                                camera.screenLocationFromPosition(user.lastLocationValue!!.coord.toPosition())
                            if (center !in size.toDpSize()) continue

                            Circle(center.toOffset(this), Color.Green, primaryColor, 75f)
                            CenteredText(
                                textMeasurer,
                                "${user.name[0]}",
                                center.toOffset(this),
                                primaryColor
                            )
                            // todo: show profile photo
                        }
                    }
                }
            }

            val obj = selectedObject
            if(obj is User || obj is BluetoothDevice) {
                locations[obj.id]?.let { locs ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                        Card(Modifier.fillMaxWidth(0.5f)) {
                            var isShowingPresent by remember { mutableStateOf(true) }
                            Box(Modifier.padding(8.dp)) {
                                OutlinedButton({
                                    isShowingPresent = !isShowingPresent
                                }, Modifier.fillMaxWidth()) {
                                    Text(if (isShowingPresent) "Show History" else "Show Present")
                                }
                            }
                            if (!isShowingPresent) {
                                var pickedDate by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds())}
                                val pickedLocalDate = Instant.fromEpochMilliseconds(pickedDate).toLocalDateTime(TimeZone.UTC).date
                                var showDialog by remember { mutableStateOf(false) }
                                OutlinedButton({
                                    // open date picker
                                    showDialog = true
                                }) {
                                    val datestring = LocalDate.Format {
                                        monthNumber()
                                        chars("/")
                                        dayOfMonth()
                                        chars("/")
                                        year()
                                    }.format(pickedLocalDate)
                                    Text(datestring)
                                }
                                if(showDialog) {
                                    val datePickerState = rememberDatePickerState(pickedDate)
                                    DatePickerDialog(
                                        onDismissRequest = {showDialog = false},
                                        dismissButton = {
                                            Button({showDialog = false}) {
                                                Text("Cancel")
                                            }
                                        },
                                        confirmButton = {
                                            Button({
                                                showDialog = false
                                                datePickerState.selectedDateMillis?.let {
                                                    pickedDate = it
                                                }
                                            }) {
                                                Text("Select Date")
                                            }
                                        },
                                    ) {
                                        DatePicker(datePickerState)
                                    }
                                }
                                var secondOfDay by remember { mutableStateOf(0.0) }
                                Slider(
                                    secondOfDay.toFloat(),
                                    { secondOfDay = it.toDouble() },
                                    Modifier.padding(16.dp),
                                    valueRange = 0f..(3600*24f-1)
                                )
                                val time = LocalTime.fromSecondOfDay(secondOfDay.toInt())
                                val simulatedTimestamp = pickedLocalDate.atTime(time).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

                                val points = locs.map { it.timestamp to it.coord }
                                val closest =
                                    points.minBy { abs(it.first - simulatedTimestamp) }
                                LaunchedEffect(closest) {
                                    val newZoom = max(camera.position.zoom, 14.0)
                                    camera.animateTo(
                                        camera.position.copy(
                                            target = closest.second.toPosition(),
                                            zoom = newZoom
                                        )
                                    )
                                }
                                ListItem(TextP(LocalTime.Format {
                                    amPmHour()
                                    chars(":")
                                    minute()
                                    chars(" ")
                                    amPmMarker("am", "pm")
                                }.format(time)))

                            }
                        }
                    }
                }
            }
        }
    }
}

private operator fun DpSize.contains(dpOffset: DpOffset): Boolean {
    return dpOffset.x in 0.dp..width && dpOffset.y in 0.dp..height
}

private fun DpOffset.getDistance(): Float {
    return sqrt(x.value * x.value + y.value * y.value)
}

private fun Coord.toPosition() = Position(lon, lat)


private fun Position.toCoord() = Coord(latitude, longitude)

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
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.share_your_location))
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
        val waypointNewName = SimpleOutlinedTextField(stringResource(Res.string.saved_place_name), readOnly = !isEditingWaypoint, initial = waypoint.name)
        val waypointNewRadius = SimpleOutlinedTextField(stringResource(Res.string.saved_place_range), initial = waypoint.range.toString(), suffix = {Text("meters")}, readOnly = !isEditingWaypoint, onChange = {
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
                Text(stringResource(Res.string.save))
            }
        }

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
                        platform.database.usersDao().delete(user)
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
                        platform.database.usersDao().delete(user)
                    }
                }) {
                    Icon(Icons.Default.Delete, null)
                }
            })
    }
}

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

var AddPersonPopupInitial: ULong? = null;

@Composable
fun DialogScope.AddPersonPopup(users: List<User>) {
    var usersAwaitingRequest = users.filter { it.requestStatus == RequestStatus.AWAITING_REQUEST }.map { it.id };
    var usersAlreadySharing = users.filter { it.requestStatus == RequestStatus.MUTUAL_CONNECTION }.map { it.id };
    var usersAwaitingResponse = users.filter { it.requestStatus == RequestStatus.AWAITING_RESPONSE }.map { it.id };
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
                            stringResource(Res.string.tap_pick_contact),
                            fontWeight = FontWeight.Bold
                        )
                    })
            }
    }
    val recipientID = SimpleOutlinedTextField(stringResource(Res.string.contact_findfamily_id), initial = AddPersonPopupInitial?.encodeBase26()?:"", isError = { it.decodeBase26() in (usersAlreadySharing + usersAwaitingResponse) }, subtext = {
        when(it.decodeBase26()) {
            Networking.userid -> "Cannot share your location with yourself"
            in usersAlreadySharing -> "Already sharing with this person"
            in usersAwaitingResponse -> "Already requested to share with this person"
            in usersAwaitingRequest -> "This person has requested your location"
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
            val requestStatus = if(usersAwaitingRequest.contains(trueID)) {
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
            platform.database.usersDao().upsert(newUser)
            close()
        }
    }, enabled = contactName.isNotEmpty() && recipientID().isNotEmpty() && recipientID().decodeBase26() !in (usersAlreadySharing + usersAwaitingResponse)
    ) {
        if(recipientID().decodeBase26() in usersAwaitingResponse) {
            Text(stringResource(Res.string.accept_start_sharing))
        } else {
            Text(stringResource(Res.string.request_start_sharing))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
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
    Column {
        var expanded by remember { mutableStateOf(false) }
        OutlinedTextField(expiryTime, {}, Modifier.clickable { expanded = true }, readOnly = true, enabled = false, label = TextP(
            stringResource(Res.string.temporary_link_expiry)
        ),
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
            listOf("15 $minutes_str", "30 $minutes_str", "1 $hour_str", "2 $hours_str", "6 $hours_str", "12 $hours_str", "24 $hours_str").forEach { selectionOption ->
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
                false, RequestStatus.MUTUAL_CONNECTION, null, null,
                deleteAt = Clock.System.now() + when (expiryTime) {
                        "15 $minutes_str" -> 15.minutes
                        "30 $minutes_str" -> 30.minutes
                        "1 $hour_str" -> 1.hours
                        "2 $hours_str" -> 2.hours
                        "6 $hours_str" -> 6.hours
                        "12 $hours_str" -> 12.hours
                        "24  $hours_str" -> 1.days
                        else -> throw IllegalStateException("Invalid expiry time for location sharing")
                    },
                encryptionKey = keypair.publicKey.encodeToByteArray(RSA.PublicKey.Format.PEM).encodeBase64()
            )
            platform.database.usersDao().upsert(newUser)
            close()
        } }, Modifier,contactName().isNotEmpty() && expiryTime.isNotEmpty()) {
        Text(stringResource(Res.string.temporary_link_submit))
    }
}

@Composable
fun SimpleOutlinedTextField(label: String, initial: String = "", suffix: @Composable (() -> Unit)? = null, readOnly: Boolean = false, onChange: (String) -> String? = {null}, isError: (String) -> Boolean = {false}, subtext: (String) -> String? = {null}): ()->String {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(text, { text = it; text = onChange(it)?:text }, label = { Text(label) }, suffix = suffix, readOnly = readOnly, isError = isError(text), supportingText = {subtext(text)?.let { Text(it) }})
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