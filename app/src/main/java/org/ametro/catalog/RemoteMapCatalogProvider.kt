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
    private val downloadAttempts = 4

    fun getMapCatalog(forceUpdate: Boolean): MapCatalog? {
        if (!forceUpdate && cache.hasValidCache()) {
            return loadCatalog(false)
        }
        for (a in 1..downloadAttempts) {
            try {
                cache.refreshCache()
                break
            } catch (ex: Exception) {
                Log.e(
                    Constants.LOG,
                    "Cannot refresh remote map catalog cache: attempt $a of $downloadAttempts, " +
                            "forceUpdate $forceUpdate, exception $ex"
                )
            }
        }
        return loadCatalog(true)
    }

    private fun loadCatalog(forceLocal: Boolean): MapCatalog? {
        return try {
            localizationProvider.createCatalog(
                deserializeMapInfoArray(
                    FileUtils.readAllText(cache.mapCatalogFile)
                )
            )
        } catch (ex: Exception) {
            Log.e(Constants.LOG, "Cannot read remote map catalog cache: forceLocal $forceLocal, exception $ex")
            cache.invalidateCache()
            if (forceLocal) null
            else getMapCatalog(true)
        }
    }

    fun getMapFileUrl(map: MapInfo): URI {
        return serviceUri.resolve(map.fileName)
    }
}