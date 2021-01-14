package com.securefilemanager.app.extensions


fun Any.toInt() = Integer.parseInt(toString())

fun <T> Any.privateField(name: String): T {
    val field = this::class.java.getDeclaredField(name)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as T
}
