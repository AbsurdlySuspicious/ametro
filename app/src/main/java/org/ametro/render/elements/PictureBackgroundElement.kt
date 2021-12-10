package org.ametro.render.elements

import android.graphics.*
import org.ametro.render.elements.DrawingElement
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeSegment
import org.ametro.render.RenderConstants
import org.ametro.render.utils.RenderUtils
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeStation
import org.ametro.model.entities.MapSchemeTransfer
import org.ametro.utils.ModelUtils
import android.graphics.Paint.Align
import org.ametro.render.elements.StationNameElement

class PictureBackgroundElement(scheme: MapScheme, private val picture: Picture) : DrawingElement() {
    override val boundingBox: Rect = Rect(0, 0, scheme.width, scheme.height)
    override val priority: Int = RenderConstants.TYPE_BACKGROUND

    override fun draw(canvas: Canvas) {
        canvas.drawPicture(picture)
    }
}