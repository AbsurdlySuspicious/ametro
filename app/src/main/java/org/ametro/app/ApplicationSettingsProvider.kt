package org.ametro.app

import android.content.Context
import android.content.SharedPreferences
import org.ametro.app.ApplicationSettingsProvider
import java.io.File
import java.util.*

class ApplicationSettingsProvider(context: Context) {
    private val settings: SharedPreferences

    init {
        settings = context.getSharedPreferences(PREFS_NAME, 0)
    }

    var currentMap: File?
        get() {
            val mapFilePath = settings.getString(SELECTED_MAP, null) ?: return null
            val mapFile = File(mapFilePath)
            if (!mapFile.exists()) {
                currentMap = null
                return null
            }
            return mapFile
        }
        set(mapFile) {
            val editor = settings.edit()
            editor.putString(SELECTED_MAP, mapFile?.absolutePath)
            editor.apply()
        }
    val defaultLanguage: String
        get() = Locale.getDefault().language.lowercase(Locale.getDefault())
    val preferredMapLanguage: String
        get() {
            val languageCode = settings.getString(PREFERRED_LANGUAGE, null)
            return languageCode ?: defaultLanguage
        }

    companion object {
        private const val PREFS_NAME = "aMetroPreferences"
        private const val SELECTED_MAP = "selectedMap"
        private const val PREFERRED_LANGUAGE = "preferredLanguage"
    }
}