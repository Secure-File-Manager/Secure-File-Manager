package com.securefilemanager.app.extensions

import android.content.Context
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setError(context: Context, id: Int) {
    this.error = context.getString(id)
}
