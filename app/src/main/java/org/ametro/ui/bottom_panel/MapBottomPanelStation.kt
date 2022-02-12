package org.ametro.ui.bottom_panel

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.databinding.WidgetItemBotStationBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.misc.ColorUtils

class MapBottomPanelStation(
    private val sheet: MapBottomPanelSheet,
    private val listener: MapBottomPanelStationListener
) : PanelAdapterBinder<WidgetItemBotStationBinding> {

    private val adapter = sheet.adapter
    private val context = sheet.sheetView.context

    private val density = context.resources.displayMetrics.density
    private val stationTintFg = ColorUtils.fromColorInt(Color.parseColor("#a9a9a9"))
    private val stationTintBg = ColorUtils.fromColorInt(Color.parseColor("#2a2a2a"))

    private lateinit var binding: WidgetItemBotStationBinding
    private val stationTextView get() = binding.station
    private val lineTextView get() = binding.line
    private val stationLayout get() = binding.stationLayout
    private val detailsHint get() = binding.detailsIcon
    private val detailsProgress get() = binding.detailsLoading
    private val lineIcon get() = binding.lineIcon
    private val beginButton get() = binding.actionStart
    private val endButton get() = binding.actionEnd

    init {
        sheet.addSheetStateCallbackPre { sheetView, newState ->
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    if (sheet.pendingOpen == MapBottomPanelSheet.PENDING_OPEN_NO) {
                        hideCleanup()
                        detach()
                    }
                }
                else -> {}
            }
        }
        adapter.stationBinder = this
    }

    override fun createPanel(bind: WidgetItemBotStationBinding) {
        binding = bind
    }

    override fun attachItem() {
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
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
    }

    private fun showImpl() {
        stationTextView.text = station!!.displayName
        lineTextView.text = line!!.displayName
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
        (lineIcon.drawable as GradientDrawable).setColor(line!!.lineColor)

        routeStation(
            sheet.app.currentRoute.routeStart,
            binding.textStartHint,
            binding.replaceIconStart,
            binding.textStartStation
        )

        routeStation(
            sheet.app.currentRoute.routeEnd,
            binding.textEndHint,
            binding.replaceIconEnd,
            binding.textEndStation
        )

        sheet.app.bottomPanelOpen = true
        sheet.app.bottomPanelStation = Pair(line!!, station!!)
    }

    fun show(line: MapSchemeLine, station: MapSchemeStation, showDetails: Boolean) {
        this.line = line
        this.station = station
        this.hasDetails = showDetails

        val prep = {
            hideCleanup()
            if (!adapter.showStation)
                adapter.showStation = true
            showImpl()
        }

        if (adapter.showRoute && sheet.isOpened) {
            sheet.panelExpandCollapse(true) {
                sheet.panelShow(MapBottomPanelSheet.OPENED_CHANGE_VIEW, false, prep)
            }
        } else {
            sheet.panelShow(MapBottomPanelSheet.OPENED_REOPEN, false, prep)
        }
    }

    private fun detach() {
        adapter.showStation = false
    }

    private fun hideCleanup() {
        sheet.app.bottomPanelOpen = false
        sheet.app.bottomPanelStation = null
    }

    fun hide() {
        hideCleanup()
        if (sheet.adapter.showStation) {
            if (sheet.adapter.showRoute)
                sheet.panelExpandCollapse(true) { detach() }
            else
                sheet.panelHide { detach() }
        }
    }

    interface MapBottomPanelStationListener {
        fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectBeginStation(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectEndStation(line: MapSchemeLine?, station: MapSchemeStation?)
    }
}