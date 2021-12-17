package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
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

class RoutePagerAdapter(private val context: Context) :
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
    PanelAdapterBinder<LinearLayout, WidgetItemBotRouteBinding> {

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
        sheet.panelShow(MapBottomPanelSheet.OPENED_REOPEN, true) {
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

    override fun bindItem(view: LinearLayout, bind: WidgetItemBotRouteBinding) {
        bind.pager.adapter = adapter
    }

    interface MapBottomPanelRouteListener {
        fun onPanelHidden()
    }
}
