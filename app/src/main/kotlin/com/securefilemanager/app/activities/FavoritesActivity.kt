package com.securefilemanager.app.activities

import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.securefilemanager.app.R
import com.securefilemanager.app.adapters.ManageFavoritesAdapter
import com.securefilemanager.app.dialogs.FilePickerDialog
import com.securefilemanager.app.extensions.beVisibleIf
import com.securefilemanager.app.extensions.config
import com.securefilemanager.app.extensions.getInternalStoragePath
import com.securefilemanager.app.interfaces.RefreshRecyclerViewListener
import kotlinx.android.synthetic.main.activity_favorites.*

class FavoritesActivity : BaseAbstractActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        updateFavorites()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_favorites, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_favorite -> addFavorite()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun updateFavorites() {
        val favorites = ArrayList<String>()
        config.favorites.mapTo(favorites) { it }
        manage_favorites_placeholder.beVisibleIf(favorites.isEmpty())

        manage_favorites_placeholder_2.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            beVisibleIf(favorites.isEmpty())
            setOnClickListener {
                addFavorite()
            }
        }

        ManageFavoritesAdapter(this, favorites, this, manage_favorites_list) { }.apply {
            manage_favorites_list.adapter = this
        }
    }

    override fun refreshItems() {
        updateFavorites()
    }

    private fun addFavorite() {
        FilePickerDialog(
            this,
            currPath = getInternalStoragePath(),
            pickFile = false
        ) {
            config.addFavorite(it)
            updateFavorites()
        }
    }
}
