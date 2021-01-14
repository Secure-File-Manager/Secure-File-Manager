package com.securefilemanager.app.adapters

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.models.ListItem
import com.securefilemanager.app.views.MyRecyclerView
import kotlinx.android.synthetic.main.item_list_file_dir.view.*

class DecompressItemsAdapter(
    activity: BaseAbstractActivity,
    listItems: MutableList<ListItem>,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) :
    ItemAbstractAdapter(activity, listItems, recyclerView, null, itemClick) {

    override fun getSelectableItemCount() = 0

    override fun getItemSelectionKey(position: Int) = 0

    override fun getItemKeyPosition(key: Int) = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        createViewHolder(R.layout.item_decompression_list_file_dir, parent)

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            val icon = holder.itemView.item_icon
            if (icon != null) {
                Glide.with(activity).clear(icon)
            }
        }
    }

    override fun setupView(view: View, fileDirItem: FileDirItem) {
        view.apply {
            val fileName = fileDirItem.name
            item_name.text = fileName
            item_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            super.setFileIcon(item_icon, fileDirItem)
        }
    }
}
