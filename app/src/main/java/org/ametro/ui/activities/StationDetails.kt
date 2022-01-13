package org.ametro.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import org.ametro.ui.views.SlidingTabLayout.TabColorizer
import android.os.Bundle
import org.ametro.R
import org.ametro.ui.toolbar.FragmentPagerArrayAdapter
import org.ametro.app.ApplicationEx
import org.ametro.ui.toolbar.FragmentPagerTabInfo
import org.ametro.ui.fragments.StationMapFragment
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.ametro.app.Constants
import org.ametro.databinding.ActivityStationDetailsViewBinding
import org.ametro.model.entities.MapStationInformation
import java.util.ArrayList

class StationDetails : AppCompatActivityEx(), TabColorizer {
    private lateinit var binding: ActivityStationDetailsViewBinding

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStationDetailsViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includeToolbar.toolbar)

        val lineName = intent.getStringExtra(Constants.LINE_NAME)
        val stationName = intent.getStringExtra(Constants.STATION_NAME)

        val app = ApplicationEx.getInstanceActivity(this)
        val stationScheme = app.container!!.findSchemeStation(app.schemeName, lineName, stationName)
        val stationInfo = app.container!!.findStationInformation(lineName, stationName)

        if (stationScheme == null || stationInfo == null || stationInfo.mapFilePath == null)
            return finish() // todo check other info (about) when implemented

        supportActionBar?.run {
            setDefaultDisplayHomeAsUpEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = stationScheme.displayName
        }

        val viewPager = binding.viewpager
        viewPager.adapter = FragmentPagerArrayAdapter(supportFragmentManager, genTabs(stationInfo))

        binding.slidingTabs.run {
            setDistributeEvenly(true)
            setViewPager(viewPager)
            setCustomTabColorizer(this@StationDetails)
        }
    }

    private fun genTabs(station: MapStationInformation): ArrayList<FragmentPagerTabInfo> {
        val tabs = ArrayList<FragmentPagerTabInfo>()
        val mapFragment: Fragment = StationMapFragment(station)
        tabs.add(FragmentPagerTabInfo(getString(R.string.tab_map), mapFragment))

        // todo investigate ("about station" tab in details/info)
        // Fragment aboutFragment = new StationAboutFragment();
        // aboutFragment.setArguments(getIntent().getExtras());
        // tabs.add(new FragmentPagerTabInfo(getString(R.string.tab_about), aboutFragment));
        return tabs
    }

    override fun getIndicatorColor(position: Int): Int {
        return ContextCompat.getColor(this, R.color.accent)
    }
}