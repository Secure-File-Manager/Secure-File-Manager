package com.securefilemanager.app.extensions

import android.content.Context
import com.securefilemanager.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

const val DELAY = 10L

fun ZipFile.checkDecompressionCollision(
    context: Context,
    destination: String,
    callback: (collision: Boolean) -> Unit
) {
    this.fileHeaders.forEach { fileHeader ->
        val name = fileHeader.fileName
        val extractionPath = "${destination.trimEnd('/')}/${name}"
        if (context.getDoesFilePathExist(extractionPath)) {
            context.toast(R.string.decompressing_failed)
            context.toastLong(
                String.format(
                    context.getString(R.string.decompress_conflicts),
                    name.trimEnd('/')
                )
            )
            callback.invoke(true)
            return
        }
    }
    callback.invoke(false)
}

fun ZipFile.insertAll(context: Context, sourcePaths: List<String>, parameters: ZipParameters) {
    val zipFile: ZipFile = this
    sourcePaths
        .map { sourcePath -> File(sourcePath) }
        .partition { file -> context.getIsPathDirectory(file.absolutePath) }
        .apply {
            zipFile.addFoldersAndFiles(this, parameters)
        }
}

fun ZipFile.addFoldersAndFiles(
    foldersAndFilesToAdd: Pair<List<File>, List<File>>,
    parameters: ZipParameters
) {
    val (folders: List<File>, files: List<File>) = foldersAndFilesToAdd
    runBlocking {
        waitAndAddFiles(files, parameters)
        waitAndAddFolders(folders, parameters)
    }
}

suspend fun ZipFile.waitAndAddFiles(filesToAdd: List<File>, parameters: ZipParameters) {
    if (filesToAdd.isNotEmpty()) {
        waitToReady(this) {
            this.addFiles(filesToAdd, parameters)
        }
    }
}

suspend fun ZipFile.waitAndAddFolders(foldersToAdd: List<File>, parameters: ZipParameters) {
    val zipFile: ZipFile = this
    foldersToAdd.forEach { folder ->
        waitToReady(zipFile) {
            zipFile.addFolder(folder, parameters)
        }
    }
}

private suspend fun waitToReady(zipFile: ZipFile, callback: () -> Unit) {
    while (zipFile.progressMonitor.state != ProgressMonitor.State.READY) {
        delay(DELAY)
    }
    callback.invoke()
}
