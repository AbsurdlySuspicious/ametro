package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetBotRoutePageBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.StringUtils
import java.util.*

typealias RoutePagerStation =
        Pair<MapSchemeLine, MapSchemeStation>

data class RoutePagerTransfer(
    val txf: MapSchemeLine,
    val partsCount: Int,
    val partsDelays: Int
) {
    val length: Int
        get() = partsCount
}

data class RoutePagerItem(
    val delay: Int,
    val routeStart: RoutePagerStation,
    val routeEnd: RoutePagerStation,
    val transfers: List<RoutePagerTransfer>
)

class RoutePagerAdapter(
    private val context: Context,
    private val listener: MapBottomPanelRoute.MapBottomPanelRouteListener
) :
    RecyclerView.Adapter<RoutePagerAdapter.PageHolder>() {

    var leaveTime: Calendar? = null
    var recycler: RecyclerView? = null
        private set
    var items: ArrayList<RoutePagerItem> = arrayListOf()
        private set

    private val resources = context.applicationContext.resources
    private val inflater = LayoutInflater.from(context)

    fun replaceItems(items: ArrayList<RoutePagerItem>, currentPage: Int, moveToFirst: Boolean) {
        val oldSize = this.items.size
        val newSize = items.size
        this.items = items

        if (moveToFirst)
            this.notifyItemMoved(currentPage, 0)

        if (oldSize > newSize) {
            this.notifyItemRangeRemoved(newSize, oldSize - newSize)
            this.notifyItemRangeChanged(0, newSize, Object())
        } else if (oldSize < newSize) {
            this.notifyItemRangeInserted(oldSize, newSize - oldSize)
            this.notifyItemRangeChanged(0, oldSize, Object())
        } else { // oldSize == newSize
            this.notifyItemRangeChanged(0, newSize, Object())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = WidgetBotRoutePageBinding.inflate(inflater, parent, false)
        return PageHolder(binding)
    }

    private fun bindRoutePoint(
        icon: ImageView,
        station: AppCompatTextView,
        bg: View,
        point: Pair<MapSchemeLine, MapSchemeStation>
    ) {
        (icon.drawable as GradientDrawable).setColor(point.first.lineColor)
        station.text = point.second.displayName

        bg.setOnLongClickListener {
            Toast
                .makeText(bg.context, point.second.displayName, Toast.LENGTH_SHORT)
                .show()
            true
        }

        bg.setOnClickListener {
            listener.onOpenDetails(point)
        }
    }

    private fun formatRangeTime(c: Calendar) =
        if (ApplicationEx.getInstanceContext(context.applicationContext)!!.is24HourTime)
            DateFormat.format("HH:mm", c)
        else
            DateFormat.format("h:mm", c)

    private fun setRangeText(item: RoutePagerItem, leaveTime: Calendar?, bind: WidgetBotRoutePageBinding) {
        val timeF = leaveTime ?: Calendar.getInstance()
        val timeT = (timeF.clone() as Calendar)
            .also { it.add(Calendar.SECOND, item.delay) }

        bind.routeTimeRangeLeave.text = formatRangeTime(timeF)
        bind.routeTimeRangeArrive.text = formatRangeTime(timeT)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val item = items[position]
        val nextItem = items.getOrNull(position + 1)
        val bind = holder.binding

        val time =
            StringUtils.humanReadableTimeRoute(item.delay)
        bind.routeTime.text = time.first
        bind.routeTimeSec.text = time.second

        nextItem?.let {
            val nextTime =
                StringUtils.humanReadableTimeRoute(nextItem.delay)
            bind.nextRouteTime.text = nextTime.first
            bind.nextRouteTimeSec.text = nextTime.second
        }

        setRangeText(item, this.leaveTime, bind)
        bind.routeTimeRangeBg.setOnClickListener {
            setRangeText(item, null, bind)
            // todo custom leave time dialog
        }

        bindRoutePoint(bind.lineIconStart, bind.stationStart, bind.stationStartBg, item.routeStart)
        bindRoutePoint(bind.lineIconEnd, bind.stationEnd, bind.stationEndBg, item.routeEnd)

        val txfItemsThisPage =
            if (item.transfers.isEmpty())
                mutableListOf(RoutePagerTransfer(item.routeStart.first, 1, 1))
            else
                item.transfers.toMutableList()
        val txfItemsNextPage =
            nextItem?.transfers?.toMutableList()
        bind.transfersRecycler
            .replaceItems(txfItemsThisPage, txfItemsNextPage, true, position)

        if (item.transfers.size < 2) {
            val color = ResourcesCompat.getColor(resources, R.color.route_panel_misc_icon_disabled, null)
            bind.transferCount.text = 0.toString()
            bind.transferCount.setTextColor(color)
            bind.transferCountIcon.drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            bind.transferCount.text = (item.transfers.size - 1).toString()
            bind.transferCount
                .setTextColor(ResourcesCompat.getColor(resources, R.color.route_panel_misc_icon_text, null))
            bind.transferCountIcon.drawable
                .setColorFilter(
                    ResourcesCompat.getColor(resources, R.color.route_panel_misc_icon, null),
                    PorterDuff.Mode.SRC_IN
                )
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recycler = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recycler = null
    }

    override fun getItemCount(): Int =
        items.size

    inner class PageHolder(val binding: WidgetBotRoutePageBinding) :
        RecyclerView.ViewHolder(binding.root)
}