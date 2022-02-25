package org.ametro.ui.activities

import org.ametro.ui.fragments.MapListFragment.IMapListEventListener
import org.ametro.ui.tasks.MapInstallerAsyncTask.IMapInstallerEventListener
import org.ametro.ui.fragments.MapListFragment
import android.app.ProgressDialog
import org.ametro.catalog.entities.MapInfo
import android.annotation.SuppressLint
import android.os.Bundle
import org.ametro.R
import android.content.Intent
import android.os.Build
import android.widget.Toast
import org.ametro.app.ApplicationEx
import org.ametro.ui.loaders.ExtendedMapInfo
import org.ametro.ui.loaders.ExtendedMapStatus
import androidx.core.app.NavUtils
import org.ametro.ui.tasks.MapInstallerAsyncTask
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import org.ametro.app.Constants
import org.ametro.databinding.ActivityMapListViewBinding
import org.ametro.ui.tasks.TaskHelpers
import org.ametro.utils.StringUtils
import org.ametro.utils.ui.*
import java.util.ArrayList

class MapList : AppCompatActivityEx(), IMapListEventListener, IMapInstallerEventListener {

    private lateinit var binding: ActivityMapListViewBinding
    private val messagePanel: View
        get() = binding.message

    private lateinit var listFragment: MapListFragment
    private var progressDialog: ProgressDialog? = null

    private var outdatedMaps: Array<MapInfo>? = null
    private var waitingForActivityResult: Boolean = false

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapListViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includeToolbar.toolbar)

        supportActionBar?.run {
            setDefaultDisplayHomeAsUpEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            setFitSystemWindowsFlags(binding.root)
            applyInsets(makeTopInsetsApplier(binding.includeToolbar.toolbar))
        }

        listFragment = supportFragmentManager.findFragmentById(R.id.list) as MapListFragment
        listFragment.setMapListEventListener(this)
    }

    override fun onOpenMap(map: MapInfo) {
        Intent().also {
            it.putExtra(Constants.MAP_PATH, map.fileName)
            setResult(RESULT_OK, it)
        }
        finish()
    }

    override fun onDeleteMaps(maps: Array<MapInfo>) {
        if (maps.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_maps_selected), Toast.LENGTH_LONG).show()
            return
        }
        ApplicationEx.getInstanceActivity(this).getLocalMapCatalogManager().deleteMapAll(maps)
        listFragment.forceUpdate()
        Toast.makeText(this, getString(R.string.msg_maps_deleted), Toast.LENGTH_LONG).show()
    }

    override fun onLoadedMaps(maps: Array<ExtendedMapInfo>) {
        val outdated: MutableList<MapInfo> = ArrayList()
        for (map in maps) {
            if (map.status == ExtendedMapStatus.Outdated) {
                outdated.add(MapInfo(map))
            }
        }
        outdatedMaps = outdated.toTypedArray()
        messagePanel.visibility = if (outdatedMaps!!.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun onAddMap() {
        if (waitingForActivityResult) return
        waitingForActivityResult = true
        startActivityForResult(Intent(this, CityList::class.java), ADD_ACTION)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map_list, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(listFragment)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.action_add -> {
                onAddMap()
                return true
            }
            R.id.action_update -> {
                updateMaps()
                return true
            }
            R.id.action_delete -> {
                listFragment.startContextActionMode()
                return true
            }
            R.id.action_close_map -> {
                Intent().also {
                    it.putExtra(Constants.MAP_PATH, null as String?)
                    setResult(RESULT_OK, it)
                }
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        waitingForActivityResult = false
        when (requestCode) {
            ADD_ACTION -> {
                listFragment.forceUpdate()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun updateMaps() {
        if (outdatedMaps == null || outdatedMaps!!.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_maps_all_updated), Toast.LENGTH_LONG).show()
            return
        }
        val downloadTask = MapInstallerAsyncTask(this, this, outdatedMaps)
        progressDialog = ProgressDialog(this)
        with(progressDialog!!) {
            setMessage(getString(R.string.msg_maps_updating))
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(true)
            isIndeterminate = false
            max = 100
            progress = 0
            setOnCancelListener { downloadTask.cancel(true) }
            show()
        }
        downloadTask.execute()
    }

    override fun onMapDownloadingProgress(currentSize: Long, totalSize: Long, downloadingMap: MapInfo) {
        with(progressDialog!!) {
            progress = (currentSize * 100 / totalSize).toInt()
            setMessage(
                String.format(
                    getString(R.string.msg_download_progress),
                    downloadingMap.fileName + ": " + String.format(
                        "%s / %s",
                        StringUtils.humanReadableByteCount(currentSize, false),
                        StringUtils.humanReadableByteCount(totalSize, false)
                    )
                )
            )
        }
    }

    override fun onMapDownloadingComplete(maps: Array<MapInfo>) {
        progressDialog!!.dismiss()
        Toast.makeText(this, getString(R.string.msg_maps_updated, maps.size.toString()), Toast.LENGTH_LONG).show()
        listFragment.forceUpdate()
    }

    override fun onMapDownloadingFailed(maps: Array<MapInfo>, reason: Throwable) {
        progressDialog!!.dismiss()
        TaskHelpers.displayFailReason(this, reason)
    }

    companion object {
        private const val ADD_ACTION = 1
    }
}