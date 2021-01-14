package com.securefilemanager.app.extensions

import android.content.Context
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.models.FileDirItem
import java.io.File
import java.security.MessageDigest

fun File.isMediaFile() = absolutePath.isMediaFile()

fun File.getProperSize(): Long {
    return if (isDirectory) {
        getDirectorySize(this)
    } else {
        length()
    }
}

private fun getDirectorySize(dir: File): Long {
    var size = 0L
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            for (i in files.indices) {
                size += if (files[i].isDirectory) {
                    getDirectorySize(files[i])
                } else {
                    files[i].length()
                }
            }
        }
    }
    return size
}

fun File.getFileCount(): Int {
    return if (isDirectory) {
        getDirectoryFileCount(this)
    } else {
        1
    }
}

private fun getDirectoryFileCount(dir: File): Int {
    var count = -1
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            count++
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
    }
    return count
}

fun File.getDirectChildrenCount() =
    listFiles()?.size ?: 0

fun File.toFileDirItem(context: Context) = FileDirItem(
    absolutePath,
    name,
    context.getIsPathDirectory(absolutePath),
    0,
    length(),
    lastModified()
)

fun File.isEncrypted() =
    this.name.endsWith(encryptionExtensionDotted)

fun File.getDigest(algorithm: String): String =
    this.inputStream().use { fis ->
        val md = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(8192)
        generateSequence {
            when (val bytesRead = fis.read(buffer)) {
                -1 -> null
                else -> bytesRead
            }
        }.forEach { bytesRead -> md.update(buffer, 0, bytesRead) }
        md.digest().joinToString("") { "%02x".format(it) }
    }

fun File.md5(): String = this.getDigest(MD5)
fun File.sha1(): String = this.getDigest(SHA1)
fun File.sha256(): String = this.getDigest(SHA256)
fun File.sha512(): String = this.getDigest(SHA512)
