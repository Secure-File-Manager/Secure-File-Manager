package com.securefilemanager.app.adapters

import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.securefilemanager.app.R
import com.securefilemanager.app.activities.BaseAbstractActivity
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.helpers.getFilePlaceholderDrawables
import com.securefilemanager.app.models.FileDirItem
import com.securefilemanager.app.views.FastScroller
import com.securefilemanager.app.views.MyRecyclerView
import java.util.*

abstract class ItemAbstractAdapter(
    activity: BaseAbstractActivity,
    private val fileDirItems: List<FileDirItem>,
    recyclerView: MyRecyclerView,
    fastScroller: FastScroller?,
    itemClick: (Any) -> Unit
) : RecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private var fileDrawable: Drawable? = null
    private var fileDrawables = HashMap<String, Drawable>()
    private lateinit var folderDrawable: Drawable

    protected var fontSize = 0f
    protected var smallerFontSize = 0f
    protected var dateFormat = ""
    protected var timeFormat = ""
    private val cornerRadius = resources.getDimension(R.dimen.rounded_corner_radius_small).toInt()

    init {
        initDrawables()
        fontSize = activity.getTextSize()
        smallerFontSize = fontSize * 0.8f
        updateDateTimeFormat()
    }

    abstract fun setupView(view: View, fileDirItem: FileDirItem)

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getItemCount() = fileDirItems.size

    override fun getSelectableItemCount() = fileDirItems.size

    override fun getIsItemSelectable(position: Int) = false

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun getItemSelectionKey(position: Int) =
        fileDirItems.getOrNull(position)?.path?.hashCode()

    override fun getItemKeyPosition(key: Int): Int =
        fileDirItems.indexOfFirst { it.path.hashCode() == key }

    override fun onBindViewHolder(holder: RecyclerViewAdapter.ViewHolder, position: Int) {
        val fileDirItem = fileDirItems[position]
        holder.bindView(
            fileDirItem,
            allowSingleClick = true,
            allowLongClick = false
        ) { itemView, _ ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    fun updateDateTimeFormat() {
        dateFormat = activity.config.dateFormat
        timeFormat = activity.getTimeFormat()
        notifyDataSetChanged()
    }

    protected fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return activity.resources.getQuantityString(R.plurals.items, children, children)
    }

    protected fun setFileIcon(view: ImageView, listItem: FileDirItem) {
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }

        if (listItem.isDirectory) {
            view.setImageDrawable(this.folderDrawable)
            return
        }

        val path = this.getImagePathToLoad(listItem.path)
        val placeholder = this.getPlaceholder(listItem.name)
        val options = this.getGlideOptions(listItem, placeholder)

        if (path.toString().isGif()) {
            Glide.with(activity)
                .asBitmap()
                .load(path)
                .apply(options)
                .into(view)
        } else {
            Glide.with(activity)
                .load(path)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(options)
                .transform(CenterCrop(), RoundedCorners(cornerRadius))
                .into(view)
        }
    }

    private fun getPlaceholder(fileName: String): Drawable {
        return this.fileDrawables
            .getOrElse(fileName.substringAfterLast(".")
                .toLowerCase(
                    Locale.getDefault()
                ), { this.fileDrawable!! })
    }

    private fun getImagePathToLoad(path: String): Any? =
        if (this.activity.config.showMediaPreview) {
            path
        } else {
            null
        }

    private fun getGlideOptions(listItem: FileDirItem, placeholder: Drawable) =
        RequestOptions()
            .signature(listItem.getKey())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .error(placeholder)
            .transform(CenterCrop(), RoundedCorners(10))

    private fun initDrawables() {
        folderDrawable =
            getColoredDrawableWithColor(activity, R.drawable.ic_folder_vector)
        folderDrawable.alpha = 180
        fileDrawable = activity.getDrawableById(R.drawable.ic_file_generic)
        fileDrawables = getFilePlaceholderDrawables(activity)
    }
}
