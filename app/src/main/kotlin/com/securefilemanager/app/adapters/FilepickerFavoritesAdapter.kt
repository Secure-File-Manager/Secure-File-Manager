package com.securefilemanager.app.adapters

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.config
import com.securefilemanager.app.extensions.getTextSize
import com.securefilemanager.app.extensions.standardizePath
import com.securefilemanager.app.views.MyRecyclerView
import kotlinx.android.synthetic.main.filepicker_favorite.view.*

class FilepickerFavoritesAdapter(
    activity: BaseAbstractActivity,
    var paths: List<String>,
    recyclerView: MyRecyclerView,
    isMovingOperation: Boolean,
    itemClick: (Any) -> Unit
) : RecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private var fontSize = 0f

    init {
        if (isMovingOperation) {
            val hiddenPath = activity.config.hiddenPath
            paths = paths.filter { !it.startsWith(hiddenPath) }
        }
        fontSize = activity.getTextSize()
    }

    override fun getActionMenuId() = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        createViewHolder(R.layout.filepicker_favorite, parent)

    override fun onBindViewHolder(holder: RecyclerViewAdapter.ViewHolder, position: Int) {
        val path = paths[position]
        holder.bindView(
            path,
            allowSingleClick = true,
            allowLongClick = false
        ) { itemView, _ ->
            setupView(itemView, activity.standardizePath(path))
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = paths.size

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = paths.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemKeyPosition(key: Int) = paths.indexOfFirst { it.hashCode() == key }

    override fun getItemSelectionKey(position: Int) = paths.getOrNull(position)?.hashCode()

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    private fun setupView(view: View, path: String) {
        view.apply {
            filepicker_favorite_label.text = path
            filepicker_favorite_label.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
        }
    }
}
