package org.ametro.ui.navigation

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.ametro.R
import org.ametro.catalog.entities.TransportTypeHelper
import org.ametro.catalog.localization.MapInfoLocalizationProvider
import org.ametro.databinding.ActivityMapViewBinding
import org.ametro.model.MapContainer
import org.ametro.model.entities.MapDelay
import org.ametro.model.entities.MapDelayTimeRange
import org.ametro.model.entities.MapDelayType
import org.ametro.model.entities.MapMetadata.Scheme
import org.ametro.providers.IconProvider
import org.ametro.providers.TransportIconsProvider
import org.ametro.ui.navigation.adapter.NavigationDrawerAdapter
import org.ametro.ui.navigation.entities.*
import org.ametro.ui.navigation.helpers.DelayResources
import org.ametro.utils.ListUtils
import org.ametro.utils.ListUtils.IPredicate
import org.ametro.utils.StringUtils.isNullOrEmpty
import java.util.*
import kotlin.collections.ArrayList

class NavigationController(
    activity: AppCompatActivity,
    private val listener: INavigationControllerListener,
    binding: ActivityMapViewBinding,
    private val transportIconProvider: TransportIconsProvider,
    private val countryIconProvider: IconProvider,
    private val localizationProvider: MapInfoLocalizationProvider
) : OnItemClickListener {
    private val drawerMenuAdapter: NavigationDrawerAdapter
    private val drawerToggle: ActionBarDrawerToggle
    private val resources: Resources
    private val context: AppCompatActivity

    val drawerLayout: DrawerLayout
    val toolbar: Toolbar

    private var delayItems: Array<NavigationItem> = emptyArray()
    private var transportNameLocalizations: MutableMap<String, String>? = null

    init {
        toolbar = activity.findViewById(R.id.toolbar)
        activity.setSupportActionBar(toolbar)

        context = activity
        resources = activity.resources
        createTransportsLocalizationTable()

        drawerMenuAdapter =
            NavigationDrawerAdapter(activity, createNavigationItems(null, null, null, null))
        binding.drawer.apply {
            adapter = drawerMenuAdapter
            onItemClickListener = this@NavigationController
            choiceMode = ListView.CHOICE_MODE_NONE
        }

        drawerLayout = binding.drawerLayout
        drawerToggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.isDrawerIndicatorEnabled = true

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    fun setNavigation(
        container: MapContainer?,
        schemeName: String?,
        enabledTransports: Array<String>?,
        currentDelay: MapDelay?
    ) {
        drawerMenuAdapter.setNavigationItems(
            createNavigationItems(
                container,
                schemeName,
                enabledTransports,
                currentDelay
            )
        )
    }

    fun onPostCreate() {
        drawerToggle.syncState()
    }

    fun onConfigurationChanged(newConfig: Configuration?) {
        drawerToggle.onConfigurationChanged(newConfig)
    }

    fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return drawerToggle.onOptionsItemSelected(item)
    }

    val isDrawerOpen: Boolean
        get() = drawerLayout.isDrawerOpen(GravityCompat.START)

    fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val item = drawerMenuAdapter.getItem(position)
        if (!item.isEnabled) {
            return
        }
        var complete = false
        when (item.action) {
            OPEN_MAPS_ACTION -> complete = listener.onOpenMaps()
            OPEN_SETTINGS_ACTION -> complete = listener.onOpenSettings()
            OPEN_ABOUT_ACTION -> complete = listener.onOpenAbout()
            OPEN_SCHEME_ACTION -> complete = listener.onChangeScheme(item.source as String)
            TOGGLE_TRANSPORT_ACTION -> {
                val checkbox = drawerMenuAdapter.getItem(position) as NavigationCheckBoxItem
                val newCheckedState = !checkbox.isChecked
                if (listener.onToggleTransport(item.source as String, newCheckedState)) {
                    checkbox.isChecked = newCheckedState
                    drawerMenuAdapter.notifyDataSetChanged()
                }
            }
            CHANGE_DELAY_ACTION -> {
                for (delayItem in delayItems) {
                    delayItem.isSelected = delayItem === item
                }
                drawerMenuAdapter.notifyDataSetChanged()
                complete = listener.onDelayChanged(item.source as MapDelay)
            }
        }
        if (complete && isDrawerOpen) {
            closeDrawer()
        }
    }

    private fun createNavigationItems(
        container: MapContainer?,
        schemeName: String?,
        enabledTransports: Array<String>?,
        currentDelay: MapDelay?
    ): Array<NavigationItem> {
        val items = ArrayList(listOf(createHeaderNavigationItem(container)))
        items.add(
            NavigationSubHeader(
                resources.getString(R.string.nav_options), arrayOf(
                    NavigationTextItem(
                        OPEN_MAPS_ACTION,
                        ContextCompat.getDrawable(context, R.drawable.ic_public_black_18dp),
                        resources.getString(R.string.nav_select_map)
                    ),
                    NavigationTextItem(
                        OPEN_SETTINGS_ACTION,
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_black_18dp),
                        resources.getString(R.string.nav_settings)
                    ),
                    NavigationTextItem(
                        OPEN_ABOUT_ACTION,
                        ContextCompat.getDrawable(context, R.drawable.ic_info_black_24dp),
                        resources.getString(R.string.nav_about)
                    ),
                    NavigationSplitter()
                )
            )
        )
        delayItems = createDelayNavigationItems(container, currentDelay)
        if (delayItems.size > 1) {
            items.add(
                NavigationSubHeader(
                    NavigationItem.INVALID_ACTION,
                    resources.getString(R.string.nav_delays),
                    delayItems
                )
            )
            items.add(NavigationSplitter())
        }
        val transportItems = createTransportNavigationItems(container, schemeName, enabledTransports)
        if (transportItems.size > 1) {
            items.add(
                NavigationSubHeader(
                    NavigationItem.INVALID_ACTION,
                    resources.getString(R.string.nav_using),
                    transportItems
                )
            )
            items.add(NavigationSplitter())
        }
        val schemeItems = createSchemeNavigationItems(container, schemeName)
        if (schemeItems.size > 1) {
            items.add(
                NavigationSubHeader(
                    NavigationItem.INVALID_ACTION,
                    resources.getString(R.string.nav_schemes),
                    schemeItems
                )
            )
            items.add(NavigationSplitter())
        }
        return items.toTypedArray()
    }

    private fun createHeaderNavigationItem(container: MapContainer?): NavigationItem {
        if (container == null) {
            return NavigationHeader(resources.getString(R.string.nav_no_city))
        }
        val meta = container.metadata
        val local = localizationProvider.getEntity(meta.cityId)
        return NavigationHeader(
            countryIconProvider.getIcon(local.countryIsoCode),
            local.cityName,
            local.countryName,
            meta.comments,
            transportIconProvider.getTransportIcons(
                TransportTypeHelper.parseTransportTypes(meta.transportTypes)
            )
        )
    }

    private fun createSchemeNavigationItems(container: MapContainer?, schemeName: String?): Array<NavigationItem> {
        if (container == null) {
            return emptyArray()
        }

        val meta = container.metadata
        val schemeMetadataList = ArrayList<Scheme>()
        meta.schemes.asSequence()
            .filter { it.value.typeName != SCHEME_TYPE_OTHER }
            .mapTo(schemeMetadataList) { it.value }

        Collections.sort(schemeMetadataList, SchemeNavigationListComparator())
        val schemes: MutableList<NavigationItem> = ArrayList()
        for (schemeMeta in schemeMetadataList) {
            var icon: Drawable? = null
            if (schemeMeta.typeName == SCHEME_TYPE_ROOT) {
                val defaultTransport = ListUtils.firstOrDefault(
                    listOf(*schemeMeta.defaultTransports),
                    ListUtils.firstOrDefault(
                        listOf(*schemeMeta.defaultTransports),
                        null
                    )
                )
                if (defaultTransport != null) {
                    icon = transportIconProvider.getTransportIcon(
                        TransportTypeHelper.parseTransportType(
                            meta.getTransport(defaultTransport).typeName
                        )
                    )
                }
            }
            val item = NavigationTextItem(
                OPEN_SCHEME_ACTION,
                icon,
                schemeMeta.displayName,
                true,
                schemeMeta.name
            )
            if (schemeMeta.name == schemeName) {
                item.isSelected = true
                item.isEnabled = false
            }
            schemes.add(item)
        }
        return schemes.toTypedArray()
    }

    private fun createTransportNavigationItems(
        container: MapContainer?,
        schemeName: String?,
        enabledTransports: Array<String>?
    ): Array<NavigationItem> {
        if (container == null) {
            return emptyArray()
        }

        val meta = container.metadata
        val enabledTransportsSet = hashSetOf(
            *enabledTransports ?: container.getScheme(schemeName).defaultTransports
        )
        val transports: MutableList<NavigationItem> = ArrayList()
        for (name in meta.getScheme(schemeName).transports) {
            val transportSchemeMeta = meta.getTransport(name)
            var displayName = transportNameLocalizations!![transportSchemeMeta.typeName.lowercase(Locale.getDefault())]
            if (displayName == null) {
                displayName = "#" + transports.size
            }
            transports.add(
                NavigationCheckBoxItem(
                    TOGGLE_TRANSPORT_ACTION,
                    displayName,
                    enabledTransportsSet.contains(name),
                    transportSchemeMeta.name
                )
            )
        }
        return transports.toTypedArray()
    }

    private fun createDelayNavigationItems(container: MapContainer?, currentDelay: MapDelay?): Array<NavigationItem> {
        if (container == null) {
            return emptyArray()
        }
        val meta = container.metadata
        val delays: MutableList<NavigationItem> = ArrayList()
        var defaultDelayWasSet = false
        for (delay in meta.delays) {
            val item = NavigationTextItem(
                CHANGE_DELAY_ACTION,
                null,
                createDelayItemName(delay),
                true,
                delay
            )
            if (delay === currentDelay) {
                item.isSelected = true
                defaultDelayWasSet = true
            }
            delays.add(item)
        }
        if (delays.size > 0 && !defaultDelayWasSet) {
            delays[0].isSelected = true
        }
        return delays.toTypedArray()
    }

    private fun createDelayItemName(delay: MapDelay): String {
        return if (delay.delayType == MapDelayType.Custom) {
            createDelayName(
                delay.displayName,
                resources.getString(DelayResources.getDelayWeekendTypeTextId(delay.weekdays)),
                delay.ranges
            )
        } else createDelayName(
            resources.getString(DelayResources.getDelayTypeTextId(delay.delayType)),
            resources.getString(DelayResources.getDelayWeekendTypeTextId(delay.weekdays)),
            null
        )
    }

    private fun createDelayName(name: String?, weekdayName: String, ranges: Array<MapDelayTimeRange>?): String {
        val sb = StringBuilder()
        if (name != null) {
            sb.append(name)
        }
        if (!isNullOrEmpty(weekdayName)) {
            sb.append(" [")
            sb.append(weekdayName)
            sb.append("]")
        }
        if (ranges != null) {
            sb.append(" [")
            for (range in ranges) {
                sb.append(range.toString())
                sb.append(',')
            }
            sb.setLength(sb.length - 1)
            sb.append("]")
        }
        return sb.toString().trim { it <= ' ' }
    }

    @Deprecated("")
    private fun createTransportsLocalizationTable() {
        transportNameLocalizations = HashMap()
        val transportTypes = resources.getStringArray(R.array.transport_types)
        val transportTypeNames = resources.getStringArray(R.array.transport_type_names)
        for (i in transportTypes.indices) {
            transportNameLocalizations!![transportTypes[i].lowercase(Locale.getDefault())] =
                transportTypeNames[i]
        }
    }

    companion object {
        private const val SCHEME_TYPE_OTHER = "OTHER"
        private const val SCHEME_TYPE_ROOT = "ROOT"
        private const val OPEN_MAPS_ACTION = 1
        private const val OPEN_SETTINGS_ACTION = 2
        private const val OPEN_ABOUT_ACTION = 3
        private const val TOGGLE_TRANSPORT_ACTION = 4
        private const val CHANGE_DELAY_ACTION = 5
        private const val OPEN_SCHEME_ACTION = 6
    }
}