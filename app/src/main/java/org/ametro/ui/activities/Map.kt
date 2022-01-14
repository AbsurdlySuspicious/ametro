package org.ametro.ui.activities

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.ApplicationSettingsProvider
import org.ametro.app.Constants
import org.ametro.app.SavedRoute
import org.ametro.databinding.ActivityMapViewBinding
import org.ametro.model.MapContainer
import org.ametro.model.ModelUtil
import org.ametro.model.entities.MapDelay
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.model.serialization.MapSerializationException
import org.ametro.providers.TransportIconsProvider
import org.ametro.routes.MapRouteProvider
import org.ametro.routes.RouteUtils
import org.ametro.routes.entities.MapRoute
import org.ametro.routes.entities.MapRoutePart
import org.ametro.routes.entities.MapRouteQueryParameters
import org.ametro.ui.adapters.StationSearchAdapter
import org.ametro.ui.bottom_panel.*
import org.ametro.ui.navigation.INavigationControllerListener
import org.ametro.ui.navigation.NavigationController
import org.ametro.ui.tasks.MapLoadAsyncTask
import org.ametro.ui.tasks.MapLoadAsyncTask.IMapLoadingEventListener
import org.ametro.ui.testing.DebugToast
import org.ametro.ui.testing.TestMenuOptionsProcessor
import org.ametro.ui.views.MultiTouchMapView
import org.ametro.ui.bottom_panel.MapBottomPanelStation.MapBottomPanelStationListener
import org.ametro.ui.widgets.MapSelectionIndicatorsWidget
import org.ametro.ui.widgets.MapSelectionIndicatorsWidget.IMapSelectionEventListener
import org.ametro.utils.StringUtils
import org.ametro.utils.misc.convertPair
import java.lang.StringBuilder
import java.util.*

class Map : AppCompatActivityEx(), IMapLoadingEventListener, INavigationControllerListener,
    IMapSelectionEventListener, MapBottomPanelStationListener, MapBottomPanelRoute.MapBottomPanelRouteListener,
    BottomPanelSheetListener {

    private var backPressTime = 0L
    private var backPressCount = 0

    private var enabledTransportsSet: MutableSet<String?>? = null
    private var container: MapContainer? = null
    private var scheme: MapScheme? = null
    private var schemeName: String? = null
    private var currentDelay: MapDelay? = null
    private var waitingForActivityResult: Boolean = false

    private lateinit var binding: ActivityMapViewBinding
    private lateinit var mapSelectionIndicators: MapSelectionIndicatorsWidget
    private lateinit var mapBottomSheet: MapBottomPanelSheet
    private lateinit var mapBottomStation: MapBottomPanelStation
    private lateinit var mapBottomRoute: MapBottomPanelRoute
    private val mapPanelView: View
        get() = binding.mapPanel
    private val mapContainerView: ViewGroup
        get() = binding.mapContainer

    private var mapView: MultiTouchMapView? = null
    private var loadingProgressDialog: ProgressDialog? = null

    private lateinit var testMenuOptionsProcessor: TestMenuOptionsProcessor
    private lateinit var app: ApplicationEx
    private lateinit var settingsProvider: ApplicationSettingsProvider
    private lateinit var navigationController: NavigationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = ApplicationEx.getInstanceActivity(this)

        mapBottomSheet = MapBottomPanelSheet(binding.includeBottomPanel.mapBottomPanel, app, this, this)
        mapBottomStation = MapBottomPanelStation(mapBottomSheet, this)
        mapBottomRoute = MapBottomPanelRoute(mapBottomSheet, this)

        mapSelectionIndicators = MapSelectionIndicatorsWidget(
            this,
            binding.beginIndicator,
            binding.endIndicator
        )

        settingsProvider = app.applicationSettingsProvider
        binding.includeEmptyMap.mapEmptyPanel.setOnClickListener {
            if (!waitingForActivityResult) onOpenMaps()
        }
        testMenuOptionsProcessor = TestMenuOptionsProcessor(this)

        navigationController = NavigationController(
            this,
            this,
            TransportIconsProvider(this),
            ApplicationEx.getInstanceActivity(this).getCountryFlagProvider(),
            ApplicationEx.getInstanceActivity(this).getLocalizedMapInfoProvider()
        )
    }

    override fun updatePanelPadding(newPadding: Int) {
        mapView?.panelPadding = newPadding
    }

    private val isResuming: Boolean
        get() = lifecycle.currentState != Lifecycle.State.RESUMED

    private fun ifNotResuming(action: () -> Unit) {
        if (!isResuming) action()
    }

    override fun onPause() {
        super.onPause()
        mapView?.let {
            app.centerPositionAndScale = convertPair(it.centerPositionAndScale)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun restoreRoute(route: SavedRoute, ignoreStation: Boolean) {
        app.centerPositionAndScale?.let {
            mapView!!.setCenterPositionAndScale(it.first, it.second, false)
        }

        route.routeStart?.let {
            mapSelectionIndicators.setBeginStation(it)
        }

        route.routeEnd?.let {
            mapSelectionIndicators.setEndStation(it)
        }

        if (!ignoreStation && !mapBottomStation.isOpened && app.bottomPanelOpen) run {
            val station = app.bottomPanelStation ?: return@run
            val hasDetails = container!!
                .findStationInformation(station.first.name, station.second.name)
                ?.mapFilePath != null
            mapBottomStation.show(station.first, station.second, hasDetails)
        }
    }

    override fun onResume() {
        super.onResume()

        initMapViewState()

        if (mapView != null && container != null) {
            val route = app.currentRoute
            mapSelectionIndicators.clearSelection()
            restoreRoute(route, ignoreStation = false)
        } else {
            app.clearCurrentMapViewState()
            settingsProvider.currentMap?.let {
                MapLoadAsyncTask(
                    this, this, MapContainer(it, settingsProvider.preferredMapLanguage)
                ).execute()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navigationController.onPostCreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navigationController.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return (navigationController.onOptionsItemSelected(item)
                || testMenuOptionsProcessor.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        val manager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem.actionView as SearchView
        val selectedStation = arrayOfNulls<MapSchemeStation>(1)
        searchView.isSubmitButtonEnabled = false
        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))
        searchView.setOnCloseListener {
            selectedStation[0] = null
            true
        }
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val station = (searchView.suggestionsAdapter as StationSearchAdapter).getStation(position)
                searchView.setQuery(station.displayName, true)
                selectedStation[0] = station
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (scheme == null) {
                    return true
                }
                selectedStation[0]?.let {
                    ModelUtil.findStationByUid(scheme, it.uid.toLong())
                }?.let { stationInfo ->
                    val stationInformation = container!!
                        .findStationInformation(stationInfo.first.name, stationInfo.second.name)
                    val p = PointF(selectedStation[0]!!.position.x, selectedStation[0]!!.position.y)
                    mapView!!.setCenterPositionAndScale(p, mapView!!.scale, true)
                    mapBottomStation.show(
                        stationInfo.first,
                        stationInfo.second,
                        stationInformation?.mapFilePath != null
                    )
                    searchMenuItem.collapseActionView()
                }
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (scheme == null) {
                    return true
                }
                searchView.suggestionsAdapter = StationSearchAdapter.createFromMapScheme(this@Map, scheme, query)
                return true
            }
        })
        return true
    }

    override fun onBackPressed() {
        val currentPress = System.currentTimeMillis()
        val delta = currentPress - backPressTime
        backPressTime = currentPress

        if (delta < 200)
            backPressCount += 1
        else
            backPressCount = 0

        if (backPressCount >= 2)
            return super.onBackPressed()

        if (navigationController.isDrawerOpen)
            navigationController.closeDrawer()
        else if (mapBottomStation.isOpened)
            mapBottomStation.hide()
        else if (mapSelectionIndicators.hasSelection())
            mapSelectionIndicators.clearSelection()
        else {
            when (mapBottomSheet.bottomSheet.state) {
                BottomSheetBehavior.STATE_SETTLING,
                BottomSheetBehavior.STATE_DRAGGING -> return
                else -> if (delta < 1000) return
            }
            if (app.restorePrevRoute())
                restoreRoute(app.currentRoute, ignoreStation = true)
            else
                super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        waitingForActivityResult = false
        when (requestCode) {
            OPEN_MAPS_ACTION -> if (resultCode == RESULT_OK) {
                app.clearCurrentMapViewState()
                val localMapCatalogManager = app.getLocalMapCatalogManager()
                MapLoadAsyncTask(
                    this, this, MapContainer(
                        localMapCatalogManager.getMapFile(
                            localMapCatalogManager.findMapByName(
                                data!!.getStringExtra(Constants.MAP_PATH)
                            )
                        ),
                        settingsProvider.preferredMapLanguage
                    )
                ).execute()
            }
            OPEN_STATION_DETAILS -> {
                mapBottomStation.detailsClosed()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun dismissLoadingDialog() {
        if (loadingProgressDialog != null) {
            loadingProgressDialog!!.dismiss()
            loadingProgressDialog = null
        }
    }

    override fun onBeforeMapLoading(container: MapContainer, schemeName: String, enabledTransports: Array<String>?) {
        if (!container.isLoaded(schemeName, enabledTransports)) {
            dismissLoadingDialog()
            loadingProgressDialog = ProgressDialog(this)
            with(loadingProgressDialog!!) {
                isIndeterminate = true
                setProgressStyle(ProgressDialog.STYLE_SPINNER)
                setCancelable(false)
                setMessage(resources.getString(R.string.msg_map_loading_progress, schemeName))
                show()
            }
        }
    }

    override fun onMapLoadComplete(
        container: MapContainer,
        schemeName: String,
        enabledTransports: Array<String>?,
        time: Long
    ) {
        dismissLoadingDialog()
        DebugToast.show(this, getString(R.string.msg_map_loaded, time.toString()), Toast.LENGTH_LONG)
        app.setCurrentMapViewState(container, schemeName, enabledTransports)
        initMapViewState()
        settingsProvider.currentMap = container.mapFile
    }

    private fun initMapViewState() {
        if (app.container == null || !app.container!!.mapFile.exists()) {
            app.clearCurrentMapViewState()
            navigationController.setNavigation(null, null, null, null)
            mapPanelView.visibility = View.GONE
            setupNavbarForceDark()
            return
        }
        setupNavbar()
        container = app.container
        schemeName = app.schemeName
        scheme = container!!.getScheme(schemeName)
        enabledTransportsSet = HashSet(
            listOf(
                *app.enabledTransports ?: container!!.getScheme(schemeName).defaultTransports
            )
        )
        navigationController.setNavigation(container, schemeName, app.enabledTransports, currentDelay)
        mapPanelView.visibility = View.VISIBLE
        mapSelectionIndicators.clearSelection()
        mapView = MultiTouchMapView(this, container, schemeName, mapSelectionIndicators)
        mapView!!.setOnClickListener {
            ModelUtil.findTouchedStation(scheme, mapView!!.touchPoint)?.let { stationInfo ->
                val stationInformation = container!!
                    .findStationInformation(stationInfo.first.name, stationInfo.second.name)
                mapBottomStation.show(
                    stationInfo.first,
                    stationInfo.second,
                    stationInformation?.mapFilePath != null
                )
            } ?: mapBottomStation.hide()
        }
        mapContainerView.removeAllViews()
        mapContainerView.addView(mapView)
        mapView!!.requestFocus()
    }

    override fun onMapLoadFailed(
        container: MapContainer,
        schemeName: String,
        enabledTransports: Array<String>?,
        reason: Throwable
    ) {
        dismissLoadingDialog()
        Toast.makeText(this, getString(R.string.msg_map_loading_failed, reason.message), Toast.LENGTH_LONG).show()
        Log.e(Constants.LOG, "Map load failed due exception: " + reason.message, reason)
    }

    override fun onOpenMaps(): Boolean {
        mapBottomStation.hide()
        startActivityForResult(Intent(this, MapList::class.java), OPEN_MAPS_ACTION)
        waitingForActivityResult = true
        return true
    }

    override fun onOpenSettings(): Boolean {
        mapBottomStation.hide()
        startActivityForResult(Intent(this, SettingsList::class.java), OPEN_SETTINGS_ACTION)
        return true
    }

    override fun onOpenAbout(): Boolean {
        startActivity(Intent(this, About::class.java))
        return false
    }

    override fun onChangeScheme(schemeName: String): Boolean {
        mapBottomStation.hide()
        MapLoadAsyncTask(this, this, container, schemeName, enabledTransportsSet!!.toTypedArray()).execute()
        return true
    }

    override fun onToggleTransport(transportName: String, checked: Boolean): Boolean {
        return try {
            if (checked) {
                enabledTransportsSet!!.add(transportName)
                container!!.loadSchemeWithTransports(
                    schemeName,
                    enabledTransportsSet!!.toTypedArray()
                )
            } else {
                enabledTransportsSet!!.remove(transportName)
            }
            true
        } catch (e: MapSerializationException) {
            false
        }
    }

    override fun onDelayChanged(delay: MapDelay): Boolean {
        currentDelay = delay
        return true
    }

    override fun onPanelHidden() {
        mapSelectionIndicators.clearSelection()
    }

    override fun onOpenDetails(station: Pair<MapSchemeLine, MapSchemeStation>) {
        onShowMapDetail(station.first, station.second)
    }

    override fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?) {
        if (waitingForActivityResult || station == null || line == null) return
        waitingForActivityResult = true
        val intent = Intent(this, StationDetails::class.java)
        intent.putExtra(Constants.LINE_NAME, line.name)
        intent.putExtra(Constants.STATION_NAME, station.name)
        intent.putExtra(Constants.STATION_UID, station.uid)
        startActivityForResult(intent, OPEN_STATION_DETAILS)
    }

    override fun onSelectBeginStation(line: MapSchemeLine?, station: MapSchemeStation?) {
        ifNotResuming {
            mapBottomStation.hide()
            app.resetSelectedRoute()
        }
        if (line != null && station != null) {
            mapSelectionIndicators.setBeginStation(Pair(line, station))
            app.setRouteStart(line, station)
        } else {
            mapSelectionIndicators.setBeginStation(null)
        }
    }

    override fun onSelectEndStation(line: MapSchemeLine?, station: MapSchemeStation?) {
        ifNotResuming {
            mapBottomStation.hide()
            app.resetSelectedRoute()
        }
        if (line != null && station != null) {
            mapSelectionIndicators.setEndStation(Pair(line, station))
            app.setRouteEnd(line, station)
        } else {
            mapSelectionIndicators.setEndStation(null)
        }
    }

    private fun highlightRoute(route: MapRoute) {
        mapView!!
            .highlightsElements(RouteUtils.convertRouteToSchemeObjectIds(route, scheme!!))
    }

    override fun onRouteSelectionComplete(
        begin: Pair<MapSchemeLine, MapSchemeStation>,
        end: Pair<MapSchemeLine, MapSchemeStation>
    ) {
        val routeParams = MapRouteQueryParameters(
            container,
            enabledTransportsSet,
            currentDelayIndex,
            begin.second.uid,
            end.second.uid
        )

        val routes = MapRouteProvider.findRoutes(routeParams, maxRoutes = 5)

        if (routes.isEmpty()) {
            mapView!!.highlightsElements(null)
            mapBottomRoute.hide()
            Toast.makeText(
                this,
                String.format(
                    getString(R.string.msg_no_route_found),
                    begin.second.displayName,
                    end.second.displayName
                ),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val panelRoutes = ArrayList<RoutePagerItem>(routes.size)

        routes.mapTo(panelRoutes) {
            val txfs = arrayListOf<RoutePagerTransfer>()
            var lastPartsCount = 0
            var lastPartsDelays = 0L
            var lastTxf: MapRoutePart? = null

            for (p in it.parts.iterator()) {
                lastPartsCount += 1
                lastPartsDelays += p.delay
                if (!p.isTransfer) continue

                val station = ModelUtil.findStationByUid(scheme!!, p.from.toLong())
                txfs.add(RoutePagerTransfer(station.first, lastPartsCount, lastPartsDelays.toInt()))

                lastTxf = p
                lastPartsCount = 0
                lastPartsDelays = 0
            }

            if (lastTxf != null) {
                val lastStation = ModelUtil.findStationByUid(scheme!!, lastTxf.to.toLong())
                txfs.add(RoutePagerTransfer(lastStation.first, lastPartsCount, lastPartsDelays.toInt()))
            }

            RoutePagerItem(
                delay = it.delay,
                routeStart = begin,
                routeEnd = end,
                transfers = txfs
            )
        }

        val initRoute =
            app.currentRoute.selectedRoute.let { if (it > 0) it else 0 }
        highlightRoute(routes[initRoute])

        if (!isResuming || app.lastLeaveTime == null) {
            app.lastLeaveTime = Calendar.getInstance()
        }

        mapBottomRoute.setSlideCallback { pos ->
            routes.getOrNull(pos)?.let {
                highlightRoute(it)
                app.currentRoute.selectedRoute = pos
            }
        }
        mapBottomRoute.show(panelRoutes, app.lastLeaveTime, initRoute)
    }

    override fun onRouteSelectionCleared() {
        mapView?.highlightsElements(null)
        ifNotResuming {
            app.clearRoute()
            mapBottomRoute.hide()
        }
    }

    private val currentDelayIndex: Int?
        get() {
            val delays = container!!.metadata.delays
            if (delays.isEmpty()) {
                return null
            }
            for ((index, delay) in delays.withIndex()) {
                if (delay === currentDelay) {
                    return index
                }
            }
            return 0
        }

    companion object {
        private const val OPEN_MAPS_ACTION = 1
        private const val OPEN_SETTINGS_ACTION = 2
        private const val OPEN_STATION_DETAILS = 3
    }
}