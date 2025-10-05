package com.opengps.locationsharing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.math.PI
import kotlin.math.pow
import kotlin.time.ExperimentalTime

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