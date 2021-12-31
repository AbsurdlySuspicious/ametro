package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetBotRoutePageBinding
import org.ametro.databinding.WidgetBotRouteTransferBinding
import org.ametro.databinding.WidgetItemBotRouteBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.StringUtils
import org.ametro.utils.misc.AnimUtils
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

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

class RouteTransfersLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val inflater = LayoutInflater.from(context)
    private val viewStash: MutableList<ImageView> = arrayListOf()
    private var transfers: MutableList<RoutePagerTransfer> = arrayListOf()

    private val lineHeight: Int = context.resources
        .getDimensionPixelSize(R.dimen.panel_bottom_route_line_long_height)
    private val lineMargin: Int = context.resources
        .getDimensionPixelSize(R.dimen.panel_bottom_route_line_long_margin)
    private val lineDrawable = ResourcesCompat
        .getDrawable(context.resources, R.drawable.line_long, null)!!

    init {
        this.orientation = HORIZONTAL
    }

    private fun createImg(color: Int?, width: Int, margin: Int) = ImageView(context).also { img ->
        val d = lineDrawable.mutate() as GradientDrawable
        color?.let { d.setColor(it) }
        img.layoutParams = LayoutParams(width, lineHeight).also {
            it.rightMargin = margin
        }
        img.setImageDrawable(d)
    }

    private fun createZeroImg() =
        createImg(null, 0, 0)

    fun replaceItems(transfers: MutableList<RoutePagerTransfer>, animate: Boolean) {
        this.post {
            val txfLengthSum = transfers.fold(0) { acc, i -> acc + i.length }
            val txfPartLength = txfLengthSum / this.width
            val txfCount = transfers.size

            val calcWidth = { i: Int, t: RoutePagerTransfer ->
                t.length * txfPartLength +
                        if (i == txfCount - 1)
                            txfLengthSum % this.width
                        else 0
            }

            val resetView = { v: View ->
                (v.layoutParams as LayoutParams).also {
                    it.width = 0
                    it.leftMargin = 0
                    it.rightMargin = 0
                }
            }

            val addViews = {
                this.removeAllViews()
                for (i in 0 until txfCount)
                    this.addView(viewStash[i])
            }

            if (!animate || viewStash.isEmpty() || transfers.isEmpty()) {
                for (i in 0 until max(txfCount, viewStash.size)) {
                    val v = viewStash.getOrNull(i)
                    val t = transfers.getOrNull(i)

                    if (v != null && t != null) {
                        (v.drawable as GradientDrawable).setColor(t.txf.lineColor)
                        (v.layoutParams as LayoutParams).also {
                            it.width = calcWidth(i, t)
                            it.rightMargin = lineMargin
                        }
                        v.requestLayout()
                    } else if (v != null) {
                        resetView(v)
                    } else if (t != null) {
                        val img =
                            createImg(t.txf.lineColor, calcWidth(i, t), lineMargin)
                        viewStash.add(img)
                    }
                }

                addViews()

            } else {
                val oldTxf = this.transfers
                val animTxf = ArrayList<AnimatedTxf>()
                for (i in 0 until max(transfers.size, oldTxf.size)) {
                    val o = oldTxf.getOrNull(i)
                    val t = transfers.getOrNull(i)
                    val v = viewStash.getOrNull(i)

                    if (o != null && t != null) {
                        val pw = v!!.width
                        val at =
                            AnimatedTxf(o.txf.lineColor, t.txf.lineColor, pw, calcWidth(i, t) - pw, ACTION_RESIZE)
                        animTxf.add(at)
                    } else if (o != null) {
                        val color = o.txf.lineColor
                        val at = AnimatedTxf(color, color, v!!.width, 0, ACTION_HIDE)
                        animTxf.add(at)
                    } else if (t != null) {
                        if (v != null) resetView(v)
                        else viewStash.add(createZeroImg())

                        val color = t.txf.lineColor
                        val at = AnimatedTxf(color, color, 0, calcWidth(i, t), ACTION_SHOW)
                        animTxf.add(at)
                    }
                }

                addViews()

                // AnimUtils.getValueAnimator() todo
            }

            this.transfers = transfers
        }
    }

    data class AnimatedTxf(
        val srcColor: Int,
        val dstColor: Int,
        val widthPrev: Int,
        val widthDelta: Int,
        val action: Int
    )

    companion object {
        private const val ACTION_RESIZE = 0
        private const val ACTION_HIDE = 1
        private const val ACTION_SHOW = 2
    }
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
