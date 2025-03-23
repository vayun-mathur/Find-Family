package com.opengps.locationsharing

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt


@Serializable
data class Coord(val lat: Double, val lon: Double)

fun doProjection(coord: Coord): Pair<Double, Double> {
    val num = coord.lon * 0.017453292519943295 // 2*pi / 360
    val X = 6378137.0 * num
    val a = coord.lat * 0.017453292519943295
    val Y = 3189068.5 * ln((1.0 + sin(a)) / (1.0 - sin(a)))
    return Pair(normalize(X, min = X0, max = -X0), normalize(Y, min = -X0, max = X0))
}

fun doInverseProjection(x: Double, y: Double): Coord {
    val X = inverseNormalize(x, min = X0, max = -X0)
    val Y = inverseNormalize(y, min = -X0, max = X0)
    val a = asin((exp(Y/3189068.5) - 1)/(exp(Y/3189068.5) + 1))
    val num = X / 6378137.0
    val lon = num / 0.017453292519943295
    val lat = a / 0.017453292519943295
    return Coord(lat, lon)
}

private fun inverseNormalize(res: Double, min: Double, max: Double): Double {
    return res * (max - min) + min
}

private fun normalize(t: Double, min: Double, max: Double): Double {
    return (t - min) / (max - min)
}

private const val X0 = -2.0037508342789248E7

fun havershine(p1: Coord, p2: Coord): Double {
    val R = 6371000 // Radius of the earth in m
    val dLat = radians(p2.lat-p1.lat)  // deg2rad below
    val dLon = radians(p2.lon-p1.lon)
    val a =
        sin(dLat/2) * sin(dLat/2) +
                cos(radians(p1.lat)) * cos(radians(p2.lat)) *
                sin(dLon/2) * sin(dLon/2)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    val d = R * c // Distance in m
    return d
}