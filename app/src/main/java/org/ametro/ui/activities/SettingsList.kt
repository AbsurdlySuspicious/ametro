package org.ametro.ui.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import org.ametro.R
import androidx.core.app.NavUtils
import org.ametro.app.Constants
import org.ametro.databinding.ActivitySettingsListViewBinding
import org.ametro.ui.fragments.SettingsListFragment
import org.ametro.utils.ui.*

class SettingsList : AppCompatActivityEx() {
    private lateinit var binding: ActivitySettingsListViewBinding

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsListViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includeToolbar.toolbar)

        supportActionBar?.run {
            setDefaultDisplayHomeAsUpEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            setFitSystemWindowsFlags(binding.root, keepNavbar = true)
            setNavbarSolid()
            applyToolbarInsets(binding.includeToolbar.toolbar)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.list, SettingsListFragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}