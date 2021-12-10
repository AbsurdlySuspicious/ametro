package org.ametro.catalog.service

import org.ametro.utils.FileUtils
import java.io.IOException
import java.net.URI
import java.net.URL

class ServiceTransport : IServiceTransport {
    @Throws(IOException::class)
    override fun httpGet(uri: URI): String {
        val url = URL(uri.toASCIIString())
        return FileUtils.readAllText(url.openStream())
    }
}