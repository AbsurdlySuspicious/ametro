package org.ametro.render.elements

import android.graphics.*
import android.graphics.Paint.Align
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.render.RenderConstants
import org.ametro.render.utils.RenderUtils
import org.ametro.utils.ModelUtils
import java.util.*

class StationNameElement(scheme: MapScheme, line: MapSchemeLine, station: MapSchemeStation) : DrawingElement() {
    private val vertical: Boolean
    private val textPaints = arrayOfNulls<Paint>(RenderConstants.LAYER_COUNT)
    private val borderPaints = arrayOfNulls<Paint>(RenderConstants.LAYER_COUNT)
    private var firstLine: String? = null
    private var firstLinePosition: MapPoint? = null
    private var secondLine: String? = null
    private var secondLinePosition: MapPoint? = null

    override val boundingBox: Rect
    override val priority: Int

    init {
        uid = station.uid
        val name = if (scheme.isUpperCase) station.displayName.uppercase(Locale.getDefault()) else station.displayName
        val textLength = name.length
        val textRect = ModelUtils.toRect(station.labelPosition)
        val point = station.position
        vertical = textRect.width() < textRect.height()
        val align =
            if (vertical) if (point.y > textRect.centerY()) Align.LEFT else Align.RIGHT else if (point.x > textRect.centerX()) Align.RIGHT else Align.LEFT
        val textColor = line.labelColor
        val paint = createTextPaint(textColor)
        textPaints[RenderConstants.LAYER_VISIBLE] = paint
        textPaints[RenderConstants.LAYER_VISIBLE]!!.textAlign = align
        textPaints[RenderConstants.LAYER_GRAYED] = createTextPaint(RenderUtils.getGrayedColor(textColor))
        textPaints[RenderConstants.LAYER_GRAYED]!!.textAlign = align
        var borderColor = line.labelBackgroundColor
        var borderGrayedColor = RenderUtils.getGrayedColor(borderColor)
        if (borderColor == -1) {
            borderColor = Color.WHITE
            borderGrayedColor = Color.WHITE
        }
        borderPaints[RenderConstants.LAYER_VISIBLE] = createBorderPaint(paint, borderColor)
        borderPaints[RenderConstants.LAYER_VISIBLE]!!.textAlign = align
        borderPaints[RenderConstants.LAYER_GRAYED] = createBorderPaint(paint, borderGrayedColor)
        borderPaints[RenderConstants.LAYER_GRAYED]!!.textAlign = align
        splitTextToLines(scheme, name, textLength, textRect, align, paint)

        boundingBox = textRect
        priority = RenderConstants.TYPE_STATION_NAME
    }

    private fun splitTextToLines(
        scheme: MapScheme,
        name: String,
        textLength: Int,
        textRect: Rect,
        align: Align,
        paint: Paint
    ) {
        val rect = if (vertical) {
            if (align == Align.LEFT) {
                Rect(
                    textRect.left,
                    textRect.bottom,
                    textRect.left + textRect.height(),
                    textRect.bottom + textRect.width()
                )
            } else {
                Rect(
                    textRect.left - textRect.height(),
                    textRect.top,
                    textRect.left,
                    textRect.top + textRect.width()
                )
            }
        } else Rect(textRect)

        val bounds = Rect()
        paint.getTextBounds(name, 0, textLength, bounds)
        var isNeedSecondLine = bounds.width() > rect.width() && scheme.isWordWrap
        var spacePosition = -1
        if (isNeedSecondLine) {
            spacePosition = name.indexOf(' ')
            isNeedSecondLine = spacePosition != -1
        }
        if (isNeedSecondLine) {
            val firstText = name.substring(0, spacePosition)
            val secondText = name.substring(spacePosition + 1)
            val secondRect = Rect(
                rect.left, rect.top
                        + bounds.height() + 2, rect.right, rect.bottom
                        + bounds.height() + 2
            )
            firstLine = firstText
            firstLinePosition = initializeLine(firstText, vertical, rect, paint, align)
            secondLine = secondText
            secondLinePosition = initializeLine(secondText, vertical, secondRect, paint, align)
            secondLinePosition =
                MapPoint(secondLinePosition!!.x - firstLinePosition!!.x, secondLinePosition!!.y - firstLinePosition!!.y)
        } else {
            firstLine = name
            firstLinePosition = initializeLine(name, vertical, rect, paint, align)
        }
    }

    private fun createTextPaint(color: Int): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.typeface = Typeface.DEFAULT
        paint.isFakeBoldText = true
        paint.textSize = 10f
        paint.textAlign = Align.LEFT
        paint.color = color
        paint.style = Paint.Style.FILL
        return paint
    }

    private fun createBorderPaint(paint: Paint, color: Int): Paint {
        val borderPaint = Paint(paint)
        borderPaint.color = color
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 2f
        return borderPaint
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(firstLinePosition!!.x, firstLinePosition!!.y)
        if (vertical) {
            canvas.rotate(-90f)
        }
        canvas.drawText(firstLine!!, 0f, 0f, borderPaints[layer]!!)
        canvas.drawText(firstLine!!, 0f, 0f, textPaints[layer]!!)
        if (secondLine != null) {
            canvas.translate(secondLinePosition!!.x, secondLinePosition!!.y)
            canvas.drawText(secondLine!!, 0f, 0f, borderPaints[layer]!!)
            canvas.drawText(secondLine!!, 0f, 0f, textPaints[layer]!!)
        }
        canvas.restore()
    }

    companion object {
        private fun initializeLine(text: String, vertical: Boolean, rect: Rect, paint: Paint, align: Align): MapPoint {
            val position: MapPoint
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            position = if (align == Align.RIGHT) { // align to right
                MapPoint(
                    (rect.right + if (vertical) bounds.height() else 0).toFloat(),
                    (rect.top + if (vertical) 0 else bounds.height()).toFloat()
                )
            } else { // align to left
                MapPoint(
                    (rect.left + if (vertical) bounds.height() else 0).toFloat(),
                    (rect.top + if (vertical) 0 else bounds.height()).toFloat()
                )
            }
            return position
        }
    }
}