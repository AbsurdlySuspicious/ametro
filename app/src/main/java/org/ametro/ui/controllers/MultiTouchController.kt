package org.ametro.ui.controllers

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.Scroller
import org.ametro.utils.AnimationInterpolator
import kotlin.math.abs
import kotlin.math.min

class MultiTouchController(context: Context, private val listener: IMultiTouchListener) {
    interface IMultiTouchListener {
        var positionAndScaleMatrix: Matrix?
        fun onTouchModeChanged(mode: Int)
        fun onPerformClick(position: PointF?)
        fun onPerformLongClick(position: PointF?)
    }

    companion object {
        private const val MIN_FLING_TIME = 250
        private const val ANIMATION_TIME = 250
        const val MODE_NONE = 1
        const val MODE_INIT = 2
        const val MODE_DRAG_START = 3
        const val MODE_DRAG = 4
        const val MODE_ZOOM = 5
        const val MODE_SHORTPRESS_START = 6
        const val MODE_LONGPRESS_START = 8
        const val MODE_DT_ZOOM = 9
        const val MODE_ANIMATION = 100
        private const val MSG_SWITCH_TO_SHORTPRESS = 1
        private const val MSG_SWITCH_TO_LONGPRESS = 2
        private const val MSG_PROCESS_FLING = 3
        private const val MSG_PROCESS_ANIMATION = 4
        private const val MSG_DO_SHORTPRESS = 5
        private val DELAY_MSG_SHORTPRESS = ViewConfiguration.getTapTimeout().toLong()
        private val DELAY_MSG_LONGPRESS = ViewConfiguration.getLongPressTimeout().toLong()
        private val DELAY_MSG_DOUBLETAP = ViewConfiguration.getDoubleTapTimeout().toLong()
        private const val ZOOM_LEVEL_DISTANCE = 1.5f

        private object MultiTouchHandler : Handler()  {
            override fun handleMessage(msg: Message) {
                (msg.obj as MultiTouchController).apply {
                    when (msg.what) {
                        MSG_PROCESS_ANIMATION -> {
                            if (mode == MODE_ANIMATION) {
                                val more = computeAnimation()
                                if (more) {
                                    sendMessage(MSG_PROCESS_ANIMATION)
                                } else {
                                    controllerMode = MODE_NONE
                                    listener.positionAndScaleMatrix = matrix
                                }
                            }
                        }
                        MSG_PROCESS_FLING -> {
                            val more = computeScroll()
                            if (more) {
                                sendMessage(MSG_PROCESS_FLING)
                            }
                        }
                        MSG_SWITCH_TO_SHORTPRESS -> {
                            if (mode == MODE_INIT) {
                                controllerMode = MODE_SHORTPRESS_START
                                sendMessageDelay(MSG_SWITCH_TO_LONGPRESS, DELAY_MSG_LONGPRESS)
                            }
                        }
                        MSG_SWITCH_TO_LONGPRESS -> {
                            controllerMode = MODE_LONGPRESS_START
                            performLongClick()
                        }
                        MSG_DO_SHORTPRESS -> {
                            doubleTapZoomInit = false
                            performClick()
                        }
                        else -> super.handleMessage(msg)
                    }
                }
            }
        }
        
    }

    private var initialized = false
    private val touchSlopSquare: Int
    private val matrix = Matrix()
    private val invertedMatrix = Matrix()
    private val savedMatrix = Matrix()
    private val animationInterpolator = AnimationInterpolator()

    /** controller states  */
    private var doubleTapZoomInit = false
    private var mode = MODE_NONE

    /** point of first touch  */
    private val touchStartPoint = PointF()

    /** first touch time  */
    private var touchStartTime: Long = 0

    /** point between first and second touch  */
    private val zoomCenter = PointF()

    /** starting length between first and second touch  */
    private var zoomBase = 1f
    private var swipeZoomBase = 1f
    private val matrixValues = FloatArray(9)
    private var maxScale = 0f
    private var minScale = 0f
    private var contentHeight = 0f
    private var contentWidth = 0f
    private var displayRect: RectF? = null
    private val scroller: Scroller
    private var velocityTracker: VelocityTracker?
    private val displayMetrics: DisplayMetrics
    private val density: Float
    private val animationEndPoint = PointF()
    private val animationStartPoint = PointF()

    init {
        scroller = Scroller(context)
        val vc = ViewConfiguration.get(context)
        val slop = vc.scaledTouchSlop
        touchSlopSquare = slop * slop
        velocityTracker = null
        displayMetrics = context.resources.displayMetrics
        density = displayMetrics.density
    }
    
    private fun sendMessage(msgId: Int) {
        val msg = Message.obtain(MultiTouchHandler, msgId, this)
        MultiTouchHandler.sendMessage(msg)
    }
    
    private fun sendMessageDelay(msgId: Int, delayMs: Long) {
        val msg = Message.obtain(MultiTouchHandler, msgId, this)
        MultiTouchHandler.sendMessageDelayed(msg, delayMs)
    }
    
    private fun removeMessages(msgId: Int) {
        MultiTouchHandler.removeMessages(msgId)
    }

    /** Map point from model to screen coordinates  */
    fun mapPoint(point: PointF) {
        val pts = FloatArray(2)
        pts[0] = point.x
        pts[1] = point.y
        matrix.mapPoints(pts)
        point.x = pts[0]
        point.y = pts[1]
    }

    /** Map point from screen to model coordinates  */
    fun unmapPoint(point: PointF) {
        matrix.invert(invertedMatrix)
        val pts = FloatArray(2)
        pts[0] = point.x
        pts[1] = point.y
        invertedMatrix.mapPoints(pts)
        point.x = pts[0]
        point.y = pts[1]
    }

    fun setViewRect(newContentWidth: Float, newContentHeight: Float, newDisplayRect: RectF) {
        contentWidth = newContentWidth
        contentHeight = newContentHeight
        if (displayRect != null) {
            matrix.postTranslate(
                (newDisplayRect.width() - displayRect!!.width()) / 2,
                (newDisplayRect.height() - displayRect!!.height()) / 2
            )
        }
        displayRect = newDisplayRect
        swipeZoomBase = 60f * density
        // calculate zoom bounds
        maxScale = 2.0f * density
        minScale = Math.min(displayRect!!.width() / contentWidth, displayRect!!.height() / contentHeight)
        adjustScale()
        adjustPan()
        listener.positionAndScaleMatrix = matrix
    }

    fun onMultiTouchEvent(rawEvent: MotionEvent?): Boolean {
        if (mode == MODE_ANIMATION) {
            return false
        }
        val event = MotionEventWrapper.create(rawEvent)
        if (!initialized) {
            matrix.set(listener.positionAndScaleMatrix)
            initialized = true
        }
        val action = event.action
        var handled = true
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                handled = doActionDown(event)
            }
            MotionEventWrapper.ACTION_POINTER_DOWN -> {
                handled = doActionPointerDown(event)
            }
            MotionEvent.ACTION_UP, MotionEventWrapper.ACTION_POINTER_UP -> {
                handled = doActionUp(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                handled = doActionCancel(event)
            }
            MotionEvent.ACTION_MOVE -> {
                handled = doActionMove(event)
            }
        }
        listener.positionAndScaleMatrix = matrix
        return handled
    }

    private fun doActionDown(event: MotionEventWrapper): Boolean {
        controllerMode = if (!scroller.isFinished) {
            scroller.abortAnimation()
            MODE_DRAG_START
        } else {
            MODE_INIT
        }
        touchStartPoint[event.x] = event.y
        touchStartTime = event.eventTime
        if (mode == MODE_INIT) {
            if (doubleTapZoomInit) {
                removeMessages(MSG_DO_SHORTPRESS)
                doubleTapZoomInit = false
                zoomStart()
                zoomCenter.set(touchStartPoint)
                controllerMode = MODE_DT_ZOOM
                return true
            } else {
                sendMessageDelay(MSG_SWITCH_TO_SHORTPRESS, DELAY_MSG_SHORTPRESS)
            }
        }
        velocityTracker = VelocityTracker.obtain()
        savedMatrix.set(matrix)
        return true
    }

    /** Determine the distance between the first two fingers  */
    private fun distance(event: MotionEventWrapper): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun distance(event: MotionEventWrapper, secondPoint: PointF): Float {
        val x = event.x - secondPoint.x
        val y = event.y - secondPoint.y
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun swipeScale(event: MotionEventWrapper): Float {
        val dist = event.y - zoomCenter.y
        return dist / swipeZoomBase + 1f
    }

    private fun doActionPointerDown(event: MotionEventWrapper): Boolean {
        zoomBase = distance(event)
        if (zoomBase > 10f) {
            zoomStart()
            val x = event.getX(0) + event.getX(1)
            val y = event.getY(0) + event.getY(1)
            zoomCenter[x / 2] = y / 2
            controllerMode = MODE_ZOOM
        }
        return true
    }

    private fun zoomStart() {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        savedMatrix.set(matrix)
    }

    private inline fun zoomMove(scaleF: () -> Float) {
        matrix.set(savedMatrix)
        var scale = scaleF()
        matrix.getValues(matrixValues)
        val currentScale = matrixValues[Matrix.MSCALE_X]

        // limit zoom
        if (scale * currentScale > maxScale) {
            scale = maxScale / currentScale
        } else if (scale * currentScale < minScale) {
            scale = minScale / currentScale
        }
        matrix.postScale(scale, scale, zoomCenter.x, zoomCenter.y)
        adjustPan()
    }

    private fun doActionMove(event: MotionEventWrapper): Boolean {
        when (mode) {
            MODE_NONE, MODE_LONGPRESS_START -> {
                // no dragging during scroll zoom animation or while long press is not released
                return false
            }
            MODE_ZOOM -> {
                val newDist = distance(event)
                if (newDist > 10f)
                    zoomMove {  newDist / zoomBase }
                return true
            }
            MODE_DT_ZOOM -> {
                zoomMove { swipeScale(event) }
                return true
            }
            else -> {
                velocityTracker!!.addMovement(event.getEvent())
                if (mode != MODE_DRAG) {
                    val deltaX = (touchStartPoint.x - event.x).toInt()
                    val deltaY = (touchStartPoint.y - event.y).toInt()
                    if (deltaX * deltaX + deltaY * deltaY < touchSlopSquare) {
                        return false
                    }
                    if (mode == MODE_SHORTPRESS_START) {
                        removeMessages(MSG_SWITCH_TO_LONGPRESS)
                    } else if (mode == MODE_INIT) {
                        removeMessages(MSG_SWITCH_TO_SHORTPRESS)
                    }
                    controllerMode = MODE_DRAG
                }
                matrix.set(savedMatrix)
                val dx = event.x - touchStartPoint.x
                val dy = event.y - touchStartPoint.y
                matrix.postTranslate(dx, dy)
                adjustPan()
                return true
            }
        }
    }

    private fun doActionUp(event: MotionEventWrapper): Boolean {
        when (mode) {
            MODE_INIT, MODE_SHORTPRESS_START -> {
                removeMessages(MSG_DO_SHORTPRESS)
                removeMessages(MSG_SWITCH_TO_SHORTPRESS)
                removeMessages(MSG_SWITCH_TO_LONGPRESS)
                sendMessageDelay(MSG_DO_SHORTPRESS, DELAY_MSG_DOUBLETAP)
                doubleTapZoomInit = true
                if (velocityTracker != null) {
                    velocityTracker!!.recycle()
                    velocityTracker = null
                }
                controllerMode = MODE_NONE
                return true
            }
            MODE_LONGPRESS_START -> {}
            MODE_DRAG, MODE_DRAG_START ->
                // if the user waits a while w/o moving before the
                // up, we don't want to do a fling
                if (event.eventTime - touchStartTime <= MIN_FLING_TIME) {
                    velocityTracker!!.addMovement(event.getEvent())
                    velocityTracker!!.computeCurrentVelocity(1000)
                    matrix.getValues(matrixValues)
                    val currentY = matrixValues[Matrix.MTRANS_Y]
                    val currentX = matrixValues[Matrix.MTRANS_X]
                    val currentScale = matrixValues[Matrix.MSCALE_X]
                    val currentHeight = contentHeight * currentScale
                    val currentWidth = contentWidth * currentScale
                    val vx = -velocityTracker!!.xVelocity.toInt() / 2
                    val vy = -velocityTracker!!.yVelocity.toInt() / 2
                    val maxX = Math.max(currentWidth - displayRect!!.width(), 0f).toInt()
                    val maxY = Math.max(currentHeight - displayRect!!.height(), 0f).toInt()
                    scroller.fling(-currentX.toInt(), -currentY.toInt(), vx, vy, 0, maxX, 0, maxY)
                    sendMessage(MSG_PROCESS_FLING)
                }
        }
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
        controllerMode = MODE_NONE
        return true
    }

    private fun doActionCancel(event: MotionEventWrapper): Boolean {
        removeMessages(MSG_SWITCH_TO_SHORTPRESS)
        removeMessages(MSG_SWITCH_TO_LONGPRESS)
        removeMessages(MSG_DO_SHORTPRESS)
        controllerMode = MODE_NONE
        return true
    }

    /** adjust map position to prevent zoom to outside of map  */
    private fun adjustScale() {
        matrix.getValues(matrixValues)
        val currentScale = matrixValues[Matrix.MSCALE_X]
        if (currentScale < minScale) {
            matrix.setScale(minScale, minScale)
        }
    }

    /** adjust map position to prevent pan to outside of map  */
    private fun adjustPan() {
        matrix.getValues(matrixValues)
        val currentY = matrixValues[Matrix.MTRANS_Y]
        val currentX = matrixValues[Matrix.MTRANS_X]
        val currentScale = matrixValues[Matrix.MSCALE_X]
        val currentHeight = contentHeight * currentScale
        val currentWidth = contentWidth * currentScale
        val drawingRect = RectF(
            currentX, currentY, currentX + currentWidth,
            currentY + currentHeight
        )
        val diffUp = Math.min(
            displayRect!!.bottom - drawingRect.bottom,
            displayRect!!.top - drawingRect.top
        )
        val diffDown = Math.max(
            displayRect!!.bottom - drawingRect.bottom,
            displayRect!!.top - drawingRect.top
        )
        val diffLeft = Math.min(
            displayRect!!.left - drawingRect.left,
            displayRect!!.right - drawingRect.right
        )
        val diffRight = Math.max(
            displayRect!!.left - drawingRect.left,
            displayRect!!.right - drawingRect.right
        )
        var dx = 0f
        var dy = 0f
        if (diffUp > 0) {
            dy += diffUp
        }
        if (diffDown < 0) {
            dy += diffDown
        }
        if (diffLeft > 0) {
            dx += diffLeft
        }
        if (diffRight < 0) {
            dx += diffRight
        }
        if (currentWidth < displayRect!!.width()) {
            dx = -currentX + (displayRect!!.width() - currentWidth) / 2
        }
        if (currentHeight < displayRect!!.height()) {
            dy = -currentY + (displayRect!!.height() - currentHeight) / 2
        }
        matrix.postTranslate(dx, dy)
    }

    fun computeScroll(): Boolean {
        val more = scroller.computeScrollOffset()
        if (more) {
            val x = scroller.currX.toFloat()
            val y = scroller.currY.toFloat()
            matrix.getValues(matrixValues)
            val currentY = -matrixValues[Matrix.MTRANS_Y]
            val currentX = -matrixValues[Matrix.MTRANS_X]
            val dx = currentX - x
            val dy = currentY - y
            matrix.postTranslate(dx, dy)
            adjustPan()
            listener.positionAndScaleMatrix = matrix
        }
        return more
    }

    fun computeAnimation(): Boolean {
        if (mode == MODE_ANIMATION) {
            animationInterpolator.next()
            if (animationInterpolator.hasScale()) {
                val scale = animationInterpolator.scale / scale
                matrix.postScale(scale, scale)
            }
            if (animationInterpolator.hasScroll()) {
                val newCenter = animationInterpolator.point
                mapPoint(newCenter)
                val dx = newCenter.x - displayRect!!.width() / 2
                val dy = newCenter.y - displayRect!!.height() / 2
                matrix.postTranslate(-dx, -dy)
            }
            adjustScale()
            adjustPan()
            listener.positionAndScaleMatrix = matrix
            return animationInterpolator.more()
        }
        return false
    }

    /*package*/
    var controllerMode: Int
        get() = mode
        set(newMode) {
            val fireUpdate = mode != newMode
            mode = newMode
            if (fireUpdate) {
                listener.onTouchModeChanged(newMode)
            }
        }

    val positionAndScale: Matrix
        get() = Matrix(matrix)

    val screenTouchPoint: PointF
        get() = PointF(
            touchStartPoint.x,
            touchStartPoint.y
        )

    /** Return last touch point in model coordinates  */
    val touchPoint: PointF
        get() {
            val p = PointF()
            p.set(touchStartPoint)
            unmapPoint(p)
            return p
        }
    val scale: Float
        get() {
            matrix.getValues(matrixValues)
            return matrixValues[Matrix.MSCALE_X]
        }
    val touchRadius: Float
        get() = touchSlopSquare.toFloat()

    fun getPositionAndScale(position: PointF?): Float {
        matrix.getValues(matrixValues)
        val scale = matrixValues[Matrix.MSCALE_X]
        position?.set(
            -matrixValues[Matrix.MTRANS_X] / scale,
            -matrixValues[Matrix.MTRANS_Y] / scale
        )
        return scale
    }

    fun setPositionAndScale(position: PointF, scale: Float) {
        matrix.setScale(scale, scale)
        matrix.postTranslate(-position.x * scale, -position.y * scale)
        adjustScale()
        adjustPan()
        listener.positionAndScaleMatrix = matrix
    }

    fun performLongClick() {
        listener.onPerformLongClick(touchPoint)
    }

    fun performClick() {
        listener.onPerformClick(touchPoint)
    }

    fun doScrollAndZoomAnimation(center: PointF?, scale: Float?) {
        if (mode == MODE_NONE || mode == MODE_LONGPRESS_START) {
            animationStartPoint[displayRect!!.width() / 2] = displayRect!!.height() / 2
            unmapPoint(animationStartPoint)
            if (center != null) {
                animationEndPoint.set(center)
            } else {
                animationEndPoint.set(animationStartPoint)
            }
            val currentScale = this.scale
            animationInterpolator.begin(
                animationStartPoint,
                animationEndPoint,
                currentScale,
                scale ?: currentScale,
                ANIMATION_TIME.toLong()
            )
            sendMessage(MSG_PROCESS_ANIMATION)
            controllerMode = MODE_ANIMATION
        }
    }
}