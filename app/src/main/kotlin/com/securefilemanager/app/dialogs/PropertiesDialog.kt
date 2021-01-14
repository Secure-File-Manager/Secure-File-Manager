package com.securefilemanager.app.dialogs

import android.app.Activity
import android.content.res.Resources
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.ensureBackgroundThread
import com.securefilemanager.app.helpers.sumByInt
import com.securefilemanager.app.helpers.sumByLong
import com.securefilemanager.app.models.FileDirItem
import kotlinx.android.synthetic.main.dialog_properties.view.*
import kotlinx.android.synthetic.main.property_item.view.*
import java.io.File
import java.util.*

class PropertiesDialog() {
    private lateinit var mInflater: LayoutInflater
    private lateinit var mPropertyView: ViewGroup
    private lateinit var mResources: Resources
    private lateinit var mActivity: Activity

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     */
    constructor(activity: Activity, path: String) : this() {
        if (!activity.getDoesFilePathExist(path)) {
            activity.toast(
                String.format(
                    activity.getString(R.string.source_file_doesnt_exist),
                    path
                )
            )
            return
        }

        mActivity = activity
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = View.inflate(activity, R.layout.dialog_properties, null)
        mPropertyView = view.properties_holder!!

        val fileDirItem =
            FileDirItem(path, path.getFilenameFromPath(), activity.getIsPathDirectory(path))
        addProperty(R.string.name, fileDirItem.name)
        addProperty(R.string.path, fileDirItem.getParentPath())
        addProperty(R.string.size, "…", R.id.properties_size)

        ensureBackgroundThread {
            val fileCount = fileDirItem.getProperFileCount()
            val size = fileDirItem.getProperSize().formatSize()
            activity.runOnUiThread {
                view.findViewById<TextView>(R.id.properties_size).property_value.text = size

                if (fileDirItem.isDirectory) {
                    view.findViewById<TextView>(R.id.properties_file_count).property_value.text =
                        fileCount.toString()
                }
            }

            if (!fileDirItem.isDirectory) {
                val projection = arrayOf(MediaStore.Images.Media.DATE_MODIFIED)
                val uri = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                val selectionArgs = arrayOf(path)
                val cursor =
                    activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        val dateModified =
                            cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L
                        updateLastModified(activity, view, dateModified)
                    } else {
                        updateLastModified(activity, view, fileDirItem.getLastModified())
                    }
                }

                val exif = ExifInterface(fileDirItem.path)

                val latLon = FloatArray(2)
                if (exif.getLatLong(latLon)) {
                    activity.runOnUiThread {
                        addProperty(R.string.gps_coordinates, "${latLon[0]}, ${latLon[1]}")
                    }
                }

                val altitude = exif.getAltitude(0.0)
                if (altitude != 0.0) {
                    activity.runOnUiThread {
                        addProperty(R.string.altitude, "${altitude}m")
                    }
                }
            }
        }

        when {
            fileDirItem.isDirectory -> {
                addProperty(
                    R.string.direct_children_count,
                    fileDirItem.getDirectChildrenCount().toString()
                )
                addProperty(R.string.files_count, "…", R.id.properties_file_count)
            }
            fileDirItem.path.isImageSlow() -> {
                fileDirItem.getResolution(activity)
                    ?.let { addProperty(R.string.resolution, it.formatAsResolution()) }
            }
            fileDirItem.path.isAudioSlow() -> {
                fileDirItem.getDuration()?.let { addProperty(R.string.duration, it) }
                fileDirItem.getSongTitle()?.let { addProperty(R.string.song_title, it) }
                fileDirItem.getArtist()?.let { addProperty(R.string.artist, it) }
                fileDirItem.getAlbum()?.let { addProperty(R.string.album, it) }
            }
            fileDirItem.path.isVideoSlow() -> {
                fileDirItem.getDuration()?.let { addProperty(R.string.duration, it) }
                fileDirItem.getResolution(activity)
                    ?.let { addProperty(R.string.resolution, it.formatAsResolution()) }
                fileDirItem.getArtist()?.let { addProperty(R.string.artist, it) }
                fileDirItem.getAlbum()?.let { addProperty(R.string.album, it) }
            }
        }

        val isDirectory = fileDirItem.isDirectory
        if (isDirectory) {
            addProperty(
                R.string.last_modified,
                fileDirItem.getLastModified().formatDate(activity)
            )
        } else {
            addProperty(R.string.last_modified, "…", R.id.properties_last_modified)
            try {
                addExifProperties(path, activity)
            } catch (e: Exception) {
                activity.showErrorToast(e)
                return
            }
        }

        AlertDialog.Builder(activity).apply {
            setPositiveButton(R.string.ok, null)
            if (!isDirectory) {
                setNeutralButton(R.string.checksum, null)
            }
            create().apply {
                activity.setupDialogStuff(view, this, R.string.properties) {
                    val button = getButton(AlertDialog.BUTTON_NEUTRAL)
                    button.setOnClickListener {
                        button.beGone()
                        calculateChecksum(activity, view, path)
                    }
                }
            }
        }
    }

    private fun updateLastModified(activity: Activity, view: View, timestamp: Long) {
        activity.runOnUiThread {
            view.findViewById<TextView>(R.id.properties_last_modified).property_value.text =
                timestamp.formatDate(activity)
        }
    }

    private fun calculateChecksum(activity: Activity, view: View, path: String) {
        val file = File(path)
        addProperty(R.string.md5, "…", R.id.properties_md5)
        addProperty(R.string.sha1, "…", R.id.properties_sha1)
        addProperty(R.string.sha256, "…", R.id.properties_sha256)
        addProperty(R.string.sha512, "…", R.id.properties_sha512)
        ensureBackgroundThread {
            val md5 = file.md5()
            activity.runOnUiThread {
                view.findViewById<TextView>(R.id.properties_md5).property_value.text = md5
            }
        }
        ensureBackgroundThread {
            val sha1 = file.sha1()
            activity.runOnUiThread {
                view.findViewById<TextView>(R.id.properties_sha1).property_value.text = sha1
            }
        }
        ensureBackgroundThread {
            val sha256 = file.sha256()
            activity.runOnUiThread {
                view.findViewById<TextView>(R.id.properties_sha256).property_value.text = sha256
            }
        }
        ensureBackgroundThread {
            val sha512 = file.sha512()
            activity.runOnUiThread {
                view.findViewById<TextView>(R.id.properties_sha512).property_value.text = sha512
            }
        }
    }

    /**
     * A File Properties dialog constructor with an optional parameter, usable at multiple items selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param paths the file paths
     */
    constructor(
        activity: Activity,
        paths: List<String>
    ) : this() {
        mActivity = activity
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = View.inflate(activity, R.layout.dialog_properties, null)
        mPropertyView = view.properties_holder

        val fileDirItems = ArrayList<FileDirItem>(paths.size)
        paths.forEach {
            val fileDirItem =
                FileDirItem(it, it.getFilenameFromPath(), activity.getIsPathDirectory(it))
            fileDirItems.add(fileDirItem)
        }

        val isSameParent = isSameParent(fileDirItems)

        addProperty(R.string.items_selected, paths.size.toString())
        if (isSameParent) {
            addProperty(R.string.path, fileDirItems[0].getParentPath())
        }

        addProperty(R.string.size, "…", R.id.properties_size)
        addProperty(R.string.files_count, "…", R.id.properties_file_count)

        ensureBackgroundThread {
            val fileCount =
                fileDirItems.sumByInt { it.getProperFileCount() }
            val size =
                fileDirItems.sumByLong { it.getProperSize() }.formatSize()
            activity.runOnUiThread {
                view.findViewById<TextView>(R.id.properties_size).property_value.text = size
                view.findViewById<TextView>(R.id.properties_file_count).property_value.text =
                    fileCount.toString()
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.properties)
            }
    }

    private fun addExifProperties(path: String, activity: Activity) {
        val exif = ExifInterface(path)

        val dateTaken = exif.getExifDateTaken(activity)
        if (dateTaken.isNotEmpty()) {
            addProperty(R.string.date_taken, dateTaken)
        }

        val cameraModel = exif.getExifCameraModel()
        if (cameraModel.isNotEmpty()) {
            addProperty(R.string.camera, cameraModel)
        }

        val exifString = exif.getExifProperties()
        if (exifString.isNotEmpty()) {
            addProperty(R.string.exif, exifString)
        }
    }

    private fun isSameParent(fileDirItems: List<FileDirItem>): Boolean {
        var parent = fileDirItems[0].getParentPath()
        for (file in fileDirItems) {
            val curParent = file.getParentPath()
            if (curParent != parent) {
                return false
            }

            parent = curParent
        }
        return true
    }

    private fun addProperty(labelId: Int, value: String?, viewId: Int = 0) {
        if (value == null)
            return

        mInflater.inflate(R.layout.property_item, mPropertyView, false).apply {
            property_label.text = mResources.getString(labelId)
            property_value.text = value
            mPropertyView.properties_holder.addView(this)

            property_value.setOnLongClickListener {
                mActivity.copyToClipboard(property_value.value)
                true
            }

            if (viewId != 0) {
                id = viewId
            }
        }
    }
}
