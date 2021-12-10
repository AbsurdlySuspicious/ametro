package org.ametro.render.elements

import android.graphics.*
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeTransfer
import org.ametro.render.RenderConstants

class TransferElement(scheme: MapScheme, transfer: MapSchemeTransfer) : DrawingElement() {
    private val from: MapPoint
    private val to: MapPoint
    private val radius: Float
    private val paint: Paint

    override val boundingBox: Rect
    override val priority: Int

    init {
        uid = transfer.uid
        from = transfer.fromStationPosition
        to = transfer.toStationPosition
        paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.strokeWidth = scheme.linesWidth.toFloat() + 1.2f
        paint.isAntiAlias = true
        radius = scheme.stationsDiameter.toFloat() / 2 + 2.2f

        boundingBox = Rect(
            (Math.min(from.x, to.x) - radius).toInt(),
            (Math.min(from.y, to.y) - radius).toInt(),
            (Math.max(from.x, to.x) + radius).toInt(),
            (Math.max(from.y, to.y) + radius).toInt()
        )
        priority = RenderConstants.TYPE_TRANSFER
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(from.x, from.y, radius, paint)
        canvas.drawCircle(to.x, to.y, radius, paint)
        canvas.drawLine(from.x, from.y, to.x, to.y, paint)
    }
}