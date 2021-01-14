package com.securefilemanager.app.extensions

import android.content.Context
import android.graphics.drawable.Drawable

fun getColoredDrawableWithColor(
    context: Context,
    drawableId: Int,
    alpha: Int = 255
): Drawable {
    val drawable = context.getDrawableById(drawableId)!!
    drawable.mutate().alpha = alpha
    return drawable
}
