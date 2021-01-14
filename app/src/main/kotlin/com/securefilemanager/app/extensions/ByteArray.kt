package com.securefilemanager.app.extensions

import java.util.*

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun ByteArray.toASCIIPrintableString(): String =
    joinToString("") { (kotlin.math.abs(it % 95) + 32).toChar().toString() }

fun ByteArray.flush() = Arrays.fill(this, 0x0)
