package com.securefilemanager.app.extensions

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.*

fun ExifInterface.getExifProperties(): String {
    var exifString = ""
    getAttribute(ExifInterface.TAG_F_NUMBER).let {
        if (it?.isNotEmpty() == true) {
            val number = it.trimEnd('0').trimEnd('.')
            exifString += "F/$number  "
        }
    }

    getAttribute(ExifInterface.TAG_FOCAL_LENGTH).let {
        if (it?.isNotEmpty() == true) {
            val values = it.split('/')
            val focalLength = "${values[0].toDouble() / values[1].toDouble()}mm"
            exifString += "$focalLength  "
        }
    }

    getAttribute(ExifInterface.TAG_EXPOSURE_TIME).let {
        if (it?.isNotEmpty() == true) {
            val exposureValue = it.toFloat()
            exifString += if (exposureValue > 1f) {
                "${exposureValue}s  "
            } else {
                "1/${Math.round(1 / exposureValue)}s  "
            }
        }
    }

    getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS).let {
        if (it?.isNotEmpty() == true) {
            exifString += "ISO-$it"
        }
    }

    return exifString.trim()
}

fun ExifInterface.getExifDateTaken(context: Context): String {
    val dateTime = getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: getAttribute(ExifInterface.TAG_DATETIME)
    dateTime.let {
        if (it?.isNotEmpty() == true) {
            try {
                val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd kk:mm:ss", Locale.ENGLISH)
                return simpleDateFormat.parse(it)!!.time.formatDate(context).trim()
            } catch (ignored: Exception) {
            }
        }
    }
    return ""
}

fun ExifInterface.getExifCameraModel(): String {
    getAttribute(ExifInterface.TAG_MAKE).let {
        if (it?.isNotEmpty() == true) {
            val model = getAttribute(ExifInterface.TAG_MODEL)
            return "$it $model".trim()
        }
    }
    return ""
}
