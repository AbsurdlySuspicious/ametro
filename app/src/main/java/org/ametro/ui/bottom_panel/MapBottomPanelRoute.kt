package org.ametro.ui.bottom_panel

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetBotRoutePageBinding
import org.ametro.databinding.WidgetItemBotRouteBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.StringUtils
import org.ametro.utils.misc.AnimUtils
import org.ametro.utils.misc.epsilonEqual
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

        val itemsForTxfBar =
            if (item.transfers.isEmpty())
                mutableListOf(RoutePagerTransfer(item.routeStart.first, 1, 1))
            else
                item.transfers.toMutableList()
        bind.transfersRecycler
            .replaceItems(itemsForTxfBar, true, position)

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

class RouteTransfersLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ACTION_RESIZE = 0
        private const val ACTION_HIDE = 1
        private const val ACTION_SHOW = 2
    }

    private val viewStash: MutableList<ImageView> = arrayListOf() // todo remove views from stash?
    private var transfers: MutableList<RoutePagerTransfer> = arrayListOf()
    private var touchAnimProgram: ArrayList<AnimatedTxf>? = null

    private val lineHeight: Int = context.resources
        .getDimensionPixelSize(R.dimen.panel_bottom_route_line_long_height)
    private val lineMargin: Int = context.resources
        .getDimensionPixelSize(R.dimen.panel_bottom_route_line_long_margin)
    private val lineDrawable: Drawable
        get() = ResourcesCompat
            .getDrawable(context.resources, R.drawable.line_long, null)!!.mutate()

    init {
        this.orientation = HORIZONTAL
    }

    private fun resetView(v: View) {
        (v.layoutParams as LayoutParams).also {
            it.width = 0
            it.leftMargin = 0
            it.rightMargin = 0
        }
    }

    private fun createView() = ImageView(context).also {
        it.layoutParams = LayoutParams(0, lineHeight)
        it.setImageDrawable(lineDrawable.mutate())
        viewStash.add(it)
    }

    private fun addViews(w: TxfWidths) {
        val viewsCount = this.childCount
        if (w.txfCount > viewsCount)
            for (i in viewsCount until w.txfCount)
                this.addView(viewStash[i])
    }

    private fun removeViews(w: TxfWidths) {
        val viewsCount = this.childCount
        if (w.txfCount < viewsCount)
            this.removeViews(w.txfCount, viewsCount - w.txfCount)
    }

    private fun makeWidths(transfers: MutableList<RoutePagerTransfer>): TxfWidths {
        val txfLengthSum = transfers.fold(0) { acc, i -> acc + i.length }
        val txfCount = transfers.size
        val txfPartLength =
            if (txfLengthSum == 0 || txfCount == 0) 0
            else this.width / txfLengthSum
        return TxfWidths(txfLengthSum, txfCount, txfPartLength)
    }

    private fun calcWidth(w: TxfWidths, i: Int, t: RoutePagerTransfer): Int {
        var width = t.length * w.txfPartLength
        if (i == w.txfCount - 1)
            width += this.width % w.txfLengthSum
        else
            width -= lineMargin
        return width
    }

    data class TxfWidths(
        val txfLengthSum: Int,
        val txfCount: Int,
        val txfPartLength: Int,
    )

    data class AnimatedTxf(
        val srcColor: Int,
        val dstColor: Int,
        val widthPrev: Int,
        val widthDelta: Int,
        val action: Int
    )

    fun replaceItems(transfers: MutableList<RoutePagerTransfer>, animate: Boolean, page: Int) {
        this.post {
            val w = makeWidths(transfers)

            if (!animate || page != 0 || viewStash.isEmpty() || transfers.isEmpty()) {
                for (i in 0 until max(w.txfCount, viewStash.size)) {
                    var v = viewStash.getOrNull(i)
                    val t = transfers.getOrNull(i)

                    if (t != null) {
                        if (v == null)
                            v = createView()

                        (v.drawable as GradientDrawable).setColor(t.txf.lineColor)
                        (v.layoutParams as LayoutParams).also {
                            it.width = calcWidth(w, i, t)
                            it.rightMargin = lineMargin
                        }
                        v.requestLayout()
                    } else if (v != null) {
                        resetView(v)
                    }
                }

                addViews(w)
                removeViews(w)
            } else {
                val animTxf = animProgram(w, this.transfers, transfers)
                addViews(w)

                AnimUtils.getValueAnimator(true, 300, AccelerateDecelerateInterpolator()) { p ->
                    animateViews(animTxf, p)
                }.also {
                    it.doOnEnd { removeViews(w) }
                    it.start()
                }
            }

            this.transfers = transfers
        }
    }

    private fun silentReset() {
        replaceItems(this.transfers, animate = false, -1)
    }

    val isTouchAnimationActive: Boolean
        get() = touchAnimProgram != null

    fun touchAnimationStart(targetTransfers: MutableList<RoutePagerTransfer>) {
        if (touchAnimProgram != null)
            silentReset()
        val widths = makeWidths(targetTransfers)
        touchAnimProgram = animProgram(widths, this.transfers, targetTransfers)
    }

    fun touchAnimationEnd() {
        touchAnimProgram = null
        this.post { silentReset() }
    }

    fun touchAnimate(progress: Float) {
        touchAnimProgram?.let { animateViews(it, progress) }
    }

    private fun animateViews(animTxf: ArrayList<AnimatedTxf>, p: Float) {
        for ((i, t) in animTxf.withIndex()) {
            val v = viewStash[i]
            val lp = v.layoutParams as LayoutParams

            when (t.action) {
                ACTION_SHOW -> lp.rightMargin = (lineMargin * p).toInt()
                ACTION_HIDE -> lp.rightMargin = (lineMargin - lineMargin * p).toInt()
            }

            lp.width = (t.widthPrev + t.widthDelta * p).toInt()
            v.requestLayout()

            val color = AnimUtils.argbEvaluate(p, t.srcColor, t.dstColor)
            (v.drawable as GradientDrawable).setColor(color)
        }
    }

    private fun animProgram(
        w: TxfWidths,
        oldTxf: MutableList<RoutePagerTransfer>,
        newTxf: MutableList<RoutePagerTransfer>
    ): ArrayList<AnimatedTxf> {
        val animTxf = ArrayList<AnimatedTxf>()
        for (i in 0 until max(newTxf.size, oldTxf.size)) {
            val o = oldTxf.getOrNull(i)
            val t = newTxf.getOrNull(i)
            var v = viewStash.getOrNull(i)

            if (o != null && t != null) {
                val pw = v!!.width
                val at =
                    AnimatedTxf(o.txf.lineColor, t.txf.lineColor, pw, calcWidth(w, i, t) - pw, ACTION_RESIZE)
                animTxf.add(at)
            } else if (o != null) {
                val pw = v!!.width
                val color = o.txf.lineColor
                val at = AnimatedTxf(color, color, pw, -pw, ACTION_HIDE)
                animTxf.add(at)
            } else if (t != null) {
                if (v == null)
                    v = createView()
                resetView(v)

                val color = t.txf.lineColor
                val at = AnimatedTxf(color, color, 0, calcWidth(w, i, t), ACTION_SHOW)
                animTxf.add(at)
            }
        }
        return animTxf
    }

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

    fun show(routes: ArrayList<RoutePagerItem>, leaveTime: Calendar?, setPage: Int) {
        sheet.panelShow(MapBottomPanelSheet.OPENED_CHANGE_VIEW, false) {
            adapter.leaveTime = leaveTime
            adapter.replaceItems(routes, currentPage, setPage <= 0)
            this.setPage(setPage)
            sheet.adapter.showRoute = true
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
        // nope
    }

    override fun createHolder(bind: ViewBinding) {
        castBind(bind).also {
            it.pager.adapter = adapter
        }
    }

    override fun attachItem(bind: ViewBinding) {
        castBind(bind).also {
            it.pager.setCurrentItem(currentPage, false)
            it.pager.registerOnPageChangeCallback(pageChangedCallback)
            it.dots.setViewPager2(it.pager)
            it.dots.refreshDots()
            this.binding = it

            val t = object : ViewPager2.PageTransformer {
                override fun transformPage(view: View, position: Float) {
                    if (position < 0) {
                        if (position > -1)
                            view.translationX = view.width * -position
                        else
                            view.translationX = 0f
                    } else {
                        if (position > 0)
                            view.translationX = view.width * (1f - position)
                        else
                            view.translationX = 0f
                    }
                    Log.d("AM2", "pt $position, tx ${view.translationX}")
                }
            }
            it.pager.setPageTransformer(t)

            val p = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
                    Log.d("AM2", "pcc: pos $position, off $offset, px $offsetPx")
                    val holder = adapter.recycler
                        ?.findViewHolderForAdapterPosition(position)!! as RoutePagerAdapter.PageHolder
                    val txf = holder.binding.transfersRecycler

                    if (!txf.isTouchAnimationActive) {
                        adapter.items.getOrNull(position + 1)?.let { page ->
                            txf.touchAnimationStart(page.transfers.toMutableList())
                            txf.touchAnimate(offset)
                        }
                    } else {
                        txf.touchAnimate(offset)
                    }

                    if (epsilonEqual(offset, 1f))
                        txf.touchAnimationEnd()
                }
            }
            it.pager.registerOnPageChangeCallback(p)
        }
    }

    override fun detachItem(bind: ViewBinding) {
        castBind(bind).also {
            it.pager.unregisterOnPageChangeCallback(pageChangedCallback)
            it.dots.pager?.removeOnPageChangeListener()
            this.binding = null
        }
    }

    interface MapBottomPanelRouteListener {
        fun onPanelHidden()
        fun onOpenDetails(station: Pair<MapSchemeLine, MapSchemeStation>)
    }
}
