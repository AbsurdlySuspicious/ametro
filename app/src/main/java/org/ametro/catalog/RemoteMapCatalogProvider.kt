package org.ametro.catalog

import android.util.Log
import org.ametro.app.Constants
import org.ametro.catalog.entities.MapCatalog
import org.ametro.catalog.entities.MapInfo
import org.ametro.catalog.localization.MapInfoLocalizationProvider
import org.ametro.catalog.serialization.MapCatalogSerializer.deserializeMapInfoArray
import org.ametro.catalog.service.IMapServiceCache
import org.ametro.utils.FileUtils
import java.net.URI

class RemoteMapCatalogProvider(
    private val serviceUri: URI,
    private val cache: IMapServiceCache,
    private val localizationProvider: MapInfoLocalizationProvider
) {
    fun getMapCatalog(forceUpdate: Boolean): MapCatalog? {
        if (!forceUpdate && cache.hasValidCache()) {
            return loadCatalog()
        }
        try {
            cache.refreshCache()
        } catch (ex: Exception) {
            Log.e(
                Constants.LOG,
                String.format("Cannot refresh remote map catalog cache due exception: %s", ex.toString())
            )
        }
        return loadCatalog()
    }

    private fun loadCatalog(): MapCatalog? {
        return try {
            localizationProvider.createCatalog(
                deserializeMapInfoArray(
                    FileUtils.readAllText(cache.mapCatalogFile)
                )
            )
        } catch (ex: Exception) {
            Log.e(Constants.LOG, String.format("Cannot read remote map catalog cache due exception: %s", ex.toString()))
            cache.invalidateCache()
            null
        }
    }

    fun getMapFileUrl(map: MapInfo): URI {
        return serviceUri.resolve(map.fileName)
    }
}