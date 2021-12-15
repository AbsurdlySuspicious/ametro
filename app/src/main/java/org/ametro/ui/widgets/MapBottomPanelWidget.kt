package org.ametro.ui.widgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetItemBotStationBinding
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.misc.ColorUtils

private interface AdapterBinder<V: View, B: ViewBinding> {
    fun bindItem(view: V, bind: B)
}

private abstract class BaseItem(
    val viewType: Int,
    val priority: Int
)

private object RouteItem: BaseItem(Adapter.TYPE_ROUTE, 1)
private object StationItem: BaseItem(Adapter.TYPE_STATION, 2)

private class Holder(val viewType: Int, view: View, val binding: ViewBinding) : RecyclerView.ViewHolder(view)

private class Adapter(context: Context) : RecyclerView.Adapter<Holder>() {
    companion object {
        const val TYPE_ROUTE = 1
        const val TYPE_STATION = 2
    }

    private val inflater = LayoutInflater.from(context)
    private lateinit var recyclerView: RecyclerView

    private val itemList: MutableList<BaseItem> = mutableListOf()

    var showRoute: Boolean = false
        set(value) { field = value; refreshList() }
    var routeBinder: AdapterBinder<ViewGroup, ViewBinding>? = null
    var showStation: Boolean = false
        set(value) { field = value; refreshList() }
    var stationBinder: AdapterBinder<ConstraintLayout, WidgetItemBotStationBinding>? = null

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return when (viewType) {
            TYPE_STATION -> {
                val bind = WidgetItemBotStationBinding.inflate(inflater, parent, false)
                Holder(viewType, bind.root, bind)
            }
            TYPE_ROUTE -> TODO()
            else -> throw Exception("Unknown view $viewType")
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        when (holder.viewType) {
            TYPE_STATION -> stationBinder?.bindItem(
                holder.itemView as ConstraintLayout,
                holder.binding as WidgetItemBotStationBinding
            )
            TYPE_ROUTE -> TODO()
        }
    }

}

class MapBottomPanelWidget(
    private val view: NestedScrollView,
    private val app: ApplicationEx,
    private val listener: IMapBottomPanelEventListener
): AdapterBinder<ConstraintLayout, WidgetItemBotStationBinding> {

    private val density = view.context.resources.displayMetrics.density
    private val stationTintFg = ColorUtils.fromColorInt(Color.parseColor("#a9a9a9"))
    private val stationTintBg = ColorUtils.fromColorInt(Color.parseColor("#2a2a2a"))

    private val bottomSheet = BottomSheetBehavior.from(view)
    private val bindingParent = WidgetMapBottomPanelBinding.bind(view)
    private val binding = bindingParent.includeBotStation
    private val stationTextView = binding.station
    private val lineTextView = binding.line
    private val stationLayout = binding.stationLayout
    private val detailsHint = binding.detailsIcon
    private val detailsProgress = binding.detailsLoading
    private val lineIcon = binding.lineIcon
    private val beginButton = binding.actionStart
    private val endButton = binding.actionEnd

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(sheetView: View, newState: Int) {
            // Log.i("MEME", "Bottom sheet state: ${BottomSheetUtils.stateToString(newState)}")
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    if (pendingOpen) {
                        pendingOpen = false
                        showImpl()
                    } else {
                        hideCleanup()
                    }
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    updatePeekHeight(sheetView)
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    updatePeekHeight(sheetView)
                }
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    init {
        val progressTint =
            view.context.resources.getColor(R.color.panel_secondary_icon)
        detailsProgress.indeterminateDrawable.mutate().apply {
            setColorFilter(progressTint, PorterDuff.Mode.SRC_IN)
            detailsProgress.indeterminateDrawable = this
        }

        bottomSheet.apply {
            isHideable = true
            isDraggable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            addBottomSheetCallback(bottomSheetCallback)
        }
    }

    private fun updatePeekHeight(sheetView: View) {
        if (openTriggered) {
            openTriggered = false
            bottomSheet.setPeekHeight(sheetView.height, true)
        }
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
        get() = bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN

    private var pendingOpen: Boolean = false
    private var openTriggered: Boolean = false
    private var hasDetails: Boolean = false
    private var line: MapSchemeLine? = null
    private var station: MapSchemeStation? = null

    private val detailsVisibility
        get() = viewVisible(hasDetails)

    init {
        val clickListener = View.OnClickListener { v ->
            if (v === stationLayout && hasDetails) {
                detailsHint.visibility = View.INVISIBLE
                detailsProgress.visibility = View.VISIBLE
                this@MapBottomPanelWidget.listener.onShowMapDetail(line, station)
            } else if (v === beginButton) {
                this@MapBottomPanelWidget.listener.onSelectBeginStation(line, station)
            } else if (v === endButton) {
                this@MapBottomPanelWidget.listener.onSelectEndStation(line, station)
            }
        }
        view.setOnClickListener(clickListener)
        stationLayout.setOnClickListener(clickListener)
        beginButton.setOnClickListener(clickListener)
        endButton.setOnClickListener(clickListener)
    }

    fun detailsClosed() {
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
    }

    private fun showImpl() {
        view.visibility = View.VISIBLE
        stationTextView.text = station!!.displayName
        lineTextView.text = line!!.displayName
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
        (lineIcon.drawable as GradientDrawable).setColor(line!!.lineColor)

        routeStation(
            app.routeStart,
            binding.textStartHint,
            binding.replaceIconStart,
            binding.textStartStation
        )

        routeStation(
            app.routeEnd,
            binding.textEndHint,
            binding.replaceIconEnd,
            binding.textEndStation
        )

        app.bottomPanelOpen = true
        app.bottomPanelStation = Pair(line, station)

        bottomSheet.apply {
            if (state == BottomSheetBehavior.STATE_HIDDEN)
                state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun show(line: MapSchemeLine, station: MapSchemeStation, showDetails: Boolean) {
        if (isOpened && this.line === line && this.station === station) {
            return
        }

        this.line = line
        this.station = station
        this.hasDetails = showDetails
        this.openTriggered = true

        if (isOpened) {
            pendingOpen = true
            hideCleanup()
            hideImpl()
        } else {
            pendingOpen = false
            showImpl()
        }
    }

    private fun hideImpl() {
        bottomSheet.apply {
            if (state != BottomSheetBehavior.STATE_HIDDEN)
                state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun hideCleanup() {
        app.bottomPanelOpen = false
        app.bottomPanelStation = null
    }

    fun hide() {
        pendingOpen = false
        hideCleanup()
        hideImpl()
    }

    interface IMapBottomPanelEventListener {
        fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectBeginStation(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectEndStation(line: MapSchemeLine?, station: MapSchemeStation?)
    }

    override fun bindItem(view: ConstraintLayout, bind: WidgetItemBotStationBinding) {
        TODO("Not yet implemented")
    }
}