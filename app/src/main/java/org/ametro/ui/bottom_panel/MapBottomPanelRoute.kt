package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.databinding.WidgetBotRoutePageBinding
import org.ametro.databinding.WidgetItemBotRouteBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation

data class RoutePagerItem(
    val time: String,
    val timeSeconds: String,
    val routeStart: Pair<MapSchemeLine, MapSchemeStation>,
    val routeEnd: Pair<MapSchemeLine, MapSchemeStation>
)

class RoutePagerAdapter(context: Context) :
    RecyclerView.Adapter<RoutePagerAdapter.PageHolder>() {

    private val inflater = LayoutInflater.from(context)
    private var items: ArrayList<RoutePagerItem> = arrayListOf()

    fun replaceItems(items: ArrayList<RoutePagerItem>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = WidgetBotRoutePageBinding.inflate(inflater, parent, false)
        return PageHolder(binding)
    }

    private fun bindRoutePoint(
        icon: ImageView,
        station: AppCompatTextView,
        point: Pair<MapSchemeLine, MapSchemeStation>
    ) {
        (icon.drawable as GradientDrawable).setColor(point.first.lineColor)
        station.text = point.second.displayName
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val item = items[position]
        val bind = holder.binding

        bind.routeTime.text = item.time
        bind.routeTimeSec.text = item.timeSeconds

        bindRoutePoint(bind.lineIconStart, bind.stationStart, item.routeStart)
        bindRoutePoint(bind.lineIconEnd, bind.stationEnd, item.routeEnd)
    }

    override fun getItemCount(): Int = items.size

    inner class PageHolder(val binding: WidgetBotRoutePageBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class MapBottomPanelRoute(private val sheet: MapBottomPanelSheet, private val listener: MapBottomPanelRouteListener) :
    PanelAdapterBinder {

    private var binding: WidgetItemBotRouteBinding? = null
    private var slideHandler: ((Int) -> Unit)? = null
    private var currentPage: Int = 0
    private val adapter = RoutePagerAdapter(sheet.sheetView.context)

    init {
        sheet.addSheetStateCallbackPre { _, state ->
            when (state) {
                BottomSheetBehavior.STATE_HIDDEN ->
                    if (sheet.adapter.showRoute) {
                        listener.onPanelHidden()
                    }
            }
        }
        sheet.adapter.routeBinder = this
    }

    fun show(routes: ArrayList<RoutePagerItem>) {
        sheet.panelShow(MapBottomPanelSheet.OPENED_CHANGE_VIEW, true) {
            adapter.replaceItems(routes)
            sheet.adapter.showRoute = true
        }
    }

    fun hide() {
        val after = {
            sheet.adapter.showRoute = false
        }

        if (!sheet.adapter.showRoute || sheet.adapter.showStation)
            after()
        else
            sheet.panelHide(after)
    }

    fun setSlideCallback(f: (Int) -> Unit) {
        slideHandler = f
    }

    fun setPage(i: Int, smooth: Boolean = false) {
        currentPage = i
        binding?.pager?.setCurrentItem(i, smooth)
    }

    private val pageChangedCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            slideHandler?.let { it(position) }
        }
    }

    private fun castBind(bind: ViewBinding) =
        bind as WidgetItemBotRouteBinding

    override fun bindItem(bind: ViewBinding) {
        binding = castBind(bind)
        binding!!.pager.adapter = adapter
    }

    override fun attachItem(holder: PanelHolder) {
        binding = castBind(holder.binding)
        binding!!.pager.apply {
            setCurrentItem(currentItem, false)
            registerOnPageChangeCallback(pageChangedCallback)
        }
    }

    override fun detachItem(holder: PanelHolder) {
        castBind(holder.binding)
            .pager.unregisterOnPageChangeCallback(pageChangedCallback)
    }

    interface MapBottomPanelRouteListener {
        fun onPanelHidden()
    }
}
