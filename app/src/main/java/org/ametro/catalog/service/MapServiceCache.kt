package org.ametro.catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import org.ametro.model.serialization.CommonTypes
import org.ametro.utils.FileUtils
import java.io.File
import java.io.IOException
import java.net.URI

class MapServiceCache(
    private val serviceTransport: IServiceTransport,
    private val serviceUrl: URI,
    workingDirectory: File?,
    private val languageCode: String,
    private val ticksToExpiration: Long
) : IMapServiceCache {
    private val cacheDirectory: File = File(workingDirectory, "cache")

    @Throws(ServiceUnavailableException::class, IOException::class)
    override fun refreshCache() {
        downloadFile("locales/locales.json", "locales.json")
        val json = reader.readTree(FileUtils.readAllText(File(cacheDirectory, "locales.json")))
        for (code in CommonTypes.asStringArray(json)) {
            val fileName = "cities.$code.json"
            downloadFile("locales/$fileName", fileName)
        }
        downloadFile("locales/cities.default.json", "cities.default.json")
        downloadFile("index.json", "index.json")
    }

    override fun hasValidCache(): Boolean {
        return mapCatalogFile.exists() &&
                System.currentTimeMillis() - mapCatalogFile.lastModified() < ticksToExpiration
    }

    override fun getMapCatalogFile(): File {
        return File(cacheDirectory, "index.json")
    }

    override fun getLocalizationFile(): File {
        val languageFile = File(cacheDirectory, "cities.$languageCode.json")
        return if (languageFile.exists()) languageFile
        else File(cacheDirectory, "cities.default.json")
    }

    override fun invalidateCache() {
        if (mapCatalogFile.exists()) {
            FileUtils.safeDelete(mapCatalogFile)
        }
        if (localizationFile.exists()) {
            FileUtils.safeDelete(localizationFile)
        }
    }

    @Throws(ServiceUnavailableException::class, IOException::class)
    private fun downloadFile(remoteFileName: String, localFileName: String) {
        val fileToStore = File(cacheDirectory, localFileName)
        if (!fileToStore.parentFile.exists()) {
            if (!fileToStore.parentFile.mkdirs()) {
                throw IOException("Cannot create directory " + fileToStore.parentFile)
            }
        }
        val content = try {
            serviceTransport.httpGet(serviceUrl.resolve(remoteFileName))
        } catch (ex: Exception) {
            throw ServiceUnavailableException(ex)
        }
        FileUtils.writeAllText(fileToStore, content)
    }

    companion object {
        private val reader: ObjectReader = ObjectMapper().reader()
    }
}