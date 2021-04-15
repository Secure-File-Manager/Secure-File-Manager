package com.securefilemanager.app.fragments

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.activities.MainActivity
import com.securefilemanager.app.adapters.ItemsAdapter
import com.securefilemanager.app.dialogs.CreateNewItemDialog
import com.securefilemanager.app.dialogs.StoragePickerDialog
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.interfaces.ItemOperationsListener
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.models.ListItem
import com.securefilemanager.app.services.ZipManagerService
import com.securefilemanager.app.views.Breadcrumbs
import com.securefilemanager.app.views.MyLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_items.view.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ItemsFragment : Fragment(), ItemOperationsListener, Breadcrumbs.BreadcrumbsListener {
    var currentPath = ""
    var isGetContentIntent = false
    var isGetRingtonePicker = false
    var isPickMultipleIntent = false

    private var isFirstResume = true
    private var skipItemUpdating = false
    private var isSearchOpen = false
    private var lastSearchedText = ""
    private var scrollStates = HashMap<String, Parcelable>()
    private var storedDateFormat = ""
    private var storedTimeFormat = ""

    private var storedItems = ArrayList<ListItem>()
    private var currentMediaFile: File? = null

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                ZipManagerService.ACTION_LOCAL_COMPLETE -> refreshItems()
            }
        }
    }

    lateinit var mView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mView = inflater.inflate(R.layout.fragment_items, container, false)!!
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hasDeviceCamera = context?.hasDeviceCamera()!!
        mView.apply {
            items_swipe_refresh.setOnRefreshListener { refreshItems() }
            items_fab.setOnClickListener { createNewItem() }
            hide_fab.setOnClickListener { openPath(requireActivity().hiddenPath, true) }
            camera_fab.setOnClickListener { takeVideo() }
            photo_fab.setOnClickListener { takePicture() }
            camera_fab.isVisible = hasDeviceCamera
            photo_fab.isVisible = hasDeviceCamera
            breadcrumbs.listener = this@ItemsFragment
            breadcrumbs.updateFontSize(requireContext().getTextSize())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager
            .getInstance(this.requireContext())
            .registerReceiver(
                mMessageReceiver,
                IntentFilter(ZipManagerService.ACTION_LOCAL_COMPLETE)
            )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH, currentPath)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            currentPath = savedInstanceState.getString(PATH)!!
            storedItems.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        if (storedDateFormat != requireContext().config.dateFormat || storedTimeFormat != requireContext().getTimeFormat()) {
            getRecyclerAdapter()?.updateDateTimeFormat()
        }
        if (!isFirstResume) {
            refreshItems()
        }
        isFirstResume = false
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        LocalBroadcastManager
            .getInstance(this.requireContext())
            .unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VIDEO_REQUEST_CODE || requestCode == IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val path = currentMediaFile?.path
                if (path != null && activity?.isPathOnHidden(path)!!) {
                    activity?.rescanPaths(arrayListOf(path))
                }
            } else {
                currentMediaFile?.delete()
            }
            currentMediaFile = null
        }
    }

    private fun storeStateVariables() {
        requireContext().config.apply {
            storedDateFormat = dateFormat
            storedTimeFormat = context.getTimeFormat()
        }
    }

    fun openPath(path: String, forceRefresh: Boolean = false) {
        if (!isAdded) {
            return
        }

        val context = this.requireContext()
        val isOnSdCard = context.isPathOnSD(path)
        this.mView.hide_fab.beGoneIf(path.startsWith(requireActivity().hiddenPath))
        this.mView.camera_fab.beGoneIf(isOnSdCard)
        this.mView.photo_fab.beGoneIf(isOnSdCard)

        var realPath = path.trimEnd('/')
        if (realPath.isEmpty()) {
            realPath = "/"
        }

        scrollStates[currentPath] = getScrollState()!!
        currentPath = realPath
        getItems(currentPath) { originalPath, listItems ->
            if (currentPath != originalPath || !isAdded) {
                return@getItems
            }

            FileDirItem.sorting = context.config.getFolderSorting(currentPath)
            listItems.sort()
            activity?.runOnUiThread {
                activity?.invalidateOptionsMenu()
                addItems(listItems, forceRefresh)
            }
        }
    }

    private fun addItems(items: ArrayList<ListItem>, forceRefresh: Boolean = false) {
        skipItemUpdating = false
        mView.apply {
            activity?.runOnUiThread {
                items_swipe_refresh?.isRefreshing = false
                mView.breadcrumbs.setBreadcrumb(currentPath)

                items_empty.apply {
                    beVisibleIf(items.size == 0)
                }

                if (!forceRefresh && items.hashCode() == storedItems.hashCode()) {
                    return@runOnUiThread
                }

                storedItems = items
                ItemsAdapter(
                    activity as BaseAbstractActivity,
                    storedItems,
                    this@ItemsFragment,
                    items_list,
                    isPickMultipleIntent,
                    items_fastscroller
                ) {
                    itemClicked(it as FileDirItem)
                }.apply {
                    items_list.adapter = this
                }

                items_list.scheduleLayoutAnimation()
                items_fastscroller.setViews(items_list, mView.items_swipe_refresh) {
                    val listItem = getRecyclerAdapter()?.listItems?.getOrNull(it)
                    items_fastscroller.updateBubbleText(
                        listItem?.getBubbleText(
                            context,
                            storedDateFormat,
                            storedTimeFormat
                        ) ?: ""
                    )
                }

                getRecyclerLayoutManager().onRestoreInstanceState(scrollStates[currentPath])
                items_list.onGlobalLayout {
                    items_fastscroller.setScrollToY(items_list.computeVerticalScrollOffset())
                }
            }
        }
    }

    private fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    private fun getRecyclerLayoutManager() =
        (mView.items_list.layoutManager as MyLinearLayoutManager)

    private fun getItems(
        path: String,
        callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit
    ) {
        skipItemUpdating = false
        ensureBackgroundThread {
            if (activity?.isDestroyed == false && activity?.isFinishing == false) {
                getRegularItemsOf(path, callback)
            }
        }
    }

    private fun getRegularItemsOf(
        path: String,
        callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit
    ) {
        val items = ArrayList<ListItem>()
        val files = File(path).listFiles()?.filterNotNull()
        if (context == null) {
            callback(path, items)
            return
        }

        val lastModifieds = getFolderLastModifieds(path)
        val isSortingBySize =
            requireContext().config.getFolderSorting(currentPath) and SORT_BY_SIZE != 0
        if (files != null) {
            for (file in files) {
                val fileDirItem = getFileDirItemFromFile(file, isSortingBySize, lastModifieds)
                if (fileDirItem != null) {
                    items.add(fileDirItem)
                }
            }
        }

        callback(path, items)
    }

    private fun getFileDirItemFromFile(
        file: File,
        isSortingBySize: Boolean,
        lastModifieds: HashMap<String, Long> = HashMap<String, Long>()
    ): ListItem? {
        val curPath = file.absolutePath
        val curName = file.name
        val isDirectory = file.isDirectory
        val children = if (isDirectory) file.getDirectChildrenCount() else 0
        val size = if (isDirectory) {
            if (isSortingBySize) {
                file.getProperSize()
            } else {
                0L
            }
        } else {
            file.length()
        }

        var lastModified = lastModifieds.remove(curPath)
        if (lastModified == null) {
            lastModified = file.lastModified()
        }

        return ListItem(curPath, curName, isDirectory, children, size, lastModified, false)
    }

    private fun getFolderLastModifieds(folder: String): HashMap<String, Long> {
        val lastModifieds = HashMap<String, Long>()
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val uri = MediaStore.Files.getContentUri("external")
        val selection =
            "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} IS NOT NULL" // avoid selecting folders
        val selectionArgs = arrayOf("$folder/%", "$folder/%/%")

        val cursor =
            requireContext().contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        val lastModified =
                            cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000
                        if (lastModified != 0L) {
                            val name = cursor.getStringValue(MediaStore.Images.Media.DISPLAY_NAME)
                            lastModifieds["$folder/$name"] = lastModified
                        }
                    } catch (e: Exception) {
                    }
                } while (cursor.moveToNext())
            }
        }

        return lastModifieds
    }

    private fun itemClicked(item: FileDirItem) {
        if (item.isDirectory) {
            (activity as? MainActivity)?.apply {
                skipItemUpdating = isSearchOpen
                openedDirectory()
            }
            openPath(item.path)
        } else {
            val path = item.path
            if (isGetContentIntent) {
                (activity as MainActivity).pickedPath(path)
            } else if (isGetRingtonePicker) {
                if (path.isAudioFast()) {
                    (activity as MainActivity).pickedRingtone(path)
                } else {
                    activity?.toast(R.string.select_audio_file)
                }
            } else {
                requireActivity().tryOpenPathIntent(path, false)
            }
        }
    }

    fun searchQueryChanged(text: String) {
        val searchText = text.trim()
        lastSearchedText = searchText
        ensureBackgroundThread {
            if (context == null) {
                return@ensureBackgroundThread
            }

            when {
                searchText.isEmpty() -> activity?.runOnUiThread {
                    mView.apply {
                        items_list.beVisible()
                        getRecyclerAdapter()?.updateItems(storedItems)
                        items_placeholder.beGone()
                        items_placeholder_2.beGone()
                    }
                }
                searchText.length == 1 -> activity?.runOnUiThread {
                    mView.apply {
                        items_list.beGone()
                        items_placeholder.beVisible()
                        items_placeholder_2.beVisible()
                    }
                }
                else -> {
                    val files = searchFiles(searchText, currentPath).apply {
                        sortBy { it.getParentPath() }
                    }

                    if (lastSearchedText != searchText) {
                        return@ensureBackgroundThread
                    }

                    val listItems = ArrayList<ListItem>()

                    var previousParent = ""
                    files.forEach {
                        val parent = it.mPath.getParentPath()
                        if (parent != previousParent && context != null) {
                            listItems.add(
                                ListItem(
                                    "",
                                    requireContext().humanizePath(parent),
                                    false,
                                    0,
                                    0,
                                    0,
                                    true
                                )
                            )
                            previousParent = parent
                        }
                        listItems.add(it)
                    }

                    activity?.runOnUiThread {
                        getRecyclerAdapter()?.updateItems(listItems, text)
                        mView.apply {
                            items_list.beVisibleIf(listItems.isNotEmpty())
                            items_placeholder.beVisibleIf(listItems.isEmpty())
                            items_placeholder_2.beGone()
                        }
                    }
                }
            }
        }
    }

    private fun searchFiles(text: String, path: String): ArrayList<ListItem> {
        val files = ArrayList<ListItem>()
        if (context == null) {
            return files
        }

        val sorting = requireContext().config.getFolderSorting(path)
        FileDirItem.sorting = requireContext().config.getFolderSorting(currentPath)
        val isSortingBySize = sorting and SORT_BY_SIZE != 0
        File(path).listFiles()?.forEach {
            if (it.isDirectory) {
                files.addAll(searchFiles(text, it.absolutePath))
            } else {
                if (it.name.contains(text, true)) {
                    val fileDirItem = getFileDirItemFromFile(it, isSortingBySize)
                    if (fileDirItem != null) {
                        files.add(fileDirItem)
                    }
                }
            }
        }
        files.sort()
        return files
    }

    fun searchOpened() {
        isSearchOpen = true
        lastSearchedText = ""
    }

    fun searchClosed() {
        isSearchOpen = false
        if (!skipItemUpdating) {
            getRecyclerAdapter()?.updateItems(storedItems)
        }
        skipItemUpdating = false
        lastSearchedText = ""
    }

    private fun createNewItem() {
        CreateNewItemDialog(activity as BaseAbstractActivity, currentPath) {
            if (it) {
                refreshItems()
            } else {
                activity?.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun takeMedia(action: String, extension: String, requestCode: Int) {
        Intent(action).also { takePictureIntent ->
            takePictureIntent.resolveActivity(context?.packageManager!!)?.also {
                createMediaFile(currentPath, extension).also {
                    currentMediaFile = it
                    val mediaURI: Uri = this.requireContext().getUriForFile(it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaURI)
                    startActivityForResult(takePictureIntent, requestCode)
                }
            }
        }
    }

    private fun takeVideo() {
        takeMedia(MediaStore.ACTION_VIDEO_CAPTURE, ".mp4", VIDEO_REQUEST_CODE)
    }

    private fun takePicture() {
        takeMedia(MediaStore.ACTION_IMAGE_CAPTURE, ".jpg", IMAGE_REQUEST_CODE)
    }

    private fun getRecyclerAdapter() = mView.items_list.adapter as? ItemsAdapter

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(activity as BaseAbstractActivity, currentPath) {
                getRecyclerAdapter()?.finishActMode()
                openPath(it)
            }
        } else {
            val item = mView.breadcrumbs.getChildAt(id).tag as FileDirItem
            openPath(item.path)
        }
    }

    override fun refreshItems() {
        openPath(currentPath)
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {
        val hasFolder = files.any { it.isDirectory }
        val firstPath = files.firstOrNull()?.path
        if (firstPath == null || firstPath.isEmpty() || context == null) {
            return
        }


        (activity as BaseAbstractActivity).deleteFiles(files, hasFolder) {
            if (!it) {
                requireActivity().runOnUiThread {
                    requireActivity().toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    override fun selectedPaths(paths: ArrayList<String>) {
        (activity as MainActivity).pickedPaths(paths)
    }
}
