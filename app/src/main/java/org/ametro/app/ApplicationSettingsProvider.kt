package org.ametro.app

import android.content.Context
import android.content.SharedPreferences
import org.ametro.ui.fragments.LanguageListItem
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ApplicationSettingsProvider(context: Context) {
    private val autoSettings = context.getSharedPreferences(AUTO_PREFS_NAME, Context.MODE_PRIVATE)
    private val userSettings = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)

    val availableLanguagesMap: List<LanguageListItem>
    val availableLanguagesApp: List<LanguageListItem>

    init {
        val localeMapper = { it: Locale ->
            LanguageListItem(it.displayName, it.language.lowercase(), it.getDisplayName(it))
        }

        ArrayList<LanguageListItem>().also { dest ->
            Locale.getAvailableLocales().mapTo(dest, localeMapper)
            availableLanguagesMap = dest
        }

        ArrayList<LanguageListItem>().also { dest ->
            arrayOf("en", "ru", "fr", "nl").mapTo(dest) { localeMapper(Locale(it)) }
            availableLanguagesApp = dest
        }
    }

    var currentMap: File?
        get() {
            val mapFilePath = AUTO_SELECTED_MAP.getParam(autoSettings) ?: return null
            val mapFile = File(mapFilePath)
            if (!mapFile.exists()) {
                currentMap = null
                return null
            }
            return mapFile
        }
        set(mapFile) {
            editTransaction(autoSettings) {
                AUTO_SELECTED_MAP.setParam(it, mapFile?.absolutePath)
            }
        }

    val defaultLanguage: String
        get() = Locale.getDefault().language.lowercase(Locale.getDefault())

    var preferredAppLanguage: String?
        get() = USER_APP_LANGUAGE.getParam(userSettings)
        set(lang) {
            editTransaction(userSettings) {
                USER_APP_LANGUAGE.setParam(it, lang)
            }
        }

    val preferredMapLanguage: String
        get() = preferredMapLangSetting ?: preferredAppLanguage ?: defaultLanguage

    var preferredMapLangSetting: String?
        get() = USER_MAP_LANGUAGE.getParam(userSettings)
        set(lang) {
            editTransaction(userSettings) {
                USER_MAP_LANGUAGE.setParam(it, lang)
            }
        }

    val maxRoutes: Int
        get() = USER_MAX_ROUTES.getParam(userSettings)

    val dtZoomSensitivityRaw: Int
        get() = USER_DT_SENSITIVITY.getParam(userSettings)

    val dtZoomSensitivityF: Float
        get() {
            val default = USER_DT_SENSITIVITY.default
            val raw = dtZoomSensitivityRaw
            return 1f / default * (default * 2 + 1 - raw)
        }

    private fun editTransaction(prefs: SharedPreferences,
                                edit: (SharedPreferences.Editor) -> Unit) {
        prefs.edit().also {
            edit(it)
            it.apply()
        }
    }

    companion object {
        data class SettingsItem<T>(
            val key: String,
            val default: T,
            val getF: (SharedPreferences) -> (String, T) -> T,
            val setF: (SharedPreferences.Editor) -> (String, T) -> Unit
        ) {
            fun getParam(prefs: SharedPreferences): T = getF(prefs)(key, default)
            fun setParam(editor: SharedPreferences.Editor, value: T): Unit = setF(editor)(key, value)
        }

        const val AUTO_PREFS_NAME = "aMetroPreferences"
        const val USER_PREFS_NAME = "settings"

        val AUTO_SELECTED_MAP = SettingsItem("selectedMap", null, {it::getString}, {it::putString})
        val USER_MAP_LANGUAGE = SettingsItem("map_language", null, {it::getString}, {it::putString})
        val USER_APP_LANGUAGE = SettingsItem("app_language", null, {it::getString}, {it::putString})
        val USER_MAX_ROUTES = SettingsItem("max_routes", 5, {it::getInt}, {it::putInt})
        val USER_DT_SENSITIVITY = SettingsItem("dt_zoom_sensitivity", 5, {it::getInt}, {it::putInt})
    }
}