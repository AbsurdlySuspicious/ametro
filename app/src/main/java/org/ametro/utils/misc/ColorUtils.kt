package org.ametro.utils.misc

import androidx.annotation.ColorInt

data class ColorUtils(
    val a: Float,
    val r: Float,
    val g: Float,
    val b: Float
) {
    companion object {
        fun fromColorInt(@ColorInt color: Int): ColorUtils {
            return ColorUtils(
                /* alpha */ ((color shr 24) and 0xff) / 255.0f,
                /* red   */ ((color shr 16) and 0xff) / 255.0f,
                /* green */ ((color shr 8) and 0xff) / 255.0f,
                /* blue  */ ((color) and 0xff) / 255.0f
            )
        }
    }

    @ColorInt
    fun toColorInt(): Int {
        return /* */ ((a * 255.0f + 0.5f).toInt() shl 24) or
                /**/ ((r * 255.0f + 0.5f).toInt() shl 16) or
                /**/ ((g * 255.0f + 0.5f).toInt() shl 8) or
                /**/ ((b * 255.0f + 0.5f).toInt())
    }

    fun multiply(other: ColorUtils): ColorUtils {
        return ColorUtils(
            a * other.a,
            r * other.r,
            g * other.g,
            b * other.b
        )
    }

    fun multiply(@ColorInt other: Int): ColorUtils {
        return multiply(fromColorInt(other))
    }
}