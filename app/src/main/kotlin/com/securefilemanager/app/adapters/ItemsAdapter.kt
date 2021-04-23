package com.securefilemanager.app.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.dialogs.*
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.*
import com.securefilemanager.app.interfaces.ItemOperationsListener
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.models.ListItem
import com.securefilemanager.app.models.RadioItem
import com.securefilemanager.app.services.ZipManagerService
import com.securefilemanager.app.services.ZipManagerService.Companion.EXTRA_DESTINATION
import com.securefilemanager.app.services.ZipManagerService.Companion.EXTRA_PASSWORD
import com.securefilemanager.app.services.ZipManagerService.Companion.EXTRA_PATH
import com.securefilemanager.app.views.FastScroller
import com.securefilemanager.app.views.MyRecyclerView
import kotlinx.android.synthetic.main.item_list_file_dir.view.*
import kotlinx.android.synthetic.main.item_list_file_dir.view.item_frame
import kotlinx.android.synthetic.main.item_list_file_dir.view.item_icon
import kotlinx.android.synthetic.main.item_section.view.*
import java.io.File

class ItemsAdapter(
    activity: BaseAbstractActivity,
    var listItems: MutableList<ListItem>,
    private val listener: ItemOperationsListener?,
    recyclerView: MyRecyclerView,
    private val isPickMultipleIntent: Boolean,
    fastScroller: FastScroller,
    private val swipeRefreshLayout: SwipeRefreshLayout,
    itemClick: (Any) -> Unit
) :
    ItemAbstractAdapter(activity, listItems, recyclerView, fastScroller, itemClick) {

    private var currentItemsHash = listItems.hashCode()
    private var textToHighlight = ""

    init {
        setupDragListener()
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        val isPathOnHidden = isPathOnHidden()
        val isPathOnSd = isPathOnSd()
        val selectedFileDirItems = getSelectedFileDirItems()
        menu.apply {
            // visibility
            findItem(R.id.cab_decompress).isVisible =
                selectedFileDirItems.filter { it.path.isZipFile() }.size == selectedFileDirItems.size &&
                    !isPathOnSd
            findItem(R.id.cab_compress).isVisible = !isPathOnSd
            findItem(R.id.cab_confirm_selection).isVisible = isPickMultipleIntent
            findItem(R.id.cab_copy_path).isVisible = isOneItemSelected()
            findItem(R.id.cab_open_with).isVisible = isOneFileSelected()
            findItem(R.id.cab_open_as).isVisible = isOneFileSelected()
            findItem(R.id.cab_set_as).isVisible = isOneFileSelected()
            findItem(R.id.cab_hide).isVisible = !isPathOnHidden
            findItem(R.id.cab_unhide).isVisible = isPathOnHidden
            findItem(R.id.cab_encrypt).isVisible =
                selectedFileDirItems.any { !it.isEncrypted() || it.isDirectory } &&
                    !isPathOnSd
            findItem(R.id.cab_decrypt).isVisible =
                selectedFileDirItems.any { it.isEncrypted() || it.isDirectory } &&
                    !isPathOnSd
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_confirm_selection -> confirmSelection()
            R.id.cab_rename -> displayRenameDialog()
            R.id.cab_properties -> showProperties()
            R.id.cab_share -> beforeShareFiles()
            R.id.cab_hide -> fileHide(HideAction.HIDE)
            R.id.cab_unhide -> fileHide(HideAction.UNHIDE)
            R.id.cab_encrypt -> fileEncrypt(EncryptionAction.ENCRYPT)
            R.id.cab_decrypt -> fileEncrypt(EncryptionAction.DECRYPT)
            R.id.cab_copy_path -> copyPath()
            R.id.cab_set_as -> setAs()
            R.id.cab_open_with -> openWith()
            R.id.cab_open_as -> openAs()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> copyMoveTo(false)
            R.id.cab_compress -> compressSelection()
            R.id.cab_decompress -> decompressSelection()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = listItems.filter { !it.isSectionTitle }.size

    override fun getIsItemSelectable(position: Int) = !listItems[position].isSectionTitle

    override fun getItemSelectionKey(position: Int) =
        listItems.getOrNull(position)?.path?.hashCode()

    override fun getItemKeyPosition(key: Int) = listItems.indexOfFirst { it.path.hashCode() == key }

    override fun onActionModeCreated() {
        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = false
    }

    override fun onActionModeDestroyed() {
        swipeRefreshLayout.isEnabled = true
    }

    override fun getItemViewType(position: Int): Int {
        return if (listItems[position].isSectionTitle) {
            TYPE_SECTION
        } else {
            TYPE_FILE_DIR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout =
            if (viewType == TYPE_SECTION) R.layout.item_section else R.layout.item_list_file_dir
        return createViewHolder(layout, parent)
    }

    override fun onBindViewHolder(holder: RecyclerViewAdapter.ViewHolder, position: Int) {
        val fileDirItem = listItems[position]
        holder.bindView(
            fileDirItem,
            true,
            !fileDirItem.isSectionTitle
        ) { itemView, _ ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    private fun isOneFileSelected() =
        isOneItemSelected() && getItemWithKey(selectedKeys.first())?.isDirectory == false

    private fun getItemWithKey(key: Int): FileDirItem? =
        listItems.firstOrNull { it.path.hashCode() == key }

    private fun confirmSelection() {
        if (selectedKeys.isNotEmpty()) {
            val paths =
                getSelectedFileDirItems().asSequence()
                    .filter { !it.isDirectory }
                    .map { it.path }
                    .toMutableList() as ArrayList<String>
            if (paths.isEmpty()) {
                finishActMode()
            } else {
                listener?.selectedPaths(paths)
            }
        }
    }

    private fun displayRenameDialog() {
        val fileDirItems = getSelectedFileDirItems()
        val paths = fileDirItems.asSequence().map { it.path }.toMutableList() as ArrayList<String>
        when (paths.size) {
            1 -> {
                val oldPath = paths.first()
                RenameItemDialog(activity, oldPath) {
                    activity.config.moveFavorite(oldPath, it)
                    activity.runOnUiThread {
                        listener?.refreshItems()
                        finishActMode()
                    }
                }
            }
            else -> RenameItemsDialog(activity, paths) {
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }

    private fun showProperties() {
        if (selectedKeys.size <= 1) {
            PropertiesDialog(activity, getFirstSelectedItemPath())
        } else {
            val paths = getSelectedFileDirItems().map { it.path }
            PropertiesDialog(activity, paths)
        }
    }

    private fun beforeShareFiles() =
        if (isPathOnHidden()) {
            askConfirmShareFromHidden()
        } else {
            shareFiles()
        }

    private fun askConfirmShareFromHidden() {
        val selectionSize = selectedKeys.size
        val items =
            resources.getQuantityString(R.plurals.items, selectionSize, selectionSize)
        val question =
            String.format(resources.getString(R.string.share_hidden_file_confirmation), items)
        ConfirmationDialog(activity, question) {
            shareFiles()
        }
    }


    private fun shareFiles() {
        val selectedItems = getSelectedFileDirItems()
        val paths = ArrayList<String>(selectedItems.size)
        selectedItems.forEach {
            addFileUris(it.path, paths)
        }
        activity.sharePaths(paths)
    }

    private fun fileHide(hideAction: HideAction) {
        val (files, source) = getSelected()
        FilePickerDialog(
            activity,
            activity.hiddenPath,
            pickFile = false,
            showFAB = true,
            hideAction = hideAction
        ) {
            activity.copyMoveFilesTo(
                files,
                source,
                it,
                isCopyOperation = true,
                copyPhotoVideoOnly = false,
                hideAction = hideAction
            ) { _: String, copiedAll: Boolean ->
                if (copiedAll) {
                    listener?.deleteFiles(files)
                }
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }

    private fun fileEncrypt(encryptionAction: EncryptionAction) {
        val (files, source) = getSelected()
        activity.copyMoveFilesTo(
            files,
            source,
            source,
            isCopyOperation = true,
            copyPhotoVideoOnly = false,
            encryptionAction = encryptionAction
        ) { _: String, _: Boolean ->
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun addFileUris(path: String, paths: ArrayList<String>) {
        if (activity.getIsPathDirectory(path)) {
            File(path).listFiles()
                ?.forEach {
                    addFileUris(it.absolutePath, paths)
                }
        } else {
            paths.add(path)
        }
    }

    private fun copyPath() {
        val clip =
            ClipData.newPlainText(activity.getString(R.string.app_name), getFirstSelectedItemPath())
        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
            clip
        )
        finishActMode()
        activity.toast(R.string.path_copied)
    }

    private fun setAs() {
        activity.setAs(getFirstSelectedItemPath())
    }

    private fun openWith() {
        activity.tryOpenPathIntent(getFirstSelectedItemPath(), true)
    }

    private fun openAs() {
        val res = activity.resources
        val items = arrayListOf(
            RadioItem(OPEN_AS_TEXT, res.getString(R.string.text_file)),
            RadioItem(OPEN_AS_IMAGE, res.getString(R.string.image_file)),
            RadioItem(OPEN_AS_AUDIO, res.getString(R.string.audio_file)),
            RadioItem(OPEN_AS_VIDEO, res.getString(R.string.video_file)),
            RadioItem(OPEN_AS_OTHER, res.getString(R.string.other_file))
        )

        RadioGroupDialog(activity, items) {
            activity.tryOpenPathIntent(getFirstSelectedItemPath(), false, it as Int)
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val (files, source) = getSelected()
        FilePickerDialog(
            activity,
            source,
            pickFile = false,
            showFAB = true,
            isMovingOperation = !isCopyOperation
        ) {
            activity.copyMoveFilesTo(
                files,
                source,
                it,
                isCopyOperation,
                false
            ) { _: String, _: Boolean ->
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }

    private fun getSelected(): Pair<ArrayList<FileDirItem>, String> {
        val files = getSelectedFileDirItems()
        val firstFile = files[0]
        val source = if (firstFile.isDirectory) firstFile.path else firstFile.getParentPath()
        return Pair(files, source)
    }

    private fun compressSelection() {
        val firstPath = getFirstSelectedItemPath()
        CompressAsDialog(activity, firstPath) { destination, password ->
            val paths = getSelectedFileDirItems().map { it.path }
            val startIntent = Intent(activity, ZipManagerService::class.java).apply {
                action = ZipManagerService.ACTION_COMPRESSION
                putStringArrayListExtra(EXTRA_PATH, ArrayList(paths))
                putExtra(EXTRA_DESTINATION, destination)
                putExtra(EXTRA_PASSWORD, password)
            }
            activity.startService(startIntent)
            activity.runOnUiThread {
                finishActMode()
            }
        }
    }

    private fun decompressSelection() {
        val firstPath = getFirstSelectedItemPath()

        val path = getSelectedFileDirItems().asSequence()
            .map { it.path }
            .find { it.isZipFile() }
            ?: return // this should not happen

        this.activity.decompressHandle(path, firstPath) { destination, password ->
            this.activity.decompressZip(path, destination, password)
            activity.runOnUiThread {
                finishActMode()
            }
        }
    }

    private fun askConfirmDelete() {
        val selectionSize = selectedKeys.size
        val items =
            resources.getQuantityString(R.plurals.delete_items, selectionSize, selectionSize)
        val question = String.format(resources.getString(R.string.deletion_confirmation), items)
        ConfirmationDialog(activity, question) {
            deleteFiles()
        }
    }

    private fun deleteFiles() {
        if (selectedKeys.isEmpty()) {
            return
        }

        activity.handleSAFDialog(getFirstSelectedItemPath()) {
            if (!it) {
                return@handleSAFDialog
            }

            val files = ArrayList<FileDirItem>(selectedKeys.size)
            val positions = ArrayList<Int>()
            selectedKeys.forEach { selectedKey ->
                activity.config.removeFavorite(getItemWithKey(selectedKey)?.path ?: "")
                val position = listItems.indexOfFirst { it.path.hashCode() == selectedKey }
                if (position != -1) {
                    positions.add(position)
                    files.add(listItems[position])
                }
            }

            positions.sortDescending()
            removeSelectedItems(positions)
            listener?.deleteFiles(files)
            positions.forEach {
                listItems.removeAt(it)
            }
        }
    }

    private fun getFirstSelectedItemPath() = getSelectedFileDirItems().first().path

    private fun getSelectedFileDirItems() =
        listItems.filter { selectedKeys.contains(it.path.hashCode()) } as ArrayList<FileDirItem>

    fun updateItems(newItems: ArrayList<ListItem>, highlightText: String = "") {
        if (newItems.hashCode() != currentItemsHash) {
            currentItemsHash = newItems.hashCode()
            textToHighlight = highlightText
            listItems = newItems.clone() as ArrayList<ListItem>
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
        fastScroller?.measureRecyclerView()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            val icon = holder.itemView.item_icon
            if (icon != null) {
                Glide.with(activity).clear(icon)
            }
        }
    }

    override fun setupView(view: View, fileDirItem: FileDirItem) {}

    private fun setupView(view: View, fileDirItem: ListItem) {
        val isSelected = selectedKeys.contains(fileDirItem.path.hashCode())
        view.apply {
            if (fileDirItem.isSectionTitle) {
                item_section.text =
                    if (textToHighlight.isEmpty()) fileDirItem.mName else fileDirItem.mName.highlightTextPart(
                        textToHighlight,
                        adjustedPrimaryColor
                    )
                item_section.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            } else {
                item_frame.isSelected = isSelected
                val fileName = fileDirItem.name
                item_name.text =
                    if (textToHighlight.isEmpty()) fileName else fileName.highlightTextPart(
                        textToHighlight,
                        adjustedPrimaryColor
                    )
                item_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                item_details.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                item_date.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerFontSize)

                super.setFileIcon(item_icon, fileDirItem)

                if (fileDirItem.isDirectory) {
                    item_details.text = getChildrenCnt(fileDirItem)
                    item_date.beGone()
                } else {
                    item_details.text = fileDirItem.size.formatSize()
                    item_date.beVisible()
                    item_date.text =
                        fileDirItem.modified.formatDate(activity, dateFormat, timeFormat)
                }
            }
        }
    }

    private fun isPathOnHidden(): Boolean {
        return getSelectedFileDirItems().any { activity.isPathOnHidden(it.path) }
    }

    private fun isPathOnSd(): Boolean {
        return getSelectedFileDirItems().any { activity.isPathOnSD(it.path) }
    }

    fun updateChildCount(path: String, count: Int) {
        val position = getItemKeyPosition(path.hashCode())
        val item = listItems.getOrNull(position) ?: return
        item.children = count
        notifyItemChanged(position, Unit)
    }

    companion object {
        private const val TYPE_FILE_DIR = 1
        private const val TYPE_SECTION = 2
    }
}
