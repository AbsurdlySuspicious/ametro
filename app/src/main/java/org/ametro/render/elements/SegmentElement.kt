package org.ametro.render.elements

import android.graphics.*
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeSegment
import org.ametro.render.RenderConstants
import org.ametro.render.utils.RenderUtils

class SegmentElement(line: MapSchemeLine, segment: MapSchemeSegment) : DrawingElement() {
    val paints = arrayOfNulls<Paint>(RenderConstants.LAYER_COUNT)
    val path: Path
    override val boundingBox: Rect
    override val priority: Int

    init {
        uid = segment.uid
        val lineColor = line.lineColor
        val isWorking = segment.isWorking
        val lineWidth = line.lineWidth.toFloat()
        paints[RenderConstants.LAYER_VISIBLE] = createPaint(lineColor, lineWidth, isWorking)
        paints[RenderConstants.LAYER_GRAYED] = createPaint(
            RenderUtils.getGrayedColor(lineColor), lineWidth, isWorking
        )
        val points = segment.points
        val minX = (Math.min(points[0].x, points[points.size - 1].x) - lineWidth).toInt()
        val maxX = (Math.max(points[0].x, points[points.size - 1].x) + lineWidth).toInt()
        val minY = (Math.min(points[0].y, points[points.size - 1].y) - lineWidth).toInt()
        val maxY = (Math.max(points[0].y, points[points.size - 1].y) + lineWidth).toInt()
        boundingBox = Rect(minX, minY, maxX, maxY)

        path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val p = points[i]
            path.lineTo(p.x, p.y)
            boundingBox.union(p.x.toInt(), p.y.toInt())
        }

        priority =
            if (segment.isWorking) RenderConstants.TYPE_LINE
            else RenderConstants.TYPE_LINE_DASHED
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, paints[layer]!!)
    }

    private fun createPaint(color: Int, lineWidth: Float, isWorking: Boolean): Paint {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.color = color
        if (isWorking) {
            paint.strokeWidth = lineWidth
            paint.pathEffect = CornerPathEffect(lineWidth * 0.2f)
        } else {
            paint.strokeWidth = lineWidth * 0.75f
            paint.pathEffect = ComposePathEffect(
                DashPathEffect(floatArrayOf(lineWidth * 0.8f, lineWidth * 0.2f), 0f),
                CornerPathEffect(lineWidth * 0.2f)
            )
        }
        return paint
    }
}