package org.ametro.app

import android.os.Build
import java.net.URI

object Constants {
    @JvmField
    val MAP_SERVICE_URI: URI = URI.create("https://absurdlysuspicious.github.io/ametro-services/repo/")
    const val MAP_EXPIRATION_PERIOD_MILLISECONDS = (1000 * 60 * 5).toLong() // 5 min
    const val LINE_NAME = "LINE_NAME"
    const val MAP_CITY = "MAP_CITY"
    const val MAP_COUNTRY = "MAP_COUNTRY"
    const val MAP_PATH = "MAP_PATH"
    const val STATION_NAME = "STATION_NAME"
    const val STATION_UID = "STATION_UID"
    const val LOG = "AMETRO"
    const val ANIMATION_DURATION: Long = 100
    const val INSETS_MIN_API = Build.VERSION_CODES.LOLLIPOP
}