package com.opengps.locationsharing

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import location_sharing.shared.generated.resources.Res
import location_sharing.shared.generated.resources.temporary_link_expiry
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun radians(degrees: Double) = degrees * PI / 180

@OptIn(ExperimentalTime::class)
fun timestring(timestamp: Long): String {
    val currentTime = Clock.System.now()
    val timestampInstant = Instant.fromEpochMilliseconds(timestamp)
    val duration = currentTime - timestampInstant
    return if(duration.inWholeSeconds < 60) {
        "just now"
    } else if(duration.inWholeMinutes < 60) {
        "${duration.inWholeMinutes} minutes ago"
    } else if(duration.inWholeHours < 24) {
        "${duration.inWholeHours} hours ago"
    } else {
        "${duration.inWholeDays} days ago"
    }
}

fun String.isPositiveNumber(): Boolean = this.toDoubleOrNull() != null && this.toDouble() > 0

fun String.decodeBase26(): ULong {
    var value = 0uL
    for(i in this.indices)
        value += (this[i].code - 65).toULong() * 26.0.pow(this.length - i - 1).toULong()
    return value
}

fun ULong.encodeBase26(): String {
    var result = ""
    var remaining = this
    while(remaining > 0uL) {
        result = ((remaining % 26uL) + 65uL).toInt().toChar() + result
        remaining /= 26uL
    }
    return result
}

fun SuspendScope(block: suspend () -> Unit): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        block()
    }
}
fun UISuspendScope(block: suspend () -> Unit): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        block()
    }
}

operator fun Offset.minus(intSize: IntSize): Offset {
    return Offset(x - intSize.width, y - intSize.height)
}

object DateFormats {
    // example: Jun 4
    val MONTH_DAY = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        chars(" ")
        day()
    }
    // example: 05/12/2025
    val DATE_INPUT = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        chars(" ")
        day()
    }
    // example: 10:05 am
    val TIME_AM_PM = LocalTime.Format {
        amPmHour()
        chars(":")
        minute()
        chars(" ")
        amPmMarker("am", "pm")
    }
    // example: 10:05 am
    val TIME_SECOND_AM_PM = LocalTime.Format {
        amPmHour()
        chars(":")
        minute()
        chars(":")
        second()
        chars(" ")
        amPmMarker("AM", "PM")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerHelper(localDate: LocalDate, onDatePicked: (LocalDate) -> Unit) {
    var pickedDate by remember { mutableStateOf(localDate.atTime(0, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds())}
    val pickedLocalDate by remember { derivedStateOf { Instant.fromEpochMilliseconds(pickedDate).toLocalDateTime(TimeZone.UTC).date } }
    LaunchedEffect(pickedLocalDate) {
        onDatePicked(pickedLocalDate)
    }
    var showDialog by remember { mutableStateOf(false) }
    OutlinedButton({
        showDialog = true
    }) {
        Text(pickedLocalDate.format(DateFormats.DATE_INPUT))
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
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        valueRange = valueRange,
        modifier = modifier
            .layout { measurable, constraints ->
                // 1. Measure the slider horizontally, but swap width and height constraints.
                // The slider's "width" will be constrained by the layout's "maxHeight".
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                    )
                )

                // 2. The layout's dimensions are now swapped.
                // The layout's "width" is the slider's "height".
                // The layout's "height" is the slider's "width".
                layout(placeable.height, placeable.width) {
                    // 3. Place the slider, rotating it -90 degrees.
                    // We must also translate it to account for the rotation.
                    placeable.placeRelativeWithLayer(
                        x = -(placeable.width / 2 - placeable.height / 2),
                        y = -(placeable.height / 2 - placeable.width / 2),
                    ) {
                        rotationZ = -90f
                    }
                }
            }
    )
}

@Composable
fun SimpleOutlinedTextField(label: String, initial: String = "", suffix: @Composable (() -> Unit)? = null, readOnly: Boolean = false, onChange: (String) -> String? = {null}, isError: (String) -> Boolean = {false}, subtext: (String) -> String? = {null}): ()->String {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(text, { text = it; text = onChange(it)?:text }, label = { Text(label) }, suffix = suffix, readOnly = readOnly, isError = isError(text), supportingText = {subtext(text)?.let { Text(it) }})
    return { text }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(value: String, setValue: (String) -> Unit, options: Collection<String>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value, {}, Modifier.fieldClickable { expanded = true }, readOnly = true,
            label = TextP(
                stringResource(Res.string.temporary_link_expiry)
            ),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
        )
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { selectionOption ->
                DropdownMenuItem(TextP(text = selectionOption), {
                    setValue(selectionOption)
                    expanded = false
                })
            }
        }
    }
}

inline fun Modifier.fieldClickable(crossinline onClick: () -> Unit): Modifier =
    this.then(composed {
        pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null) {
                    onClick()
                }
            }
        }
    })