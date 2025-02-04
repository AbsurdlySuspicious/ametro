package org.ametro.ui.fragments

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.Constants
import org.ametro.catalog.CatalogLoader
import org.ametro.catalog.CatalogLoaderCallbacks
import org.ametro.catalog.entities.MapCatalog
import org.ametro.catalog.entities.MapInfo
import org.ametro.catalog.entities.MapInfoHelpers
import org.ametro.databinding.FragmentMapListViewBinding
import org.ametro.ui.adapters.MapListAdapter
import org.ametro.ui.loaders.ExtendedMapInfo
import org.ametro.ui.loaders.ExtendedMapStatus
import org.ametro.utils.misc.*
import org.ametro.utils.ui.*

class MapListFragment : Fragment(), SearchView.OnQueryTextListener, SearchView.OnCloseListener,
    CatalogLoaderCallbacks, OnItemClickListener, MultiChoiceModeListener, View.OnClickListener {

    private var binding: FragmentMapListViewBinding? = null
    private var adapter: MapListAdapter? = null
    private var filterValue: String? = null
    private var localMapCatalog: MapCatalog? = null
    private var remoteMapCatalog: MapCatalog? = null
    private var actionMode: ActionMode? = null
    private val actionModeSelection: MutableSet<String> = HashSet()
    private var catalogLoader: CatalogLoader? = null
    
    private var listener: IMapListEventListener = object : IMapListEventListener {
        override fun onOpenMap(map: MapInfo) {}
        override fun onDeleteMaps(map: Array<MapInfo>) {}
        override fun onLoadedMaps(maps: Array<ExtendedMapInfo>) {}
        override fun onLoadedRemoteMaps() {}
        override fun onAddMap() {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMapListViewBinding.inflate(inflater, container, false)
        adapter = MapListAdapter(activity, ApplicationEx.getInstanceActivity(requireActivity()).getCountryFlagProvider())
        catalogLoader = CatalogLoader(requireContext(), this)

        binding!!.noMaps.setOnClickListener(this)
        binding!!.list.apply {
            onItemClickListener = this@MapListFragment
            isLongClickable = true
            choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            setMultiChoiceModeListener(this@MapListFragment)
            adapter = this@MapListFragment.adapter
        }

        applyInsets(makeBottomInsetsApplier(binding!!.list, keepHeight = true))

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        catalogLoader = null
        adapter = null
    }

    override fun onResume() {
        super.onResume()
        forceUpdate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_ACTION_MODE, actionMode != null)
        outState.putStringArrayList(STATE_SELECTION, MapInfoHelpers.toFileNameArray(adapter!!.selection))
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState == null) {
            return
        }
        if (savedInstanceState.getBoolean(STATE_ACTION_MODE)) {
            actionMode = binding!!.list.startActionMode(ContextualActionModeCallback())
        }
        actionModeSelection.clear()
        actionModeSelection.addAll(savedInstanceState.getStringArrayList(STATE_SELECTION)!!)
    }

    fun forceUpdate() {
        val lm = LoaderManager.getInstance(this)
        lm.initLoader(CatalogLoader.LOCAL_CATALOG_LOADER, null, catalogLoader!!).forceLoad()
        lm.initLoader(CatalogLoader.REMOTE_CATALOG_LOADER, null, catalogLoader!!).forceLoad()
    }

    fun startContextActionMode() {
        actionMode = binding!!.list.startActionMode(ContextualActionModeCallback())
    }

    fun setMapListEventListener(newListener: IMapListEventListener) {
        listener = newListener
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (actionMode != null) {
            val checkedCount = binding!!.list.checkedItemCount
            actionMode!!.title = "$checkedCount Selected"
            adapter!!.toggleSelection(position)
            return
        }
        adapter!!.getItem(position)?.let{ listener.onOpenMap(it) }
    }

    override fun onLoadFinished(loader: Loader<MapCatalog?>, data: MapCatalog?) {
        if (data == null)
            return
        
        when (loader.id) {
            CatalogLoader.LOCAL_CATALOG_LOADER -> localMapCatalog = data
            CatalogLoader.REMOTE_CATALOG_LOADER -> remoteMapCatalog = data
        }
        
        resetAdapter()
    }

    override fun onCreateLoader(id: Int, args: Bundle?, loader: Loader<MapCatalog?>) {}
    override fun onLoaderReset(loader: Loader<MapCatalog?>) {}

    override fun onClose(): Boolean {
        filterValue = null
        adapter!!.filter.filter(null)
        return true
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        filterValue = s
        adapter!!.filter.filter(s)
        return true
    }

    override fun onQueryTextChange(s: String): Boolean {
        filterValue = s
        adapter!!.filter.filter(s)
        return true
    }

    private fun setNoMapsShown() {
        binding!!.list.emptyView = null
        binding!!.progressText.visibility = View.GONE
        binding!!.progressBar.visibility = View.GONE
        binding!!.list.visibility = View.GONE
        binding!!.empty.visibility = View.GONE
        binding!!.noMaps.visibility = View.VISIBLE
    }

    private fun setListShown() {
        binding!!.progressText.visibility = View.GONE
        binding!!.progressBar.visibility = View.GONE
        binding!!.noMaps.visibility = View.GONE
        binding!!.empty.visibility = View.VISIBLE
        binding!!.list.apply {
            visibility = View.VISIBLE
            emptyView = binding!!.empty
        }
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        val checkedCount = binding!!.list.checkedItemCount
        mode.title = checkedCount.toString() + " " + getText(R.string.msg_selected)
        adapter!!.toggleSelection(position)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.map_list_context_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                listener.onDeleteMaps(adapter!!.selection)
                mode.finish()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter!!.clearSelection()
        actionModeSelection.clear()
    }

    override fun onClick(v: View) {
        listener.onAddMap()
    }

    interface IMapListEventListener {
        fun onOpenMap(map: MapInfo)
        fun onDeleteMaps(map: Array<MapInfo>)
        fun onLoadedMaps(maps: Array<ExtendedMapInfo>)
        fun onLoadedRemoteMaps()
        fun onAddMap()
    }

    private fun resetAdapter() {
        if (localMapCatalog == null || localMapCatalog!!.maps.isEmpty()) {
            adapter!!.clear()
            adapter!!.filter.filter(filterValue)
            setNoMapsShown()
            return
        }

        val maps = localMapCatalog!!.maps.mapArray { localMap ->
            ExtendedMapInfo(
                localMap,
                remoteMapCatalog?.let {
                    getMapStatus(localMap, it)
                } ?: ExtendedMapStatus.Fetching
            )
        }

        if (actionModeSelection.size > 0) {
            for (map in maps) {
                if (actionModeSelection.contains(map.fileName)) {
                    map.isSelected = true
                }
            }
        }

        adapter!!.clear()
        adapter!!.addAll(*maps)
        adapter!!.filter.filter(filterValue)
        setListShown()
        listener.onLoadedMaps(maps)

        if (localMapCatalog != null && remoteMapCatalog != null)
            listener.onLoadedRemoteMaps()
    }

    private fun getMapStatus(map: MapInfo, catalog: MapCatalog): ExtendedMapStatus {
        val remoteMap = catalog.findMap(map.fileName) ?: return ExtendedMapStatus.Unknown
        return if (remoteMap.timestamp == map.timestamp) {
            ExtendedMapStatus.Installed
        } else ExtendedMapStatus.Outdated
    }

    private inner class ContextualActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            binding!!.list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.map_list_context_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    listener.onDeleteMaps(adapter!!.selection)
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            binding!!.list.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            adapter!!.clearSelection()
            actionModeSelection.clear()
            actionMode = null
        }
    }

    companion object {
        private const val STATE_ACTION_MODE = "STATE_ACTION_MODE"
        private const val STATE_SELECTION = "STATE_SELECTION"
    }
}