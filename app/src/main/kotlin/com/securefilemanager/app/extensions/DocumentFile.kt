package com.securefilemanager.app.extensions

import androidx.documentfile.provider.DocumentFile

fun DocumentFile.getItemSize(): Long {
    return if (isDirectory) {
        getDirectorySize(this)
    } else {
        length()
    }
}

private fun getDirectorySize(dir: DocumentFile): Long {
    var size = 0L
    if (dir.exists()) {
        val files = dir.listFiles()
        for (i in files.indices) {
            val file = files[i]
            if (file.isDirectory) {
                size += getDirectorySize(file)
            } else {
                size += file.length()
            }
        }
    }
    return size
}

fun DocumentFile.getFileCount(): Int {
    return if (isDirectory) {
        getDirectoryFileCount(this)
    } else {
        1
    }
}

private fun getDirectoryFileCount(dir: DocumentFile): Int {
    var count = 0
    if (dir.exists()) {
        val files = dir.listFiles()
        for (i in files.indices) {
            val file = files[i]
            if (file.isDirectory) {
                count++
                count += getDirectoryFileCount(file)
            } else {
                count++
            }
        }
    }
    return count
}
