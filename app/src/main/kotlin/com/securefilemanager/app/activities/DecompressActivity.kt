package com.securefilemanager.app.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.securefilemanager.app.R
import com.securefilemanager.app.adapters.DecompressItemsAdapter
import com.securefilemanager.app.extensions.decompressZip
import com.securefilemanager.app.extensions.getFilenameFromPath
import com.securefilemanager.app.extensions.getParentPath
import com.securefilemanager.app.extensions.showErrorToast
import com.securefilemanager.app.models.ListItem
import kotlinx.android.synthetic.main.activity_decompress.*
import net.lingala.zip4j.ZipFile

class DecompressActivity : BaseAbstractActivity() {
    private val allFiles = ArrayList<ListItem>()
    private var currentPath = ""
    private var zipPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decompress)
        zipPath = intent.extras!!.getString(EXTRA_PATH)!!
        title = zipPath.getFilenameFromPath()
        fillAllListItems(zipPath)
        updateCurrentPath("")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_decompress, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.decompress -> decompress()
        }

        return true
    }

    override fun onBackPressed() {
        if (currentPath.isEmpty()) {
            super.onBackPressed()
        } else {
            val newPath = if (currentPath.contains("/")) currentPath.getParentPath() else ""
            updateCurrentPath(newPath)
        }
    }

    private fun updateCurrentPath(path: String) {
        currentPath = path
        try {
            val listItems = getFolderItems(currentPath)
            DecompressItemsAdapter(this, listItems, decompress_list) {
                if ((it as ListItem).isDirectory) {
                    updateCurrentPath(it.path)
                }
            }.apply {
                decompress_list.adapter = this
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun decompress() {
        this.decompressHandle(zipPath) { destination, password ->
            this.decompressZip(zipPath, destination, password)
        }
    }

    private fun getFolderItems(parent: String): ArrayList<ListItem> {
        return allFiles
            .filter {
                val fileParent = if (it.path.contains("/")) {
                    it.path.getParentPath()
                } else {
                    ""
                }

                fileParent == parent
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.mName }))
            .toMutableList() as ArrayList<ListItem>
    }

    private fun fillAllListItems(path: String) {
        ZipFile(path).fileHeaders.forEach { fileHeader ->
            val filename = fileHeader.fileName.removeSuffix("/")
            val listItem = ListItem(
                filename,
                filename.getFilenameFromPath(),
                fileHeader.isDirectory,
                0,
                fileHeader.uncompressedSize,
                fileHeader.lastModifiedTime,
                false
            )
            allFiles.add(listItem)
        }
    }

    companion object {
        const val EXTRA_PATH = "EXTRA_PATH"
    }
}
