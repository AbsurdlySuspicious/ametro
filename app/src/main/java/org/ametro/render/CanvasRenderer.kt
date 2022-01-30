package org.ametro.render

import android.app.Activity
import android.app.ActivityManager
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.View
import org.ametro.R
import org.ametro.model.entities.MapScheme
import org.ametro.render.elements.DrawingElement
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

typealias ElementsToHighlight = (() -> java.util.HashSet<Int>?)?

class CanvasRenderer(private val canvasView: View, private val mapScheme: MapScheme, renderProgram: RenderProgram?) {

    companion object {
        private const val MSG_HIGHLIGHT_ELEMENTS = 1
        private const val MSG_RENDER_PARTIAL_CACHE = 2
        private const val MSG_REBUILD_CACHE = 3
        private const val MSG_UPDATE_CACHE = 4

        private val rendererThread =
            HandlerThread("map-renderer").also { it.start() }
    }

    private var renderProgram: RenderProgram? = null

    private val cache: AtomicReference<MapCache?> = AtomicReference(null)
    private val oldCache: AtomicReference<MapCache?> = AtomicReference(null)
    private val swapCache: AtomicReference<MapCache?> = AtomicReference(null)
    private val mipmapCache: AtomicReference<MapCache?> = AtomicReference(null)

    private val matrix = Matrix()
    private val invertedMatrix = Matrix()
    private val renderMatrix = Matrix()
    private val bgMatrix = Matrix()
    private val matrixValues = FloatArray(9)

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

    private val isRenderFailed = AtomicBoolean(false)
    private val isUpdatesEnabled = AtomicBoolean(false)
    private val isRebuildPending = AtomicBoolean(false)
    private val isCacheRebuilding = AtomicBoolean(false)
    private val isEntireMapCached = AtomicBoolean(false)

    private val density: Float
    private val renderFailedErrorText: String
    private val renderFailedTextPaint = Paint().also {
        it.color = Color.RED
        it.textAlign = Paint.Align.CENTER
    }

    private val handler = object : Handler(rendererThread.looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REBUILD_CACHE -> {
                    rebuildCache()
                    canvasView.invalidate()
                }
                MSG_UPDATE_CACHE -> {
                    val oldCache = oldCache.get()
                    if (oldCache != null && oldCache.scale == scale) {
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
                    // rebuildOnDraw()
                    // canvasView.invalidate()
                    rebuildMipmap()
                    postRebuildCache()
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

    fun setScheme(renderProgram: RenderProgram?) {
        this.renderProgram = renderProgram
        val m = Matrix()
        m.setTranslate(1.0f, 1.0f)
        setMatrix(m)
        recycleCache(full = true)
    }

    fun setUpdatesEnabled(enabled: Boolean) {
        isUpdatesEnabled.set(enabled)
    }

    fun rebuildOnDraw() {
        isRebuildPending.set(true)
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

        val swapCache = this.swapCache.get()
        val mainCache = this.cache.get()

        if (isCacheRebuilding.get() && swapCache?.image != null) {
            Log.d("AM1", "draw: rebuilding + swap cache")
            drawImpl(canvas, swapCache, noUpdates = true)
        } else if (mainCache?.image != null) {
            Log.d("AM1", "draw: has cache")
            drawImpl(canvas, mainCache, noUpdates = false)
            if (isRebuildPending.get())
                postRebuildCache().also { Log.d("AM1", "draw: rebuild pending") }
        } else {
            val willRebuild = isCacheRebuilding.get()
            Log.d("AM1", "draw: no cache, will rebuild: ${!willRebuild}")
            if (!willRebuild)
                postRebuildCache()
        }

        if (isRenderFailed.get())
            drawFailure(canvas)

        canvas.restore()
    }

    private fun drawFailure(canvas: Canvas) {
        renderFailedTextPaint.textSize = density * 50f
        canvas.drawText(renderFailedErrorText, screenRect.width() / 2, screenRect.height() / 2, renderFailedTextPaint)
    }

    private fun drawImpl(canvas: Canvas, drawCache: MapCache, noUpdates: Boolean) {
        maximumBitmapWidth = canvas.maximumBitmapWidth
        maximumBitmapHeight = canvas.maximumBitmapHeight
        val entireMapCached = isEntireMapCached.get()
        var mipmap: MapCache? = null
        val bgColor = Color.WHITE

        // prepare transform matrix
        val m = renderMatrix
        if (drawCache.scale != scale) {
            // if we're zooming - at first "roll-back" previous cache transform
            m.set(drawCache.invertedMatrix)
            // next apply current transformation
            m.postConcat(matrix)
        } else {
            // if we're using cache - simple translate origin
            m.setTranslate(currentX - drawCache.x, currentY - drawCache.y)
        }

        canvas.clipRect(screenRect)
        canvas.drawColor(bgColor)
        if (!entireMapCached && mipmapCache.get().also { mipmap = it } != null) {
            canvas.drawBitmap(mipmap!!.image!!, matrix, null)
            Log.d("AM1", "draw: mipmap path")
        } else {
            Log.d("AM1", "draw: entire path")
        }
        canvas.drawBitmap(drawCache.image!!, m, null)

        if (!noUpdates && isUpdatesEnabled.get()) {
            //Log.w(TAG, "cache: " + StringUtil.formatRectF(cache.ViewRect) + " vs. screen: " + StringUtil.formatRectF(schemeRect) + ", hit = "+ cache.hit(schemeRect) );
            if (drawCache.scale != scale) {
                postRebuildCache()
            } else if (!entireMapCached && !drawCache.hit(schemeRect)) {
                postUpdateCache()
            }
        }
    }

    /** set transformation matrix for content  */
    @Synchronized
    fun setMatrix(newMatrix: Matrix?) {
        matrix.set(newMatrix)
        matrix.invert(invertedMatrix)
        matrix.getValues(matrixValues)
        scale = matrixValues[Matrix.MSCALE_X]
        currentX = matrixValues[Matrix.MTRANS_X]
        currentY = matrixValues[Matrix.MTRANS_Y]
        currentWidth = mapScheme.width * scale
        currentHeight = mapScheme.height * scale
        updateViewRect()
        isRenderFailed.set(false)
    }

    fun updateViewRect() {
        schemeRect[0f, 0f, canvasView.width.toFloat()] = canvasView.height.toFloat()
        invertedMatrix.mapRect(schemeRect)
        screenRect.set(schemeRect)
        matrix.mapRect(screenRect)
    }

    @Synchronized
    fun rebuildMipmap() {
        Log.d("AM1", "rebuild mipmap")
        val r = RectF(0f, 0f, mapScheme.width.toFloat(), mapScheme.height.toFloat())
        val mipmapOld = mipmapCache.getAndSet(null)
        renderEntireCacheTo(mipmapCache, mipmapOld, bgMatrix, r, 1f)
    }

    @Synchronized
    fun rebuildCache() {
        Log.d("AM1", "rebuild cache")
        isCacheRebuilding.set(true)
        isRebuildPending.set(false)
        isEntireMapCached.set(false)
        try {
            swapCache.set(cache.get())
            cache.set(null)
            recycleCache(noSwap = true, noGC = true)

            if (currentWidth > maximumBitmapWidth || currentHeight > maximumBitmapHeight) {
                renderPartialCache()
                Log.d("AM1", "rebuild cache: end partial 1")
                return
            }
            val memoryLimit = 4 * 1024 * 1024 * memoryClass / 16
            val bitmapSize = currentWidth.toInt() * currentHeight.toInt() * 2
            if (bitmapSize > memoryLimit) {
                renderPartialCache()
                Log.d("AM1", "rebuild cache: end partial 2")
                return
            }
            try {
                renderEntireCache()
                isEntireMapCached.set(true)
                Log.d("AM1", "rebuild cache: end entire")
            } catch (ex: OutOfMemoryError) {
                recycleCache(full = true, noSwap = true)
                renderPartialCache()
                Log.d("AM1", "rebuild cache: end partial oom")
            }
        } finally {
            isCacheRebuilding.set(false)
            recycleCache(noOld = true)
        }
    }

    @Synchronized
    private fun renderEntireCache() {
        val r = RectF(0f, 0f, currentWidth, currentHeight)
        val m = Matrix(matrix)
        m.postTranslate(-currentX, -currentY)
        renderEntireCacheTo(cache, oldCache.get(), m, r, scale)
    }

    private fun renderEntireCacheTo(to: AtomicReference<MapCache?>, reuse: MapCache?, m: Matrix,
                                    viewRect: RectF, scale: Float) {
        try {
            //Log.w(TAG,"render entire");
            val i = Matrix().also { m.invert(it) }
            val newCache = MapCache.reuse(
                reuse, viewRect.right.toInt(), viewRect.bottom.toInt(),
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
            to.set(newCache)
        } catch (ex: Exception) {
            isRenderFailed.set(true)
        }
    }

    @Synchronized
    private fun renderPartialCache() {
        try {
            //Log.w(TAG,"render partial");
            val newCache = MapCache.reuse(
                oldCache.get(),
                canvasView.width,
                canvasView.height,
                matrix,
                invertedMatrix,
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
            oldCache.set(cache.get())
            cache.set(newCache)
        } catch (ex: Exception) {
            //Log.w(TAG,"render partial failed", ex);
            isRenderFailed.set(true)
        }
    }

    @Synchronized
    private fun updatePartialCache() {
        try {
            //Log.w(TAG,"update partial");
            val newCache = MapCache.reuse(
                oldCache.get(),
                canvasView.width,
                canvasView.height,
                matrix,
                invertedMatrix,
                currentX,
                currentY,
                scale,
                schemeRect
            )
            val cache = this.cache.get()!!
            val c = Canvas(newCache.image!!)
            val renderAll = splitRenderViewPort(newCache.schemeRect, cache.schemeRect)
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
                c.drawBitmap(cache.image!!, newCache.x - cache.x, newCache.y - cache.y, null)
            }
            oldCache.set(cache)
            this.cache.set(newCache)
            if (!renderAll) {
                handler.removeMessages(MSG_RENDER_PARTIAL_CACHE)
                handler.sendEmptyMessageDelayed(MSG_RENDER_PARTIAL_CACHE, 300)
            }
        } catch (ex: Exception) {
            isRenderFailed.set(true)
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

    private fun recycleCacheSingleNow(cache: MapCache?, noGC: Boolean = false) {
        if (cache == null) return
        val image = cache.image
        cache.image = null
        image?.let {
            it.recycle()
            if (!noGC) System.gc()
        }
    }

    private fun recycleCacheSingle(toRecycle: ArrayList<Bitmap>, cache: MapCache?) {
        if (cache == null) return
        val image = cache.image
        cache.image = null
        image?.let { toRecycle.add(it) }
    }

    fun recycleCache(full: Boolean = false, noSwap: Boolean = false, noOld: Boolean = false, noGC: Boolean = false) {
        val q: ArrayList<Bitmap> = ArrayList(3)

        if (!noSwap) recycleCacheSingle(q, swapCache.getAndSet(null))
        if (!noOld) recycleCacheSingle(q, oldCache.getAndSet(null))
        if (full) recycleCacheSingle(q, cache.getAndSet(null))

        q.forEach { it.recycle() }
        if (!noGC) System.gc()
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
}