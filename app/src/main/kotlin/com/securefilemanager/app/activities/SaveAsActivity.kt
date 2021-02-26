package com.securefilemanager.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.securefilemanager.app.R
import com.securefilemanager.app.dialogs.FilePickerDialog
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.ensureBackgroundThread
import java.io.File
import java.util.*

class SaveAsActivity : BaseAbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_save_as)

        if (intent.extras?.containsKey(Intent.EXTRA_STREAM) != true) {
            this.error()
            return
        }

        when (intent?.action) {
            Intent.ACTION_SEND -> handleSave(false)
            Intent.ACTION_SEND_MULTIPLE -> handleSave(true)
            else -> error()
        }
    }

    private fun handleSave(isMultiple: Boolean) {
        FilePickerDialog(
            this,
            pickFile = false,
            showFAB = true,
            finishOnBackPress = true,
            callbackNegative = { finish() }
        ) { destination ->
            handleSAFDialog(destination) {
                if (!it) {
                    return@handleSAFDialog
                }

                this.toast(R.string.saving)
                ensureBackgroundThread {
                    try {
                        if (!this.getDoesFilePathExist(destination)) {
                            if (this.needsStupidWritePermissions(destination)) {
                                val document = this.getDocumentFile(destination)
                                document!!.createDirectory(destination.getFilenameFromPath())
                            } else {
                                File(destination).mkdirs()
                            }
                        }

                        val sources =
                            if (isMultiple)
                                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                            else
                                arrayListOf(intent.getParcelableExtra(Intent.EXTRA_STREAM))


                        val destinationPaths = ArrayList<String>()
                        for (source in sources!!) {
                            val mimeType = source!!.toString().getMimeType()
                            val inputStream = contentResolver.openInputStream(source)
                            val filename = source.toString().getFilenameFromPath()

                            val destinationPath = "$destination/$filename"
                            destinationPaths.add(destinationPath)
                            val outputStream =
                                this.getFileOutputStreamSync(destinationPath, mimeType, null)!!
                            inputStream!!.copyTo(outputStream)
                        }

                        this.rescanPaths(destinationPaths)
                        this.toast(R.string.file_saved)
                        this.finish()
                    } catch (e: Exception) {
                        this.showErrorToast(e)
                        this.finish()
                    }
                }
            }
        }
    }

    private fun error() {
        this.toast(R.string.unknown_error_occurred)
        this.finish()
    }

}
