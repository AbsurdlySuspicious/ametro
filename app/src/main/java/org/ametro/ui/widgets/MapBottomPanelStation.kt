package org.ametro.ui.widgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetItemBotStationBinding
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.misc.BottomSheetUtils
import org.ametro.utils.misc.ColorUtils

interface PanelAdapterBinder<V : View, B : ViewBinding> {
    fun bindItem(view: V, bind: B)
}

private abstract class BaseItem(
    val viewType: Int,
    val priority: Int
)

private object RouteItem : BaseItem(PanelAdapter.TYPE_ROUTE, 1)
private object StationItem : BaseItem(PanelAdapter.TYPE_STATION, 2)

class PanelHolder(val viewType: Int, view: View, val binding: ViewBinding) : RecyclerView.ViewHolder(view)

class PanelAdapter(context: Context) : RecyclerView.Adapter<PanelHolder>() {
    companion object {
        const val TYPE_ROUTE = 1
        const val TYPE_STATION = 2
    }

    private val inflater = LayoutInflater.from(context)
    private lateinit var recyclerView: RecyclerView

    private val itemList: MutableList<BaseItem> = mutableListOf()

    var showRoute: Boolean = false
        set(value) {
            field = value; refreshList()
        }
    var routeBinder: PanelAdapterBinder<ViewGroup, ViewBinding>? = null
    var showStation: Boolean = false
        set(value) {
            field = value; refreshList()
        }
    var stationBinder: PanelAdapterBinder<ConstraintLayout, WidgetItemBotStationBinding>? = null

    private fun refreshList() {
        if (showRoute)
            itemList.add(RouteItem)
        else
            itemList.removeAll { it.viewType == TYPE_ROUTE }

        if (showStation)
            itemList.add(StationItem)
        else
            itemList.removeAll { it.viewType == TYPE_STATION }

        itemList.sortBy { it.priority }
        this.notifyDataSetChanged()
    }

    fun topmostHeight(): Int {
        return recyclerView.findViewHolderForLayoutPosition(0)?.itemView?.height ?: 0
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return itemList[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelHolder {
        return when (viewType) {
            TYPE_STATION -> {
                val bind = WidgetItemBotStationBinding.inflate(inflater, parent, false)
                PanelHolder(viewType, bind.root, bind)
            }
            TYPE_ROUTE -> TODO()
            else -> throw Exception("Unknown view $viewType")
        }
    }

    override fun onBindViewHolder(holder: PanelHolder, position: Int) {
        when (holder.viewType) {
            TYPE_STATION -> stationBinder?.bindItem(
                holder.itemView as ConstraintLayout,
                holder.binding as WidgetItemBotStationBinding
            )
            TYPE_ROUTE -> TODO()
        }
    }

}

class MapBottomPanelSheet(
    val sheetView: NestedScrollView,
    val app: ApplicationEx
) {
    companion object {
        const val PENDING_OPEN_NO = 0
        const val PENDING_OPEN_EXPAND = 1
        const val PENDING_OPEN_COLLAPSE = 2

        const val OPENED_IGNORE = 0
        const val OPENED_CHANGE_VIEW = 1
        const val OPENED_REOPEN = 2
    }

    val adapter = PanelAdapter(sheetView.context)
    val bottomSheet = BottomSheetBehavior.from(sheetView)

    private val binding = WidgetMapBottomPanelBinding.bind(sheetView)
    private val recycler = binding.recycler

    private val topPadViews = listOf(binding.drag)

    private var sheetStateCallbacksPre: MutableList<(View, Int) -> Unit> = mutableListOf()
    private var pendingSheetAction: (() -> Unit)? = null

    private fun runPendingSheetAction() {
        pendingSheetAction?.let {
            it()
            pendingSheetAction = null
        }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(sheetView: View, newState: Int) {
            // Log.i("MEME", "Bottom sheet state: ${BottomSheetUtils.stateToString(newState)}")
            sheetStateCallbacksPre
                .forEach { it(sheetView, newState) }
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    runPendingSheetAction()
                    if (pendingOpen != PENDING_OPEN_NO) {
                        bottomSheet.state = when (pendingOpen) {
                            PENDING_OPEN_EXPAND -> BottomSheetBehavior.STATE_EXPANDED
                            PENDING_OPEN_COLLAPSE -> BottomSheetBehavior.STATE_COLLAPSED
                            else -> BottomSheetBehavior.STATE_HIDDEN
                        }
                        pendingOpen = PENDING_OPEN_NO
                    }
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    runPendingSheetAction()
                    updatePeekHeightTopmost()
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    runPendingSheetAction()
                    updatePeekHeightTopmost()
                }
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    val isOpened: Boolean
        get() = bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN

    var pendingOpen: Int = PENDING_OPEN_NO
        private set
    var openTriggered: Boolean = false
        private set

    init {
        bottomSheet.apply {
            isHideable = true
            isDraggable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            addBottomSheetCallback(bottomSheetCallback)
        }
        recycler.apply {
            adapter = this@MapBottomPanelSheet.adapter
            layoutManager = LinearLayoutManager(sheetView.context)
            overScrollMode = View.OVER_SCROLL_NEVER
            setHasFixedSize(true)
        }
    }

    private fun updatePeekHeightTopmost() {
        updatePeekHeight(adapter.topmostHeight())
    }

    private fun updatePeekHeight(height: Int) {
        if (openTriggered) {
            openTriggered = false
            val pad = topPadViews.fold(0) { acc, view ->
                val params = view.layoutParams as LinearLayout.LayoutParams
                acc + view.height + params.topMargin + params.bottomMargin
            }
            bottomSheet.setPeekHeight(pad + height, true)
        }
    }

    fun addSheetStateCallbackPre(f: (View, Int) -> Unit) {
        sheetStateCallbacksPre.add(f)
    }

    private fun panelHideImpl(forReopen: Int) {
        pendingOpen = forReopen
        bottomSheet.apply {
            if (state != BottomSheetBehavior.STATE_HIDDEN)
                state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun panelHide(after: (() -> Unit)? = null) {
        if (!isOpened)
            after?.let { it() }
        else {
            pendingSheetAction = after
            panelHideImpl(PENDING_OPEN_NO)
        }
    }

    fun panelExpandCollapse(collapse: Boolean, after: (() -> Unit)? = null) {
        val newState =
            if (collapse) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_EXPANDED
        if (bottomSheet.state == newState)
            after?.let { it() }
        else {
            pendingSheetAction = after
            bottomSheet.state = newState
        }
    }

    fun panelShow(openedBehavior: Int, collapsed: Boolean, prepare: () -> Unit) {
        val newState =
            if (collapsed) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_EXPANDED
        if (!isOpened) {
            prepare()
            openTriggered = true
            pendingOpen = PENDING_OPEN_NO
            bottomSheet.state = newState
        } else {
            when (openedBehavior) {
                OPENED_REOPEN -> {
                    val pending =
                        if (collapsed) PENDING_OPEN_COLLAPSE
                        else PENDING_OPEN_EXPAND
                    openTriggered = true
                    pendingSheetAction = prepare
                    panelHideImpl(pending)
                }
                OPENED_CHANGE_VIEW -> {
                    prepare()
                    updatePeekHeightTopmost()
                    bottomSheet.state = newState
                }
                OPENED_IGNORE -> {}
            }
        }
    }
}

class MapBottomPanelStation(
    private val sheet: MapBottomPanelSheet,
    private val listener: MapBottomPanelStationListener
) : PanelAdapterBinder<ConstraintLayout, WidgetItemBotStationBinding> {

    private val adapter = sheet.adapter
    private val context = sheet.sheetView.context

    private val density = context.resources.displayMetrics.density
    private val stationTintFg = ColorUtils.fromColorInt(Color.parseColor("#a9a9a9"))
    private val stationTintBg = ColorUtils.fromColorInt(Color.parseColor("#2a2a2a"))

    private var binding: WidgetItemBotStationBinding? = null
    private val stationTextView get() = binding!!.station
    private val lineTextView get() = binding!!.line
    private val stationLayout get() = binding!!.stationLayout
    private val detailsHint get() = binding!!.detailsIcon
    private val detailsProgress get() = binding!!.detailsLoading
    private val lineIcon get() = binding!!.lineIcon
    private val beginButton get() = binding!!.actionStart
    private val endButton get() = binding!!.actionEnd

    init {
        sheet.addSheetStateCallbackPre { sheetView, newState ->
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    if (sheet.pendingOpen != MapBottomPanelSheet.PENDING_OPEN_NO)
                        hideCleanup()
                }
                else -> {}
            }
        }
        adapter.stationBinder = this
    }

    override fun bindItem(view: ConstraintLayout, bind: WidgetItemBotStationBinding) {
        binding = bind

        val progressTint =
            context.resources.getColor(R.color.panel_secondary_icon)
        detailsProgress.indeterminateDrawable.mutate().apply {
            setColorFilter(progressTint, PorterDuff.Mode.SRC_IN)
            detailsProgress.indeterminateDrawable = this
        }

        val clickListener = View.OnClickListener { v ->
            if (v === stationLayout && hasDetails) {
                detailsHint.visibility = View.INVISIBLE
                detailsProgress.visibility = View.VISIBLE
                this@MapBottomPanelStation.listener.onShowMapDetail(line, station)
            } else if (v === beginButton) {
                this@MapBottomPanelStation.listener.onSelectBeginStation(line, station)
            } else if (v === endButton) {
                this@MapBottomPanelStation.listener.onSelectEndStation(line, station)
            }
        }
        stationLayout.setOnClickListener(clickListener)
        beginButton.setOnClickListener(clickListener)
        endButton.setOnClickListener(clickListener)
    }

    private fun viewVisible(visible: Boolean): Int {
        return if (visible) View.VISIBLE else View.INVISIBLE
    }

    private fun routeStation(
        info: Pair<MapSchemeLine, MapSchemeStation>?,
        hint: View,
        replaceIcon: View,
        station: TextView
    ) {
        val selected = info != null
        hint.visibility = viewVisible(!selected)
        replaceIcon.visibility = viewVisible(selected)
        station.visibility = viewVisible(selected)
        if (selected) {
            val lineColor = ColorUtils
                .fromColorInt(info!!.first.lineColor)
            val bgColor = lineColor
                .screen(stationTintBg)
                .toColorInt()
            val fgColor = lineColor
                .screen(stationTintFg)
                .toColorInt()
            val padding = Rect(
                /* left   */ (density * 10f).toInt(),
                /* top    */ (density * 2f).toInt(),
                /* right  */ (density * 0f).toInt(),
                /* bottom */ (density * 2f).toInt()
            )
            val rnd = density * 2.5f

            val draw = { width: Int, height: Int ->
                val w = width.toFloat()
                val h = height.toFloat()
                val off = density * 5f

                val bitmap = Bitmap
                    .createBitmap(width, height, Bitmap.Config.ARGB_8888)

                Canvas(bitmap).apply {
                    // bottom depth
                    // drawRoundRect(RectF(0f, 0f, w, h), rnd, rnd, Paint().apply { color = bgColor })
                    // drawRoundRect(RectF(0f, 0f, w, h - off), rnd, rnd, Paint().apply { color = fgColor })

                    // left no depth
                    // drawRect(RectF(0f, 0f, w, h), Paint().apply { color = bgColor })
                    // drawRect(RectF(off, 0f, w, h), Paint().apply { color = fgColor })

                    // left no bg
                    drawRoundRect(RectF(0f, 0f, off, h), rnd, rnd, Paint().apply { color = bgColor })
                }

                bitmap
            }


            val bg = PaintDrawable().apply {
                val shaderMode = Shader.TileMode.CLAMP
                shape = RectShape()
                //setCornerRadius(rnd)
                setPadding(padding)
                shaderFactory = object : ShapeDrawable.ShaderFactory() {
                    override fun resize(width: Int, height: Int): Shader {
                        return BitmapShader(draw(width, height), shaderMode, shaderMode)
                    }
                }
            }

            station.text = info.second.displayName
            station.setBackgroundDrawable(bg)
        }
    }

    val isOpened: Boolean
        get() = sheet.isOpened && adapter.showStation

    private var hasDetails: Boolean = false
    private var line: MapSchemeLine? = null
    private var station: MapSchemeStation? = null

    private val detailsVisibility
        get() = viewVisible(hasDetails)

    fun detailsClosed() {
        binding ?: return
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
    }

    private fun showImpl() {
        binding ?: return

        stationTextView.text = station!!.displayName
        lineTextView.text = line!!.displayName
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
        (lineIcon.drawable as GradientDrawable).setColor(line!!.lineColor)

        routeStation(
            sheet.app.routeStart,
            binding!!.textStartHint,
            binding!!.replaceIconStart,
            binding!!.textStartStation
        )

        routeStation(
            sheet.app.routeEnd,
            binding!!.textEndHint,
            binding!!.replaceIconEnd,
            binding!!.textEndStation
        )

        sheet.app.bottomPanelOpen = true
        sheet.app.bottomPanelStation = Pair(line, station)
    }

    fun show(line: MapSchemeLine, station: MapSchemeStation, showDetails: Boolean) {
        if (sheet.isOpened && adapter.showStation &&
            this.line === line && this.station === station
        ) return

        this.line = line
        this.station = station
        this.hasDetails = showDetails

        val prep = {
            hideCleanup()
            showImpl()
            if (!adapter.showStation)
                adapter.showStation = true
        }

        if (adapter.showRoute && sheet.isOpened) {
            sheet.panelExpandCollapse(true) {
                sheet.panelShow(MapBottomPanelSheet.OPENED_CHANGE_VIEW, false, prep)
            }
        } else {
            sheet.panelShow(MapBottomPanelSheet.OPENED_REOPEN, false, prep)
        }
    }

    private fun hideCleanup() {
        sheet.app.bottomPanelOpen = false
        sheet.app.bottomPanelStation = null
    }

    fun hide() {
        hideCleanup()

        val after = {
            adapter.showStation = false
        }

        if (adapter.showRoute)
            sheet.panelExpandCollapse(true, after)
        else
            sheet.panelHide(after)
    }

    interface MapBottomPanelStationListener {
        fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectBeginStation(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectEndStation(line: MapSchemeLine?, station: MapSchemeStation?)
    }
}