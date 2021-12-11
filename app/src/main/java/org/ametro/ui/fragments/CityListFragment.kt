package org.ametro.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import org.ametro.app.ApplicationEx.Companion.getInstance
import org.ametro.app.Constants
import org.ametro.catalog.entities.MapInfo
import org.ametro.databinding.FragmentCityListViewBinding
import org.ametro.providers.FilteringMapGeographyProvider
import org.ametro.ui.adapters.CityListAdapter
import org.ametro.utils.ListUtils
import java.util.*

class CityListFragment : Fragment(), OnChildClickListener, LoaderManager.LoaderCallbacks<Array<MapInfo>?>,
    SearchView.OnQueryTextListener {

    private var citySelectionListener: ICitySelectionListener = object : ICitySelectionListener {
        override fun onCitySelected(maps: Array<MapInfo?>?) {}
    }

    private var maps: Array<MapInfo> = emptyArray()
    private var adapter: CityListAdapter? = null
    private var geographyProvider: FilteringMapGeographyProvider? = null
    private var country: String? = null
    private var city: String? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var list: ExpandableListView
    private lateinit var noConnectionView: View
    private lateinit var emptyView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentCityListViewBinding.inflate(inflater, container, false)

        if (savedInstanceState != null) {
            country = savedInstanceState.getString(Constants.MAP_COUNTRY)
            city = savedInstanceState.getString(Constants.MAP_CITY)
        }

        progressBar = binding.progressBar
        progressText = binding.progressText
        noConnectionView = binding.noConnection
        emptyView = binding.empty
        list = binding.list
        list.setOnChildClickListener(this)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loaderManager.initLoader(0, null, this).forceLoad()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(Constants.MAP_CITY, country)
        outState.putString(Constants.MAP_CITY, city)
    }

    override fun onChildClick(
        parent: ExpandableListView,
        v: View,
        groupPosition: Int,
        childPosition: Int,
        id: Long
    ): Boolean {
        country = adapter!!.getGroup(groupPosition) as String
        city = adapter!!.getChild(groupPosition, childPosition) as String
        val result: MutableList<MapInfo?> = ArrayList()
        for (map in maps) {
            if (map.city == city && map.country == country) {
                result.add(map)
            }
        }
        citySelectionListener.onCitySelected(result.toTypedArray())
        return true
    }

    fun setCitySelectionListener(newListener: ICitySelectionListener) {
        citySelectionListener = newListener
    }

    private class MapInfoAsyncTaskLoader(act: Activity?) : AsyncTaskLoader<Array<MapInfo>?>(
        act!!
    ) {
        override fun loadInBackground(): Array<MapInfo>? {
            val app = getInstance(this)
            val loadedMaps: MutableSet<String> = HashSet()
            for (m in app!!.getLocalMapCatalogManager().mapCatalog.maps) {
                loadedMaps.add(m.fileName)
            }
            val remoteMapCatalog = app.getRemoteMapCatalogProvider()
                .getMapCatalog(false) ?: return null
            val remoteMaps = ListUtils.filter(
                listOf(*remoteMapCatalog.maps)
            ) { map: MapInfo -> !loadedMaps.contains(map.fileName) }
            return remoteMaps.toTypedArray()
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Array<MapInfo>?> {
        return MapInfoAsyncTaskLoader(activity)
    }

    override fun onLoadFinished(loader: Loader<Array<MapInfo>?>, data: Array<MapInfo>?) {
        if (data != null && data.isNotEmpty()) {
            setListShown()
            list.emptyView = emptyView
            maps = data
            geographyProvider = FilteringMapGeographyProvider(data)
            adapter = CityListAdapter(
                activity,
                geographyProvider,
                getInstance(activity)!!.getCountryFlagProvider()
            )
            list.setAdapter(adapter)
        } else {
            setNoConnectionShown()
        }
    }

    override fun onLoaderReset(loader: Loader<Array<MapInfo>?>) {
        geographyProvider?.setData(emptyArray())
    }

    override fun onQueryTextSubmit(s: String?): Boolean {
        if (adapter == null) {
            return true
        }
        if (s != null && s.isNotEmpty()) geographyProvider!!.setFilter(s)
        else geographyProvider!!.setFilter(null)
        adapter!!.notifyDataSetChanged()
        return false
    }

    override fun onQueryTextChange(s: String?): Boolean {
        if (adapter == null) {
            return true
        }
        if (s != null && s.isNotEmpty()) geographyProvider!!.setFilter(s)
        else geographyProvider!!.setFilter(null)
        adapter!!.notifyDataSetChanged()
        return false
    }

    private fun setNoConnectionShown() {
        progressText.visibility = View.GONE
        progressBar.visibility = View.GONE
        noConnectionView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        list.visibility = View.GONE
    }

    private fun setListShown() {
        progressText.visibility = View.GONE
        progressBar.visibility = View.GONE
        noConnectionView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        list.visibility = View.VISIBLE
    }

    interface ICitySelectionListener {
        fun onCitySelected(maps: Array<MapInfo?>?)
    }
}