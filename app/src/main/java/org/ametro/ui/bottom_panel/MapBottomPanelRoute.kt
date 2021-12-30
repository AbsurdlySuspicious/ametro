package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetBotRoutePageBinding
import org.ametro.databinding.WidgetBotRouteTransferBinding
import org.ametro.databinding.WidgetItemBotRouteBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.StringUtils
import java.util.*
import kotlin.collections.ArrayList

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

    private val inflater = LayoutInflater.from(context)
    private var items: ArrayList<RoutePagerItem> = arrayListOf()

    fun replaceItems(items: ArrayList<RoutePagerItem>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = WidgetBotRoutePageBinding.inflate(inflater, parent, false)

        binding.transfersRecycler.also {
            it.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            it.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            it.adapter = RouteTransferAdapter(inflater)
        }

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
        val bind = holder.binding

        val time =
            StringUtils.humanReadableTimeRoute(item.delay)

        bind.routeTime.text = time.first
        bind.routeTimeSec.text = time.second

        setRangeText(item, this.leaveTime, bind)
        bind.routeTimeRangeBg.setOnClickListener {
            setRangeText(item, null, bind)
            // todo custom leave time dialog
        }

        bindRoutePoint(bind.lineIconStart, bind.stationStart, bind.stationStartBg, item.routeStart)
        bindRoutePoint(bind.lineIconEnd, bind.stationEnd, bind.stationEndBg, item.routeEnd)

        (bind.transfersRecycler.adapter as RouteTransferAdapter?)
            ?.replaceItems(item.transfers.toMutableList())


        if (item.transfers.size < 2) {
            bind.transferCount.text = ""
            bind.transferCountIcon.visibility = View.INVISIBLE
        } else {
            bind.transferCount.text = (item.transfers.size - 1).toString()
            bind.transferCountIcon.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int =
        items.size

    inner class PageHolder(val binding: WidgetBotRoutePageBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class RouteTransferAdapter(private val inflater: LayoutInflater) :
    RecyclerView.Adapter<RouteTransferAdapter.TransferHolder>() {

    private lateinit var recyclerView: RecyclerView
    private var items: MutableList<RoutePagerTransfer> = arrayListOf()
    private var txfLengthSum = 0

    fun replaceItems(items: MutableList<RoutePagerTransfer>) {
        this.items = items
        this.txfLengthSum = items.fold(0) { acc, i -> acc + i.length }
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferHolder {
        val binding = WidgetBotRouteTransferBinding.inflate(inflater, parent, false)
        return TransferHolder(binding)
    }

    override fun onBindViewHolder(holder: TransferHolder, position: Int) {
        val item = items[position]
        val lineIcon = holder.binding.lineIconTransfer

        recyclerView.post {
            (lineIcon.drawable as GradientDrawable)
                .setColor(item.txf.lineColor)

            val lineLayout =
                lineIcon.layoutParams as LinearLayout.LayoutParams
            var width =
                recyclerView.width / txfLengthSum * item.length
            if (position == items.size - 1) {
                width += recyclerView.width % txfLengthSum
                lineLayout.rightMargin = 0
                lineLayout.leftMargin = 0
            }
            width -=
                lineLayout.leftMargin + lineLayout.rightMargin
            Log.i(
                "MEME3", "rw ${recyclerView.width}, ls $txfLengthSum, " +
                        "w $width, rem ${recyclerView.width % txfLengthSum}, " +
                        "pos $position (${items.size})"
            )
            lineLayout.width = width
            lineIcon.requestLayout()
        }
    }

    override fun getItemCount(): Int =
        items.size

    inner class TransferHolder(val binding: WidgetBotRouteTransferBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class MapBottomPanelRoute(private val sheet: MapBottomPanelSheet, private val listener: MapBottomPanelRouteListener) :
    PanelAdapterBinder {

    private var binding: WidgetItemBotRouteBinding? = null
    private var slideHandler: ((Int) -> Unit)? = null
    private var currentPage: Int = 0
    private val adapter = RoutePagerAdapter(sheet.sheetView.context, listener)

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

    fun show(routes: ArrayList<RoutePagerItem>, leaveTime: Calendar?) {
        sheet.panelShow(MapBottomPanelSheet.OPENED_CHANGE_VIEW, true) {
            adapter.leaveTime = leaveTime
            adapter.replaceItems(routes)
            sheet.adapter.showRoute = true
            binding?.let {
                val anim = sheet.bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN
                sheet.updatePeekHeightImpl(it.root.height, anim, force = true)
            }
        }
    }

    fun hide() {
        val after = {
            sheet.adapter.showRoute = false
            adapter.leaveTime = null
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
            currentPage = position
            slideHandler?.let { it(position) }
        }
    }

    private fun castBind(bind: ViewBinding) =
        bind as WidgetItemBotRouteBinding

    override fun bindItem(bind: ViewBinding) {
        binding = castBind(bind)
        binding!!.also {
            it.pager.adapter = adapter
        }
    }

    override fun attachItem(holder: PanelHolder) {
        binding = castBind(holder.binding)
        binding!!.also {
            it.pager.setCurrentItem(currentPage, false)
            it.pager.registerOnPageChangeCallback(pageChangedCallback)
            it.dots.setViewPager2(it.pager)
            it.dots.refreshDots()
        }
    }

    override fun detachItem(holder: PanelHolder) {
        castBind(holder.binding).also {
            it.pager.unregisterOnPageChangeCallback(pageChangedCallback)
            it.dots.pager?.removeOnPageChangeListener()
        }
    }

    interface MapBottomPanelRouteListener {
        fun onPanelHidden()
        fun onOpenDetails(station: Pair<MapSchemeLine, MapSchemeStation>)
    }
}
