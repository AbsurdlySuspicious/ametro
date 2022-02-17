package org.ametro.app

import org.ametro.providers.IconProvider
import org.ametro.catalog.RemoteMapCatalogProvider
import org.ametro.catalog.service.IMapServiceCache
import org.ametro.catalog.MapCatalogManager
import org.ametro.catalog.localization.MapInfoLocalizationProvider
import org.ametro.model.MapContainer
import android.graphics.PointF
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import androidx.core.content.ContextCompat
import org.ametro.R
import org.ametro.catalog.service.MapServiceCache
import org.ametro.catalog.service.ServiceTransport
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.format.DateFormat
import androidx.loader.content.AsyncTaskLoader
import androidx.multidex.MultiDexApplication
import kotlinx.parcelize.Parcelize
import org.ametro.model.entities.MapDelay
import org.ametro.utils.Lazy
import java.util.*

@Parcelize
data class SavedRoute(
    var routeStart: Pair<MapSchemeLine, MapSchemeStation>? = null,
    var routeEnd: Pair<MapSchemeLine, MapSchemeStation>? = null,
    var selectedRoute: Int = -1
): Parcelable {
    val isUnset: Boolean
        get() = routeStart == null && routeEnd == null
}

@Parcelize
data class SavedState(
    val enabledTransports: MutableList<String>?,
    val posCenter: Pair<PointF, Float>?,
    val route: SavedRoute,
    val bottomPanelOpen: Boolean,
    val bottomPanelStation: Pair<MapSchemeLine, MapSchemeStation>?
): Parcelable

class ApplicationEx : MultiDexApplication() {

    val is24HourTime by lazy { DateFormat.is24HourFormat(this) }

    private var appSettingsProvider: Lazy<ApplicationSettingsProvider>? = null
    private var countryFlagProvider: Lazy<IconProvider>? = null
    private var remoteMapCatalogProvider: Lazy<RemoteMapCatalogProvider>? = null
    private var mapServiceCache: Lazy<IMapServiceCache>? = null
    private var localMapCatalogManager: Lazy<MapCatalogManager>? = null
    private var localizedMapInfoProvider: Lazy<MapInfoLocalizationProvider>? = null
    private var isNew: Boolean = false
    private val bundleKey: String = "application-ex-state"
    var container: MapContainer? = null
        private set
    var schemeName: String? = null
        private set
    var enabledTransports: Array<String>? = null
    var delay: MapDelay? = null
    var centerPositionAndScale: Pair<PointF, Float>? = null

    var currentRoute = SavedRoute()
        private set
    var previousRoute: SavedRoute? = null
        private set

    var bottomPanelOpen: Boolean = false
    var bottomPanelStation: Pair<MapSchemeLine, MapSchemeStation>? = null

    // todo save (all of above) to instance state too

    var lastLeaveTime: Calendar? = null

    fun checkIsNew(): Boolean {
        return isNew.also { isNew = false }
    }

    fun saveState(savedInstanceState: Bundle) {
        val state = SavedState(
            enabledTransports = enabledTransports?.toMutableList(),
            posCenter = centerPositionAndScale,
            route = currentRoute,
            bottomPanelOpen = bottomPanelOpen,
            bottomPanelStation = bottomPanelStation,
        )
        savedInstanceState.putParcelable(bundleKey, state)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        val state = savedInstanceState?.getParcelable<SavedState>(bundleKey) ?: return
        enabledTransports = state.enabledTransports?.toTypedArray()
        centerPositionAndScale = state.posCenter
        currentRoute = state.route
        bottomPanelStation = state.bottomPanelStation
        bottomPanelOpen = state.bottomPanelOpen
    }

    override fun onCreate() {
        super.onCreate()
        appSettingsProvider = Lazy {
            ApplicationSettingsProvider(this@ApplicationEx)
        }
        countryFlagProvider = Lazy {
            IconProvider(
                this@ApplicationEx,
                ContextCompat.getDrawable(applicationContext, R.drawable.no_country),
                "country_icons"
            )
        }
        mapServiceCache = Lazy {
            MapServiceCache(
                ServiceTransport(),
                Constants.MAP_SERVICE_URI,
                filesDir,
                applicationSettingsProvider.preferredMapLanguage,
                Constants.MAP_EXPIRATION_PERIOD_MILLISECONDS
            )
        }
        localizedMapInfoProvider = Lazy {
            MapInfoLocalizationProvider(mapServiceCache!!.instance)
        }
        remoteMapCatalogProvider = Lazy {
            RemoteMapCatalogProvider(
                Constants.MAP_SERVICE_URI,
                mapServiceCache!!.instance,
                localizedMapInfoProvider!!.instance
            )
        }
        localMapCatalogManager = Lazy {
            MapCatalogManager(filesDir, getLocalizedMapInfoProvider())
        }
    }

    fun getLocalizedMapInfoProvider(): MapInfoLocalizationProvider {
        return localizedMapInfoProvider!!.instance
    }

    val applicationSettingsProvider: ApplicationSettingsProvider
        get() = appSettingsProvider!!.instance

    fun getCountryFlagProvider(): IconProvider {
        return countryFlagProvider!!.instance
    }

    fun getRemoteMapCatalogProvider(): RemoteMapCatalogProvider {
        return remoteMapCatalogProvider!!.instance
    }

    fun getLocalMapCatalogManager(): MapCatalogManager {
        return localMapCatalogManager!!.instance
    }

    fun clearMapListCache() {
        mapServiceCache!!.clearInstance()
        localizedMapInfoProvider!!.clearInstance()
        remoteMapCatalogProvider!!.clearInstance()
        localMapCatalogManager!!.clearInstance()
    }

    fun setCurrentMapViewState(container: MapContainer?, schemeName: String?, enabledTransports: Array<String>?) {
        clearCurrentMapViewState()
        this.container = container
        this.schemeName = schemeName
        this.enabledTransports = enabledTransports
    }

    fun clearContainer() {
        this.container = null
    }

    fun clearCurrentMapViewState() {
        container = null
        schemeName = null
        enabledTransports = null
        delay = null
        centerPositionAndScale = null
        currentRoute = SavedRoute()
        previousRoute = null
        lastLeaveTime = null
    }

    fun clearRoute() {
        lastLeaveTime = null
        if (currentRoute.isUnset) return
        previousRoute = currentRoute
        currentRoute = SavedRoute()
    }

    fun restorePrevRoute(): Boolean {
        currentRoute = previousRoute ?: return false
        previousRoute = null
        if (currentRoute.isUnset) return false
        return true
    }

    fun resetSelectedRoute() {
        currentRoute.selectedRoute = -1
    }

    fun setRouteStart(line: MapSchemeLine, station: MapSchemeStation) {
        currentRoute.routeStart = Pair(line, station)
    }

    fun setRouteEnd(line: MapSchemeLine, station: MapSchemeStation) {
        currentRoute.routeEnd = Pair(line, station)
    }

    companion object {
        @JvmStatic
        fun getInstanceActivity(activity: Activity): ApplicationEx {
            return activity.application as ApplicationEx
        }

        @JvmStatic
        fun getInstanceContext(applicationContext: Context?): ApplicationEx? {
            return applicationContext as ApplicationEx?
        }

        @JvmStatic
        fun getInstanceLoader(loader: AsyncTaskLoader<*>): ApplicationEx? {
            return getInstanceContext(loader.context.applicationContext)
        }
    }
}