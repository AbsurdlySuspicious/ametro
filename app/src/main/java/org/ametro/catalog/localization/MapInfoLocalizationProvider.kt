package org.ametro.catalog.localization

import org.ametro.catalog.service.IMapServiceCache
import org.ametro.catalog.entities.MapInfoEntityName
import org.ametro.catalog.entities.MapInfoEntity
import org.ametro.catalog.entities.MapCatalog
import org.ametro.catalog.entities.MapInfo
import org.ametro.catalog.serialization.MapCatalogSerializer
import org.ametro.catalog.serialization.SerializationException
import org.ametro.utils.FileUtils
import java.io.IOException
import java.lang.RuntimeException
import java.util.HashMap

class MapInfoLocalizationProvider(private val cache: IMapServiceCache) {
    private val localizationMap: HashMap<Int, MapInfoEntityName> by lazy {
        try {
            val newLocalizationMap = HashMap<Int, MapInfoEntityName>()
            val entities = MapCatalogSerializer.deserializeLocalization(
                FileUtils.readAllText(cache.localizationFile)
            )
            for (entity in entities) {
                newLocalizationMap[entity.cityId] = entity
            }
            newLocalizationMap
        } catch (ex: SerializationException) {
            throw RuntimeException("Localization data has an invalid format", ex)
        } catch (ex: IOException) {
            throw RuntimeException("Localization cannot be read", ex)
        }
    }

    fun getEntity(cityId: Int): MapInfoEntityName {
        return localizationMap[cityId]!!
    }

    fun getCityName(cityId: Int): String {
        return getEntity(cityId).cityName
    }

    fun getCountryName(cityId: Int): String {
        return getEntity(cityId).countryName
    }

    fun getCountryIsoCode(cityId: Int): String {
        return getEntity(cityId).countryIsoCode
    }

    fun createCatalog(maps: Array<MapInfoEntity>): MapCatalog {
        val localizations = localizationMap
        val localizedMaps = arrayOfNulls<MapInfo>(maps.size)
        for ((i, map) in maps.withIndex()) {
            localizedMaps[i] = localizations[map.cityId]!!.run {
                MapInfo(map, cityName, countryName, countryIsoCode)
            }
        }
        return MapCatalog(localizedMaps)
    }
}