package org.ametro.ui.widgets

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.misc.BottomSheetUtils
import org.ametro.utils.misc.ColorUtils


class MapBottomPanelWidget(
    private val view: ConstraintLayout,
    private val app: ApplicationEx,
    private val listener: IMapBottomPanelEventListener
) {

    private val density = view.context.resources.displayMetrics.density
    private val stationTintFg = ColorUtils.fromColorInt(Color.parseColor("#a9a9a9"))
    private val stationTintBg = ColorUtils.fromColorInt(Color.parseColor("#2a2a2a"))

    private val binding = WidgetMapBottomPanelBinding.bind(view)
    private val bottomSheet = BottomSheetBehavior.from(view)
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
            Log.i("MEME", "Bottom sheet state: ${BottomSheetUtils.stateToString(newState)}")
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
                    // updatePeekHeight(sheetView)
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

    var testAnim: Boolean = false
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

        val testAnimator = { testView: View, from: Int, to: Int, after: () -> Unit ->
            ValueAnimator.ofInt(from, to).apply {
                addUpdateListener {
                    testView.layoutParams = testView.layoutParams.apply {
                        height = animatedValue as Int
                    }
                }
                doOnEnd {
                    testView.layoutParams = testView.layoutParams
                        .apply { height = to }
                    after()
                }
                start()
            }
        }

        binding.testButton.setOnClickListener {
            val testView = binding.test
            if (testAnim) {
                testAnimator(testView, testView.measuredHeight, 1) {
                    testView.visibility = View.INVISIBLE
                    binding.testButton.text = "G"
                    testAnim = false
                }
            } else {
                testView.visibility = View.VISIBLE
                testAnimator(testView, 1, binding.testInside.height) {
                    binding.testButton.text = "V"
                    testAnim = true
                }
            }
        }
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
}