package org.ametro.render.elements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import org.ametro.model.entities.MapScheme
import org.ametro.render.RenderConstants

class BitmapBackgroundElement(scheme: MapScheme, private val bitmap: Bitmap) : DrawingElement() {
    private val paint: Paint = Paint()
    override val boundingBox: Rect = Rect(0, 0, scheme.width, scheme.height)
    override val priority: Int = RenderConstants.TYPE_BACKGROUND

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
}
