package com.securefilemanager.app.adapters

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.formatSize
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.views.MyRecyclerView
import kotlinx.android.synthetic.main.filepicker_list_item.view.*

class FilePickerItemsAdapter(
    activity: BaseAbstractActivity,
    fileDirItems: List<FileDirItem>,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : ItemAbstractAdapter(activity, fileDirItems, recyclerView, null, itemClick) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        createViewHolder(R.layout.filepicker_list_item, parent)

    override fun setupView(view: View, fileDirItem: FileDirItem) {
        view.apply {
            list_item_name.text = fileDirItem.name
            list_item_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            list_item_details.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            super.setFileIcon(list_item_icon, fileDirItem)

            if (fileDirItem.isDirectory) {
                list_item_details.text = getChildrenCnt(fileDirItem)
            } else {
                list_item_details.text = fileDirItem.size.formatSize()
            }
        }
    }

}
