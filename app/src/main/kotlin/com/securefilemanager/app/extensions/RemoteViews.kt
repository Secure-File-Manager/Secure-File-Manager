package com.securefilemanager.app.extensions

import android.view.View
import android.widget.RemoteViews

fun RemoteViews.setText(id: Int, text: String) {
    setTextViewText(id, text)
}

fun RemoteViews.setVisibleIf(id: Int, beVisible: Boolean) {
    setViewVisibility(id, if (beVisible) View.VISIBLE else View.GONE)
}
