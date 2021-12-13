package org.ametro.ui.widgets

import android.animation.Animator
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.Constants
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.misc.ColorUtils


class MapBottomPanelWidget(
    private val view: ViewGroup,
    private val binding: WidgetMapBottomPanelBinding,
    private val app: ApplicationEx,
    private val listener: IMapBottomPanelEventListener
) :
    Animator.AnimatorListener {

    private val resources =
        view.context.resources
    private val routeStationTint =
        resources.getColor(R.color.panel_actions_station_tint)
    private val density =
        view.context.resources.displayMetrics.density

    private val stationTextView = binding.station
    private val lineTextView = binding.line
    private val stationLayout = binding.stationLayout
    private val detailsHint = binding.detailsIcon
    private val detailsProgress = binding.detailsLoading
    private val lineIcon = binding.lineIcon
    private val beginButton = binding.actionStart
    private val endButton = binding.actionEnd

    init {
        val progressTint =
            view.context.resources.getColor(R.color.panel_background_button_icon)
        detailsProgress.indeterminateDrawable.mutate().apply {
            setColorFilter(progressTint, PorterDuff.Mode.SRC_IN)
            detailsProgress.indeterminateDrawable = this
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
            val bgColor =
                info!!.first.lineColor
            val fgColor = ColorUtils
                .fromColorInt(bgColor)
                .screen(routeStationTint)
                .toColorInt()
            val padding = Rect(
                /* left   */ (density * 5f).toInt(),
                /* top    */ (density * 2f).toInt(),
                /* right  */ (density * 5f).toInt(),
                /* bottom */ (density * 6f).toInt()
            )

            val draw = { width: Int, height: Int ->
                val w = width.toFloat()
                val h = height.toFloat()
                val rnd = density * 2.5f
                val off = density * 3f

                val bitmap = Bitmap
                    .createBitmap(width, height, Bitmap.Config.ARGB_8888)

                Canvas(bitmap).apply {
                    drawRoundRect(RectF(0f, 0f, w, h), rnd, rnd, Paint().apply { color = bgColor })
                    drawRoundRect(RectF(0f, 0f, w, h - off), rnd, rnd, Paint().apply { color = fgColor })
                }

                bitmap
            }


            val bg = PaintDrawable().apply {
                val shaderMode = Shader.TileMode.CLAMP
                shape = RectShape()
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

    private val hideAnimation = Runnable {
        app.bottomPanelOpen = false
        app.bottomPanelStation = null

        view.animate()
            .setDuration(Constants.ANIMATION_DURATION)
            .setListener(this@MapBottomPanelWidget)
            .translationY(view.height.toFloat())
    }

    private val showAnimation = Runnable {
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

        view.animate()
            .setDuration(Constants.ANIMATION_DURATION)
            .setListener(this@MapBottomPanelWidget)
            .translationY(0f)
    }

    private var actionOnEndAnimation: Runnable? = null
    var isOpened: Boolean = false
        private set

    private val detailsVisibility
        get() = viewVisible(hasDetails)

    private var wasOpened = false
    private var firstTime: Boolean = true
    private var hasDetails: Boolean = false
    private var line: MapSchemeLine? = null
    private var station: MapSchemeStation? = null

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

    fun show(line: MapSchemeLine, station: MapSchemeStation, showDetails: Boolean) {
        if (isOpened && this.line === line && this.station === station) {
            return
        }

        this.line = line
        this.station = station
        this.hasDetails = showDetails
        this.wasOpened = false

        if (!isOpened && !firstTime) {
            isOpened = true
            showAnimation.run()
            return
        }

        isOpened = true
        firstTime = false
        actionOnEndAnimation = showAnimation
        hideAnimation.run()
    }

    fun hide() {
        if (!isOpened) {
            return
        }
        isOpened = false
        hideAnimation.run()
    }

    override fun onAnimationStart(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator) {
        if (!isOpened) view.visibility = View.INVISIBLE
        if (actionOnEndAnimation != null) {
            actionOnEndAnimation!!.run()
            actionOnEndAnimation = null
        }
    }

    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
    interface IMapBottomPanelEventListener {
        fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectBeginStation(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectEndStation(line: MapSchemeLine?, station: MapSchemeStation?)
    }
}