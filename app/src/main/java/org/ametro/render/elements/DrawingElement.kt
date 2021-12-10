package org.ametro.render.elements

import android.graphics.Canvas
import android.graphics.Rect

abstract class DrawingElement : Comparable<DrawingElement> {
    var uid: Int? = null
    var layer = 0
    abstract val boundingBox: Rect
    abstract val priority: Int

    abstract fun draw(canvas: Canvas)

    override fun compareTo(other: DrawingElement): Int {
        val byLayer = layer - other.layer
        if (byLayer != 0) {
            return byLayer
        }
        val byPriority = priority - other.priority
        return if (byPriority != 0) {
            byPriority
        } else 0
    }
}