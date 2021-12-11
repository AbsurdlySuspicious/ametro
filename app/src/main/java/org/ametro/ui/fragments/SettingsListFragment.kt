package org.ametro.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.ametro.R

class SettingsListFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}