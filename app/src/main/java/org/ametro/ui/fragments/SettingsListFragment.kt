package org.ametro.ui.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.preference.*
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.ApplicationSettingsProvider.Companion.SettingsItem
import org.ametro.ui.activities.About
import org.ametro.ui.activities.Map
import org.ametro.ui.activities.SettingsList
import org.ametro.utils.misc.getAppVersion
import org.ametro.app.ApplicationSettingsProvider as Provider

class AppLanguagePreference(context: Context) : DialogPreference(context)
class MapLanguagePreference(context: Context) : DialogPreference(context)

class SettingsListFragment : PreferenceFragmentCompat() {
    companion object {
        private const val EXTRA_PENDING_RESULT = "pending_result"
    }

    private lateinit var settings: org.ametro.app.ApplicationSettingsProvider
    private var pendingResult: Int = 0

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_PENDING_RESULT, pendingResult)
        super.onSaveInstanceState(outState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        pendingResult = savedInstanceState?.getInt(EXTRA_PENDING_RESULT, 0) ?: 0
        if (pendingResult != 0)
            requireActivity().setResult(pendingResult)

        val context = preferenceManager.context
        val resources = context.applicationContext.resources
        settings = ApplicationEx.getInstanceContext(context.applicationContext)!!.applicationSettingsProvider

        preferenceManager.sharedPreferencesName = Provider.USER_PREFS_NAME
        val screen = preferenceManager.createPreferenceScreen(context)

        AppLanguagePreference(context).also {
            it.key = "dialog_app_lang"
            it.title = resources.getString(R.string.settings_app_lang)
            it.summary = settings.preferredAppLanguage
                ?: resources.getString(R.string.settings_app_lang_default)
            screen.addPreference(it)
        }

        MapLanguagePreference(context).also {
            it.key = "dialog_map_lang"
            it.title = resources.getString(R.string.settings_map_lang)
            it.summary = settings.preferredMapLangSetting
                ?: resources.getString(R.string.settings_map_lang_default)
            screen.addPreference(it)
        }

        createSeekbar(screen, resources, Provider.USER_MAX_ROUTES, R.string.settings_max_routes, 1, 5)
        createSeekbar(screen, resources, Provider.USER_DT_SENSITIVITY, R.string.settings_dt_zoom_sensitivity, 1, 10)

        Preference(context).also {
            it.key = "about"
            it.title = resources.getString(R.string.settings_about)
            it.summary = getAppVersion(context)
            it.setOnPreferenceClickListener {
                startActivity(Intent(context, About::class.java))
                true
            }
            screen.addPreference(it)
        }

        preferenceScreen = screen
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is AppLanguagePreference -> {
                val listener = object : LanguageDialogListener {
                    override fun onResult(selectedItem: LanguageListItem?, forceDefault: Boolean, confirmed: Boolean) {
                        if ((confirmed && selectedItem != null) || forceDefault) {
                            settings.preferredAppLanguage = selectedItem?.code
                            pendingResult = Map.CONFIGURATION_CHANGED_RESULT
                            requireActivity().recreate()
                        }
                    }
                }

                val dialogFragment = LanguageDialogFragment(settings.availableLanguagesApp, listener)
                dialogFragment.show(parentFragmentManager, null)
            }
            is MapLanguagePreference -> {
                val listener = object : LanguageDialogListener {
                    override fun onResult(selectedItem: LanguageListItem?, forceDefault: Boolean, confirmed: Boolean) {
                        if ((confirmed && selectedItem != null) || forceDefault) {
                            settings.preferredMapLangSetting = selectedItem?.code
                            pendingResult = Map.CONFIGURATION_CHANGED_RESULT
                            requireActivity().recreate()
                        }
                    }
                }

                val dialogFragment = LanguageDialogFragment(settings.availableLanguagesMap, listener)
                dialogFragment.show(parentFragmentManager, null)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun createSeekbar(
        screen: PreferenceScreen,
        res: Resources,
        pref: SettingsItem<Int>,
        @StringRes titleRes: Int,
        min: Int,
        max: Int
    ) =
        SeekBarPreference(screen.context).also {
            it.key = pref.key
            it.title = res.getString(titleRes)
            it.setDefaultValue(pref.default)
            it.min = min
            it.max = max
            it.showSeekBarValue = true
            screen.addPreference(it)
        }
}