package org.ametro.render.elements

import android.graphics.*
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeTransfer
import org.ametro.render.RenderConstants
import org.ametro.render.utils.RenderUtils

class TransferBackgroundElement(scheme: MapScheme, transfer: MapSchemeTransfer) : DrawingElement() {
    private val from: MapPoint
    private val to: MapPoint
    private val radius: Float
    private val paints = arrayOfNulls<Paint>(RenderConstants.LAYER_COUNT)

    override val boundingBox: Rect
    override val priority: Int

    init {
        uid = transfer.uid
        from = transfer.fromStationPosition
        to = transfer.toStationPosition
        radius = scheme.stationsDiameter.toFloat() / 2 + 3.5f
        val linesWidth = scheme.linesWidth.toFloat()
        paints[RenderConstants.LAYER_VISIBLE] = createPaint(Color.BLACK, linesWidth)
        paints[RenderConstants.LAYER_GRAYED] = createPaint(
            RenderUtils.getGrayedColor(Color.BLACK), linesWidth
        )

        boundingBox = Rect(
            (Math.min(from.x, to.x) - radius).toInt(),
            (Math.min(from.y, to.y) - radius).toInt(),
            (Math.max(from.x, to.x) + radius).toInt(),
            (Math.max(from.y, to.y) + radius).toInt()
        )
        priority = RenderConstants.TYPE_TRANSFER_BACKGROUND
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(from.x, from.y, radius, paints[layer]!!)
        canvas.drawCircle(to.x, to.y, radius, paints[layer]!!)
        canvas.drawLine(from.x, from.y, to.x, to.y, paints[layer]!!)
    }

    private fun createPaint(color: Int, linesWidth: Float): Paint {
        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.FILL
        paint.strokeWidth = linesWidth + 4.5f
        paint.isAntiAlias = true
        return paint
    }
}