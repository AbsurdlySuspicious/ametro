package org.ametro.render

import android.app.Activity
import android.app.ActivityManager
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.view.View
import org.ametro.R
import org.ametro.model.entities.MapScheme
import org.ametro.render.elements.DrawingElement

typealias ElementsToHighlight = (() -> java.util.HashSet<Int>?)?

class CanvasRenderer(private val canvasView: View, private val mapScheme: MapScheme, renderProgram: RenderProgram?) {

    private var renderProgram: RenderProgram? = null
    private var cache: MapCache? = null
    private var oldCache: MapCache? = null
    private val matrix = Matrix()
    private val mInvertedMatrix = Matrix()
    private val renderMatrix = Matrix()
    private val screenRect = RectF()
    private val schemeRect = RectF()
    private val renderViewPort = RectF()
    private val renderViewPortVertical = RectF()
    private val renderViewPortHorizontal = RectF()
    private val renderViewPortIntersection = RectF()
    private val memoryClass: Int
    private var maximumBitmapWidth = 0
    private var maximumBitmapHeight = 0
    private var scale = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var currentWidth = 0f
    private var currentHeight = 0f
    private val matrixValues = FloatArray(9)

    private var isRenderFailed = false
    private var isUpdatesEnabled = false
    private var isRebuildPending = false
    private var isEntireMapCached = false

    private lateinit var rendererThread: HandlerThread
    private lateinit var handler: Handler

    private val density: Float
    private val renderFailedErrorText: String
    private val renderFailedTextPaint = Paint().also {
        it.color = Color.RED
        it.textAlign = Paint.Align.CENTER
    }

    private fun createHandler(looper: Looper) = object : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REBUILD_CACHE -> {
                    rebuildCache()
                    canvasView.invalidate()
                }
                MSG_UPDATE_CACHE -> {
                    if (oldCache != null && oldCache!!.scale == scale) {
                        updatePartialCache()
                        //canvasView.invalidate();
                    } else {
                        renderPartialCache()
                        canvasView.invalidate()
                    }
                }
                MSG_RENDER_PARTIAL_CACHE -> {
                    renderPartialCache()
                    canvasView.invalidate()
                }
                MSG_HIGHLIGHT_ELEMENTS -> {
                    @Suppress("UNCHECKED_CAST")
                    val lazyIds = msg.obj as ElementsToHighlight
                    renderProgram!!.highlightsElements(lazyIds?.invoke())
                    rebuildOnDraw()
                    canvasView.invalidate()
                }
            }
        }
    }

    init {
        val res = canvasView.context.applicationContext.resources
        density = res.displayMetrics.density
        renderFailedErrorText = res.getString(R.string.render_failed)

        val ac = canvasView.context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        memoryClass = ac.memoryClass
        setScheme(renderProgram)
    }

    fun onAttachedToWindow() {
        rendererThread = HandlerThread("map-renderer")
        rendererThread.start()
        handler = createHandler(rendererThread.looper)
    }

    fun onDetachedFromWindow() {
        rendererThread.looper.quit()
    }

    fun setScheme(renderProgram: RenderProgram?) {
        this.renderProgram = renderProgram
        val m = Matrix()
        m.setTranslate(1.0f, 1.0f)
        setMatrix(m)
        recycleCache()
    }

    fun setUpdatesEnabled(enabled: Boolean) {
        isUpdatesEnabled = enabled
    }

    fun rebuildOnDraw() {
        isRebuildPending = true
    }

    fun highlightElements(lazyIds: ElementsToHighlight) {
        val msg = Message().also {
            it.what = MSG_HIGHLIGHT_ELEMENTS
            it.obj = lazyIds
        }
        handler.sendMessage(msg)
    }

    fun draw(canvas: Canvas) {
        canvas.save()

        if (cache != null) {
            drawImpl(canvas)
            if (isRebuildPending)
                postRebuildCache()
        } else {
            postRebuildCache()
        }

        if (isRenderFailed)
            drawFailure(canvas)

        canvas.restore()
    }

    private fun drawFailure(canvas: Canvas) {
        renderFailedTextPaint.textSize = density * 50f
        canvas.drawText(renderFailedErrorText, screenRect.width() / 2, screenRect.height() / 2, renderFailedTextPaint)
    }

    private fun drawImpl(canvas: Canvas) {
        maximumBitmapWidth = canvas.maximumBitmapWidth
        maximumBitmapHeight = canvas.maximumBitmapHeight

        // prepare transform matrix
        val m = renderMatrix
        if (cache!!.scale != scale) {
            // if we're zooming - at first "roll-back" previous cache transform
            m.set(cache!!.invertedMatrix)
            // next apply current transformation
            m.postConcat(matrix)
        } else {
            // if we're using cache - simple translate origin
            m.setTranslate(currentX - cache!!.x, currentY - cache!!.y)
        }
        canvas.clipRect(screenRect)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(cache!!.image!!, m, null)
        if (isUpdatesEnabled) {
            //Log.w(TAG, "cache: " + StringUtil.formatRectF(cache.ViewRect) + " vs. screen: " + StringUtil.formatRectF(schemeRect) + ", hit = "+ cache.hit(schemeRect) );
            if (cache!!.scale != scale) {
                postRebuildCache()
            } else if (!isEntireMapCached && !cache!!.hit(schemeRect)) {
                postUpdateCache()
            }
        }
    }

    /** set transformation matrix for content  */
    @Synchronized
    fun setMatrix(newMatrix: Matrix?) {
        matrix.set(newMatrix)
        matrix.invert(mInvertedMatrix)
        matrix.getValues(matrixValues)
        scale = matrixValues[Matrix.MSCALE_X]
        currentX = matrixValues[Matrix.MTRANS_X]
        currentY = matrixValues[Matrix.MTRANS_Y]
        currentWidth = mapScheme.width * scale
        currentHeight = mapScheme.height * scale
        updateViewRect()
        isRenderFailed = false
    }

    fun updateViewRect() {
        schemeRect[0f, 0f, canvasView.width.toFloat()] = canvasView.height.toFloat()
        mInvertedMatrix.mapRect(schemeRect)
        screenRect.set(schemeRect)
        matrix.mapRect(screenRect)
    }

    @Synchronized
    fun rebuildCache() {
        recycleCache()
        isRebuildPending = false
        isEntireMapCached = false
        if (currentWidth > maximumBitmapWidth || currentHeight > maximumBitmapHeight) {
            renderPartialCache()
            return
        }
        val memoryLimit = 4 * 1024 * 1024 * memoryClass / 16
        val bitmapSize = currentWidth.toInt() * currentHeight.toInt() * 2
        if (bitmapSize > memoryLimit) {
            renderPartialCache()
            return
        }
        try {
            renderEntireCache()
            isEntireMapCached = true
        } catch (ex: OutOfMemoryError) {
            recycleCache()
            renderPartialCache()
        }
    }

    @Synchronized
    private fun renderEntireCache() {
        try {
            //Log.w(TAG,"render entire");
            val viewRect = RectF(0f, 0f, currentWidth, currentHeight)
            val m = Matrix(matrix)
            m.postTranslate(-currentX, -currentY)
            val i = Matrix()
            m.invert(i)
            val newCache = MapCache.reuse(
                oldCache, currentWidth.toInt(), currentHeight.toInt(),
                m,
                i, 0f, 0f,
                scale,
                viewRect
            )
            val c = Canvas(newCache.image!!)
            c.drawColor(Color.WHITE)
            c.setMatrix(newCache.cacheMatrix)
            val elements: List<DrawingElement> = renderProgram!!.allDrawingElements
            c.drawColor(Color.WHITE)
            for (elem in elements) {
                elem.draw(c)
            }
            cache = newCache
        } catch (ex: Exception) {
            isRenderFailed = true
        }
    }

    @Synchronized
    private fun renderPartialCache() {
        try {
            //Log.w(TAG,"render partial");
            val newCache = MapCache.reuse(
                oldCache,
                canvasView.width,
                canvasView.height,
                matrix,
                mInvertedMatrix,
                currentX,
                currentY,
                scale,
                schemeRect
            )
            val c = Canvas(newCache.image!!)
            c.setMatrix(newCache.cacheMatrix)
            c.clipRect(newCache.schemeRect)
            val elements = renderProgram!!.getClippedDrawingElements(newCache.schemeRect)
            c.drawColor(Color.WHITE)
            for (elem in elements) {
                elem.draw(c)
            }
            oldCache = cache
            cache = newCache
        } catch (ex: Exception) {
            //Log.w(TAG,"render partial failed", ex);
            isRenderFailed = true
        }
    }

    @Synchronized
    private fun updatePartialCache() {
        try {
            //Log.w(TAG,"update partial");
            val newCache = MapCache.reuse(
                oldCache,
                canvasView.width,
                canvasView.height,
                matrix,
                mInvertedMatrix,
                currentX,
                currentY,
                scale,
                schemeRect
            )
            val c = Canvas(newCache.image!!)
            val renderAll = splitRenderViewPort(newCache.schemeRect, cache!!.schemeRect)
            if (renderAll) {
                c.setMatrix(newCache.cacheMatrix)
                c.clipRect(newCache.schemeRect)
                val elements = renderProgram!!.getClippedDrawingElements(newCache.schemeRect)
                c.drawColor(Color.WHITE)
                for (elem in elements) {
                    elem.draw(c)
                }
            } else {
                c.save()
                c.setMatrix(newCache.cacheMatrix)
                c.clipRect(newCache.schemeRect)
                val elements: List<DrawingElement> =
                    renderProgram!!.getClippedDrawingElements(renderViewPortHorizontal, renderViewPortVertical)
                c.drawColor(Color.WHITE)
                for (elem in elements) {
                    elem.draw(c)
                }
                c.restore()
                c.drawBitmap(cache!!.image!!, newCache.x - cache!!.x, newCache.y - cache!!.y, null)
            }
            oldCache = cache
            cache = newCache
            if (!renderAll) {
                handler.removeMessages(MSG_RENDER_PARTIAL_CACHE)
                handler.sendEmptyMessageDelayed(MSG_RENDER_PARTIAL_CACHE, 300)
            }
        } catch (ex: Exception) {
            isRenderFailed = true
        }
    }

    private fun splitRenderViewPort(schemeRect: RectF, cacheRect: RectF): Boolean {
        val vp = renderViewPort
        val v = renderViewPortVertical
        val h = renderViewPortHorizontal
        val i = renderViewPortIntersection
        vp.set(schemeRect)
        renderViewPortVertical.set(vp)
        renderViewPortHorizontal.set(vp)
        renderViewPortIntersection.set(vp)
        renderViewPortIntersection.intersect(cacheRect)
        var renderAll = false
        if (vp.right == i.right && vp.bottom == i.bottom) {
            h.bottom = i.top
            v.right = i.left
        } else if (vp.right == i.right && vp.top == i.top) {
            h.top = i.bottom
            v.right = i.left
        } else if (vp.left == i.left && vp.bottom == i.bottom) {
            h.bottom = i.top
            v.left = i.right
        } else if (vp.left == i.left && vp.top == i.top) {
            h.top = i.bottom
            v.left = i.right
        } else {
            renderAll = true
        }
        return renderAll
    }

    private fun clearQueue() {
        handler.removeMessages(MSG_REBUILD_CACHE)
        handler.removeMessages(MSG_RENDER_PARTIAL_CACHE)
        handler.removeMessages(MSG_UPDATE_CACHE)
    }

    private fun postRebuildCache() {
        clearQueue()
        handler.sendEmptyMessage(MSG_REBUILD_CACHE)
    }

    private fun postUpdateCache() {
        clearQueue()
        handler.sendEmptyMessage(MSG_UPDATE_CACHE)
    }

    fun recycleCache() {
        if (cache != null) {
            cache!!.image!!.recycle()
            cache!!.image = null
            cache = null
        }
        if (oldCache != null) {
            oldCache!!.image!!.recycle()
            oldCache!!.image = null
            oldCache = null
        }
        System.gc()
    }

    private class MapCache {
        var cacheMatrix = Matrix()
        var invertedMatrix = Matrix()
        var scale = 0f
        var x = 0f
        var y = 0f
        var schemeRect = RectF()
        var image: Bitmap? = null
        fun equals(width: Int, height: Int): Boolean {
            return image!!.width == width && image!!.height == height
        }

        fun hit(viewRect: RectF?): Boolean {
            return schemeRect.contains(viewRect!!)
        }

        companion object {
            fun reuse(
                oldCache: MapCache?,
                width: Int,
                height: Int,
                matrix: Matrix?,
                invertedMatrix: Matrix?,
                x: Float,
                y: Float,
                scale: Float,
                schemeRect: RectF?
            ): MapCache {
                val newCache: MapCache
                if (oldCache != null) {
                    newCache = oldCache
                    if (!newCache.equals(width, height)) {
                        newCache.image!!.recycle()
                        newCache.image = null
                        System.gc()
                    }
                } else {
                    newCache = MapCache()
                }
                if (newCache.image == null) {
                    newCache.image = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                }
                newCache.cacheMatrix.set(matrix)
                newCache.invertedMatrix.set(invertedMatrix)
                newCache.x = x
                newCache.y = y
                newCache.scale = scale
                newCache.schemeRect.set(schemeRect!!)
                return newCache
            }
        }
    }

    companion object {
        private const val MSG_HIGHLIGHT_ELEMENTS = 1
        private const val MSG_RENDER_PARTIAL_CACHE = 2
        private const val MSG_REBUILD_CACHE = 3
        private const val MSG_UPDATE_CACHE = 4
    }
}