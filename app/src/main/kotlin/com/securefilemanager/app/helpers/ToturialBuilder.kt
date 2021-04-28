package com.securefilemanager.app.helpers

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.getDrawableById
import com.securefilemanager.app.extensions.privateField
import kotlinx.android.synthetic.main.fragment_items.*
import java.util.*

class TapTargetTutorial(activity: AppCompatActivity) {

    private var mActivity: AppCompatActivity = activity

    private var mDeviceWith: Int = this.getDeviceWidth()

    private val mTutorialListener: TapTargetSequence.Listener =
        object : TapTargetSequence.Listener {
            override fun onSequenceFinish() {}

            override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                when (lastTarget?.id()) {
                    1 -> mActivity.items_list
                        .findViewHolderForAdapterPosition(0)
                        ?.itemView
                        ?.performLongClick()
                    4 -> mActivity.items_list
                        .findViewHolderForAdapterPosition(0)
                        ?.itemView
                        ?.performClick()
                }
            }

            override fun onSequenceCanceled(lastTarget: TapTarget?) {}
        }

    fun getTutorialListener() = this.mTutorialListener

    fun getTutorialTapTargets(cancellable: Boolean = true): ArrayList<TapTarget> {
        val location = IntArray(2)
        this.getToolbar().getLocationOnScreen(location)
        return ArrayList<TapTarget>().apply {
            add(getBreadcrumbTapTarget(size, cancellable))
            add(getFileTapTarget(size, cancellable))
            add(getHideTapTarget(size, cancellable, mDeviceWith, location))
            add(getEncryptTarget(size, cancellable, mDeviceWith, location))
            add(getMoreMenuTapTarget(size, cancellable, mDeviceWith, location))
        }
    }

    private fun getString(id: Int): String =
        this.mActivity.getString(id)

    private fun getStringHtml(id: Int): Spanned =
        htmlText(this.getString(id))

    private fun getDrawable(id: Int): Drawable? =
        this.mActivity.getDrawableById(id)

    private fun getDeviceWidth(): Int {
        val displayMetrics = DisplayMetrics()
        this.mActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getToolbar(): Toolbar =
        (this.mActivity.supportActionBar as Any)
            .privateField<Any>("mDecorToolbar")
            .privateField("mToolbar")

    private fun getBreadcrumbTapTarget(id: Int, cancellable: Boolean): TapTarget =
        TapTarget
            .forView(
                this.mActivity.breadcrumbs.getChildAt(0),
                this.getString(R.string.breadcrumb_title),
                this.getStringHtml(R.string.breadcrumb_desc)
            )
            .cancelable(cancellable)
            .transparentTarget(true)
            .targetRadius(60)
            .id(id)

    private fun getFileTapTarget(id: Int, cancellable: Boolean): TapTarget {
        val location = IntArray(2)
        this.mActivity.breadcrumbs.getChildAt(0).getLocationOnScreen(location)
        val top = location[1] + 150
        return TapTarget
            .forBounds(
                Rect(
                    0,
                    top,
                    335,
                    top + 135,
                ),
                getString(R.string.file_title),
                getString(R.string.file_desc)
            )
            .cancelable(cancellable)
            .transparentTarget(true)
            .targetRadius(70)
            .id(id)
    }

    private fun getHideTapTarget(
        id: Int,
        cancellable: Boolean,
        deviceWidth: Int,
        toolbarLocation: IntArray
    ): TapTarget =
        TapTarget
            .forBounds(
                Rect(
                    deviceWidth - 340,
                    toolbarLocation[1],
                    deviceWidth - 220,
                    toolbarLocation[1] + 155,
                ),
                getString(R.string.hide_title),
                getString(R.string.hide_desc)
            )
            .cancelable(cancellable)
            .icon(this.getDrawable(R.drawable.ic_hide_vector))
            .id(id)

    private fun getMoreMenuTapTarget(
        id: Int,
        cancellable: Boolean,
        deviceWidth: Int,
        toolbarLocation: IntArray
    ): TapTarget =
        TapTarget
            .forBounds(
                Rect(
                    deviceWidth - 120,
                    toolbarLocation[1],
                    deviceWidth,
                    toolbarLocation[1] + 155,
                ),
                getString(R.string.more_menu_title),
                getStringHtml(R.string.more_menu_desc)
            )
            .cancelable(cancellable)
            .icon(this.getDrawable(R.drawable.ic_more_vert_vector))
            .id(id)

    private fun getEncryptTarget(
        id: Int,
        cancellable: Boolean,
        deviceWidth: Int,
        toolbarLocation: IntArray
    ): TapTarget =
        TapTarget
            .forBounds(
                Rect(
                    deviceWidth - 220,
                    toolbarLocation[1],
                    deviceWidth - 120,
                    toolbarLocation[1] + 155,
                ),
                getString(R.string.encrypt_title),
                getString(R.string.encrypt_desc)
            )
            .cancelable(cancellable)
            .icon(this.getDrawable(R.drawable.ic_lock_vector))
            .id(id)

}
