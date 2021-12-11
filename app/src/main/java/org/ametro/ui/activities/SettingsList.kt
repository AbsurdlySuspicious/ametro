package org.ametro.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import org.ametro.R
import androidx.core.app.NavUtils
import org.ametro.databinding.ActivitySettingsListViewBinding
import org.ametro.ui.fragments.SettingsListFragment

class SettingsList : AppCompatActivity() {
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