package com.securefilemanager.app.dialogs

import android.os.Parcelable
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.adapters.FilePickerItemsAdapter
import com.securefilemanager.app.adapters.FilepickerFavoritesAdapter
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.views.Breadcrumbs
import kotlinx.android.synthetic.main.dialog_filepicker.view.*
import java.io.File
import java.util.*

/**
 * The only filepicker constructor with a couple optional parameters
 *
 * @param activity has to be activity to avoid some Theme.AppCompat issues
 * @param currPath initial path of the dialog, defaults to the external storage
 * @param pickFile toggle used to determine if we are picking a file or a folder
 * @param showFAB toggle the displaying of a Floating Action Button for creating new folders
 * @param callback the callback used for returning the selected file/folder
 */
class FilePickerDialog(
    val activity: BaseAbstractActivity,
    var currPath: String = activity.config.homeFolder,
    val pickFile: Boolean = true,
    val showFAB: Boolean = false,
    private val isMovingOperation: Boolean = false,
    private val hideAction: HideAction = HideAction.NONE,
    val finishOnBackPress: Boolean = false,
    val callbackNegative: (() -> Unit)? = null,
    val callback: (pickedPath: String) -> Unit,
) : Breadcrumbs.BreadcrumbsListener {

    private var mFirstUpdate = true
    private var mPrevPath = ""
    private var mScrollStates = HashMap<String, Parcelable>()

    private lateinit var mDialog: AlertDialog
    private var mDialogView = View.inflate(activity, R.layout.dialog_filepicker, null)

    init {
        if (!activity.getDoesFilePathExist(currPath) || isUnhide(hideAction)) {
            currPath = activity.internalStoragePath
        }

        if (!activity.getIsPathDirectory(currPath)) {
            currPath = currPath.getParentPath()
        }

        mDialogView.filepicker_breadcrumbs.apply {
            listener =
                if ((isMovingOperation && activity.isPathOnHidden(currPath)) || isHide(hideAction))
                    null
                else this@FilePickerDialog
            updateFontSize(activity.getTextSize())
        }

        tryUpdateItems()
        setupFavorites()

        val builder = AlertDialog.Builder(activity)
            .setNegativeButton(R.string.cancel, null)
            .setOnKeyListener { _, i, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_UP && i == KeyEvent.KEYCODE_BACK) {
                    val breadcrumbs = mDialogView.filepicker_breadcrumbs
                    if (breadcrumbs.childCount > 1) {
                        breadcrumbs.removeBreadcrumb()
                        currPath = breadcrumbs.getLastItem().path.trimEnd('/')
                        tryUpdateItems()
                    } else {
                        mDialog.dismiss()
                        if (finishOnBackPress) {
                            activity.finish()
                        }
                    }
                }
                true
            }

        if (!pickFile)
            builder.setPositiveButton(R.string.ok, null)

        if (showFAB) {
            mDialogView.filepicker_fab.apply {
                beVisible()
                setOnClickListener { createNewFolder() }
            }
        }

        val secondaryFabBottomMargin =
            activity.resources.getDimension(
                if (showFAB) R.dimen.secondary_fab_bottom_margin else R.dimen.activity_margin
            ).toInt()
        mDialogView.fabs_holder.apply {
            (layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = secondaryFabBottomMargin
        }

        mDialogView.filepicker_favorites_label.text = activity.getString(R.string.favorites)
        mDialogView.filepicker_fab_show_favorites.apply {
            beVisibleIf(context.config.favorites.isNotEmpty() && !isHide(hideAction))
            setOnClickListener {
                if (mDialogView.filepicker_favorites_holder.isVisible()) {
                    hideFavorites()
                } else {
                    showFavorites()
                }
            }
        }

        mDialogView.filepicker_fab_show_hidden.setOnClickListener {
            currPath = activity.hiddenPath
            tryUpdateItems()
        }

        mDialog = builder.create().apply {
            activity.setupDialogStuff(mDialogView, this, getTitle())
        }

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
            callbackNegative?.invoke()
            mDialog.dismiss()
        }

        if (!pickFile) {
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                verifyPath()
            }
        }

        if (isHide(hideAction) && !activity.config.isHideTutorialShowed) {
            TapTargetView.showFor(
                mDialog,
                TapTarget.forView(
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                    activity.getString(R.string.confirm_selection),
                    htmlText(activity.getString(R.string.tutorial_hide_description))
                ).transparentTarget(true)
            )
            activity.config.isHideTutorialShowed = true
        }
    }

    private fun getTitle() = if (pickFile) R.string.select_file else R.string.select_folder

    private fun createNewFolder() {
        CreateNewFolderDialog(activity, currPath) {
            callback(it)
            mDialog.dismiss()
        }
    }

    private fun tryUpdateItems() {
        setHiddenVisibility()
        ensureBackgroundThread {
            getItems(currPath) {
                activity.runOnUiThread {
                    updateItems(it as ArrayList<FileDirItem>)
                }
            }
        }
    }

    private fun updateItems(items: ArrayList<FileDirItem>) {
        if (!containsDirectory(items) && !mFirstUpdate && !pickFile && !showFAB) {
            verifyPath()
            return
        }

        val sortedItems = items.sortedWith(compareBy({ !it.isDirectory }, {
            it.name.toLowerCase(
                Locale.getDefault()
            )
        }))

        val adapter = FilePickerItemsAdapter(activity, sortedItems, mDialogView.filepicker_list) {
            if ((it as FileDirItem).isDirectory) {
                currPath = it.path
                tryUpdateItems()
            } else if (pickFile) {
                currPath = it.path
                verifyPath()
            }
        }

        val layoutManager = mDialogView.filepicker_list.layoutManager as LinearLayoutManager
        mScrollStates[mPrevPath.trimEnd('/')] = layoutManager.onSaveInstanceState()!!

        mDialogView.apply {
            filepicker_list.adapter = adapter
            filepicker_breadcrumbs.setBreadcrumb(currPath)
            filepicker_fastscroller.setViews(filepicker_list) {
                filepicker_fastscroller.updateBubbleText(
                    sortedItems.getOrNull(it)?.getBubbleText(context) ?: ""
                )
            }

            filepicker_list.scheduleLayoutAnimation()
            layoutManager.onRestoreInstanceState(mScrollStates[currPath.trimEnd('/')])
            filepicker_list.onGlobalLayout {
                filepicker_fastscroller.setScrollToY(filepicker_list.computeVerticalScrollOffset())
            }
        }

        mFirstUpdate = false
        mPrevPath = currPath
    }

    private fun verifyPath() {
        val file = File(currPath)
        if ((pickFile && file.isFile) || (!pickFile && file.isDirectory)) {
            sendSuccess()
        }
    }

    private fun sendSuccess() {
        currPath = if (currPath.length == 1) {
            currPath
        } else {
            currPath.trimEnd('/')
        }
        callback(currPath)
        mDialog.dismiss()
    }

    private fun getItems(
        path: String,
        callback: (List<FileDirItem>) -> Unit
    ) {
        val lastModifieds = activity.getFolderLastModifieds(path)
        getRegularItems(path, lastModifieds, callback)
    }

    private fun getRegularItems(
        path: String,
        lastModifieds: HashMap<String, Long>,
        callback: (List<FileDirItem>) -> Unit
    ) {
        val items = ArrayList<FileDirItem>()
        val base = File(path)
        val files = base.listFiles()
        if (files == null) {
            callback(items)
            return
        }

        for (file in files) {
            val curPath = file.absolutePath
            val curName = curPath.getFilenameFromPath()
            val size = file.length()
            var lastModified = lastModifieds.remove(curPath)
            val isDirectory = file.isDirectory
            if (lastModified == null) {
                lastModified = file.lastModified()
            }

            val children = if (isDirectory) file.getDirectChildrenCount() else 0
            items.add(FileDirItem(curPath, curName, isDirectory, children, size, lastModified))
        }
        callback(items)
    }

    private fun setupFavorites() {
        FilepickerFavoritesAdapter(
            activity,
            activity.config.favorites.toList(),
            mDialogView.filepicker_favorites_list,
            isMovingOperation
        ) {
            currPath = it as String
            verifyPath()
        }.apply {
            mDialogView.filepicker_favorites_list.adapter = this
        }
    }

    private fun setHiddenVisibility() {
        mDialogView.filepicker_fab_show_hidden.apply {
            beGoneIf(
                isUnhide(hideAction) ||
                    activity.isPathOnHidden(currPath) ||
                    mDialogView.filepicker_favorites_holder.isVisible
            )
        }
    }

    private fun showFavorites() {
        mDialogView.apply {
            filepicker_favorites_holder.beVisible()
            filepicker_files_holder.beGone()
            filepicker_fab_show_favorites.setImageResource(R.drawable.ic_folder_vector)
        }
        setHiddenVisibility()
    }

    private fun hideFavorites() {
        mDialogView.apply {
            filepicker_favorites_holder.beGone()
            filepicker_files_holder.beVisible()
            filepicker_fab_show_favorites.setImageResource(R.drawable.ic_star_on_vector)
        }
        setHiddenVisibility()
    }

    private fun containsDirectory(items: List<FileDirItem>) = items.any { it.isDirectory }

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(
                activity,
                currPath,
                isMovingOperation = isMovingOperation,
                hideAction = hideAction
            ) {
                currPath = it
                tryUpdateItems()
            }
        } else {
            val item = mDialogView.filepicker_breadcrumbs.getChildAt(id).tag as FileDirItem
            if (currPath != item.path.trimEnd('/')) {
                currPath = item.path
                tryUpdateItems()
            }
        }
    }
}
