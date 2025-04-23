package com.opengps.locationsharing

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.PI
import kotlin.math.pow

fun radians(degrees: Double): Double {
    return degrees * PI / 180
}

fun timestring(timestamp: Long): String {
    val currentTime = Clock.System.now()
    val timestampInstant = Instant.fromEpochMilliseconds(timestamp)
    val duration = currentTime - timestampInstant
    if(duration.inWholeSeconds < 60) {
        return "just now"
    } else if(duration.inWholeMinutes < 60) {
        return "${duration.inWholeMinutes} minutes ago"
    } else if(duration.inWholeHours < 24) {
        return "${duration.inWholeHours} hours ago"
    } else {
        return "${duration.inWholeDays} days ago"
    }
}

fun String.isPositiveNumber(): Boolean = this.isNotEmpty() && this.toDoubleOrNull() != null && this.toDouble() > 0

fun String.decodeBase26(): ULong {
    var value = 0uL
    for(i in this.indices) {
        value += (this[i].code - 65).toULong() * 26.0.pow(this.length - i - 1).toULong()
    }
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