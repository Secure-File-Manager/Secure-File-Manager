package com.securefilemanager.app.activities

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.securefilemanager.app.BuildConfig
import com.securefilemanager.app.R
import com.securefilemanager.app.dialogs.BetaWarningDialog
import com.securefilemanager.app.dialogs.ChangeSortingDialog
import com.securefilemanager.app.dialogs.RadioGroupDialog
import com.securefilemanager.app.extensions.*
import com.securefilemanager.app.fragments.ItemsFragment
import com.securefilemanager.app.helpers.PERMISSION_WRITE_STORAGE
import com.securefilemanager.app.helpers.TapTargetTutorial
import com.securefilemanager.app.helpers.ensureBackgroundThread
import com.securefilemanager.app.models.RadioItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_items.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class MainActivity : BaseAbstractActivity() {
    private var isSearchOpen = false
    private var wasBackJustPressed = false
    private var searchMenuItem: MenuItem? = null

    private lateinit var fragment: ItemsFragment

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == INTRO_FINISHED) {
            if (resultCode == RESULT_OK) {
                this.tryInitFileManager()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config.internalStoragePath = getInternalStoragePath()
        updateSDCardPath()

        fragment = (fragment_holder as ItemsFragment).apply {
            isGetRingtonePicker = intent.action == RingtoneManager.ACTION_RINGTONE_PICKER
            isGetContentIntent = intent.action == Intent.ACTION_GET_CONTENT
            isPickMultipleIntent = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (!config.isAppWizardDone) {
            startActivityForResult(
                IntroActivity.getIntent(applicationContext, true),
                INTRO_FINISHED
            )
        } else {
            if (savedInstanceState == null) {
                tryInitFileManager()
                checkInvalidFavorites()
            }
        }

        if (!config.isAppBetaWarningShowed) {
            BetaWarningDialog(this)
        }
    }

    override fun onPostResume() {
        super.onPostResume()

        if (config.isAppWizardDone && !config.isAppTutorialShowed) {
            this.config.isAppTutorialShowed = true
            this.openTutorial(delayed = true, cancellable = false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        setupSearch(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val favorites = config.favorites
        menu!!.apply {
            findItem(R.id.add_favorite).isVisible = !favorites.contains(fragment.currentPath)
            findItem(R.id.remove_favorite).isVisible = favorites.contains(fragment.currentPath)
            findItem(R.id.go_to_favorite).isVisible = favorites.isNotEmpty()
            findItem(R.id.go_home).isVisible = fragment.currentPath != config.homeFolder
            findItem(R.id.set_as_home).isVisible = fragment.currentPath != config.homeFolder
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.go_home -> goHome()
            R.id.go_to_favorite -> goToFavorite()
            R.id.sort -> showSortingDialog()
            R.id.add_favorite -> addFavorite()
            R.id.remove_favorite -> removeFavorite()
            R.id.set_as_home -> setAsHome()
            R.id.intro -> startActivity(IntroActivity.getIntent(applicationContext, false))
            R.id.tutorial -> openTutorial(delayed = false, cancellable = true)
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.about -> startActivity(Intent(applicationContext, AboutActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PICKED_PATH, (fragment_holder as ItemsFragment).currentPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val path = savedInstanceState.getString(PICKED_PATH) ?: internalStoragePath
        openPath(path, true)
    }

    private fun openTutorial(delayed: Boolean = false, cancellable: Boolean = true) {
        val activity = this
        val tapTarget = TapTargetTutorial(this)
        GlobalScope.launch {
            if (delayed) {
                delay(1000L)
            }

            activity.runOnUiThread {
                TapTargetSequence(activity)
                    .targets(tapTarget.getTutorialTapTargets(cancellable))
                    .listener(tapTarget.getTutorialListener())
                    .start()
            }
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(R.string.search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        fragment.searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }
        searchMenuItem!!.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                isSearchOpen = true
                fragment.searchOpened()
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                isSearchOpen = false
                fragment.searchClosed()
                return true
            }
        })
    }

    private fun tryInitFileManager() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initFileManager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun initFileManager() {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val data = intent.data
            if (data?.scheme == "file") {
                openPath(data.path!!)
            } else {
                val path = getRealPathFromURI(data!!)
                if (path != null) {
                    openPath(path)
                } else {
                    openPath(config.homeFolder)
                }
            }

            if (!File(data.path!!).isDirectory) {
                tryOpenPathIntent(data.path!!, false)
            }
        } else {
            openPath(config.homeFolder)
        }
    }

    private fun openPath(path: String, forceRefresh: Boolean = false) {
        var newPath = path
        val file = File(path)
        if (file.exists() && !file.isDirectory) {
            newPath = file.parent!!
        } else if (!file.exists()) {
            newPath = internalStoragePath
        }

        (fragment_holder as ItemsFragment).openPath(newPath, forceRefresh)
    }

    private fun goHome() {
        if (config.homeFolder != fragment.currentPath) {
            openPath(config.homeFolder)
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, fragment.currentPath) {
            fragment.refreshItems()
        }
    }

    private fun addFavorite() {
        config.addFavorite(fragment.currentPath)
    }

    private fun removeFavorite() {
        config.removeFavorite(fragment.currentPath)
    }

    private fun goToFavorite() {
        val favorites = config.favorites
        val items = ArrayList<RadioItem>(favorites.size)
        var currFavoriteIndex = -1

        favorites.forEachIndexed { index, path ->
            val titlePath = this.standardizePath(path)
            items.add(RadioItem(index, titlePath, path))
            if (path == fragment.currentPath) {
                currFavoriteIndex = index
            }
        }

        RadioGroupDialog(this, items, currFavoriteIndex, R.string.go_to_favorite) {
            openPath(it.toString())
        }
    }

    private fun setAsHome() {
        config.homeFolder = fragment.currentPath
        toast(R.string.home_folder_updated)
    }

    override fun onBackPressed() {
        if (fragment.mView.breadcrumbs.childCount <= 1) {
            if (!wasBackJustPressed) {
                wasBackJustPressed = true
                toast(R.string.press_back_again)
                Handler().postDelayed({
                    wasBackJustPressed = false
                }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                this.quitApp()
            }
        } else {
            fragment.mView.breadcrumbs.removeBreadcrumb()
            openPath(fragment.mView.breadcrumbs.getLastItem().path)
        }
    }

    private fun checkInvalidFavorites() {
        ensureBackgroundThread {
            config.favorites.forEach {
                if (!isPathOnSD(it) && !File(it).exists()) {
                    config.removeFavorite(it)
                }
            }
        }
    }

    fun pickedPath(path: String) {
        val resultIntent = Intent()
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        resultIntent.setDataAndType(uri, type)
        resultIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    fun pickedRingtone(path: String) {
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        Intent().apply {
            setDataAndType(uri, type)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    fun pickedPaths(paths: ArrayList<String>) {
        val newPaths =
            paths.map { getFilePublicUri(File(it), BuildConfig.APPLICATION_ID) } as ArrayList
        val clipData = ClipData(
            "Attachment",
            arrayOf(paths.getMimeType()),
            ClipData.Item(newPaths.removeAt(0))
        )

        newPaths.forEach {
            clipData.addItem(ClipData.Item(it))
        }

        Intent().apply {
            this.clipData = clipData
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    fun openedDirectory() {
        searchMenuItem?.collapseActionView()
    }

    companion object {
        private const val BACK_PRESS_TIMEOUT = 5000
        private const val PICKED_PATH = "picked_path"
        private const val INTRO_FINISHED = 1456
    }

}
