package org.ametro.ui.views

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.os.Handler
import android.util.Pair
import android.view.MotionEvent
import android.widget.ScrollView
import org.ametro.model.MapContainer
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapScheme
import org.ametro.render.CanvasRenderer
import org.ametro.render.ElementsToHighlight
import org.ametro.render.RenderProgram
import org.ametro.ui.controllers.MultiTouchController
import org.ametro.ui.controllers.MultiTouchController.IMultiTouchListener
import kotlin.math.min

class MultiTouchMapView @JvmOverloads constructor(
    context: Context?,
    container: MapContainer? = null,
    schemeName: String? = null,
    private val viewportChangedListener: IViewportChangedListener? = null
) : ScrollView(context), IMultiTouchListener {
    private val multiTouchController: MultiTouchController
    private val renderer: CanvasRenderer
    private val mapScheme: MapScheme
    private val rendererProgram: RenderProgram
    private val hideScrollbarsRunnable = Runnable { fadeScrollBars() }
    private var verticalScrollOffset = 0
    private var horizontalScrollOffset = 0
    private var verticalScrollRange = 0
    private var horizontalScrollRange = 0
    private var changeCenterPoint: PointF? = null
    private var changeScale: Float? = null

    init {
        isScrollbarFadingEnabled = false
        isFocusable = true
        isFocusableInTouchMode = true
        isHorizontalScrollBarEnabled = true
        isVerticalScrollBarEnabled = true
        awakeScrollBars()
        mapScheme = container!!.getScheme(schemeName)
        multiTouchController = MultiTouchController(getContext(), this)
        rendererProgram = RenderProgram(container, schemeName!!)
        renderer = CanvasRenderer(this, mapScheme, rendererProgram)
        initializeViewport()
        renderer.rebuildMipmap()
    }

    override fun computeVerticalScrollOffset(): Int {
        return verticalScrollOffset
    }

    override fun computeVerticalScrollRange(): Int {
        return verticalScrollRange
    }

    override fun computeHorizontalScrollOffset(): Int {
        return horizontalScrollOffset
    }

    override fun computeHorizontalScrollRange(): Int {
        return horizontalScrollRange
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas)
        super.onDraw(canvas)
    }

    override var positionAndScaleMatrix: Matrix
        get() = multiTouchController.positionAndScale
        set(matrix) {
            val verticalPaddingScroll =
                multiTouchController.verticalPaddingFixed
            updateScrollBars(matrix, verticalPaddingScroll)
            renderer.setMatrix(matrix)
            viewportChangedListener!!.onViewportChanged(matrix)
        }

    var panelPadding: Int
        get() = multiTouchController.verticalPadding
        set(value) {
            multiTouchController.verticalPadding = value
            updateScrollBars(
                multiTouchController.positionAndScale,
                multiTouchController.verticalPaddingFixed
            )
        }

    override fun onTouchModeChanged(mode: Int) {
        renderer.setUpdatesEnabled(
            mode != MultiTouchController.MODE_ZOOM && mode != MultiTouchController.MODE_DT_ZOOM && mode != MultiTouchController.MODE_ANIMATION
        )
    }

    override fun onPerformClick(position: PointF?) {
        performClick()
    }

    override fun onPerformLongClick(position: PointF?) {
        performLongClick()
    }

    val touchPoint: MapPoint
        get() {
            val p = multiTouchController.touchPoint
            return MapPoint(p.x, p.y)
        }

    fun setCenterPositionAndScale(position: PointF?, zoom: Float?, animated: Boolean) {
        if (!animated) {
            changeCenterPoint = position
            changeScale = zoom
            invalidate()
        } else {
            multiTouchController.doScrollAndZoomAnimation(position, zoom)
        }
    }

    val centerPositionAndScale: Pair<PointF, Float>
        get() {
            val position = PointF()
            val scale = multiTouchController.getPositionAndScale(position)
            val width = width / scale
            val height = height / scale
            position.offset(width / 2, height / 2)
            return Pair(position, scale)
        }
    val scale: Float
        get() = multiTouchController.scale

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateViewRect()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return multiTouchController.onMultiTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        updateViewRect()
        super.onSizeChanged(w, h, oldWidth, oldHeight)
    }

    fun highlightsElements(ids: ElementsToHighlight) {
        renderer.highlightElements(ids)
    }

    private fun initializeViewport() {
        val area = RectF(0f, 0f, mapScheme.width.toFloat(), mapScheme.height.toFloat())
        val scaleX = width / area.width()
        val scaleY = height / area.height()
        val targetScale = min(scaleX, scaleY)
        val currentScale = scale
        val scale = min(targetScale, currentScale)
        setCenterPositionAndScale(PointF(area.centerX(), area.centerY()), scale, false)
    }

    private fun updateViewRect() {
        multiTouchController.setViewRect(
            mapScheme.width.toFloat(),
            mapScheme.height.toFloat(),
            RectF(0f, 0f, width.toFloat(), height.toFloat())
        )
        if (changeCenterPoint != null && changeScale != null) {
            val width = width / changeScale!!
            val height = height / changeScale!!
            changeCenterPoint!!.offset(-width / 2, -height / 2)
            multiTouchController.setPositionAndScale(changeCenterPoint!!, changeScale!!)
            changeCenterPoint = null
            changeScale = null
        }
    }

    private fun updateScrollBars(matrix: Matrix, verticalPadding: Float) {
        val values = FloatArray(9)
        matrix.getValues(values)
        val scale = values[Matrix.MSCALE_X]
        horizontalScrollRange = (mapScheme.width * scale).toInt()
        verticalScrollRange = ((mapScheme.height + verticalPadding) * scale).toInt()
        horizontalScrollOffset = -values[Matrix.MTRANS_X].toInt()
        verticalScrollOffset = -values[Matrix.MTRANS_Y].toInt()
        awakeScrollBars()
    }

    private fun awakeScrollBars() {
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = true
        dispatcher.removeCallbacks(hideScrollbarsRunnable)
        dispatcher.postDelayed(hideScrollbarsRunnable, SCROLLBAR_TIMEOUT)
        invalidate()
    }

    private fun fadeScrollBars() {
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        invalidate()
    }

    interface IViewportChangedListener {
        fun onViewportChanged(matrix: Matrix)
    }

    companion object {
        private const val SCROLLBAR_TIMEOUT: Long = 1000
        private val dispatcher = Handler()
    }
}