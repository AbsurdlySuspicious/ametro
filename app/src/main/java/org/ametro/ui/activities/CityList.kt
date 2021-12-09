package org.ametro.ui.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.view.MenuItemCompat
import org.ametro.R
import org.ametro.catalog.entities.MapInfo
import org.ametro.databinding.ActivityCityListViewBinding
import org.ametro.ui.fragments.CityListFragment
import org.ametro.ui.fragments.CityListFragment.ICitySelectionListener
import org.ametro.ui.tasks.MapInstallerAsyncTask
import org.ametro.ui.tasks.MapInstallerAsyncTask.IMapInstallerEventListener
import org.ametro.ui.tasks.TaskHelpers
import org.ametro.utils.StringUtils

class CityList : AppCompatActivity(), ICitySelectionListener, IMapInstallerEventListener {
    private lateinit var binding: ActivityCityListViewBinding
    private lateinit var cityListFragment: CityListFragment
    private var loadingProgressDialog: ProgressDialog? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCityListViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includeToolbar.toolbar)

        supportActionBar?.let { actionBar ->
            actionBar.setDefaultDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        cityListFragment = supportFragmentManager
            .findFragmentById(R.id.geography_list_fragment) as CityListFragment
        cityListFragment.setCitySelectionListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_city_list, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(cityListFragment)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCitySelected(maps: Array<MapInfo>) {
        val downloadTask = MapInstallerAsyncTask(this, this, maps)
        loadingProgressDialog = ProgressDialog(this)
        with(loadingProgressDialog!!) {
            setMessage(getString(R.string.msg_downloading))
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

    override fun onMapDownloadingComplete(maps: Array<MapInfo>) {
        loadingProgressDialog!!.dismiss()
        setResult(RESULT_OK)
        finish()
    }

    override fun onMapDownloadingProgress(currentSize: Long, totalSize: Long, downloadingMap: MapInfo) {
        with(loadingProgressDialog!!) {
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

    override fun onMapDownloadingFailed(maps: Array<MapInfo>, reason: Throwable) {
        loadingProgressDialog!!.dismiss()
        TaskHelpers.displayFailReason(this, reason)
    }
}