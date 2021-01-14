package com.securefilemanager.app.extensions

import android.graphics.Point
import kotlin.math.roundToInt

fun Point.formatAsResolution() = "$x x $y ${getMPx()}"

fun Point.getMPx(): String {
    val px = x * y / 1000000.toFloat()
    val rounded = (px * 10).roundToInt() / 10.toFloat()
    return "(${rounded}MP)"
}
