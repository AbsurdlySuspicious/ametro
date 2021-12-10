package org.ametro.render

import android.app.Activity
import android.app.ActivityManager
import android.graphics.*
import android.os.Handler
import android.view.View
import org.ametro.model.entities.MapScheme
import org.ametro.render.elements.DrawingElement

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
    private var isEntireMapCached = false
    private val mPrivateHandler = Handler()

    init {
        val ac = canvasView.context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        memoryClass = ac.memoryClass
        setScheme(renderProgram)
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

    fun draw(canvas: Canvas): Boolean {
        maximumBitmapWidth = canvas.maximumBitmapWidth
        maximumBitmapHeight = canvas.maximumBitmapHeight

        //Log.d(TAG,"draw map");
        if (cache == null) {
            // render at first run
            rebuildCache()
        }
        if (isRenderFailed) {
            return false
        }
        // prepare transform matrix
        val m = renderMatrix
        if (cache!!.Scale != scale) {
            // if we're zooming - at first "roll-back" previous cache transform
            m.set(cache!!.InvertedMatrix)
            // next apply current transformation
            m.postConcat(matrix)
        } else {
            // if we're using cache - simple translate origin
            m.setTranslate(currentX - cache!!.X, currentY - cache!!.Y)
        }
        canvas.clipRect(screenRect)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(cache!!.Image!!, m, null)
        if (isUpdatesEnabled) {
            //Log.w(TAG, "cache: " + StringUtil.formatRectF(cache.ViewRect) + " vs. screen: " + StringUtil.formatRectF(schemeRect) + ", hit = "+ cache.hit(schemeRect) );
            if (cache!!.Scale != scale) {
                postRebuildCache()
            } else if (!isEntireMapCached && !cache!!.hit(schemeRect)) {
                postUpdateCache()
            }
        }
        return !isRenderFailed
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
            val c = Canvas(newCache.Image!!)
            c.drawColor(Color.WHITE)
            c.setMatrix(newCache.CacheMatrix)
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
            val c = Canvas(newCache.Image!!)
            c.setMatrix(newCache.CacheMatrix)
            c.clipRect(newCache.SchemeRect)
            val elements = renderProgram!!.getClippedDrawingElements(newCache.SchemeRect)
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
            val c = Canvas(newCache.Image!!)
            val renderAll = splitRenderViewPort(newCache.SchemeRect, cache!!.SchemeRect)
            if (renderAll) {
                c.setMatrix(newCache.CacheMatrix)
                c.clipRect(newCache.SchemeRect)
                val elements = renderProgram!!.getClippedDrawingElements(newCache.SchemeRect)
                c.drawColor(Color.WHITE)
                for (elem in elements) {
                    elem.draw(c)
                }
            } else {
                c.save()
                c.setMatrix(newCache.CacheMatrix)
                c.clipRect(newCache.SchemeRect)
                val elements: List<DrawingElement> =
                    renderProgram!!.getClippedDrawingElements(renderViewPortHorizontal, renderViewPortVertical)
                c.drawColor(Color.WHITE)
                for (elem in elements) {
                    elem.draw(c)
                }
                c.restore()
                c.drawBitmap(cache!!.Image!!, newCache.X - cache!!.X, newCache.Y - cache!!.Y, null)
            }
            oldCache = cache
            cache = newCache
            if (!renderAll) {
                mPrivateHandler.removeCallbacks(renderPartialCacheRunnable)
                mPrivateHandler.postDelayed(renderPartialCacheRunnable, 300)
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

    private val renderPartialCacheRunnable = Runnable {
        renderPartialCache()
        canvasView.invalidate()
    }
    private val rebuildCacheRunnable = Runnable {
        rebuildCache()
        canvasView.invalidate()
    }
    private val updateCacheRunnable = Runnable {
        if (oldCache != null && oldCache!!.Scale == scale) {
            updatePartialCache()
            //canvasView.invalidate();
        } else {
            renderPartialCache()
            canvasView.invalidate()
        }
    }

    fun recycleCache() {
        if (cache != null) {
            cache!!.Image!!.recycle()
            cache!!.Image = null
            cache = null
        }
        if (oldCache != null) {
            oldCache!!.Image!!.recycle()
            oldCache!!.Image = null
            oldCache = null
        }
        System.gc()
    }

    private class MapCache {
        var CacheMatrix = Matrix()
        var InvertedMatrix = Matrix()
        var Scale = 0f
        var X = 0f
        var Y = 0f
        var SchemeRect = RectF()
        var Image: Bitmap? = null
        fun equals(width: Int, height: Int): Boolean {
            return Image!!.width == width && Image!!.height == height
        }

        fun hit(viewRect: RectF?): Boolean {
            return SchemeRect.contains(viewRect!!)
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
                        newCache.Image!!.recycle()
                        newCache.Image = null
                        System.gc()
                    }
                } else {
                    newCache = MapCache()
                }
                if (newCache.Image == null) {
                    newCache.Image = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                }
                newCache.CacheMatrix.set(matrix)
                newCache.InvertedMatrix.set(invertedMatrix)
                newCache.X = x
                newCache.Y = y
                newCache.Scale = scale
                newCache.SchemeRect.set(schemeRect!!)
                return newCache
            }
        }
    }

    private fun postRebuildCache() {
        mPrivateHandler.removeCallbacks(rebuildCacheRunnable)
        mPrivateHandler.removeCallbacks(renderPartialCacheRunnable)
        mPrivateHandler.removeCallbacks(updateCacheRunnable)
        mPrivateHandler.post(rebuildCacheRunnable)
    }

    private fun postUpdateCache() {
        mPrivateHandler.removeCallbacks(rebuildCacheRunnable)
        mPrivateHandler.removeCallbacks(renderPartialCacheRunnable)
        mPrivateHandler.removeCallbacks(updateCacheRunnable)
        mPrivateHandler.post(updateCacheRunnable)
    }

    fun onAttachedToWindow() {
        // do nothing
    }

    fun onDetachedFromWindow() {
        // do nothing
    }
}