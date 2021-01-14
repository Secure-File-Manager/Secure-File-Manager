package com.securefilemanager.app.extensions

import java.util.*
import kotlin.math.pow


fun Int.getFormattedDuration(): String {
    val sb = StringBuilder(8)
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60

    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    }

    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

fun Int.convertKBtoKiB(): Int =
    kotlin.math.floor((10.0.pow(3.0) * this) / 2.0.pow(10.0)).toInt()
