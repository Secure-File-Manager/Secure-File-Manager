package com.securefilemanager.app.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.Pair
import androidx.lifecycle.ProcessLifecycleOwner
import com.securefilemanager.app.R
import com.securefilemanager.app.asynctasks.CopyMoveTask
import com.securefilemanager.app.dialogs.FileConflictDialog
import com.securefilemanager.app.dialogs.FilePickerDialog
import com.securefilemanager.app.dialogs.PasswordPromptDialog
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.interfaces.CopyMoveListener
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.observers.AuthenticationObserver
import com.securefilemanager.app.receivers.LockReceiver
import net.lingala.zip4j.ZipFile
import java.io.File
import java.util.*

abstract class BaseAbstractActivity : AppCompatActivity() {

    private lateinit var mAuthenticationObserver: AuthenticationObserver

    private var lockReceiver: LockReceiver = LockReceiver()
    private var actionOnPermission: ((granted: Boolean) -> Unit)? = null

    var copyMoveCallback: ((destinationPath: String, copiedAll: Boolean) -> Unit)? = null
    var checkedDocumentPath = ""

    companion object {
        var funAfterSAFPermission: ((success: Boolean) -> Unit)? = null
    }

    private val copyMoveListener = object : CopyMoveListener {
        override fun copySucceeded(
            copyOnly: Boolean,
            copiedAll: Boolean,
            destinationPath: String,
            encryptionAction: EncryptionAction,
            hideAction: HideAction
        ) {
            when {
                isEncryption(encryptionAction) -> {
                    toast(if (copiedAll) R.string.encrypting_success else R.string.encrypting_success_partial)
                }
                isDecryption(encryptionAction) -> {
                    toast(if (copiedAll) R.string.decrypting_success else R.string.decrypting_success_partial)
                }
                isHide(hideAction) ->
                    toast(if (copiedAll) R.string.hiding_success else R.string.hiding_success_partial)
                isUnhide(hideAction) ->
                    toast(if (copiedAll) R.string.unhiding_success else R.string.unhiding_success_partial)
                copyOnly -> {
                    toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
                }
                else -> {
                    toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
                }
            }

            copyMoveCallback?.invoke(destinationPath, copiedAll)
            copyMoveCallback = null
        }

        override fun copyFailed(encryptionAction: EncryptionAction) {
            toast(
                when (encryptionAction) {
                    EncryptionAction.ENCRYPT -> R.string.encryption_failed
                    EncryptionAction.DECRYPT -> R.string.decryption_failed
                    else -> R.string.copy_move_failed
                }
            )
            copyMoveCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.mAuthenticationObserver = AuthenticationObserver(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this.mAuthenticationObserver)

        this.registerReceiver(this.lockReceiver, LockReceiver.getIntent())

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            this.appLock()
        }

        val hiddenPath = this.getHiddenPath()
        this.config.hiddenPath = hiddenPath
        if (!getDoesFilePathExist(hiddenPath)) {
            createDirectorySync(hiddenPath)
        }

        this.addFlagsSecure()
        this.setTheme()

        super.onCreate(savedInstanceState)
    }

    override fun onStop() {
        super.onStop()
        actionOnPermission = null
    }

    override fun onDestroy() {
        funAfterSAFPermission = null
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this.mAuthenticationObserver)
        this.unregisterReceiver(this.lockReceiver)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        val partition = try {
            checkedDocumentPath.substring(9, 18)
        } catch (e: Exception) {
            ""
        }
        if (requestCode == OPEN_DOCUMENT_TREE) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition =
                    partition.isEmpty() || resultData.dataString!!.contains(partition)
                if (isProperSDFolder(resultData.data!!) && isProperPartition) {
                    saveTreeUri(resultData)
                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            }
        } else {
            funAfterSAFPermission?.invoke(false)
        }
    }

    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        config.treeUri = treeUri.toString()

        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri!!, takeFlags)
    }

    private fun isProperSDFolder(uri: Uri) =
        isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")

    private fun isInternalStorage(uri: Uri) =
        isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri)
            .contains("primary")

    private fun isExternalStorageDocument(uri: Uri) =
        "com.android.externalstorage.documents" == uri.authority

    fun copyMoveFilesTo(
        fileDirItems: ArrayList<FileDirItem>,
        source: String,
        destination: String,
        isCopyOperation: Boolean,
        copyPhotoVideoOnly: Boolean,
        encryptionAction: EncryptionAction = EncryptionAction.NONE,
        hideAction: HideAction = HideAction.NONE,
        callback: (destinationPath: String, copiedAll: Boolean) -> Unit
    ) {
        if (source == destination && isNotEncryption(encryptionAction)) {
            toast(R.string.source_and_destination_same)
            return
        }

        if (!getDoesFilePathExist(destination)) {
            toast(R.string.invalid_destination)
            return
        }

        handleSAFDialog(destination) {
            if (!it) {
                copyMoveListener.copyFailed()
                return@handleSAFDialog
            }

            copyMoveCallback = callback
            var fileCountToCopy = fileDirItems.size
            if (isCopyOperation || !isNotEncryption(encryptionAction)) {
                startCopyMove(
                    fileDirItems,
                    destination,
                    isCopyOperation,
                    copyPhotoVideoOnly,
                    encryptionAction,
                    hideAction
                )
            } else {
                if (
                    isPathOnSD(source) ||
                    isPathOnSD(destination) ||
                    fileDirItems.first().isDirectory
                ) {
                    handleSAFDialog(source) { success ->
                        if (success) {
                            startCopyMove(
                                fileDirItems,
                                destination,
                                isCopyOperation,
                                copyPhotoVideoOnly,
                                encryptionAction,
                                hideAction
                            )
                        }
                    }
                } else {
                    try {
                        checkConflicts(
                            fileDirItems,
                            destination,
                            0,
                            LinkedHashMap(),
                            encryptionAction
                        ) { resolutions ->
                            ensureBackgroundThread {
                                toast(R.string.moving)
                                val updatedPaths = ArrayList<String>(fileDirItems.size)
                                val destinationFolder = File(destination)
                                for (oldFileDirItem in fileDirItems) {
                                    var newFile = File(destinationFolder, oldFileDirItem.name)
                                    if (newFile.exists()) {
                                        when {
                                            getConflictResolution(
                                                resolutions,
                                                newFile.absolutePath
                                            ) == CONFLICT_SKIP -> fileCountToCopy--
                                            getConflictResolution(
                                                resolutions,
                                                newFile.absolutePath
                                            ) == CONFLICT_KEEP_BOTH -> newFile =
                                                getAlternativeFile(newFile)
                                            else ->
                                                // this file is guaranteed to be on the internal storage, so just delete it this way
                                                newFile.delete()
                                        }
                                    }

                                    if (!newFile.exists() && File(oldFileDirItem.path).renameTo(
                                            newFile
                                        )
                                    ) {
                                        if (!config.keepLastModified) {
                                            newFile.setLastModified(System.currentTimeMillis())
                                        }
                                        updatedPaths.add(newFile.absolutePath)
                                        deleteFromMediaStore(oldFileDirItem.path)
                                    }
                                }

                                runOnUiThread {
                                    if (updatedPaths.isEmpty()) {
                                        copyMoveListener.copySucceeded(
                                            false,
                                            fileCountToCopy == 0,
                                            destination,
                                            encryptionAction,
                                            hideAction
                                        )
                                    } else {
                                        copyMoveListener.copySucceeded(
                                            false,
                                            fileCountToCopy <= updatedPaths.size,
                                            destination,
                                            encryptionAction,
                                            hideAction
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        }
    }

    fun getAlternativeFile(file: File): File {
        var fileIndex = 1
        var newFile: File?
        do {
            val newName =
                String.format("%s(%d).%s", file.nameWithoutExtension, fileIndex, file.extension)
            newFile = File(file.parent, newName)
            fileIndex++
        } while (getDoesFilePathExist(newFile!!.absolutePath))
        return newFile
    }

    fun getEncryptedFile(file: File): File {
        val newName =
            String.format(
                "%s.%s.%s",
                file.nameWithoutExtension,
                file.extension,
                encryptionExtension
            )
        return File(file.parent, newName)
    }

    fun getDecryptedFile(file: File): File {
        val newName = file.name.removeSuffix(encryptionExtensionDotted)
        return File(file.parent, newName)
    }

    private fun startCopyMove(
        files: ArrayList<FileDirItem>,
        destinationPath: String,
        isCopyOperation: Boolean,
        copyPhotoVideoOnly: Boolean,
        encryptionAction: EncryptionAction,
        hideAction: HideAction
    ) {
        val availableSpace = destinationPath.getAvailableStorageB()
        val sumToCopy = files.sumByLong { it.getProperSize() }
        if (availableSpace == -1L || sumToCopy < availableSpace) {
            checkConflicts(files, destinationPath, 0, LinkedHashMap(), encryptionAction) {
                toast(
                    when {
                        isEncryption(encryptionAction) -> {
                            R.string.encrypting
                        }
                        isDecryption(encryptionAction) -> {
                            R.string.decrypting
                        }
                        isHide(hideAction) -> {
                            R.string.hiding
                        }
                        isUnhide(hideAction) -> {
                            R.string.unhiding
                        }
                        isCopyOperation -> {
                            R.string.copying
                        }
                        else -> {
                            R.string.moving
                        }
                    }
                )
                val pair = Pair(files, destinationPath)
                CopyMoveTask(
                    this,
                    isCopyOperation,
                    copyPhotoVideoOnly,
                    it,
                    copyMoveListener,
                    encryptionAction,
                    hideAction
                ).execute(pair)
            }
        } else {
            val text = String.format(
                getString(R.string.no_space),
                sumToCopy.formatSize(),
                availableSpace.formatSize()
            )
            toastLong(text)
        }
    }

    private fun checkConflicts(
        files: ArrayList<FileDirItem>,
        destinationPath: String,
        index: Int,
        conflictResolutions: LinkedHashMap<String, Int>,
        encryptionAction: EncryptionAction,
        callback: (resolutions: LinkedHashMap<String, Int>) -> Unit
    ) {
        if (index == files.size) {
            callback(conflictResolutions)
            return
        }

        val file = files[index]
        val newFileDirItem =
            FileDirItem("$destinationPath/${file.name}", file.name, file.isDirectory)
        if (getDoesFilePathExist(newFileDirItem.path)) {
            if (isNotEncryption(encryptionAction)) {
                FileConflictDialog(
                    this,
                    newFileDirItem,
                    files.size > 1
                ) { resolution, applyForAll ->
                    if (applyForAll) {
                        conflictResolutions.clear()
                        conflictResolutions[""] = resolution
                        checkConflicts(
                            files,
                            destinationPath,
                            files.size,
                            conflictResolutions,
                            encryptionAction,
                            callback
                        )
                    } else {
                        conflictResolutions[newFileDirItem.path] = resolution
                        checkConflicts(
                            files,
                            destinationPath,
                            index + 1,
                            conflictResolutions,
                            encryptionAction,
                            callback
                        )
                    }
                }
            } else {
                conflictResolutions.clear()
                checkConflicts(
                    files,
                    destinationPath,
                    files.size,
                    conflictResolutions,
                    encryptionAction,
                    callback
                )
            }
        } else {
            checkConflicts(
                files,
                destinationPath,
                index + 1,
                conflictResolutions,
                encryptionAction,
                callback
            )
        }
    }

    fun decompressHandle(
        path: String,
        dialogPath: String = this.config.homeFolder,
        decompress: (destination: String, password: CharArray?) -> Unit
    ) =
        FilePickerDialog(
            this,
            dialogPath,
            pickFile = false,
            showFAB = true,
        ) { destination ->
            ZipFile(path).checkDecompressionCollision(this, destination) { collision ->
                if (collision) {
                    return@checkDecompressionCollision
                }

                val zipFile = ZipFile(path)
                if (zipFile.isEncrypted) {
                    PasswordPromptDialog(
                        this,
                        String.format(
                            getString(R.string.decompress_password_title),
                            path.getFilenameFromPath()
                        )
                    ) { password ->
                        decompress.invoke(destination, password)
                    }
                } else {
                    decompress.invoke(destination, null)
                }
            }
        }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            actionOnPermission = callback
            ActivityCompat.requestPermissions(
                this,
                arrayOf(getPermissionString(permissionId)),
                GENERIC_PERM_HANDLER
            )
        }
    }

    // synchronous return value determines only if we are showing the SAF dialog, callback result tells if the SD permission has been granted
    fun handleSAFDialog(path: String, callback: (success: Boolean) -> Unit): Boolean {
        return if (!packageName.startsWith("com.securefilemanager")) {
            callback(true)
            false
        } else if (isShowingSAFDialog(path)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback(true)
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
            actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }

}
