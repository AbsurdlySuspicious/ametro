package org.ametro.app

import android.app.Application
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
import androidx.loader.content.AsyncTaskLoader
import android.util.Pair
import androidx.multidex.MultiDexApplication
import org.ametro.utils.Lazy

class ApplicationEx : MultiDexApplication() {
    private var appSettingsProvider: Lazy<ApplicationSettingsProvider>? = null
    private var countryFlagProvider: Lazy<IconProvider>? = null
    private var remoteMapCatalogProvider: Lazy<RemoteMapCatalogProvider>? = null
    private var mapServiceCache: Lazy<IMapServiceCache>? = null
    private var localMapCatalogManager: Lazy<MapCatalogManager>? = null
    private var localizedMapInfoProvider: Lazy<MapInfoLocalizationProvider>? = null
    var container: MapContainer? = null
        private set
    var schemeName: String? = null
        private set
    var enabledTransports: Array<String>? = null
        private set
    var centerPositionAndScale: Pair<PointF, Float>? = null
    var routeStart: Pair<MapSchemeLine, MapSchemeStation>? = null
        private set
    var routeEnd: Pair<MapSchemeLine, MapSchemeStation>? = null
        private set

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
                applicationSettingsProvider.defaultLanguage,
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

    fun setCurrentMapViewState(container: MapContainer?, schemeName: String?, enabledTransports: Array<String>?) {
        clearCurrentMapViewState()
        this.container = container
        this.schemeName = schemeName
        this.enabledTransports = enabledTransports
    }

    fun clearCurrentMapViewState() {
        container = null
        schemeName = null
        enabledTransports = null
        centerPositionAndScale = null
        clearRoute()
    }

    fun clearRoute() {
        routeStart = null
        routeEnd = null
    }

    fun setRouteStart(line: MapSchemeLine, station: MapSchemeStation) {
        routeStart = Pair(line, station)
    }

    fun setRouteEnd(line: MapSchemeLine, station: MapSchemeStation) {
        routeEnd = Pair(line, station)
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