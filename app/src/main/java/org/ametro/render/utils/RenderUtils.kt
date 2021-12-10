package org.ametro.render.utils

import android.graphics.Color

object RenderUtils {
    @JvmStatic
    fun getGrayedColor(color: Int): Int {
        if (color == Color.BLACK) {
            return -0x2f2f30
        }
        var r = Color.red(color).toFloat() / 255
        var g = Color.green(color).toFloat() / 255
        var b = Color.blue(color).toFloat() / 255
        val t = 0.8f
        r = r * (1 - t) + 1.0f * t
        g = g * (1 - t) + 1.0f * t
        b = b * (1 - t) + 1.0f * t
        return Color.argb(
            0xFF,
            Math.min(r * 255, 255f).toInt(),
            Math.min(g * 255, 255f).toInt(),
            Math.min(b * 255, 255f).toInt()
        )
    }
}