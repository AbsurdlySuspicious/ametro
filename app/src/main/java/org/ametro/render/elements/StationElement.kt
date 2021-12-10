package org.ametro.render.elements

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.render.RenderConstants
import org.ametro.render.utils.RenderUtils

class StationElement(scheme: MapScheme, line: MapSchemeLine, station: MapSchemeStation) : DrawingElement() {
    var position: MapPoint
    var radiusInternal: Float
    var radiusExternal: Float
    var paints = arrayOfNulls<Paint>(RenderConstants.LAYER_COUNT)
    var backgroundPaint: Paint

    override val boundingBox: Rect
    override val priority: Int

    init {
        uid = station.uid
        val radius = scheme.stationsDiameter.toInt() / 2
        position = station.position
        radiusInternal = radius * 0.80f
        radiusExternal = radius * 1.10f
        backgroundPaint = Paint()
        backgroundPaint.color = Color.WHITE
        backgroundPaint.style = Paint.Style.FILL_AND_STROKE
        backgroundPaint.isAntiAlias = true
        backgroundPaint.strokeWidth = 2f
        val lineColor = line.lineColor
        val isWorking = station.isWorking
        paints[RenderConstants.LAYER_VISIBLE] = createPaint(lineColor, radius.toFloat(), isWorking)
        paints[RenderConstants.LAYER_GRAYED] = createPaint(
            RenderUtils.getGrayedColor(lineColor), radius.toFloat(), isWorking
        )

        boundingBox = Rect(
            (position.x - radius).toInt(),
            (position.y - radius).toInt(),
            (position.x + radius).toInt(),
            (position.y + radius).toInt()
        )
        priority = RenderConstants.TYPE_STATION
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(position.x, position.y, radiusExternal, backgroundPaint)
        canvas.drawCircle(position.x, position.y, radiusInternal, paints[layer]!!)
    }

    private fun createPaint(color: Int, radius: Float, isWorking: Boolean): Paint {
        val paint = Paint()
        paint.color = color
        paint.isAntiAlias = true
        paint.strokeWidth = radius * 0.15f * 2
        paint.style = if (isWorking) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
        return paint
    }
}