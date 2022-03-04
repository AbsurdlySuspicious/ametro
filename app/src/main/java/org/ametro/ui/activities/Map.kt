package org.ametro.ui.activities

import android.app.SearchManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.loader.app.LoaderManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.ApplicationSettingsProvider
import org.ametro.app.Constants
import org.ametro.app.SavedRoute
import org.ametro.catalog.BackgroundUpdateCheck
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
import org.ametro.ui.bottom_panel.MapBottomPanelStation.MapBottomPanelStationListener
import org.ametro.ui.navigation.NavigationController
import org.ametro.ui.navigation.NavigationControllerListener
import org.ametro.ui.tasks.MapLoadAsyncTask
import org.ametro.ui.tasks.MapLoadAsyncTask.IMapLoadingEventListener
import org.ametro.ui.testing.DebugToast
import org.ametro.ui.testing.TestMenuOptionsProcessor
import org.ametro.ui.views.MultiTouchMapView
import org.ametro.ui.widgets.MapSelectionIndicatorsWidget
import org.ametro.ui.widgets.MapSelectionIndicatorsWidget.IMapSelectionEventListener
import org.ametro.utils.misc.ColorUtils
import org.ametro.utils.misc.convertPair
import java.util.*

class Map : AppCompatActivityEx(), IMapLoadingEventListener, NavigationControllerListener,
    IMapSelectionEventListener, MapBottomPanelStationListener, MapBottomPanelRoute.MapBottomPanelRouteListener,
    BottomPanelSheetListener, MultiTouchMapView.IViewportChangedListener {

    private var backPressTime = 0L
    private var backPressCount = 0

    private var enabledTransportsSet: MutableSet<String>? = null
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

    private lateinit var testMenuOptionsProcessor: TestMenuOptionsProcessor
    private lateinit var app: ApplicationEx
    private lateinit var settingsProvider: ApplicationSettingsProvider
    private lateinit var navigationController: NavigationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeEmptyMap.loadingProgress.apply {
            val color =
                ResourcesCompat.getColor(context.resources, R.color.map_loading_bar, null)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val wrapDrawable = DrawableCompat.wrap(indeterminateDrawable)
                DrawableCompat.setTint(wrapDrawable, color)
                indeterminateDrawable = DrawableCompat.unwrap(wrapDrawable)
            } else {
                indeterminateTintList = ColorStateList.valueOf(color)
            }
        }

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
            binding,
            TransportIconsProvider(this),
            ApplicationEx.getInstanceActivity(this).getCountryFlagProvider(),
            ApplicationEx.getInstanceActivity(this).getLocalizedMapInfoProvider()
        )

        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            setFitSystemWindowsFlags(binding.root)

            val toolbar = navigationController.toolbar
            val emptyView = binding.includeEmptyMap.mapEmptyPanel
            applyToolbarInsets(toolbar) {
                emptyView.updatePadding(bottom = toolbar.layoutParams.height)
            }
        }

        handleMapUpdate(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.also { handleMapUpdate(it) }
    }

    private fun handleMapUpdate(intent: Intent) {
        when (intent.getIntExtra(EXTRA_REQUEST, 0)) {
            REQUEST_MAPS -> openMaps(false)
            REQUEST_MAPS_UPDATE -> openMaps(true)
            else -> BackgroundUpdateCheck(this).also {
                it.initLoaders(LoaderManager.getInstance(this))
            }
        }
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (Build.VERSION.SDK_INT < Constants.INSETS_MIN_API)
            return
        window.statusBarColor = ColorUtils(0.4f * slideOffset, 0f, 0f, 0f).toColorInt()
        if (app.container == null) {
            if (slideOffset > 0.05f)
                setNavbarTransparent()
            else
                setNavbarSolidPrimary()
        }
    }

    fun mapEmptyNavbar(empty: Boolean) {
        if (Build.VERSION.SDK_INT < Constants.INSETS_MIN_API)
            return
        if (empty)
            setNavbarSolidPrimary()
        else
            setNavbarTransparent()
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
            mapView!!.invalidate()
        } else {
            app.clearCurrentMapViewState()
            settingsProvider.currentMap?.let {
                MapLoadAsyncTask(
                    this, this, MapContainer(it, settingsProvider.preferredMapLanguage)
                ).execute()
            }
        }

        navigationController.callDrawerSlideListener()
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

        val searchViewShowStation = { station: MapSchemeStation ->
            if (scheme != null) ModelUtil.findStationByUid(scheme, station.uid.toLong())?.let { stationInfo ->
                val mapView = mapView ?: return@let

                var newScale = mapView.scale
                val targetScale = density * 1.5f
                if (newScale < targetScale)
                    newScale = targetScale

                val stationInformation = container!!
                    .findStationInformation(stationInfo.first.name, stationInfo.second.name)
                val p = PointF(station.position.x, station.position.y)
                mapView.setCenterPositionAndScale(p, newScale, true)
                mapBottomStation.show(
                    stationInfo.first,
                    stationInfo.second,
                    stationInformation?.mapFilePath != null
                )
                searchMenuItem.collapseActionView()
            }
        }

        searchView.isSubmitButtonEnabled = false
        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val station = (searchView.suggestionsAdapter as StationSearchAdapter).getStation(position)
                // searchView.setQuery(station.displayName, false)
                searchViewShowStation(station)
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                var adapter = searchView.suggestionsAdapter as StationSearchAdapter
                if (adapter.query != query && scheme != null)
                    adapter = StationSearchAdapter.createFromMapScheme(this@Map, scheme, query)
                if (adapter.stationCount > 0)
                    searchViewShowStation(adapter.getStation(0))
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
                val mapPath = data?.getStringExtra(Constants.MAP_PATH)
                if (mapPath != null) {
                    val localMapCatalogManager = app.getLocalMapCatalogManager()
                    val map = localMapCatalogManager.findMapByName(mapPath)
                    val mapFile = localMapCatalogManager.getMapFile(map)
                    val mapContainer = MapContainer(mapFile, settingsProvider.preferredMapLanguage)
                    MapLoadAsyncTask(this, this, mapContainer).execute()
                } else {
                    onMapLoadComplete(null, null, null, 0)
                }
            }
            OPEN_STATION_DETAILS_ACTION -> {
                mapBottomStation.detailsClosed()
            }
            OPEN_SETTINGS_ACTION -> {
                if (resultCode == CONFIGURATION_CHANGED_RESULT) {
                    app.clearMapListCache()
                    app.clearContainer()
                    recreate()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun dismissLoadingBar() {
        showHideLoadingBar(false)
    }

    private fun showHideLoadingBar(show: Boolean) {
        val states = arrayOf(View.INVISIBLE, View.VISIBLE)
        if (show) states.reverse()

        binding.includeEmptyMap.apply {
            arrayOf(loadingProgress, loadingText)
                .forEach { it.visibility = states[0] }
            arrayOf(textHelp1, textHelp2)
                .forEach { it.visibility = states[1] }
        }
    }

    override fun onBeforeMapLoading(container: MapContainer, schemeName: String, enabledTransports: Array<String>?) {
        app.viewportInitialized = false
        if (!container.isLoaded(schemeName, enabledTransports)) {
            mapView?.visibility = View.GONE
            showHideLoadingBar(true)
        }
    }

    override fun onMapLoadComplete(
        container: MapContainer?,
        schemeName: String?,
        enabledTransports: Array<String>?,
        time: Long
    ) {
        if (container != null && schemeName != null) {
            DebugToast.show(this, getString(R.string.msg_map_loaded, time.toString()), Toast.LENGTH_LONG)
            app.setCurrentMapViewState(container, schemeName, enabledTransports)
            settingsProvider.currentMap = container.mapFile
        } else {
            app.clearCurrentMapViewState()
            settingsProvider.currentMap = null
        }
        initMapViewState()
        dismissLoadingBar()
    }

    private fun initMapViewState() {
        if (app.container == null || !app.container!!.mapFile.exists()) {
            app.clearCurrentMapViewState()
            navigationController.setNavigation(null, null, null, null)
            mapPanelView.visibility = View.GONE
            mapEmptyNavbar(true)
            return
        }
        mapEmptyNavbar(false)
        container = app.container
        schemeName = app.schemeName
        scheme = container!!.getScheme(schemeName)
        enabledTransportsSet = HashSet(
            listOf(
                *app.enabledTransports ?: container!!.getScheme(schemeName).defaultTransports
            )
        )
        currentDelay = app.delay
        navigationController.setNavigation(container, schemeName, app.enabledTransports, currentDelay)
        mapPanelView.visibility = View.VISIBLE
        mapSelectionIndicators.clearSelection()
        mapView = MultiTouchMapView(this, container, schemeName, arrayOf(mapSelectionIndicators, this))
        mapView!!.viewportInitialized = app.viewportInitialized
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
        dismissLoadingBar()
        Toast.makeText(this, getString(R.string.msg_map_loading_failed, reason.message), Toast.LENGTH_LONG).show()
        Log.e(Constants.LOG, "Map load failed due exception: " + reason.message, reason)
    }

    private fun openMaps(updateNow: Boolean): Boolean {
        mapBottomStation.hide()
        val i = Intent(this, MapList::class.java)
            .also { it.putExtra(MapList.EXTRA_UPDATE_NOW, updateNow) }
        startActivityForResult(i, OPEN_MAPS_ACTION)
        waitingForActivityResult = true
        return true
    }

    override fun onOpenMaps(): Boolean {
        return openMaps(false)
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
            val transportsArray: Array<String>
            if (checked) {
                enabledTransportsSet!!.add(transportName)
                transportsArray = enabledTransportsSet!!.toTypedArray()
                container!!.loadSchemeWithTransports(schemeName, transportsArray)
            } else {
                enabledTransportsSet!!.remove(transportName)
                transportsArray = enabledTransportsSet!!.toTypedArray()
            }
            app.enabledTransports = transportsArray
            refreshRoute()
            true
        } catch (e: MapSerializationException) {
            false
        }
    }

    override fun onDelayChanged(delay: MapDelay): Boolean {
        currentDelay = delay
        app.delay = delay
        refreshRoute()
        return true
    }

    override fun onPanelHidden() {
        mapSelectionIndicators.clearSelection()
    }

    override fun onOpenDetails(station: Pair<MapSchemeLine, MapSchemeStation>) {
        onShowMapDetail(station.first, station.second)
    }

    override fun onOpenTransports() {
        navigationController.also {
            val pos = it.drawerMenuAdapter.getPositionByTag("transports")
            it.drawerLayout.openDrawer(it.drawerView)
            if (pos >= 0) it.drawerView.smoothScrollToPosition(pos)
        }
    }

    override fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?) {
        if (waitingForActivityResult || station == null || line == null) return
        waitingForActivityResult = true
        val intent = Intent(this, StationDetails::class.java)
        intent.putExtra(Constants.LINE_NAME, line.name)
        intent.putExtra(Constants.STATION_NAME, station.name)
        intent.putExtra(Constants.STATION_UID, station.uid)
        startActivityForResult(intent, OPEN_STATION_DETAILS_ACTION)
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
        mapView!!.highlightsElements {
            RouteUtils.convertRouteToSchemeObjectIds(route, scheme!!)
        }
    }

    fun refreshRoute() {
        val begin = app.currentRoute.routeStart
        val end = app.currentRoute.routeEnd
        if (begin != null && end != null)
            onRouteSelectionComplete(begin, end)
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

        val routes = MapRouteProvider.findRoutes(routeParams, settingsProvider.maxRoutes)

        if (routes.isEmpty()) {
            mapView!!.highlightsElements(null)
            mapBottomRoute.showNoRoute(begin, end)
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
                if (station != null)
                    txfs.add(RoutePagerTransfer(station.first, lastPartsCount, lastPartsDelays.toInt()))

                lastTxf = p
                lastPartsCount = 0
                lastPartsDelays = 0
            }

            if (lastTxf != null) {
                val lastStation = ModelUtil.findStationByUid(scheme!!, lastTxf.to.toLong())
                if (lastStation != null)
                    txfs.add(RoutePagerTransfer(lastStation.first, lastPartsCount, lastPartsDelays.toInt()))
            }

            RoutePagerItem(
                delay = it.delay,
                routeStart = begin,
                routeEnd = end,
                transfers = txfs
            )
        }

        var initRoute =
            app.currentRoute.selectedRoute.let { if (it > 0) it else 0 }
        if (initRoute >= routes.size)
            initRoute = routes.size - 1
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

    override fun onViewportChanged(matrix: Matrix) {}

    override fun onViewportInitialized() {
        app.viewportInitialized = mapView?.viewportInitialized ?: false
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
        private const val OPEN_STATION_DETAILS_ACTION = 3

        const val EXTRA_REQUEST = "request"
        const val REQUEST_MAPS = 1
        const val REQUEST_MAPS_UPDATE = 2

        const val CONFIGURATION_CHANGED_RESULT = 2
    }
}