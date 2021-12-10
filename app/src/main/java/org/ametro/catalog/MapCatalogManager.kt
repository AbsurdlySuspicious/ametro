package org.ametro.catalog

import android.content.res.Resources.NotFoundException
import android.util.Log
import org.ametro.app.Constants
import org.ametro.catalog.entities.MapCatalog
import org.ametro.catalog.entities.MapInfo
import org.ametro.catalog.localization.MapInfoLocalizationProvider
import org.ametro.catalog.serialization.MapCatalogSerializer.deserializeMapInfoArray
import org.ametro.catalog.serialization.MapCatalogSerializer.serializeMapInfoArray
import org.ametro.utils.FileUtils
import java.io.File
import java.io.IOException

class MapCatalogManager(
    private val workingDirectory: File,
    private val localizationProvider: MapInfoLocalizationProvider
) {
    private val file = File(workingDirectory, LOCAL_CATALOG_FILE)
    private var catalog: MapCatalog? = null

    val mapCatalog: MapCatalog
        get() {
            return catalog ?: (
                    if (!file.exists())
                        MapCatalog(emptyArray())
                    else
                        loadCatalog()
                    ).also { catalog = it }
        }

    fun addOrReplaceMapAll(newMaps: Array<MapInfo?>) {
        val maps: MutableList<MapInfo?> = arrayListOf(*mapCatalog.maps)
        val mapsSet = newMaps.toSet()
        maps.removeAll(mapsSet)
        maps.addAll(0, mapsSet)
        catalog = MapCatalog(maps.toTypedArray())
        storeCatalog()
    }

    fun deleteMapAll(deletingMaps: Array<MapInfo>) {
        val maps: MutableList<MapInfo> = arrayListOf(*mapCatalog.maps)
        maps.removeAll(deletingMaps.toSet())
        catalog = MapCatalog(maps.toTypedArray())
        storeCatalog()
        for (m in deletingMaps) {
            try {
                FileUtils.delete(getMapFile(m))
            } catch (e: IOException) {
                Log.e(Constants.LOG, "Cannot delete map " + m.fileName, e)
            }
        }
    }

    fun findMapByName(mapFileName: String?): MapInfo {
        if (mapFileName != null) for (map in mapCatalog.maps) {
            if (map.fileName == mapFileName) {
                return map
            }
        }
        throw NotFoundException("Not found map file $mapFileName")
    }

    private fun loadCatalog(): MapCatalog {
        return try {
            localizationProvider.createCatalog(
                deserializeMapInfoArray(FileUtils.readAllText(file))
            )
        } catch (ex: Exception) {
            Log.e(Constants.LOG, String.format("Cannot read map catalog due exception: %s", ex.toString()))
            FileUtils.safeDelete(file)
            MapCatalog(emptyArray())
        }
    }

    private fun storeCatalog() {
        try {
            FileUtils.writeAllText(file, serializeMapInfoArray(catalog!!.maps))
        } catch (ex: Exception) {
            Log.e(Constants.LOG, String.format("Cannot store map catalog due exception: %s", ex.toString()))
        }
    }

    fun getMapFile(map: MapInfo): File {
        return File(workingDirectory, map.fileName)
    }

    fun getTempMapFile(map: MapInfo): File {
        return File(workingDirectory, map.fileName + ".temporary")
    }

    companion object {
        private const val LOCAL_CATALOG_FILE = "local-maps-catalog.json"
    }
}